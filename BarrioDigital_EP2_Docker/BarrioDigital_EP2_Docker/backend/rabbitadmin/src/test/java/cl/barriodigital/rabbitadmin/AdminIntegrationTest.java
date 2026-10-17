package cl.barriodigital.rabbitadmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"app.auth.mode=demo","spring.rabbitmq.dynamic=false"}) @AutoConfigureMockMvc
class AdminIntegrationTest {
  @Autowired MockMvc mvc;@MockitoBean AmqpAdmin admin;@MockitoBean ManagementClient management;
  @Test void createsValidatedQueue()throws Exception{mvc.perform(post("/api/rabbitmq/queues").header("X-Demo-Role","Admin").contentType("application/json").content("{\"queueName\":\"q.test.barriodigital\"}")).andExpect(status().isCreated());verify(admin).declareQueue(argThat(q->q.getName().equals("q.test.barriodigital")));}
  @Test void blankNameIs400WithoutBrokerMutation()throws Exception{mvc.perform(post("/api/rabbitmq/queues").header("X-Demo-Role","Admin").contentType("application/json").content("{\"queueName\":\"\"}")).andExpect(status().isBadRequest());verify(admin,never()).declareQueue(any());}
  @Test void vecinoIs403()throws Exception{mvc.perform(post("/api/rabbitmq/queues").header("X-Demo-Role","Vecino").contentType("application/json").content("{\"queueName\":\"q.test.unauthorized\"}")).andExpect(status().isForbidden());verify(admin,never()).declareQueue(any());}
  @Test void functionalQueueCannotBeDeleted()throws Exception{mvc.perform(delete("/api/rabbitmq/queues/q.cmd.email").header("X-Demo-Role","Admin")).andExpect(status().isBadRequest());verify(admin,never()).deleteQueue(anyString(),anyBoolean(),anyBoolean());}
}
