package cl.barriodigital.rabbitadmin;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component
public class ManagementClient {
  private final RestClient client;private final String[] urls;private final String user,password;
  public ManagementClient(RestClient client,@Value("${app.rabbit.management-urls}")String urls,
      @Value("${spring.rabbitmq.username}")String user,@Value("${spring.rabbitmq.password}")String password){
    this.client=client;this.urls=urls.split(",");this.user=user;this.password=password;
  }
  public Object get(String path){
    RestClientException last=null;
    for(String url:urls){try{return client.get().uri(URI.create(url+"/api/"+path)).headers(h->h.setBasicAuth(user,password)).retrieve().body(Object.class);}
      catch(RestClientException e){last=e;}}
    throw last==null?new RestClientException("Management no disponible"):last;
  }
}
