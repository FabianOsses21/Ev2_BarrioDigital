# Prueba empírica de BarrioDigital

Estas son instrucciones y resultados esperados, no evidencias de un despliegue ya ejecutado. Completa tus capturas en Docker Desktop/PowerShell, RabbitMQ Management y Postman después de iniciar el proyecto.

## Preparación

1. Inicia todo con el modo elegido del README.
2. Importa `postman/BarrioDigital_EP2.postman_collection.json` y `postman/Local_Demo.postman_environment.json`.
3. Para demo, las cabeceras X-Demo-* del entorno permiten las pruebas locales. Para Entra, coloca un access token de administrador vigente en la variable local `accessToken`; las cabeceras demo son ignoradas. No exportes un entorno con tokens reales.
4. Abre RabbitMQ en 15672 y 15673. Usuario `barriodigital`, contraseña de `.env`.
5. En una terminal deja `docker compose logs -f requests notify`.

## Cluster y topología

```powershell
docker compose ps
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose exec rabbitmq-2 rabbitmqctl cluster_status
```

Debes ver `rabbit@rabbitmq-1` y `rabbit@rabbitmq-2` como Disk Nodes y Running Nodes. En Management revisa `cmd.direct`, `cmd.topic`, `cmd.dead.dlx`, las tres colas y sus tres DLQ. Cada cola principal debe mostrar un consumidor y prefetch 1.

Con volúmenes nuevos el clúster se forma automáticamente. Si reutilizaste volúmenes que tenían nodos separados, inspecciona primero su estado. La operación de la guía 2 es:

```powershell
docker compose exec rabbitmq-2 rabbitmqctl join_cluster rabbit@rabbitmq-1
```

Hazla solo si verificaste que no están unidos y que el nodo 2 no contiene datos que necesites conservar. No se incluye ningún `reset` automático. Después verifica `cluster_status` y repite el arranque.

## 1 Camino exitoso

En Postman ejecuta `01 Catálogo` y luego `02 Crear trámite correcto`:

```http
POST http://localhost:8080/api/requests
Content-Type: application/json
```

```json
{
  "tipoId": "luminarias",
  "asunto": "Luminaria apagada en la plaza",
  "descripcion": "El poste cercano al acceso principal está sin iluminación.",
  "clienteEmail": "vecino@example.test"
}
```

En demo agrega `X-Demo-Role: Admin` y `X-Demo-User: demo-admin`. En Entra agrega Bearer.

Respuesta esperada: HTTP 201 con un ID de trámite. En los siguientes segundos los logs de `requests` deben mostrar `Publicando mensaje`, `exchange=cmd.direct`, `routingKey=email.send`, `eventId` y `correlationId`, seguido de confirmación de publicación ACK. En `notify` busca `Mensaje recibido`, `queue=q.cmd.email`, `Procesamiento exitoso` y `ACK enviado` con el mismo eventId.

Revisa `03 Notificaciones procesadas`: el correo aparece con `result=EMAIL_SIMULADO`. Esto es una simulación académica registrada en BD, no un correo entregado a una casilla real. También aparecen registros de q.cmd.2 y q.cmd.3.

En Management, `messages_ready=0` y `messages_unacknowledged=0` después del procesamiento son coherentes con ACK. Repite con otro asunto para cubrir las dos ejecuciones de la guía 5.

## 2 Error del consumidor y DLQ

Ejecuta `04 Crear trámite con error controlado`. Es el mismo cuerpo válido, pero con:

```json
"clienteEmail": "error@barriodigital.test"
```

No detengas servicios ni cambies el código. Con `DEMO_FAILURE_ENABLED=true`, la operación REST guarda el trámite y el consumidor de correo falla por ese dato.

Espera unos segundos y muestra:

- Recepción en q.cmd.email.
- ERROR, intento 1/3, Retry de 1000 ms; intento 2/3, Retry de 2000 ms; intento 3/3.
- `NACK enviado requeue=false destino=q.cmd.email.dlq`.
- Al menos un mensaje en `q.cmd.email.dlq`. En Management, Get messages con requeue permite inspeccionar `eventId`, `correlationId` y la cabecera `x-death` sin retirar definitivamente la evidencia. Verifica que la razón sea `rejected`, para distinguirla de una expiración por TTL.

Las colas de auditoría y seguimiento pueden procesar el mismo trámite normalmente. El error preparado afecta al canal email.

## 3 RabbitAdmin válido e inválido

`06 Crear cola válida`:

```http
POST http://localhost:8080/api/rabbitmq/queues
```

```json
{"queueName":"q.test.barriodigital"}
```

Resultado: HTTP 201 y cola visible en Management. `07 Rechazar cola inválida` usa `{"queueName":""}` y debe retornar HTTP 400 sin crear recurso. También hay solicitudes para exchange, binding y métricas.

Puedes repetir la creación válida con los mismos argumentos; RabbitMQ declara el mismo recurso de manera idempotente. Si una cola previa tiene otros TTL/argumentos, usa otro nombre o elimina conscientemente la cola de prueba vacía antes de repetir.

## 4 Falla y recuperación de conexión

Esta prueba corresponde a la guía 4 y se ejecuta aparte de la prueba de error de consumidor.

```powershell
docker compose stop rabbitmq-1
docker compose logs --since 2m requests notify
```

Abre Management del nodo 2 (15673). Revisa conexiones AMQP, logs de reconexión y disponibilidad de colas. Como son colas clásicas, una cola alojada en nodo 1 puede quedar no disponible aunque la aplicación se conecte a nodo 2. Los nuevos eventos pueden permanecer en outbox mientras no se confirme su publicación.

Recupera el nodo y comprueba:

```powershell
docker compose start rabbitmq-1
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose logs --since 2m requests notify
```

`missing-queues-fatal=false` deja a los listeners reintentando. Si tu sesión no recupera consumidores, sigue la recuperación explícita de la guía:

```powershell
docker compose restart requests notify rabbitadmin
```

No afirmes replicación ni continuidad total de mensajes a partir de esta prueba. Registra el comportamiento observado y qué nodo alojaba cada cola.

## Prueba automatizada opcional

Con Python 3.10 o posterior:

```powershell
python scripts/smoke.py --mode demo
```

Para Entra, define `ACCESS_TOKEN` en tu sesión sin guardarlo en el proyecto y usa `--mode entra`. Debe ser de un administrador. El script crea un tipo y trámites nuevos para la evidencia; verifica HTTP, procesamiento en las tres colas, aumento de la DLQ y creación/validación/borrado de una cola de prueba. No vacía la DLQ.

## Evidencias a capturar

| Evidencia | Contenido |
| --- | --- |
| E1 | Compose con servicios healthy y cluster-check terminado en 0 |
| E2 | cluster_status con dos Running Nodes desde ambos nodos |
| E3 | Management de 15672 y 15673 con ambos nodos |
| E4 | Tres exchanges, tres colas y tres DLQ |
| E5 | POST de trámite correcto con 201 |
| E6 | Productor, confirmación, consumidor y ACK correlacionados |
| E7 | Segundo trámite correcto y segundo ACK |
| E8 | Entrada con correo de error y logs de tres intentos |
| E9 | DLQ con mensaje y x-death reason=rejected |
| E10 | RabbitAdmin 201 y recurso visible |
| E11 | Nombre vacío con HTTP 400 |
| E12 | Caída de nodo 1, conexión al nodo 2 y recuperación |
