package cl.barriodigital.requests;
import cl.barriodigital.common.CommandEvent;
import cl.barriodigital.common.MessagingNames;
import cl.barriodigital.requests.entity.Tramite;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component
public class OutboxWriter {
  private final OutboxRepository repo; private final ObjectMapper mapper;
  public OutboxWriter(OutboxRepository repo,ObjectMapper mapper){this.repo=repo;this.mapper=mapper;}
  public void enqueue(Tramite t,String type) {
    String correlation=MDC.get("correlationId"); if(correlation==null)correlation=UUID.randomUUID().toString();
    String[] keys={MessagingNames.RK_EMAIL,MessagingNames.RK_AUDIT+type,MessagingNames.RK_TRACKING+type};
    for(int i=0;i<keys.length;i++) {
      String id=UUID.randomUUID().toString();
      CommandEvent event=new CommandEvent(id,correlation,type,Instant.now().toString(),t.getId(),t.getPropietarioId(),t.getClienteEmail(),t.getAsunto(),t.getEstado().name());
      repo.save(new OutboxMessage(id,i==0?MessagingNames.EX_DIRECT:MessagingNames.EX_TOPIC,keys[i],correlation,mapper.writeValueAsString(event)));
    }
  }
}
