package cl.barriodigital.common;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=JwtSecurityTest.TestApplication.class,properties={"app.auth.mode=entra","app.auth.issuer=https://issuer.example.test/v2.0","app.auth.audience=api-client","app.auth.scope=OT.Create","app.cors.origins=http://localhost:4200","spring.rabbitmq.dynamic=false"})
@AutoConfigureMockMvc
class JwtSecurityTest {
  static RSAKey key;static HttpServer jwks;
  static {try {
    key=new RSAKeyGenerator(2048).keyID("test-key").generate();jwks=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    jwks.createContext("/keys",e->{byte[] b=new JWKSet(key.toPublicJWK()).toString().getBytes();e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();});jwks.start();
  }catch(Exception e){throw new ExceptionInInitializerError(e);}}
  @DynamicPropertySource static void config(DynamicPropertyRegistry r){r.add("app.auth.jwks",()->"http://127.0.0.1:"+jwks.getAddress().getPort()+"/keys");}
  @AfterAll static void stop(){jwks.stop(0);}
  @Autowired MockMvc mvc;
  @SpringBootConfiguration @EnableAutoConfiguration @Import({PlatformConfig.class,Endpoint.class}) static class TestApplication{}
  @RestController static class Endpoint{@GetMapping("/api/secure")public String secure(){return "ok";}}
  String token(String issuer,String audience,String scope,long expiry)throws Exception{
    var claims=new JWTClaimsSet.Builder().subject("user-1").issuer(issuer).audience(audience).expirationTime(new Date(System.currentTimeMillis()+expiry))
      .claim("scp",scope).claim("roles",List.of("ROLE_CLIENTE")).build();
    var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);jwt.sign(new RSASSASigner(key));return jwt.serialize();
  }
  @Test void validAccessTokenWorks()throws Exception{mvc.perform(get("/api/secure").header("Authorization","Bearer "+token("https://issuer.example.test/v2.0","api-client","OT.Create",60000))).andExpect(status().isOk());}
  @Test void wrongAudienceIs401()throws Exception{mvc.perform(get("/api/secure").header("Authorization","Bearer "+token("https://issuer.example.test/v2.0","another-api","OT.Create",60000))).andExpect(status().isUnauthorized());}
  @Test void wrongIssuerIs401()throws Exception{mvc.perform(get("/api/secure").header("Authorization","Bearer "+token("https://wrong.example.test","api-client","OT.Create",60000))).andExpect(status().isUnauthorized());}
  @Test void expiredTokenIs401()throws Exception{mvc.perform(get("/api/secure").header("Authorization","Bearer "+token("https://issuer.example.test/v2.0","api-client","OT.Create",-300000))).andExpect(status().isUnauthorized());}
  @Test void missingScopeIs401()throws Exception{mvc.perform(get("/api/secure").header("Authorization","Bearer "+token("https://issuer.example.test/v2.0","api-client","",60000))).andExpect(status().isUnauthorized());}
  @Test void demoHeadersCannotBypassEntraMode()throws Exception{mvc.perform(get("/api/secure").header("X-Demo-Role","Admin")).andExpect(status().isUnauthorized());}
}
