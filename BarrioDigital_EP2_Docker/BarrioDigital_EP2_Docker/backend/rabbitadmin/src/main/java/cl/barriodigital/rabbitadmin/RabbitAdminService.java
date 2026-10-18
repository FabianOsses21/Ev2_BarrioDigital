package cl.barriodigital.rabbitadmin;
import java.util.Map;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;
@Service
public class RabbitAdminService {
  public static final String QUEUE_NAME="q\\.test\\.[A-Za-z0-9_-][A-Za-z0-9._-]{0,90}";
  public static final String EXCHANGE_NAME="x\\.test\\.[A-Za-z0-9_-][A-Za-z0-9._-]{0,90}";
  private final AmqpAdmin admin;private final ManagementClient management;
  public RabbitAdminService(AmqpAdmin admin,ManagementClient management){this.admin=admin;this.management=management;}
  public void createQueue(String name,Integer ttlMs,Integer maxLength){
    var queue=QueueBuilder.durable(name).withArgument("x-queue-type","classic")
      .ttl(ttlMs==null?30000:ttlMs).maxLength(maxLength==null?10000:maxLength).build();
    admin.declareQueue(queue);
  }
  public void deleteQueue(String name){validate(name,QUEUE_NAME);admin.deleteQueue(name,true,true);}
  public void createExchange(String name,String type){admin.declareExchange(new CustomExchange(name,type,true,false));}
  public void deleteExchange(String name){validate(name,EXCHANGE_NAME);admin.deleteExchange(name);}
  public void bind(String queueName,String exchangeName,String routingKey){admin.declareBinding(binding(queueName,exchangeName,routingKey));}
  public void unbind(String queueName,String exchangeName,String routingKey){admin.removeBinding(binding(queueName,exchangeName,routingKey));}
  public int purgeQueue(String name,boolean confirm){
    validate(name,QUEUE_NAME);if(!confirm)throw new IllegalArgumentException("Confirma la limpieza");
    return admin.purgeQueue(name);
  }
  public Object queues(){return management.get("queues/%2F");}
  public Object consumers(){return management.get("consumers/%2F");}
  public Object nodes(){return management.get("nodes");}
  public Object overview(){return management.get("overview");}
  private Binding binding(String queueName,String exchangeName,String routingKey){
    return new Binding(queueName,Binding.DestinationType.QUEUE,exchangeName,routingKey,Map.of());
  }
  private void validate(String value,String pattern){if(!value.matches(pattern))throw new IllegalArgumentException("Nombre fuera del espacio de pruebas");}
}
