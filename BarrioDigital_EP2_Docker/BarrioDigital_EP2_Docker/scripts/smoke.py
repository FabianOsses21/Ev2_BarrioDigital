#!/usr/bin/env python3
"""Prueba integral contra Compose iniciado. Python 3.10+, sin dependencias externas."""
import argparse
import json
import os
import time
import urllib.error
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', default='http://localhost:8080')
    parser.add_argument('--mode', choices=['demo', 'entra'], default='demo')
    parser.add_argument('--http-only', action='store_true', help='Solo CRUD/BFF, sin verificar broker; diagnóstico parcial')
    args = parser.parse_args()
    token = os.environ.get('ACCESS_TOKEN', '')
    if args.mode == 'entra' and not token:
        parser.error('Define ACCESS_TOKEN con un access token de administrador; no lo pases en la línea de comandos.')
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    suffix = uuid.uuid4().hex[:10]
    correlation = 'smoke-' + suffix

    def call(method, path, data=None, expected=200, role='Admin', user='smoke-admin', authenticated=True):
        headers = {'Content-Type': 'application/json', 'X-Correlation-ID': correlation}
        if authenticated:
            if args.mode == 'demo':
                headers.update({'X-Demo-Role': role, 'X-Demo-User': user})
            else:
                headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(args.base.rstrip('/') + path,
                                         data=None if data is None else json.dumps(data).encode(),
                                         headers=headers, method=method)
        try:
            response = opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read().decode()
            if response.code != expected:
                raise AssertionError(f'{method} {path}: esperado {expected}, obtenido {response.code}: {body[:500]}')
        return json.loads(body) if body else None

    def until(label, check, timeout=60):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if check():
                print('OK:', label, flush=True)
                return
            time.sleep(1)
        raise AssertionError('Tiempo agotado: ' + label)

    call('GET', '/api/public', authenticated=False)
    call('GET', '/api/requests', expected=401, authenticated=False)
    kind = call('POST', '/api/catalog', {'nombre': 'Prueba integral ' + suffix,
                'descripcion': 'Tipo creado por smoke.py', 'cupoDiario': 3, 'activo': True}, 201)
    payload = {'tipoId': kind['id'], 'asunto': 'Prueba integral ' + suffix,
               'descripcion': 'Trámite generado desde BFF para verificar la integración completa.',
               'clienteEmail': 'vecino@example.test'}
    good = call('POST', '/api/requests', payload, 201)
    edited = call('PUT', '/api/requests/' + good['id'], {'asunto': 'Prueba editada ' + suffix,
                  'descripcion': 'Descripción modificada y persistida desde BFF.'})
    assert call('GET', '/api/requests/' + good['id'])['asunto'] == edited['asunto']
    if args.mode == 'demo':
        call('GET', '/api/requests/' + good['id'], expected=403, role='Vecino', user='otro-vecino')
    changed = call('PUT', '/api/requests/' + good['id'] + '/estado', {'estado': 'ADMITIDO'})
    assert changed['estado'] == 'ADMITIDO'
    call('PUT', '/api/requests/' + good['id'] + '/estado', {'estado': 'RESUELTO'}, 409)
    print('OK: BFF, persistencia, validaciones y permisos', flush=True)
    if args.http_only:
        print('Verificación HTTP parcial terminada. No se comprobó RabbitMQ.')
        return

    if args.mode == 'demo':
        call('POST', '/api/rabbitmq/queues', {'queueName': 'q.test.denied'}, 403, role='Vecino')

    until('tres consumidores procesan el trámite', lambda:
          len({r['queueName'] for r in call('GET', '/api/notifications') if r['requestId'] == good['id']}) == 3)
    queue_name = 'q.cmd.email.dlq'

    def dead_count():
        rows = call('GET', '/api/rabbitmq/queues')
        return next((int(q.get('messages_ready', 0)) for q in rows if q['name'] == queue_name), 0)

    before = dead_count()
    bad = call('POST', '/api/requests', {**payload, 'clienteEmail': 'error@barriodigital.test'}, 201)
    until('error controlado llega a q.cmd.email.dlq', lambda: dead_count() > before)
    rows = call('GET', '/api/notifications')
    assert not any(r['requestId'] == bad['id'] and r['queueName'] == 'q.cmd.email' for r in rows)

    test_queue = 'q.test.barriodigital.' + suffix
    call('POST', '/api/rabbitmq/queues', {'queueName': test_queue}, 201)
    assert any(q['name'] == test_queue for q in call('GET', '/api/rabbitmq/queues'))
    call('POST', '/api/rabbitmq/queues', {'queueName': ''}, 400)
    call('DELETE', '/api/rabbitmq/queues/' + test_queue, expected=204)
    until('outbox sin pendientes', lambda: call('GET', '/api/requests/outbox/status')['pendientes'] == 0)
    print('OK: pruebas integrales completadas. Los trámites y el mensaje DLQ quedan como evidencia.')
    print('correlationId:', correlation)


if __name__ == '__main__':
    main()
