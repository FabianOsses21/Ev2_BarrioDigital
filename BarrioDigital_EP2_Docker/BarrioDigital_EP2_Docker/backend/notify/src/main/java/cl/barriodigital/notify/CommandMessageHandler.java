package cl.barriodigital.notify;
import cl.barriodigital.common.CommandEvent;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.*;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component
public class CommandMessageHandler {
  private static final Logger log=LoggerFactory.getLogger(CommandMessageHandler.class);
  private final NotificationProcessor processor;private final ObjectMapper mapper;
  private final int attempts;private final long backoff;
  public CommandMessageHandler(NotificationProcessor processor,ObjectMapper mapper,@Value("${app.consumer.attempts:3}")int attempts,@Value("${app.consumer.backoff-ms:1000}")long backoff){
    this.processor=processor;this.mapper=mapper;this.attempts=attempts;this.backoff=backoff;
  }
  public void consume(Message message,Channel channel)throws IOException{
    var p=message.getMessageProperties();long tag=p.getDeliveryTag();String queue=p.getConsumerQueue();
    log.info("Mensaje recibido queue={} routingKey={} eventId={} correlationId={}",queue,p.getReceivedRoutingKey(),p.getMessageId(),p.getCorrelationId());
    CommandEvent event;
    try{event=mapper.readValue(message.getBody(),CommandEvent.class);}
    catch(Exception e){
      log.warn("Error no recuperable (mensaje mal formado) queue={} eventId={} motivo={}",queue,p.getMessageId(),e.getMessage());
      channel.basicNack(tag,false,false);
      log.warn("NACK enviado requeue=false destino={}.dlq eventId={}",queue,p.getMessageId());
      return;
    }
    for(int attempt=1;attempt<=attempts;attempt++){
      try {
        boolean fresh=processor.process(event,queue);
        // La transacción de la bandeja de entrada ya está confirmada al volver de process().
        log.info("Procesamiento exitoso queue={} eventId={} resultado={}",queue,event.eventId(),fresh?"REGISTRADO":"DUPLICADO_IGNORADO");
      } catch(IllegalArgumentException e){
        log.warn("Error no recuperable (validación) queue={} eventId={} motivo={}",queue,event.eventId(),e.getMessage());
        channel.basicNack(tag,false,false);
        log.warn("NACK enviado requeue=false destino={}.dlq eventId={}",queue,event.eventId());
        return;
      } catch(Exception e){
        log.warn("ERROR procesamiento queue={} eventId={} intento={}/{} motivo={}",queue,p.getMessageId(),attempt,attempts,e.getMessage());
        if(attempt<attempts){
          long delay=backoff*(1L<<(attempt-1));
          log.warn("Retry con backoff={}ms eventId={}",delay,p.getMessageId());
          try{Thread.sleep(delay);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException(interrupted);}
          continue;
        }
        channel.basicNack(tag,false,false);
        log.warn("NACK enviado requeue=false destino={}.dlq eventId={}",queue,p.getMessageId());
        return;
      }
      // Un error de transporte al enviar el ACK no cuenta como error de negocio ni causa otro NACK.
      channel.basicAck(tag,false);
      log.info("ACK enviado queue={} eventId={}",queue,p.getMessageId());
      return;
    }
  }
}
