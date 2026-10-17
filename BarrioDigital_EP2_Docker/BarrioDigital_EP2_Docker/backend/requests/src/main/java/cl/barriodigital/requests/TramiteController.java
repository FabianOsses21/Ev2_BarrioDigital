package cl.barriodigital.requests;
import cl.barriodigital.requests.entity.Tramite;
import cl.barriodigital.requests.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping({"/api/requests","/api/orders","/api/pedidos"})
public class TramiteController {
  private final TramiteService service;private final OutboxRepository outbox;
  public TramiteController(TramiteService service,OutboxRepository outbox){this.service=service;this.outbox=outbox;}
  public record Edit(@NotBlank @Size(min=5,max=100)String asunto,@NotBlank @Size(min=10,max=1000)String descripcion){}
  @GetMapping public List<Tramite> list(){return service.list();}
  @GetMapping("/{id}") public Tramite get(@PathVariable String id){return service.get(id);}
  @PostMapping public ResponseEntity<Tramite> create(@Valid @RequestBody CrearTramiteDto dto){return ResponseEntity.status(201).body(service.create(dto));}
  @PutMapping("/{id}") public Tramite edit(@PathVariable String id,@Valid @RequestBody Edit dto){return service.edit(id,dto.asunto(),dto.descripcion());}
  @PutMapping("/{id}/estado") @PreAuthorize("hasAnyRole('ADMIN','FUNCIONARIO')")
  public Tramite state(@PathVariable String id,@Valid @RequestBody ActualizarEstadoDto dto){return service.change(id,dto.getEstado());}
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id){service.delete(id);return ResponseEntity.noContent().build();}
  @GetMapping("/outbox/status") @PreAuthorize("hasRole('ADMIN')")
  public Map<String,Long> status(){return Map.of("pendientes",outbox.countBySentAtIsNull());}
  @Bean CommandLineRunner initLock(DomainLockRepository locks){return args->{if(!locks.existsById(1L))locks.save(new DomainLock(1));};}
}
