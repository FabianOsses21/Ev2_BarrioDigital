package cl.barriodigital.common;
import java.util.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class MessagingConfig {
  @Bean Declarables topology(@Value("${app.queue.ttl-ms:30000}") int ttl,@Value("${app.dlq.ttl-ms:604800000}") int dlqTtl) {
    DirectExchange direct=new DirectExchange("cmd.direct",true,false);
    TopicExchange topic=new TopicExchange("cmd.topic",true,false);
    DirectExchange dlx=new DirectExchange("cmd.dead.dlx",true,false);
    List<Declarable> all=new ArrayList<>(List.of(direct,topic,dlx));
    String[] names={"q.cmd.email","q.cmd.2","q.cmd.3"};
    String[] keys={"email.send","tramite.audit.#","tramite.estado.#"};
    String[] dead={"email.dead","cmd.2.dead","cmd.3.dead"};
    for(int i=0;i<names.length;i++) {
      Queue q=QueueBuilder.durable(names[i]).withArgument("x-queue-type","classic").ttl(ttl)
          .deadLetterExchange("cmd.dead.dlx").deadLetterRoutingKey(dead[i]).build();
      Queue d=QueueBuilder.durable(names[i]+".dlq").withArgument("x-queue-type","classic").ttl(dlqTtl).maxLength(10000).build();
      all.add(q); all.add(d);
      all.add(new Binding(q.getName(),Binding.DestinationType.QUEUE,i==0?direct.getName():topic.getName(),keys[i],Map.of()));
      all.add(BindingBuilder.bind(d).to(dlx).with(dead[i]));
    }
    return new Declarables(all);
  }
}
