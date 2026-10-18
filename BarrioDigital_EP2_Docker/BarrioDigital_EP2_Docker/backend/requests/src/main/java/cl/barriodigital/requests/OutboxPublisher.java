package cl.barriodigital.requests;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration @EnableScheduling
@ConditionalOnProperty(name="app.outbox.enabled",havingValue="true",matchIfMissing=true)
public class OutboxPublisher {
  private static final Logger log=LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxRepository repo; private final RabbitTemplate rabbit; private final TransactionTemplate tx;
  public OutboxPublisher(OutboxRepository repo,RabbitTemplate rabbit,PlatformTransactionManager manager) {
    this.repo=repo;this.rabbit=rabbit;tx=new TransactionTemplate(manager);
  }
  @Scheduled(fixedDelayString="${app.outbox.delay-ms:2000}")
  public void dispatch() {
    // El retraso de reintentos se evalúa en Java con LocalDateTime: TIMESTAMP
    // guarda y lee wall-clock de forma simétrica, sin desfase de zona horaria.
    LocalDateTime now=LocalDateTime.now();
    for(OutboxMessage event:repo.findTop20BySentAtIsNullOrderByCreatedAtAsc()) {
      if(event.nextAttempt!=null&&event.nextAttempt.isAfter(now))continue;
      event.attempts++;
      try {
        MessageProperties p=new MessageProperties(); p.setContentType("application/json");
        p.setContentEncoding("UTF-8");p.setMessageId(event.id);p.setCorrelationId(event.correlationId);p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        var correlation=new CorrelationData(event.id);
        log.info("Publicando mensaje exchange={} routingKey={} eventId={} correlationId={}",event.exchangeName,event.routingKey,event.id,event.correlationId);
        rabbit.send(event.exchangeName,event.routingKey,new Message(event.payload.getBytes(StandardCharsets.UTF_8),p),correlation);
        var confirm=correlation.getFuture().get(5,TimeUnit.SECONDS);
        if(!confirm.isAck()||correlation.getReturned()!=null)throw new IllegalStateException("Publicación no confirmada o sin ruta");
        event.sentAt=LocalDateTime.now();event.lastError=null;
        log.info("Confirmación de publicación ACK eventId={}",event.id);
      } catch(Exception e) {
        if(e instanceof InterruptedException)Thread.currentThread().interrupt();
        event.lastError=e.getClass().getSimpleName();
        event.nextAttempt=LocalDateTime.now().plusSeconds(Math.min(60,1L<<Math.min(event.attempts,6)));
        log.warn("Publicación pendiente eventId={} intento={} próximo={} motivo={}",event.id,event.attempts,event.nextAttempt,event.lastError);
      }
      repo.save(event);
    }
  }
  @Scheduled(fixedDelay=3600000)
  public void retention(){tx.executeWithoutResult(s->repo.deleteBySentAtBefore(LocalDateTime.now().minusSeconds(604800)));}
}
