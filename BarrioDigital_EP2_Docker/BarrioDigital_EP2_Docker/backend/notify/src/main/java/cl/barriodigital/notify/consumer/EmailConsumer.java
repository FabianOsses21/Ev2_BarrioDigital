package cl.barriodigital.notify.consumer;
import cl.barriodigital.common.MessagingNames;
import cl.barriodigital.notify.CommandMessageHandler;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class EmailConsumer {
  private final CommandMessageHandler handler;
  public EmailConsumer(CommandMessageHandler handler){this.handler=handler;}
  @RabbitListener(queues=MessagingNames.Q_EMAIL) public void onMessage(Message m,Channel c)throws IOException{handler.consume(m,c);}
}
