# Base de datos Oracle en AWS (Oracle XE en el EC2)

Guía para pasar de H2 a Oracle manteniendo el despliegue en el EC2.
Se usa la imagen `gvenzl/oracle-xe:21-slim-faststart` como un servicio
más del compose: misma red Docker, mismos volúmenes persistentes y sin
puertos nuevos expuestos al exterior.

Los microservicios de datos (`catalog`, `requests`, `notify`) ya
incluyen el driver `ojdbc11` y el perfil `oracle`
(`ddl-auto=validate`): solo faltan las contraseñas en `.env`.

## 1. Variables en `.env` del EC2

```bash
ORACLE_ADMIN_PASSWORD=<clave-sys-fuerte>
ORACLE_CATALOG_PASSWORD=<clave-1>
ORACLE_REQUESTS_PASSWORD=<clave-2>
ORACLE_NOTIFY_PASSWORD=<clave-3>
```

`ORACLE_ADMIN_PASSWORD` es la clave de `SYS`/`SYSTEM` de la XE. Las
otras tres son las claves de los usuarios `bd_catalog`, `bd_requests`
y `bd_notify`, que el script de init crea automáticamente.

## 2. Levantar con el perfil Oracle

```bash
cd ~/Ev2_BarrioDigital/BarrioDigital_EP2_Docker/BarrioDigital_EP2_Docker
docker compose -f compose.yml -f compose.cloud.yml -f compose.oracle.yml up -d
docker compose ps -a
```

El primer arranque tarda unos minutos: la imagen faststart desempaqueta
la base pre-creada, luego `01_setup.sh` (en
`/container-entrypoint-initdb.d`) crea los tres usuarios y ejecuta el
DDL de `infra/oracle/schema_*.sql` por esquema. `oracle-xe` debe quedar
`healthy` antes de que arranquen catalog/requests/notify (depends_on).

Seguimiento:

```bash
docker compose logs -f oracle-xe          # "DATABASE IS READY TO USE!"
docker compose logs -f catalog requests   # "Started ...Application" sin
                                          # errores de Schema-validation
```

## 3. Verificación

```bash
docker compose exec oracle-xe sqlplus -s \
  bd_requests/${ORACLE_REQUESTS_PASSWORD}@//localhost:1521/FREEPDB1 \
  <<< "select table_name from user_tables;"
```

Debe listar `TRAMITES_PEDIDOS`, `ITEMS_PEDIDO`, `TRAMITE_WRITE_LOCK`,
`OUTBOX_MESSAGES`. El catálogo se siembra solo al primer arranque
(`tipos_tramite` queda con 3 tipos). Después, repetir las pruebas HTTP
de la guía principal: los trámites y recibos persisten ahora en Oracle.

Para evidencia docente, dentro del mismo sqlplus:

```sql
select table_name from user_tables;
select count(*) from outbox_messages;
```

y en `bd_notify`: `select count(*) from notification_receipts;`
tras crear un trámite.

## 4. Notas operativas

- **RAM:** la XE usa ~2 GB. Junto a los 5 servicios Java (768 MB límite
  c/u) el total puede acercarse al límite de la `t2.large`; vigilar
  `free -m`. Si falta memoria, crear swap de 2 GB:
  `sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile &&
   sudo mkswap /swapfile && sudo swapon /swapfile`.
- **Puertos:** la XE no expone 1521 al exterior; solo la red Docker la
  ve. Para conectar un cliente SQL desde tu PC, túnel SSH:
  `ssh -L 1521:localhost:1521 ubuntu@54.158.23.72` y descomentar
  `ports: ["127.0.0.1:1521:1521"]` en el servicio.
- **Rollback a H2:** omitir `-f compose.oracle.yml` en el `up`; los
  volúmenes H2 se conservan.
- **Oracle externa:** para apuntar a una Autonomous DB u otra Oracle,
  definir `ORACLE_JDBC_URL` en `.env` (override del default interno)
  y ejecutar el DDL de `infra/oracle/schema_*.sql` en cada esquema.
  El servicio `oracle-xe` puede omitirse comentando su bloque.
