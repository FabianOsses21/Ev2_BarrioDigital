# Guion de presentación (5–10 min)

Checklist ordenado cubriendo cada punto de la pauta EP4. Tener abiertos
antes de empezar: la consola de AWS con el lab iniciado, la consola de
API Gateway, la consola de EC2, portal.azure.com, dos pestañas de
RabbitMQ Management (`:15672` y `:15673`), una terminal SSH al EC2 y
una ventana de terminal local para los `curl`.

## Pre-vuelo (30 s)

```bash
ssh -i barriodigital.pem ubuntu@100.51.118.15
cd ~/Ev2_BarrioDigital/BarrioDigital_EP2_Docker/BarrioDigital_EP2_Docker
docker compose -f compose.yml -f compose.cloud.yml -f compose.oracle.yml ps
```

Mostrar los ~11 servicios `healthy` — **"docker-compose levantando
servicios en la nube"** ✔ (incluye `oracle-xe` y los 2 nodos RabbitMQ).

## 1. API Manager en la nube (1 min)

Consola AWS → API Gateway → `barriodigital-api` (`bfg5px81d9`):

- Mostrar la API existente y su Invoke URL.
- **Integrations**: `http://100.51.118.15:8080/api/{proxy}` → apunta al
  BFF dentro del EC2. **"El API Manager llama a los endpoints del
  backend"** ✔
- **Routes**: `GET/POST/PUT/PATCH/DELETE /api/{proxy+}` con `entra-jwt`.
- **Authorization → entra-jwt**: issuer Entra v2.0 + audience del app
  registrado. **"El API Manager valida JWT"** ✔

## 2. Validación JWT del API Manager (1 min)

```bash
# Rechaza peticiones inválidas:
curl -i https://bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/catalog
# → HTTP/2 401 {"message":"Unauthorized"}  (lo responde el API GW, no el BFF)
```

Con el access token del paso 4 → `200` con el JSON del catálogo.
**"Rechaza inválidas y acepta correctas"** ✔

## 3. IDaaS: tenant y usuarios (1 min)

portal.azure.com → Entra ID → Overview (muestra el tenant) →
**Enterprise applications** → la app → **Users and groups** → usuarios
asignados con su rol. **"Tenant en IDaaS con usuarios registrados"** ✔

## 4. Frontend + OAuth/OIDC vía API Manager (2 min)

Abrir `https://100.51.118.15.sslip.io` → **Ingresar** → login Microsoft
(OAuth 2.0/OIDC con PKCE vía MSAL) ✔ → vuelve al panel.

DevTools → Network → mostrar una llamada a
`bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/...` con
`Authorization: Bearer …` respondiendo 200.
**"El frontend consume los endpoints a través del API Manager"** ✔

Crear un trámite desde la UI (tipo activo, correo `vecino@example.test`)
→ queda en la lista. **"Backend y frontend desplegados e integrados"** ✔

## 5. RabbitMQ: clúster, colas y exchanges (2 min)

- `http://100.51.118.72:15672` y `:15673` → login `barriodigital`.
- En SSH: `docker compose exec rabbitmq-1 rabbitmqctl cluster_status`
  → `rabbit@rabbitmq-1` + `rabbit@rabbitmq-2` en Running Nodes.
  **"Dos nodos RabbitMQ integrados en un clúster"** ✔
- Management → **Queues**: `q.cmd.email`, `q.cmd.2`, `q.cmd.3` (con
  consumidor) + `*.dlq`. **"Tres colas con sus DLQ"** ✔
- Management → **Exchanges**: `cmd.direct` (direct), `cmd.topic`
  (topic), `cmd.dead.dlx`. **"Exchanges direct y topic"** ✔

## 6. Mensajería en vivo + DLQ (2 min)

- En SSH, dejar corriendo:
  `docker compose logs -f requests notify`
- El trámite del paso 4 ya publicó 3 eventos: mostrar en logs
  `Publicando mensaje`, `Confirmación de publicación ACK`,
  `Mensaje recibido`, `Procesamiento exitoso`, `ACK enviado`.
- Crear otro trámite con correo `error@barriodigital.test`:
  mostrar intentos 1/3, 2/3, 3/3 con backoff y
  `NACK enviado requeue=false destino=q.cmd.email.dlq`.
- Management → `q.cmd.email.dlq` → el contador subió; **Get messages**
  para inspeccionar `x-death`. **"DLQ funcionando"** ✔
- Opcional: `GET /api/notifications` para mostrar los 3 recibos del
  trámite en la bandeja de eventos.

## 7. Admin de RabbitMQ vía REST (1 min, si hay tiempo)

Con el access token de Admin:

```bash
curl -X POST https://bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/rabbitmq/queues \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"queueName":"q.test.demo"}'          # → 201
curl https://bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/rabbitmq/queues \
  -H "Authorization: Bearer $TOKEN"         # aparece q.test.demo
curl -X DELETE .../api/rabbitmq/queues/q.test.demo -H "Authorization: Bearer $TOKEN"  # → 204
curl -X POST .../api/rabbitmq/queues -d '{"queueName":""}' ...  # → 400 validación
```

## Cierre (30 s)

Mencionar: todo en GitHub (backend Java/Spring Boot multi-módulo,
frontend Angular); código con nombres de mensajería centralizados en
`MessagingNames`, consumidores por dominio con reintentos/DLQ y un
microservicio administrador de RabbitMQ con validación.
