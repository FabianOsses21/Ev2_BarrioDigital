-- Esquema NOTIFY (bd_notify).

CREATE TABLE notification_receipts (
    id             VARCHAR2(150 CHAR) NOT NULL,
    event_id       VARCHAR2(64 CHAR),
    request_id     VARCHAR2(64 CHAR),
    correlation_id VARCHAR2(100 CHAR),
    queue_name     VARCHAR2(80 CHAR),
    recipient      VARCHAR2(150 CHAR),
    result         VARCHAR2(40 CHAR),
    processed_at   TIMESTAMP,
    CONSTRAINT pk_notification_receipts PRIMARY KEY (id)
);
