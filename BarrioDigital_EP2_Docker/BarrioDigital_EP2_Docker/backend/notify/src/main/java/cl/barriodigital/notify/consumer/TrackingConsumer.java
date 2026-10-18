package cl.barriodigital.notify.consumer;
import cl.barriodigital.common.MessagingNames;
import cl.barriodigital.notify.CommandMessageHandler;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class TrackingConsumer {
  private final CommandMessageHandler handler;
  public TrackingConsumer(CommandMessageHandler handler){this.handler=handler;}
  @RabbitListener(queues=MessagingNames.Q_TRACKING) public void onMessage(Message m,Channel c)throws IOException{handler.consume(m,c);}
}
