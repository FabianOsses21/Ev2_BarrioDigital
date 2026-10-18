package cl.barriodigital.notify;
import cl.barriodigital.common.MessagingNames;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notification_receipts")
public class Receipt {
  @Id @Column(length=150) public String id;
  @Column(length=64) public String eventId;
  @Column(length=64) public String requestId;
  @Column(length=100) public String correlationId;
  @Column(length=80) public String queueName;
  @Column(length=150) public String recipient;
  @Column(length=40) public String result;
  public Instant processedAt;
  protected Receipt(){}
  public Receipt(cl.barriodigital.common.CommandEvent event,String queue){
    id=event.eventId()+"|"+queue;eventId=event.eventId();requestId=event.requestId();
    correlationId=event.correlationId();queueName=queue;recipient=event.email();
    result=MessagingNames.Q_EMAIL.equals(queue)?"EMAIL_SIMULADO":"REGISTRADO";processedAt=Instant.now();
  }
}
