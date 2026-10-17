package cl.barriodigital.common;
public record CommandEvent(String eventId,String correlationId,String eventType,String occurredAt,
    String requestId,String ownerId,String email,String asunto,String estado) {}
