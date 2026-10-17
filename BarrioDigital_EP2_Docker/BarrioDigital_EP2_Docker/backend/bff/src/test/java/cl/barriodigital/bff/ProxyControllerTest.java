package cl.barriodigital.bff;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
class ProxyControllerTest {
  HttpServer server;ProxyController proxy;AtomicReference<String> auth=new AtomicReference<>(),body=new AtomicReference<>(),path=new AtomicReference<>();
  @BeforeEach void setup()throws Exception{
    server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/api/requests",exchange->{auth.set(exchange.getRequestHeaders().getFirst("Authorization"));body.set(new String(exchange.getRequestBody().readAllBytes()));path.set(exchange.getRequestURI().toString());
      byte[] response="{\"message\":\"invalid\"}".getBytes();exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(400,response.length);exchange.getResponseBody().write(response);exchange.close();});server.start();
    String base="http://127.0.0.1:"+server.getAddress().getPort();proxy=new ProxyController(RestClient.create(),base,base,base,base);
  }
  @AfterEach void stop(){server.stop(0);}
  @Test void relaysTokenPayloadQueryAndDownstreamError(){
    var request=new MockHttpServletRequest("POST","/api/requests");request.setQueryString("estado=INGRESADO");request.addHeader("Authorization","Bearer test-token");request.addHeader("Content-Type","application/json");
    var result=proxy.forward("requests",request,"{\"asunto\":\"hola\"}".getBytes());
    assertThat(result.getStatusCode().value()).isEqualTo(400);assertThat(auth.get()).isEqualTo("Bearer test-token");assertThat(body.get()).contains("hola");assertThat(path.get()).isEqualTo("/api/requests?estado=INGRESADO");assertThat(new String(result.getBody())).contains("invalid");
  }
  @Test void unknownRouteIsRejected(){assertThatThrownBy(()->proxy.forward("external",new MockHttpServletRequest("GET","/api/external"),null)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);}
}
