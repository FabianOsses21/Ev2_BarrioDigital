# Entra ID y conexión posterior con AWS

## Configuración de Entra ID

Los identificadores de `.env.example` provienen del proyecto original. Son identificadores públicos; no hay client secret en Angular ni en los contenedores. La SPA usa MSAL y el flujo de autorización con PKCE.

1. En el registro de la SPA, confirma `http://localhost:4200/` como URI de redirección de tipo Single-page application. Debe coincidir con la URL que abres en el navegador.
2. En el registro de la API, confirma `api://54bc913a-2f45-4e55-9eae-b30616a9f8d5/OT.Create` y que la SPA tenga permiso delegado y consentimiento cuando corresponda.
3. La API espera tokens v2 (`requestedAccessTokenVersion: 2`): issuer del tenant terminado en `/v2.0`, audience igual al ID GUID de la API y `scp` con `OT.Create`.
4. Define/asigna los roles de la API a usuarios o grupos en la aplicación empresarial. La API necesita `roles` en su **access token**. Los roles del ID token de la SPA solo controlan la interfaz y no sustituyen los de la API.
5. Si deseas que el frontend muestre las mismas opciones, conserva también sus asignaciones de roles actuales o alinea los roles de ambos registros. Aliases aceptados: `Admin`, `Administrador`, `ROLE_ADMINISTRADOR`, `Funcionario`, `ROLE_OPERADOR`, `Vecino`, `ROLE_CLIENTE`.

Una cuenta sin rol válido obtendrá 403. Token sin scope, con issuer/audience incorrectos o expirado obtiene 401. No se ha iniciado sesión ni modificado tu tenant desde este trabajo.

## Gateway local y AWS

El entorno Docker usa Nginx como puerta de entrada local. Este proxy no es una instancia de AWS API Gateway. El BFF y los servicios conservan su propia validación de tokens.

Para reutilizar el AWS HTTP API que ya tenías:

1. Publica el BFF en la infraestructura elegida; si es privado, configura una integración privada y VPC Link hacia un destino compatible de AWS. No apuntes AWS a `http://bff:8081`, que solo existe dentro de Docker.
2. Configura un JWT authorizer con issuer `https://login.microsoftonline.com/<tenant>/v2.0`, audience `<ID de aplicación de la API>` y scope `OT.Create` en las rutas protegidas.
3. Haz que `/api/{proxy+}` y sus métodos lleguen al BFF preservando el prefijo `/api`, Authorization, cuerpo, query y método. Verifica el mapeo de prefijo de stage para evitar añadir `/prod/api` al servicio.
4. Configura CORS para la URL real del frontend, métodos GET/POST/PUT/PATCH/DELETE/OPTIONS y cabeceras Authorization/Content-Type/X-Correlation-ID. Si AWS responde el preflight, evita respuestas CORS duplicadas en otra capa.
5. Establece `PUBLIC_API_URL` en `.env` con la URL del Gateway sin barra final. Actualiza la URI de redirección SPA al dominio real y usa HTTPS.

El endpoint antiguo de AWS no se usa por defecto, porque aún debe apuntarse al nuevo BFF. No se han creado, modificado ni desplegado recursos AWS.
