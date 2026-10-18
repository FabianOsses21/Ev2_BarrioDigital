# Preparación para Oracle

La conexión real queda para la próxima etapa. Por ahora, catálogo, trámites y notificaciones tienen bases H2 persistentes independientes; reiniciar contenedores no borra sus datos.

La preparación ya está completa en el repositorio:

- Driver `ojdbc11` y `application-oracle.properties` en catalog, requests y notify. El perfil Oracle exige URL, usuario y contraseña sin volver silenciosamente a H2. `ddl-auto=validate` evita crear o modificar tablas sin migración.
- `infra/oracle/schema.sql`: DDL por esquema (`tipos_tramite`, `tramites_pedidos`, `items_pedido`, `tramite_write_lock`, `outbox_messages`, `notification_receipts`) con nombres físicos snake_case que espera Hibernate.
- `compose.oracle.yml`: override listo para activar el perfil en los tres servicios.
- `.env.example`: bloque `ORACLE_*` comentado con las variables requeridas.

**Paso a paso para conectar la base en OCI:** ver [ORACLE_SETUP.md](ORACLE_SETUP.md).

Pendiente únicamente del lado de la cuenta cloud: crear la Autonomous Database, los tres usuarios/esquemas y ejecutar el DDL; luego enchufar las credenciales en `.env` del EC2. La exportación/migración de datos H2 no se contempla: el laboratorio parte con catálogo semilla en Oracle también.
