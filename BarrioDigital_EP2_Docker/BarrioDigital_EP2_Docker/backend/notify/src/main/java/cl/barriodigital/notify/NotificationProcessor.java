package cl.barriodigital.notify;
import cl.barriodigital.common.CommandEvent;
import cl.barriodigital.common.MessagingNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationProcessor {
  private final ReceiptRepository repo; private final boolean failEnabled;
  public NotificationProcessor(ReceiptRepository repo,@Value("${app.consumer.demo-failure-enabled:false}")boolean failEnabled){this.repo=repo;this.failEnabled=failEnabled;}
  @Transactional public boolean process(CommandEvent event,String queue) {
    if(event.eventId()==null||event.requestId()==null||event.correlationId()==null||event.eventType()==null)
      throw new IllegalArgumentException("Evento incompleto");
    String key=event.eventId()+"|"+queue;
    if(repo.existsById(key))return false;
    if(MessagingNames.Q_EMAIL.equals(queue)&&failEnabled&&"error@barriodigital.test".equalsIgnoreCase(event.email()))
      throw new IllegalStateException("Error controlado de notificación EP2");
    // Adaptador académico: el correo queda registrado en BD; no se conecta a un SMTP real.
    repo.saveAndFlush(new Receipt(event,queue));
    return true;
  }
}
