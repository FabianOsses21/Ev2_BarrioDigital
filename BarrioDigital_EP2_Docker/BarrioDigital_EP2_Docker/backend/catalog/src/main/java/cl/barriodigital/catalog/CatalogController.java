package cl.barriodigital.catalog;
import cl.barriodigital.catalog.entity.TipoTramite;
import cl.barriodigital.catalog.repository.TipoTramiteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
  private final TipoTramiteRepository repo;
  public CatalogController(TipoTramiteRepository repo){this.repo=repo;}
  public record Input(@Size(max=64) String id,@NotBlank @Size(max=120) String nombre,
      @NotBlank @Size(max=500) String descripcion,@NotNull @Min(0) @Max(100000) Integer cupoDiario,@NotNull Boolean activo) {}
  @GetMapping public List<TipoTramite> list(@RequestParam(defaultValue="true") boolean soloActivos) {
    return soloActivos?repo.findByActivoTrue():repo.findAll();
  }
  @GetMapping("/{id}") public TipoTramite get(@PathVariable String id) {return repo.findById(id).orElseThrow();}
  @PostMapping @PreAuthorize("hasRole('ADMIN')") @Transactional
  public ResponseEntity<TipoTramite> save(@Valid @RequestBody Input input) {
    String id=input.id()==null||input.id().isBlank()?UUID.randomUUID().toString():input.id();
    String name=input.nombre().trim();
    repo.findByNombreIgnoreCase(name).ifPresent(t->{if(!t.getId().equals(id))throw new IllegalStateException("Ya existe un tipo con ese nombre");});
    return ResponseEntity.status(201).body(repo.save(new TipoTramite(id,name,input.descripcion().trim(),input.cupoDiario(),input.activo())));
  }
  // Baja lógica: los trámites conservan la referencia histórica y el nombre capturado.
  @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
  public ResponseEntity<Void> delete(@PathVariable String id) {
    TipoTramite t=get(id); t.setActivo(false); repo.save(t); return ResponseEntity.noContent().build();
  }
  @Bean CommandLineRunner seedCatalog() {return args->{if(repo.count()==0)repo.saveAll(List.of(
      new TipoTramite("luminarias","Reparación de luminarias","Solicitud de revisión y recambio de alumbrado público dañado.",5,true),
      new TipoTramite("retiro","Retiro de residuos voluminosos","Solicitud de retiro de escombros, muebles o electrodomésticos.",3,true),
      new TipoTramite("areas-verdes","Mantención de áreas verdes","Poda, riego y corte de pasto en espacios públicos.",4,true)));
  };}
}
