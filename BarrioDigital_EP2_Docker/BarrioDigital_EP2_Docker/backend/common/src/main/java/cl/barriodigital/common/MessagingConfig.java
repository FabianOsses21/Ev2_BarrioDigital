package cl.barriodigital.common;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class MessagingConfig {
  private Queue main(String name,String deadKey,int ttl){
    return QueueBuilder.durable(name).withArgument("x-queue-type","classic").ttl(ttl)
      .deadLetterExchange(MessagingNames.EX_DEAD).deadLetterRoutingKey(deadKey).build();
  }
  private Queue dead(String name,int ttl){
    return QueueBuilder.durable(name+".dlq").withArgument("x-queue-type","classic").ttl(ttl).maxLength(10000).build();
  }
  @Bean DirectExchange cmdDirect(){return new DirectExchange(MessagingNames.EX_DIRECT,true,false);}
  @Bean TopicExchange cmdTopic(){return new TopicExchange(MessagingNames.EX_TOPIC,true,false);}
  @Bean DirectExchange cmdDead(){return new DirectExchange(MessagingNames.EX_DEAD,true,false);}
  @Bean Queue emailQueue(@Value("${app.queue.ttl-ms:30000}")int ttl){return main(MessagingNames.Q_EMAIL,MessagingNames.DL_EMAIL,ttl);}
  @Bean Queue emailDlq(@Value("${app.dlq.ttl-ms:604800000}")int ttl){return dead(MessagingNames.Q_EMAIL,ttl);}
  @Bean Queue auditQueue(@Value("${app.queue.ttl-ms:30000}")int ttl){return main(MessagingNames.Q_AUDIT,MessagingNames.DL_AUDIT,ttl);}
  @Bean Queue auditDlq(@Value("${app.dlq.ttl-ms:604800000}")int ttl){return dead(MessagingNames.Q_AUDIT,ttl);}
  @Bean Queue trackingQueue(@Value("${app.queue.ttl-ms:30000}")int ttl){return main(MessagingNames.Q_TRACKING,MessagingNames.DL_TRACKING,ttl);}
  @Bean Queue trackingDlq(@Value("${app.dlq.ttl-ms:604800000}")int ttl){return dead(MessagingNames.Q_TRACKING,ttl);}
  @Bean Binding emailBinding(Queue emailQueue,DirectExchange cmdDirect){return BindingBuilder.bind(emailQueue).to(cmdDirect).with(MessagingNames.RK_EMAIL);}
  @Bean Binding auditBinding(Queue auditQueue,TopicExchange cmdTopic){return BindingBuilder.bind(auditQueue).to(cmdTopic).with(MessagingNames.BK_AUDIT);}
  @Bean Binding trackingBinding(Queue trackingQueue,TopicExchange cmdTopic){return BindingBuilder.bind(trackingQueue).to(cmdTopic).with(MessagingNames.BK_TRACKING);}
  @Bean Binding emailDlqBinding(Queue emailDlq,DirectExchange cmdDead){return BindingBuilder.bind(emailDlq).to(cmdDead).with(MessagingNames.DL_EMAIL);}
  @Bean Binding auditDlqBinding(Queue auditDlq,DirectExchange cmdDead){return BindingBuilder.bind(auditDlq).to(cmdDead).with(MessagingNames.DL_AUDIT);}
  @Bean Binding trackingDlqBinding(Queue trackingDlq,DirectExchange cmdDead){return BindingBuilder.bind(trackingDlq).to(cmdDead).with(MessagingNames.DL_TRACKING);}
}
