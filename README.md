# BarrioDigital: inicio en modo demo, Microsoft Entra ID y pruebas de RabbitMQ

Guía para Windows y PowerShell. Guarda este documento en la raíz del proyecto, junto a `compose.yml`. Todos los comandos se ejecutan desde esa carpeta, salvo que se indique otra cosa.

## 1. Preparación

1. Extrae el proyecto completo.
2. Abre Docker Desktop y espera a que el motor esté listo.
3. Abre PowerShell en la carpeta que contiene `compose.yml`, `compose.demo.yml`, `backend` y `frontend`. Si el ZIP creó dos carpetas con el mismo nombre, entra en la carpeta interior.
4. Comprueba que Docker responde:

```powershell
docker version
```

Deben aparecer las secciones `Client` y `Server` sin error de conexión.

Crea la configuración local solamente si todavía no existe:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

## 2. Primer inicio en modo demo

```powershell
docker compose -f compose.yml -f compose.demo.yml up -d --build
```

Este comando construye las imágenes e inicia frontend, gateway, BFF, catálogo, trámites, notificaciones, administración de RabbitMQ y los dos nodos RabbitMQ. `-d` los deja ejecutándose en segundo plano.

El modo demo usa cuentas simuladas con roles Vecino, Funcionario y Administrador. Permite probar el sistema sin iniciar sesión con Microsoft.

Comprueba el estado:

```powershell
docker compose ps -a
```

Los servicios deben quedar ejecutándose y saludables cuando tienen comprobación de salud. `cluster-check` debe terminar con `Exited (0)`: es una verificación puntual, por lo que no necesita permanecer ejecutándose.

Abre <http://localhost:4200> y entra con una cuenta de demostración. La construcción puede finalizar antes de que todos los servicios estén listos; espera a que termine el arranque.

### Volver a iniciar en demo otro día

Abre Docker Desktop y ejecuta:

```powershell
docker compose -f compose.yml -f compose.demo.yml up -d
```

Agrega `--build` cuando hayas modificado código y necesites reconstruir las imágenes.

## 3. Abrir RabbitMQ y verificar el clúster

| Recurso | Dirección |
| --- | --- |
| Aplicación | http://localhost:4200 |
| Gateway para pruebas HTTP | http://localhost:8080 |
| RabbitMQ Management, nodo 1 | http://localhost:15672 |
| RabbitMQ Management, nodo 2 | http://localhost:15673 |

Credenciales iniciales de RabbitMQ, si no cambiaste `.env`:

- Usuario: `barriodigital`
- Contraseña: `barriodigital-local`

Estas credenciales son independientes del inicio de sesión de Microsoft. Si modificaste las credenciales en `.env` después de inicializar los volúmenes, cambiar las variables no actualiza automáticamente los usuarios ya guardados en RabbitMQ.

Comprueba los dos nodos:

```powershell
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose exec rabbitmq-2 rabbitmqctl cluster_status
```

Busca `rabbit@rabbitmq-1` y `rabbit@rabbitmq-2` entre los nodos en ejecución. En Management, revisa también la sección de nodos en `Overview`.

En `Exchanges` y `Queues and Streams` deben aparecer:

| Exchange | Clave de enrutamiento | Cola principal | Cola de errores (DLQ) |
| --- | --- | --- | --- |
| `cmd.direct` | `email.send` | `q.cmd.email` | `q.cmd.email.dlq` |
| `cmd.topic` | `tramite.audit.#` | `q.cmd.2` | `q.cmd.2.dlq` |
| `cmd.topic` | `tramite.estado.#` | `q.cmd.3` | `q.cmd.3.dlq` |

Las DLQ reciben mensajes mediante `cmd.dead.dlx`. Cada cola principal debe tener un consumidor cuando `notify` está funcionando. El símbolo `#` de los bindings topic admite varios segmentos.

## 4. Preparar las pruebas HTTP en modo demo

Haz primero estas pruebas mientras el proyecto sigue en demo. En una segunda ventana de PowerShell, desde la misma carpeta, deja abiertos los registros:

