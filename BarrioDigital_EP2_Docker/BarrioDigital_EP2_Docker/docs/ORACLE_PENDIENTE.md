# Preparación para Oracle

La conexión real queda para la próxima etapa. Por ahora, catálogo, trámites y notificaciones tienen bases H2 persistentes independientes; reiniciar contenedores no borra sus datos.

Cada módulo de datos incluye el driver `ojdbc11` y `application-oracle.properties`. El perfil Oracle exige URL, usuario y contraseña sin volver silenciosamente a H2. `ddl-auto=validate` evita crear o modificar tablas sin preparar una migración.

Antes de activar `compose.oracle.example.yml` hay que completar:

- Tipo de cuenta: Autonomous Database u otra base Oracle, endpoint/alias de servicio y conectividad autorizada desde el host Docker.
- Acceso TLS o mTLS. Si la cuenta exige wallet, preparar el montaje de solo lectura, archivos y dependencias de seguridad Oracle que correspondan; el wallet y su contraseña no están incluidos.
- Usuarios/esquemas para catálogo, solicitudes y notificaciones. Usar cuentas con permisos limitados al esquema propio.
- Migraciones revisadas para `tipos_tramite`, `tramites_pedidos`, `items_pedido`, `tramite_write_lock`, `outbox_messages` y `notification_receipts` según cada servicio; probar CLOB, timestamps y generación de identificadores.
- Exportación/migración de datos H2 si se desea conservar el laboratorio.

La plantilla de Compose muestra las variables necesarias. No la agregues al arranque hasta completar estas tareas. En la próxima etapa se elegirá la URL JDBC y el montaje de wallet según tu cuenta; no necesitas entregar contraseñas dentro del código.
