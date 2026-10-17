# API de BarrioDigital

Base local: `http://localhost:8080`. El frontend llama a `/api` desde su propio origen, y Nginx la reenvía al gateway. En modo Entra usar `Authorization: Bearer <access token>`. En demo usar `X-Demo-Role: Admin|Funcionario|Vecino` y `X-Demo-User: demo-vecino`.

| Método y ruta | Descripción | Rol |
| --- | --- | --- |
| GET `/api/public` | Estado del BFF | Público |
| GET `/api/catalog?soloActivos=false` | Catálogo completo | Cualquier rol válido |
| GET `/api/catalog/{id}` | Un tipo | Cualquier rol válido |
| POST `/api/catalog` | Crear o actualizar si incluye `id` | Administrador |
| DELETE `/api/catalog/{id}` | Desactivar conservando historial | Administrador |
| GET `/api/requests` | Propios para vecino; todos para personal | Cualquier rol válido |
| GET `/api/requests/{id}` | Detalle con control de propietario | Propietario o personal |
| POST `/api/requests` | Crear trámite y tres eventos outbox | Cualquier rol válido |
| PUT `/api/requests/{id}` | Editar asunto/descripción en INGRESADO | Propietario o personal |
| PUT `/api/requests/{id}/estado` | Cambiar estado y generar eventos | Funcionario o administrador |
| DELETE `/api/requests/{id}` | Eliminar en INGRESADO | Propietario o personal |
| GET `/api/requests/outbox/status` | Número de eventos pendientes | Administrador |
| GET `/api/notifications` | Últimos 100 registros procesados | Administrador |
| GET `/api/rabbitmq/queues` | Estado/métricas de colas | Administrador |
| GET `/api/rabbitmq/consumers` | Consumidores | Administrador |
| GET `/api/rabbitmq/nodes` | Nodos | Administrador |
| GET `/api/rabbitmq/overview` | Resumen de Management | Administrador |
| POST `/api/rabbitmq/queues` | Declarar cola de prueba | Administrador |
| DELETE `/api/rabbitmq/queues/{name}` | Borrar cola de prueba vacía y sin consumidores | Administrador |
| POST `/api/rabbitmq/exchanges` | Declarar exchange de prueba | Administrador |
| DELETE `/api/rabbitmq/exchanges/{name}` | Borrar exchange de prueba | Administrador |
| POST/DELETE `/api/rabbitmq/bindings` | Crear o quitar binding | Administrador |
| POST `/api/rabbitmq/queues/{name}/purge?confirm=true` | Vaciar cola de prueba | Administrador |

RabbitAdmin permite mutaciones en `q.test.*` y `x.test.*`. Las colas funcionales se gestionan mediante la topología del código, para evitar que una demostración borre el flujo de negocio. Los endpoints GET de métricas usan Management con fallback entre los nodos.

Crear trámite:

```json
{"tipoId":"luminarias","asunto":"Foco apagado en la plaza","descripcion":"El poste de la entrada de la plaza está sin iluminación.","clienteEmail":"vecino@example.test"}
```

Crear/actualizar tipo:

```json
{"nombre":"Reparación de veredas","descripcion":"Reporte de veredas dañadas.","cupoDiario":6,"activo":true}
```

Cambiar estado:

```json
{"estado":"ADMITIDO"}
```

Transiciones: INGRESADO → ADMITIDO → EN_GESTION → EN_TERRENO → RESUELTO. También se puede pasar a RECHAZADO desde cualquiera de los cuatro estados no finales. Admitir consume un cupo del tipo para el día; un trámite rechazado después de ser admitido no devuelve automáticamente ese cupo.

RabbitAdmin:

```json
{"queueName":"q.test.barriodigital","ttlMs":30000,"maxLength":10000}
```

```json
{"exchangeName":"x.test.barriodigital","type":"direct"}
```

```json
{"queueName":"q.test.barriodigital","exchangeName":"x.test.barriodigital","routingKey":"test.send"}
```

Errores de validación: 400. Token inválido/ausente: 401. Rol o propietario insuficiente: 403. Registro inexistente: 404. Transición/cupo inválido: 409. Dependencia temporalmente no disponible: 503. Un conflicto de declaración del broker (por ejemplo, cola existente con distintos argumentos) se debe resolver revisando el recurso, no repitiendo indefinidamente la petición.