```powershell
docker compose logs -f --tail=50 requests notify
```

`Ctrl + C` deja de mostrar los registros; los contenedores continúan ejecutándose.

En la primera ventana prepara las cabeceras:

```powershell
$base = "http://localhost:8080"
$cabeceras = @{
    "X-Demo-Role" = "Admin"
    "X-Demo-User" = "demo-admin"
    "X-Correlation-ID" = "prueba-" + [guid]::NewGuid().ToString()
}
```

Obtén un tipo de trámite activo del catálogo:

```powershell
$tipos = Invoke-RestMethod -Uri "$base/api/catalog" -Headers $cabeceras
$tipos | Format-Table id, nombre, activo
$tipo = $tipos | Where-Object { $_.activo -eq $true } | Select-Object -First 1
if ($null -eq $tipo) { throw "No hay tipos activos en el catálogo." }
```

## 5. Prueba de mensaje correcto y ACK

Crea un trámite válido:

```powershell
$datos = @{
    tipoId = $tipo.id
    asunto = "Prueba RabbitMQ " + (Get-Date -Format "HHmmss")
    descripcion = "Solicitud de prueba para comprobar publicacion y consumo de mensajes."
    clienteEmail = "vecino@example.test"
}
$cuerpo = $datos | ConvertTo-Json

$tramite = Invoke-RestMethod `
    -Uri "$base/api/requests" `
    -Method Post `
    -Headers $cabeceras `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($cuerpo))

$tramite
```

Resultado esperado: se crea el trámite y se devuelve su `id`. El endpoint responde HTTP 201. Cada creación genera tres eventos: correo, auditoría y seguimiento.

Espera unos segundos y observa los registros:

- En `requests`: `Publicando mensaje` y `Confirmación de publicación ACK`.
- En `notify`: `Mensaje recibido`, `Procesamiento exitoso` y `ACK enviado`.
- Sigue cada mensaje por su `eventId` y agrupa la operación por `correlationId`.

La confirmación del productor acredita la publicación aceptada por el broker; el ACK del consumidor confirma el procesamiento de ese mensaje. Son comprobaciones distintas.

Consulta los registros procesados:

```powershell
$notificaciones = Invoke-RestMethod -Uri "$base/api/notifications" -Headers $cabeceras
$notificaciones | Where-Object { $_.requestId -eq $tramite.id } | Format-List
```

Deben aparecer registros para `q.cmd.email`, `q.cmd.2` y `q.cmd.3`. El correo se registra como `EMAIL_SIMULADO`: no se envía un correo real.

En RabbitMQ Management revisa las tres colas. Después del consumo es normal ver `Ready = 0` y `Unacked = 0`: los consumidores pueden procesar tan rápido que no alcances a ver mensajes pendientes. Los logs y los registros de notificaciones permiten comprobar el recorrido.

Repite la creación con otro asunto si necesitas evidenciar dos ejecuciones exitosas.

## 6. Prueba de error, reintentos y DLQ

La configuración de laboratorio debe tener `DEMO_FAILURE_ENABLED=true` en `.env`. El dato que provoca el error controlado es el correo `error@barriodigital.test`. El nombre de esta variable es independiente de `AUTH_MODE`: también puede estar activo cuando usas Entra ID.

Antes de ejecutar, anota cuántos mensajes tiene `q.cmd.email.dlq` en Management. Luego:

```powershell
$cabeceras["X-Correlation-ID"] = "error-" + [guid]::NewGuid().ToString()
$datos.asunto = "Prueba DLQ " + (Get-Date -Format "HHmmss")
$datos.clienteEmail = "error@barriodigital.test"
$cuerpo = $datos | ConvertTo-Json

$tramiteError = Invoke-RestMethod `
    -Uri "$base/api/requests" `
    -Method Post `
    -Headers $cabeceras `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($cuerpo))

$tramiteError
```

El trámite se crea correctamente. El error ocurre después, al consumir el evento de correo.

En los siguientes segundos deben aparecer:

