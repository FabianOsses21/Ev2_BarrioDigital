package cl.barriodigital.bff;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class ProxyController {
  private final RestClient client;private final Map<String,String> routes;
  public ProxyController(RestClient client,@Value("${app.catalog.url}")String catalog,@Value("${app.requests.url}")String requests,
      @Value("${app.notify.url}")String notify,@Value("${app.rabbitadmin.url}")String admin){
    this.client=client;routes=Map.of("catalog",catalog,"requests",requests,"orders",requests,"pedidos",requests,"notifications",notify,"rabbitmq",admin);
  }
  @GetMapping("/api/public") public Map<String,String> publicStatus(){return Map.of("application","BarrioDigital BFF","status","UP");}
  @GetMapping("/api/ordenes") public String protectedStatus(){return "BFF protegido: acceso autorizado";}
  @RequestMapping(value={"/api/{resource}","/api/{resource}/**"},method={RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT,RequestMethod.PATCH,RequestMethod.DELETE})
  public ResponseEntity<byte[]> forward(@PathVariable String resource,HttpServletRequest request,@RequestBody(required=false)byte[] body){
    String destination=routes.get(resource);if(destination==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    String query=request.getQueryString();
    var outgoing=client.method(HttpMethod.valueOf(request.getMethod()))
      .uri(URI.create(destination+request.getRequestURI()+(query==null?"":"?"+query)));
    for(String name:List.of("Authorization","Content-Type","Accept","X-Demo-Role","X-Demo-User")){
      String value=request.getHeader(name);if(value!=null)outgoing.header(name,value);
    }
    if(MDC.get("correlationId")!=null)outgoing.header("X-Correlation-ID",MDC.get("correlationId"));
    if(body!=null)outgoing.body(body);
    return outgoing.exchange((req,res)->{
      HttpHeaders headers=new HttpHeaders();
      if(res.getHeaders().getContentType()!=null)headers.setContentType(res.getHeaders().getContentType());
      return new ResponseEntity<>(res.getBody().readAllBytes(),headers,res.getStatusCode());
    });
  }
}
