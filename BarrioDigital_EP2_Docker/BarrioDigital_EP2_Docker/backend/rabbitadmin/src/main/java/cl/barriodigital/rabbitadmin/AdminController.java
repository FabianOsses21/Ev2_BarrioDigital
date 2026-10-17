package cl.barriodigital.rabbitadmin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rabbitmq") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  public static final String Q="q\\.test\\.[A-Za-z0-9_-][A-Za-z0-9._-]{0,90}";
  public static final String X="x\\.test\\.[A-Za-z0-9_-][A-Za-z0-9._-]{0,90}";
  private final AmqpAdmin admin;private final ManagementClient management;
  public AdminController(AmqpAdmin admin,ManagementClient management){this.admin=admin;this.management=management;}
  public record QueueInput(@NotBlank @Pattern(regexp=Q)String queueName,@Min(1000) @Max(604800000)Integer ttlMs,@Min(1) @Max(100000)Integer maxLength){}
  public record ExchangeInput(@NotBlank @Pattern(regexp=X)String exchangeName,@NotBlank @Pattern(regexp="direct|topic")String type){}
  public record BindingInput(@NotBlank @Pattern(regexp=Q)String queueName,@NotBlank @Pattern(regexp=X)String exchangeName,@NotBlank @Size(max=100)String routingKey){}
  @PostMapping("/queues") public ResponseEntity<?> queue(@Valid @RequestBody QueueInput in){
    var queue=QueueBuilder.durable(in.queueName()).withArgument("x-queue-type","classic")
      .ttl(in.ttlMs()==null?30000:in.ttlMs()).maxLength(in.maxLength()==null?10000:in.maxLength()).build();
    admin.declareQueue(queue);return ResponseEntity.status(201).body(Map.of("queueName",in.queueName(),"created",true));
  }
  @DeleteMapping("/queues/{name}") public ResponseEntity<Void> deleteQueue(@PathVariable String name){
    validate(name,Q);admin.deleteQueue(name,true,true);return ResponseEntity.noContent().build();
  }
  @PostMapping("/exchanges") public ResponseEntity<?> exchange(@Valid @RequestBody ExchangeInput in){
    admin.declareExchange(new CustomExchange(in.exchangeName(),in.type(),true,false));
    return ResponseEntity.status(201).body(Map.of("exchangeName",in.exchangeName(),"created",true));
  }
  @DeleteMapping("/exchanges/{name}") public ResponseEntity<Void> deleteExchange(@PathVariable String name){validate(name,X);admin.deleteExchange(name);return ResponseEntity.noContent().build();}
  @PostMapping("/bindings") public ResponseEntity<?> bind(@Valid @RequestBody BindingInput in){admin.declareBinding(binding(in));return ResponseEntity.status(201).body(Map.of("created",true));}
  @DeleteMapping("/bindings") public ResponseEntity<Void> unbind(@Valid @RequestBody BindingInput in){admin.removeBinding(binding(in));return ResponseEntity.noContent().build();}
  @PostMapping("/queues/{name}/purge") public ResponseEntity<?> purge(@PathVariable String name,@RequestParam(defaultValue="false")boolean confirm){
    validate(name,Q);if(!confirm)throw new IllegalArgumentException("Confirma la limpieza");
    return ResponseEntity.ok(Map.of("removed",admin.purgeQueue(name)));
  }
  @GetMapping("/queues") public Object queues(){return management.get("queues/%2F");}
  @GetMapping("/consumers") public Object consumers(){return management.get("consumers/%2F");}
  @GetMapping("/nodes") public Object nodes(){return management.get("nodes");}
  @GetMapping("/overview") public Object overview(){return management.get("overview");}
  private Binding binding(BindingInput in){return new Binding(in.queueName(),Binding.DestinationType.QUEUE,in.exchangeName(),in.routingKey(),Map.of());}
  private void validate(String value,String pattern){if(!value.matches(pattern))throw new IllegalArgumentException("Nombre fuera del espacio de pruebas");}
}
