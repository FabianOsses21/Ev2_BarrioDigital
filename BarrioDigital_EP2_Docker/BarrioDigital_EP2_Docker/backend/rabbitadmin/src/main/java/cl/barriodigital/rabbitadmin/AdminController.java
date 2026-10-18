package cl.barriodigital.rabbitadmin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rabbitmq") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final RabbitAdminService service;
  public AdminController(RabbitAdminService service){this.service=service;}
  public record QueueInput(@NotBlank @Pattern(regexp=RabbitAdminService.QUEUE_NAME)String queueName,@Min(1000) @Max(604800000)Integer ttlMs,@Min(1) @Max(100000)Integer maxLength){}
  public record ExchangeInput(@NotBlank @Pattern(regexp=RabbitAdminService.EXCHANGE_NAME)String exchangeName,@NotBlank @Pattern(regexp="direct|topic")String type){}
  public record BindingInput(@NotBlank @Pattern(regexp=RabbitAdminService.QUEUE_NAME)String queueName,@NotBlank @Pattern(regexp=RabbitAdminService.EXCHANGE_NAME)String exchangeName,@NotBlank @Size(max=100)String routingKey){}
  @PostMapping("/queues") public ResponseEntity<?> queue(@Valid @RequestBody QueueInput in){
    service.createQueue(in.queueName(),in.ttlMs(),in.maxLength());
    return ResponseEntity.status(201).body(Map.of("queueName",in.queueName(),"created",true));
  }
  @DeleteMapping("/queues/{name}") public ResponseEntity<Void> deleteQueue(@PathVariable String name){
    service.deleteQueue(name);return ResponseEntity.noContent().build();
  }
  @PostMapping("/exchanges") public ResponseEntity<?> exchange(@Valid @RequestBody ExchangeInput in){
    service.createExchange(in.exchangeName(),in.type());
    return ResponseEntity.status(201).body(Map.of("exchangeName",in.exchangeName(),"created",true));
  }
  @DeleteMapping("/exchanges/{name}") public ResponseEntity<Void> deleteExchange(@PathVariable String name){
    service.deleteExchange(name);return ResponseEntity.noContent().build();
  }
  @PostMapping("/bindings") public ResponseEntity<?> bind(@Valid @RequestBody BindingInput in){
    service.bind(in.queueName(),in.exchangeName(),in.routingKey());
    return ResponseEntity.status(201).body(Map.of("created",true));
  }
  @DeleteMapping("/bindings") public ResponseEntity<Void> unbind(@Valid @RequestBody BindingInput in){
    service.unbind(in.queueName(),in.exchangeName(),in.routingKey());
    return ResponseEntity.noContent().build();
  }
  @PostMapping("/queues/{name}/purge") public ResponseEntity<?> purge(@PathVariable String name,@RequestParam(defaultValue="false")boolean confirm){
    return ResponseEntity.ok(Map.of("removed",service.purgeQueue(name,confirm)));
  }
  @GetMapping("/queues") public Object queues(){return service.queues();}
  @GetMapping("/consumers") public Object consumers(){return service.consumers();}
  @GetMapping("/nodes") public Object nodes(){return service.nodes();}
  @GetMapping("/overview") public Object overview(){return service.overview();}
}
