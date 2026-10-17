package cl.barriodigital.common;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
@Configuration
public class HttpClients {
  @Bean public RestClient internalClient() {
    var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    var factory=new JdkClientHttpRequestFactory(client); factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(factory).build();
  }
}
