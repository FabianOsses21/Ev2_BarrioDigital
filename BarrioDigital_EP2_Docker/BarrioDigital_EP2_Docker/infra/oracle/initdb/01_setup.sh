#!/bin/bash
# Inicialización de BarrioDigital: crea los tres esquemas y ejecuta su DDL.
# Corre una sola vez, en el primer arranque del volumen de datos.
set -euo pipefail

PDB="FREEPDB1"
SYS="sys/${ORACLE_PASSWORD}@//localhost:1521/${PDB} as sysdba"

sqlplus -s "$SYS" <<SQL
CREATE USER bd_catalog  IDENTIFIED BY "${ORACLE_CATALOG_PASSWORD}"  DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
CREATE USER bd_requests IDENTIFIED BY "${ORACLE_REQUESTS_PASSWORD}" DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
CREATE USER bd_notify   IDENTIFIED BY "${ORACLE_NOTIFY_PASSWORD}"   DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS;
GRANT CONNECT, RESOURCE TO bd_catalog;
GRANT CONNECT, RESOURCE TO bd_requests;
GRANT CONNECT, RESOURCE TO bd_notify;
EXIT;
SQL

sqlplus -s "bd_catalog/${ORACLE_CATALOG_PASSWORD}@//localhost:1521/${PDB}"   @/opt/oracle/schema/schema_catalog.sql
sqlplus -s "bd_requests/${ORACLE_REQUESTS_PASSWORD}@//localhost:1521/${PDB}" @/opt/oracle/schema/schema_requests.sql
sqlplus -s "bd_notify/${ORACLE_NOTIFY_PASSWORD}@//localhost:1521/${PDB}"     @/opt/oracle/schema/schema_notify.sql

echo "BarrioDigital: esquemas y tablas Oracle listos en ${PDB}."