1. Recepción del mensaje en `q.cmd.email`.
2. Error en el intento 1/3 y espera de 1000 ms.
3. Error en el intento 2/3 y espera de 2000 ms.
4. Error en el intento 3/3.
5. `NACK enviado requeue=false destino=q.cmd.email.dlq`.

En Management abre `q.cmd.email.dlq`: su contador debe aumentar respecto del valor inicial. Usa `Get messages` con la opción que vuelve a poner el mensaje en la cola (`requeue=true`) si quieres inspeccionarlo conservándolo como evidencia.

Busca `eventId`, `correlationId` y el encabezado `x-death`, con motivo `rejected`. El motivo `expired` indica vencimiento por TTL y corresponde a otra situación. Las colas principales tienen TTL inicial de 30 segundos; las DLQ, siete días y un máximo de 10.000 mensajes.

Los eventos de auditoría y seguimiento del trámite pueden procesarse normalmente: el error preparado afecta al evento de correo.

## 7. Prueba automatizada opcional

Si tienes Python 3.10 o posterior y el proyecto está en demo:

```powershell
python scripts/smoke.py --mode demo
```

El script verifica API, permisos, persistencia, procesamiento de las tres colas, aumento de la DLQ y creación/validación/eliminación de una cola temporal. Crea tipos y trámites de prueba; deja los trámites y el mensaje de error como evidencia. Requiere que el error controlado esté habilitado.

También puedes importar la colección y el entorno incluidos en `postman/` para ejecutar las pruebas manualmente.

## 8. Cambiar de demo a Microsoft Entra ID

Con las imágenes ya construidas, ejecuta:

```powershell
docker compose up -d
```

Al omitir `compose.demo.yml`, se aplica el modo Entra configurado en `compose.yml`. No necesitas ejecutar demo antes de usar Microsoft; aquí se presenta así para probar primero la integración local.

Si también modificaste código y quieres incorporar los cambios:

```powershell
docker compose up -d --build
```

Abre <http://localhost:4200>, actualiza con `Ctrl + F5` y pulsa **Ingresar**. Inicia sesión con Microsoft y comprueba el acceso con tu rol. En este proyecto ya se confirmó el inicio de sesión con una cuenta real y el acceso al panel con su rol.

Los trámites y datos guardados en los volúmenes se conservan al cambiar de modo. La identidad demo y la identidad Microsoft son diferentes; un Vecino autenticado puede ver una lista distinta porque solo accede a sus propios trámites.

### Repetir las pruebas HTTP con Entra ID

Las cabeceras `X-Demo-*` no autentican solicitudes en modo Entra. Para ejecutar las pruebas administrativas necesitas un **access token vigente de la API**, con los permisos y rol de administrador correspondientes. El ID token de la sesión no sustituye al access token.

Después de iniciar sesión, puedes identificar el access token en las herramientas de desarrollo del navegador, pestaña Network, dentro del encabezado `Authorization: Bearer ...` de una solicitud autenticada a `/api/`. No compartas ese valor ni lo guardes en el repositorio.

En PowerShell ingresa únicamente el token, sin la palabra `Bearer`, mediante una entrada oculta:

```powershell
$tokenSeguro = Read-Host "Pega el access token de administrador, sin Bearer" -AsSecureString
$credencialToken = [System.Net.NetworkCredential]::new("", $tokenSeguro)
$tokenApi = $credencialToken.Password
$base = "http://localhost:8080"
$cabeceras = @{
    "Authorization" = "Bearer $tokenApi"
    "X-Correlation-ID" = "entra-" + [guid]::NewGuid().ToString()
}
```

Vuelve a ejecutar la consulta de catálogo de la sección 4 y las pruebas 5 y 6 con estas cabeceras. Un HTTP 401 requiere revisar token, vigencia y configuración; un HTTP 403 requiere revisar los permisos del access token para la operación.

Para ejecutar el script automatizado con ese token:

```powershell
$env:ACCESS_TOKEN = $tokenApi
python scripts/smoke.py --mode entra
Remove-Item Env:ACCESS_TOKEN
Remove-Variable tokenApi, tokenSeguro, credencialToken, cabeceras
```

