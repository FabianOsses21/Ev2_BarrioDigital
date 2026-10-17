#!/bin/sh
set -eu
envsubst '${AUTH_MODE} ${PUBLIC_API_URL} ${AZURE_SPA_CLIENT_ID} ${AZURE_TENANT_ID} ${AZURE_BACKEND_SCOPE}' < /opt/runtime-config.template > /usr/share/nginx/html/runtime-config.js
