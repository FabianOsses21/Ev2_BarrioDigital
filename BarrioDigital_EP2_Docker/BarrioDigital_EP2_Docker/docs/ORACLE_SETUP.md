# Conexión a Oracle Cloud (Autonomous Database)

Guía para pasar de H2 a Oracle Autonomous Database manteniendo el
mismo despliegue en EC2. Los microservicios de datos (`catalog`,
`requests`, `notify`) ya incluyen el driver `ojdbc11` y el perfil
`oracle` (`ddl-auto=validate`): solo faltan la base, los esquemas y
las credenciales.

## 1. Crear la Autonomous Database (OCI Console)

1. <https://cloud.oracle.com> → **Oracle Database → Autonomous Database → Create**.
2. Compartment: el de tu cuenta. **Display name:** `barriodigital`.
3. Workload: **Transaction Processing**. Deployment: **Serverless**.
4. ECPU: 2 (mínimo free-tier), Storage 20 GB, **Always Free** si aplica.
5. Contraseña de `ADMIN` (se usará solo para crear los usuarios).
6. Network access: **Secure access from allowed IPs and VCNs** →
   agregar la Elastic IP del EC2 (`54.158.23.72/32`).
   Esa opción permite TLS simple **sin wallet** y es la más simple
   para el laboratorio. Si prefieres mTLS, ver §4.
7. Create → esperar a que quede **Available** (~2 min).

## 2. Crear los usuarios/esquemas

En la consola de la ADB → **Database Actions → SQL** (login `ADMIN`):

```sql
CREATE USER bd_catalog  IDENTIFIED BY "<clave-fuerte-1>";
CREATE USER bd_requests IDENTIFIED BY "<clave-fuerte-2>";
CREATE USER bd_notify   IDENTIFIED BY "<clave-fuerte-3>";
GRANT CONNECT, RESOURCE TO bd_catalog;
GRANT CONNECT, RESOURCE TO bd_requests;
GRANT CONNECT, RESOURCE TO bd_notify;
GRANT UNLIMITED TABLESPACE TO bd_catalog;
GRANT UNLIMITED TABLESPACE TO bd_requests;
GRANT UNLIMITED TABLESPACE TO bd_notify;
```

Luego, conectado como **cada usuario**, ejecutar el bloque
correspondiente de `infra/oracle/schema.sql` (catalog, requests o
notify). Las credenciales de Database Actions permiten cambiar de
usuario con la lista desplegable, o conectar por SQLcl/SQL Developer.

## 3. Configurar `.env` en el EC2 y activar el perfil

El `ORACLE_JDBC_URL` sale de la consola: ADB → **Database connection**
→ copiar un connection string TLS del tipo:

```
jdbc:oracle:thin:@dbname_high?TNS_ADMIN=/app/wallet   (mTLS)
jdbc:oracle:thin:@(description= ... )                (TLS one-way, copiar el string completo)
```

Con "allowed IPs" (sin wallet), el JDBC URL es el descriptor largo
TLS que entrega la consola — va entre comillas en `.env`.

`.env`:

```bash
ORACLE_JDBC_URL="jdbc:oracle:thin:@(description=...)"
ORACLE_CATALOG_USER=bd_catalog
ORACLE_CATALOG_PASSWORD=<clave-fuerte-1>
ORACLE_REQUESTS_USER=bd_requests
ORACLE_REQUESTS_PASSWORD=<clave-fuerte-2>
ORACLE_NOTIFY_USER=bd_notify
ORACLE_NOTIFY_PASSWORD=<clave-fuerte-3>
```

Recrear los tres servicios de datos:

```bash
docker compose -f compose.yml -f compose.cloud.yml -f compose.oracle.yml up -d
docker compose logs -f catalog requests notify
```

Señales de éxito: `Started CatalogApplication`/`RequestsApplication`/
`NotifyApplication` sin errores de `Schema-validation`. El catálogo
se siembra solo si `tipos_tramite` está vacía.

Verificación funcional: repetir las pruebas HTTP (crear trámite,
consumo de colas, DLQ) — los datos ahora persisten en Oracle. Revisar
en Database Actions que aparecen filas en `tramites_pedidos`,
`outbox_messages` y `notification_receipts`.

## 4. Variante mTLS (wallet)

Si la ADB exige wallet:

1. ADB → **Database connection → Download wallet** (crea contraseña
   del wallet; es del wallet, no de ADMIN).
2. Subir el ZIP al EC2 y descomprimir, p.ej. `~/wallet_bd/`.
3. En `compose.oracle.yml` descomentar el bloque `volumes` comentado
   por servicio y usar `ORACLE_JDBC_URL=jdbc:oracle:thin:@<alias>_high?TNS_ADMIN=/app/wallet`
   (el alias es uno de los servicios del `tnsnames.ora` del wallet).
4. Abrir el Security Group de la ADB o la regla egress — el wallet
   define el endpoint; con mTLS la IP allowlist no aplica igual.

## 5. Rollback a H2

Basta omitir `-f compose.oracle.yml` en el `up`: los datos H2 en los
volúmenes Docker se conservan y cada servicio vuelve a su archivo.
