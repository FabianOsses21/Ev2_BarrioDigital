package cl.barriodigital.notify;
import cl.barriodigital.common.CommandEvent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"app.auth.mode=demo","spring.rabbitmq.dynamic=false","spring.rabbitmq.listener.simple.auto-startup=false","app.consumer.demo-failure-enabled=true","spring.datasource.url=jdbc:h2:mem:notify_test;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
class InboxIntegrationTest {
  @Autowired NotificationProcessor processor;@Autowired ReceiptRepository repo;
  @BeforeEach void clean(){repo.deleteAll();}
  CommandEvent event(String email){return new CommandEvent("event-1","corr-1","creado","2026-10-17T00:00:00Z","request-1","owner-1",email,"Prueba","INGRESADO");}
  @Test void redeliveryDoesNotRepeatEffect(){assertThat(processor.process(event("ok@example.test"),"q.cmd.email")).isTrue();assertThat(processor.process(event("ok@example.test"),"q.cmd.email")).isFalse();assertThat(repo.count()).isEqualTo(1);}
  @Test void controlledEmailFailureDoesNotRecordSuccess(){assertThatThrownBy(()->processor.process(event("error@barriodigital.test"),"q.cmd.email")).isInstanceOf(IllegalStateException.class);assertThat(repo.count()).isZero();}
}
