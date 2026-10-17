package cl.barriodigital.requests;
import cl.barriodigital.requests.entity.*;
import cl.barriodigital.requests.dto.*;
import cl.barriodigital.requests.repository.TramiteRepository;
import cl.barriodigital.common.Identity;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service @Transactional
public class TramiteService {
  private final TramiteRepository repo;private final CatalogClient catalog;private final OutboxWriter outbox;
  private final Identity identity;private final DomainLockRepository lock;
  public static final Map<EstadoTramite,Set<EstadoTramite>> TRANSITIONS=Map.of(
    EstadoTramite.INGRESADO,Set.of(EstadoTramite.ADMITIDO,EstadoTramite.RECHAZADO),
    EstadoTramite.ADMITIDO,Set.of(EstadoTramite.EN_GESTION,EstadoTramite.RECHAZADO),
    EstadoTramite.EN_GESTION,Set.of(EstadoTramite.EN_TERRENO,EstadoTramite.RECHAZADO),
    EstadoTramite.EN_TERRENO,Set.of(EstadoTramite.RESUELTO,EstadoTramite.RECHAZADO),
    EstadoTramite.RESUELTO,Set.of(),EstadoTramite.RECHAZADO,Set.of());
  public TramiteService(TramiteRepository repo,CatalogClient catalog,OutboxWriter outbox,Identity identity,DomainLockRepository lock){
    this.repo=repo;this.catalog=catalog;this.outbox=outbox;this.identity=identity;this.lock=lock;
  }
  @Transactional(readOnly=true) public List<Tramite> list(){
    return identity.staff()?repo.findAllByOrderByFechaCreacionDesc():repo.findByPropietarioIdOrderByFechaCreacionDesc(identity.id());
  }
  @Transactional(readOnly=true) public Tramite get(String id){
    Tramite t=repo.findById(id).orElseThrow();
    if(!identity.staff()&&!t.getPropietarioId().equals(identity.id()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No puedes consultar este trámite");
    return t;
  }
  public Tramite create(CrearTramiteDto dto){
    lock.acquire();
    CatalogClient.Tipo tipo=catalog.get(dto.getTipoId());
    if(!Boolean.TRUE.equals(tipo.activo()))throw new IllegalStateException("El tipo de trámite está inactivo");
    Tramite t=new Tramite(UUID.randomUUID().toString(),tipo.id(),tipo.nombre(),dto.getAsunto().trim(),dto.getDescripcion().trim(),identity.id(),identity.name(),EstadoTramite.INGRESADO);
    t.setClienteEmail(dto.getClienteEmail().trim());
    repo.save(t);outbox.enqueue(t,"creado");return t;
  }
  public Tramite edit(String id,String asunto,String descripcion){
    lock.acquire();Tramite t=get(id);
    if(t.getEstado()!=EstadoTramite.INGRESADO)throw new IllegalStateException("Solo se editan trámites ingresados");
    t.setAsunto(asunto.trim());t.setDescripcion(descripcion.trim());t.setFechaActualizacion(LocalDateTime.now());return repo.save(t);
  }
  public Tramite change(String id,EstadoTramite next){
    lock.acquire();Tramite t=get(id);
    if(!TRANSITIONS.get(t.getEstado()).contains(next))throw new IllegalStateException("Transición de estado no permitida");
    if(next==EstadoTramite.ADMITIDO){
      var tipo=catalog.get(t.getTipoId());
      if(!Boolean.TRUE.equals(tipo.activo()))throw new IllegalStateException("El tipo de trámite está inactivo");
      LocalDate today=LocalDate.now();
      long count=repo.countByTipoIdAndFechaAdmisionBetween(tipo.id(),today.atStartOfDay(),today.atTime(LocalTime.MAX));
      if(count>=tipo.cupoDiario())throw new IllegalStateException("Cupo diario agotado");
      t.setFechaAdmision(LocalDateTime.now());
    }
    t.setEstado(next);t.setFechaActualizacion(LocalDateTime.now());repo.save(t);outbox.enqueue(t,"actualizado");return t;
  }
  public void delete(String id){lock.acquire();Tramite t=get(id);if(t.getEstado()!=EstadoTramite.INGRESADO)throw new IllegalStateException("Solo se eliminan trámites ingresados");repo.delete(t);}
}
