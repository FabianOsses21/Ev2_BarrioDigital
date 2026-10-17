package cl.barriodigital.requests;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class OutboxPublisherTest {
  OutboxRepository repo; RabbitTemplate rabbit; OutboxPublisher publisher; OutboxMessage event;

  @BeforeEach void setup() {
    repo=mock(OutboxRepository.class);rabbit=mock(RabbitTemplate.class);
    publisher=new OutboxPublisher(repo,rabbit,mock(PlatformTransactionManager.class));
    event=new OutboxMessage("event-1","cmd.direct","email.send","correlation-1","{}");
    when(repo.findTop20BySentAtIsNullAndNextAttemptLessThanEqualOrderByCreatedAtAsc(any()))
      .thenReturn(List.of(event));
  }

  @Test void marksSentOnlyAfterPublisherConfirm() {
    doAnswer(call->{
      Message message=call.getArgument(2);
      assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
      CorrelationData correlation=call.getArgument(3);
      correlation.getFuture().complete(new CorrelationData.Confirm(true,null));return null;
    }).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
    publisher.dispatch();
    assertThat(event.sentAt).isNotNull();assertThat(event.lastError).isNull();verify(repo).save(event);
  }

  @Test void returnedMessageRemainsPendingEvenWhenBrokerAcks() {
    doAnswer(call->{
      CorrelationData correlation=call.getArgument(3);
      correlation.setReturned(new ReturnedMessage(call.getArgument(2),312,"NO_ROUTE","cmd.direct","email.send"));
      correlation.getFuture().complete(new CorrelationData.Confirm(true,null));return null;
    }).when(rabbit).send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
    publisher.dispatch();
    assertThat(event.sentAt).isNull();assertThat(event.nextAttempt).isAfter(Instant.now());verify(repo).save(event);
  }

  @Test void brokerFailurePreservesEventForRetry() {
    doThrow(new AmqpException("broker unavailable")).when(rabbit)
      .send(anyString(),anyString(),any(Message.class),any(CorrelationData.class));
    publisher.dispatch();
    assertThat(event.sentAt).isNull();assertThat(event.attempts).isEqualTo(1);
    assertThat(event.nextAttempt).isAfter(Instant.now());verify(repo).save(event);
  }
}