## 9. Prueba opcional de caída y recuperación de un nodo

Ejecuta esta prueba después de las anteriores:

```powershell
docker compose stop rabbitmq-1
docker compose logs --since 2m requests notify
```

Abre <http://localhost:15673> y observa las conexiones y la disponibilidad de colas. El proyecto usa colas clásicas: tener dos nodos no implica que cada cola esté replicada. Una cola alojada en el nodo detenido puede quedar temporalmente no disponible. Los eventos cuya publicación no se confirma permanecen pendientes en el outbox para reintentar.

Recupera el nodo:

```powershell
docker compose start rabbitmq-1
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose logs --since 2m requests notify
```

Espera a que ambos nodos estén en ejecución. Si los consumidores no se recuperan:

```powershell
docker compose restart requests notify rabbitadmin
```

Registra el comportamiento observado; esta prueba no acredita replicación ni disponibilidad continua de las colas.

## 10. Detener, iniciar y diagnosticar

Detener sin eliminar los contenedores:

```powershell
docker compose stop
```

Volver a iniciar con Entra ID:

```powershell
docker compose up -d
```

Volver al modo demo:

```powershell
docker compose -f compose.yml -f compose.demo.yml up -d
```

Ver estado y registros:

```powershell
docker compose ps -a
docker compose logs --tail=100 rabbitadmin
docker compose logs --tail=100 requests notify
```

Si aparece `dockerDesktopLinuxEngine` no disponible, abre Docker Desktop y espera a que arranque. Si aparece `unhealthy`, consulta los registros del servicio indicado. Si aparece `COMPILATION ERROR`, corrige el archivo y la línea señalados antes de volver a usar `--build`.

No uses `docker compose down -v` para un reinicio normal: elimina los volúmenes y sus datos. Esta guía corresponde al despliegue local; EC2 requiere adaptar red, acceso público, HTTPS y redirección de Entra ID.

### Recuperar `timeout_waiting_for_leader` al iniciar RabbitMQ

Si el nodo 1 muestra `rabbit_khepri` y `timeout_waiting_for_leader`, y el nodo 2 está en `Created`, revisa `compose.yml`. Dentro de `rabbitmq-2`, la dependencia del nodo 1 debe permitir iniciar el segundo contenedor antes de que el primero esté saludable:

```yaml
    depends_on:
      rabbitmq-1: {condition: service_started}
```

Esta corrección evita un bloqueo de arranque: Khepri necesita ambos miembros para recuperar el quorum de este clúster de dos nodos. `cluster-check` sigue esperando que ambos nodos estén saludables antes de verificar el clúster. No cambies esas dependencias.

Para recuperar los contenedores ya creados sin eliminar sus volúmenes:

```powershell
docker compose up -d --no-deps rabbitmq-1 rabbitmq-2
```

Espera a que ambos nodos estén saludables. Comprueba el estado y, después, inicia el resto:

```powershell
docker compose ps -a
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose up -d
```

Si continúan fallando, revisa `docker compose logs --tail=100 rabbitmq-1 rabbitmq-2`. No uses `reset`, `force_boot` ni elimines volúmenes como parte de esta recuperación.

Referencia: [Clustering and Khepri, RabbitMQ 4.2](https://www.rabbitmq.com/docs/4.2/metadata-store/clustering).

## 11. Evidencias sugeridas

Prueba	Asunto	Descripción	Correo
Procesamiento correcto	Luminaria apagada en la plaza	El poste junto al acceso principal no enciende durante la noche.	vecino@example.test
Error y reintentos	Luminaria con falla intermitente	La luminaria se apaga repetidamente y deja el sector sin iluminación.	error@barriodigital.test
Procesamiento correcto después del error	Luminaria dañada frente a la sede	La luminaria frente a la sede vecinal permanece apagada.	otrovecino@example.test


Selecciona un tipo de trámite activo que corresponda a los ejemplos.
Antes de comenzar, abre los registros:
docker compose logs -f --tail=10 requests notify
