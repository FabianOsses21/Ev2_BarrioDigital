# Validación y límites de la entrega

## Comprobaciones ejecutadas

| Comprobación | Resultado |
| --- | --- |
| Compilación Java 21 / Spring Boot 4.1.1 de los cinco servicios y módulo común | Correcta |
| Pruebas JWT | 6 aprobadas: token válido, audience, issuer, expiración, scope y aislamiento del modo demo |
| Pruebas de trámites con contexto Spring y H2 | 5 aprobadas: propiedad, permisos, validación, cupos, edición y eliminación persistidas |
| Pruebas del productor outbox | 3 aprobadas: confirmación de publicación, devolución sin ruta y conservación del evento ante caída del broker |
| Pruebas del consumidor | 4 aprobadas: ACK, tres intentos y NACK, JSON inválido y error de transporte del ACK |
| Pruebas de bandeja de notificaciones con H2 | 2 aprobadas: deduplicación y ausencia de éxito ante fallo controlado |
| Pruebas RabbitAdmin | 4 aprobadas: creación, entrada vacía, rol insuficiente y protección de colas funcionales |
| Pruebas de reenvío BFF | 2 aprobadas: Bearer/cuerpo/query/status y rechazo de rutas desconocidas |
| Pruebas Angular/Vitest | 21 aprobadas |
| Compilación Angular para producción | Correcta; advertencia de tamaño inicial de 563 kB frente al umbral informativo de 500 kB. No supera el límite de error de 1 MB |
| Docker Compose principal y override demo | Ambos pasan `config --quiet` con `.env.example` |
| Sintaxis de scripts shell y Python | Correcta |
| Recorrido HTTP real BFF → catálogo/trámites | Correcto: procesos Java separados, H2 persistente, creación/edición/admisión y respuestas 401, 403 y 409 comprobadas |

La comprobación HTTP se ejecutó con autenticación demo, publicación outbox desactivada y un servidor local de prueba en la entrada. No sustituye la ejecución de Nginx y los contenedores Docker. El catálogo también se carga al abrir directamente su ruta, sin depender de visitar primero el dashboard.

Total de pruebas automatizadas aprobadas: **26 de Java y 21 de Angular**. Las tres pruebas del productor se ejecutaron adicionalmente sobre el módulo requests después de la suite inicial.

## Verificación que debe realizarse con Docker

El entorno de preparación no tiene un motor Docker disponible. Por ello **no se ejecutaron el build de imágenes ni el Compose completo**, ni se obtuvo evidencia de un clúster RabbitMQ real en este entorno. Las pruebas de ACK/NACK y administración usan dobles de las conexiones AMQP; las pruebas de dominio sí usan Spring y H2 reales.

En tu equipo, ejecutar las instrucciones de `PRUEBA_EP2.md` y, opcionalmente, `python scripts/smoke.py --mode demo` para validar:

- Descarga y construcción de imágenes, healthchecks y volúmenes.
- Formación del clúster, topología y comunicación AMQP.
- Confirmaciones reales del productor y ACK/NACK del consumidor.
- Mensaje en DLQ por rechazo del consumidor y métricas Management.
- Reconexión al segundo nodo, disponibilidad de colas y recuperación.

## Servicios externos pendientes

No se inició sesión en el tenant de Microsoft, no se modificó AWS y no se conectó Oracle. El JWT se comprobó con claves y un emisor controlados en las pruebas; los IDs públicos de tu configuración se conservaron. Verificar asignaciones de roles y permisos del access token al arrancar con Entra ID.

El consumidor registra notificaciones simuladas. La integración de un proveedor de correo real requiere configurar sus credenciales y un adaptador de envío. Oracle requiere completar conexión, esquemas y migraciones en la siguiente etapa.

Esta entrega contiene código e instrucciones de prueba. Las capturas de las guías originales no se presentan como evidencia de ejecución de este proyecto.
