package cl.barriodigital.requests;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@Component
public class CatalogClient {
  public record Tipo(String id,String nombre,String descripcion,Integer cupoDiario,Boolean activo){}
  private final RestClient client; private final String url;
  public CatalogClient(RestClient client,@Value("${app.catalog.url}")String url){this.client=client;this.url=url;}
  public Tipo get(String id) {
    var request=client.get().uri(url+"/api/catalog/{id}",id);
    var context=RequestContextHolder.getRequestAttributes();
    if(context instanceof ServletRequestAttributes attrs) {
      for(String key:List.of("Authorization","X-Demo-Role","X-Demo-User","X-Correlation-ID")) {
        String value=attrs.getRequest().getHeader(key); if(value!=null)request.header(key,value);
      }
    }
    return request.retrieve().onStatus(s->s.value()==404,(req,res)->{throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El tipo de trámite no existe");}).body(Tipo.class);
  }
}
