# Despliegue en AWS

Arquitectura del laboratorio desplegado. Todo corre dentro de una sola
EC2 con docker-compose; AWS API Gateway actúa como API Manager.

```
Navegador ── https://<ip>.sslip.io ──► Caddy (EC2:443) ──► frontend (127.0.0.1:4200)
    │
    └─► https://bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/*
            │
            ├─ JWT authorizer (Entra v2.0) → 401 sin token válido
            └─► http://<EC2>:8080 → nginx gateway → BFF → microservicios
                                                    ├─ catalog/requests/notify → Oracle XE (mismo EC2)
                                                    └─ RabbitMQ nodo1+nodo2 (cluster, Management :15672/:15673)
```

## Recursos desplegados

| Recurso | Valor |
|---|---|
| EC2 | `i-0d4181eab80bddd1c` (Ubuntu 24.04, t2.large, 30 GB) |
| Elastic IP | `100.51.118.15` (`eipalloc-00a9b9deb4b1ed71b`) — sobrevive stops del lab |
| Dominio frontend | `https://100.51.118.15.sslip.io` (Caddy + Let's Encrypt) |
| API Gateway | `bfg5px81d9` (HTTP API, stage `$default`) |
| Integraciones | `http://<EIP>:8080/api/{proxy}` y `…/api` |
| JWT authorizer | issuer `https://login.microsoftonline.com/f402d005-2163-4d89-a6cd-e07c25b973b2/v2.0`, audience `54bc913a-2f45-4e55-9eae-b30616a9f8d5` |
| Rutas | `GET/POST/PUT/PATCH/DELETE /api/{proxy+}` y `/api` con authorizer; OPTIONS lo responde el CORS del API GW (204) |
| CORS API GW | origin `https://100.51.118.15.sslip.io` |
| Security Group | `sg-0b090bd4860eab531`: 22, 80, 443, 8080, 15672, 15673 → `0.0.0.0/0` |
| RabbitMQ | `http://100.51.118.15:15672` / `:15673` (usuario `barriodigital`) |

## Decisiones relevantes

- **Rutas por método, no `ANY`:** `ANY` incluye OPTIONS y el
  authorizer JWT bloqueaba el preflight (401 al OPTIONS). Con rutas
  explícitas el API Gateway responde el preflight automáticamente.
- **URI de integración con `/api/{proxy}`:** API Gateway no agrega el
  prefijo de la ruta al destino; el `{proxy}` reconstruye el path que
  espera el nginx (`/api/...`).
- **sslip.io en vez de dominio propio:** Entra ID exige HTTPS en
  redirect URIs no-localhost y Let's Encrypt no emite para IPs;
  `<ip>.sslip.io` resuelve gratis y acepta certificado real.
- **Elastic IP:** la IP dinámica de EC2 cambia al detener el lab de
  AWS Academy; la EIP fija el destino del API Gateway y del DNS.
- **Oracle XE en el mismo EC2** (servicio `oracle-xe` del compose):
  misma red Docker, sin puertos expuestos, init automático de esquemas.

## Recovery tras "End Lab" de AWS Academy

La sesión del lab detiene la instancia pero conserva EBS, volúmenes
Docker y la asociación de la EIP. Para volver:

1. **Start Lab** → EC2 → **Start instance** `i-0d4181eab80bddd1c`.
2. La IP pública vuelve a ser `100.51.118.15` (EIP). Nada que reconfigurar.
3. Los contenedores arrancan solos (`restart: unless-stopped`). Esperar
   a que `oracle-xe` y los demás queden healthy:
   `docker compose -f compose.yml -f compose.cloud.yml -f compose.oracle.yml ps`.
4. Verificación rápida:
   - `curl https://100.51.118.15.sslip.io` → 200
   - `curl https://bfg5px81d9.execute-api.us-east-1.amazonaws.com/api/public` → 401
   - `curl http://100.51.118.15:15672` → 200

Solo si la EIP desapareciera (no debería): repetir `allocate-address` +
`associate-address`, actualizar el Caddyfile, `CORS_ORIGINS` en `.env`,
las URIs de integración del API GW, el CORS del API GW y la redirect
URI de la SPA en Entra.

## Prueba integral contra AWS

Con un access token de administrador (obtenido tras login en el
frontend, pestaña Network → header `Authorization` de una llamada
`/api/*`):

```bash
export ACCESS_TOKEN=<token-sin-Bearer>
python scripts/smoke.py --base https://bfg5px81d9.execute-api.us-east-1.amazonaws.com --mode entra
```
