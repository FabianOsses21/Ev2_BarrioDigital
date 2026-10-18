package cl.barriodigital.notify;
import cl.barriodigital.common.CommandEvent;
import cl.barriodigital.common.MessagingNames;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CommandMessageHandlerTest {
  NotificationProcessor processor;Channel channel;CommandMessageHandler consumer;Message message;
  @BeforeEach void setup(){
    processor=mock(NotificationProcessor.class);channel=mock(Channel.class);var mapper=JsonMapper.builder().build();
    consumer=new CommandMessageHandler(processor,mapper,3,0);
    var props=new MessageProperties();props.setConsumerQueue(MessagingNames.Q_EMAIL);props.setDeliveryTag(42);props.setMessageId("event-1");
    message=new Message(mapper.writeValueAsBytes(new CommandEvent("event-1","corr-1","creado","2026-10-17T00:00:00Z","request-1","user-1","ok@example.test","Prueba","INGRESADO")),props);
  }
  @Test void successfulProcessingIsAckedOnce()throws Exception{when(processor.process(any(),anyString())).thenReturn(true);consumer.consume(message,channel);verify(channel).basicAck(42,false);verify(channel,never()).basicNack(anyLong(),anyBoolean(),anyBoolean());}
  @Test void controlledFailureRetriesThreeTimesThenNacksWithoutRequeue()throws Exception{
    when(processor.process(any(),anyString())).thenThrow(new IllegalStateException("controlado"));consumer.consume(message,channel);
    verify(processor,times(3)).process(any(),eq(MessagingNames.Q_EMAIL));verify(channel).basicNack(42,false,false);verify(channel,never()).basicAck(anyLong(),anyBoolean());
  }
  @Test void malformedJsonReachesDlqInsteadOfLooping()throws Exception{consumer.consume(new Message("{invalid".getBytes(),message.getMessageProperties()),channel);verifyNoInteractions(processor);verify(channel).basicNack(42,false,false);}
  @Test void transportErrorDuringAckDoesNotRepeatBusinessProcessing()throws Exception{
    doThrow(new IOException("connection lost")).when(channel).basicAck(42,false);
    assertThatThrownBy(()->consumer.consume(message,channel)).isInstanceOf(IOException.class);
    verify(processor,times(1)).process(any(),anyString());verify(channel,never()).basicNack(anyLong(),anyBoolean(),anyBoolean());
  }
}
