package cl.barriodigital.requests;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="outbox_messages")
public class OutboxMessage {
  @Id @Column(length=64) public String id;
  @Column(nullable=false,length=80) public String exchangeName;
  @Column(nullable=false,length=100) public String routingKey;
  @Column(nullable=false,length=100) public String correlationId;
  @Lob @Column(nullable=false) public String payload;
  public int attempts;
  public Instant createdAt;
  public Instant nextAttempt;
  public Instant sentAt;
  @Column(length=500) public String lastError;
  protected OutboxMessage(){}
  public OutboxMessage(String id,String exchange,String key,String correlation,String payload) {
    this.id=id;exchangeName=exchange;routingKey=key;correlationId=correlation;this.payload=payload;
    createdAt=Instant.now();nextAttempt=createdAt;
  }
}
