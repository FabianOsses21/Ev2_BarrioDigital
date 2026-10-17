package cl.barriodigital.common;
import java.util.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  public static String role(String input) {
    return switch(input.toUpperCase(Locale.ROOT)) {
      case "ADMIN", "ADMINISTRADOR", "ROLE_ADMINISTRADOR", "ROLE_ADMIN" -> "ADMIN";
      case "FUNCIONARIO", "OPERADOR", "ROLE_OPERADOR", "ROLE_FUNCIONARIO" -> "FUNCIONARIO";
      case "VECINO", "CLIENTE", "ROLE_CLIENTE", "ROLE_VECINO" -> "VECINO";
      default -> "";
    };
  }
  @Bean SecurityFilterChain security(HttpSecurity http,
      @Value("${app.auth.mode}") String mode, @Value("${app.auth.issuer}") String issuer,
      @Value("${app.auth.jwks}") String jwks, @Value("${app.auth.audience}") String audience,
      @Value("${app.auth.scope}") String scope, @Value("${app.cors.origins}") String origins) throws Exception {
    CorsConfiguration cors=new CorsConfiguration();
    cors.setAllowedOrigins(Arrays.asList(origins.split(",")));
    cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization","Content-Type","X-Correlation-ID","X-Demo-Role","X-Demo-User"));
    cors.setExposedHeaders(List.of("X-Correlation-ID"));
    UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",cors);
    http.cors(c->c.configurationSource(source)).csrf(c->c.disable())
      .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**","/api/public").permitAll()
        .anyRequest().hasAnyRole("ADMIN","FUNCIONARIO","VECINO"));
    if ("demo".equals(mode)) {
      http.addFilterBefore(new OncePerRequestFilter(){
        @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
          String r=role(Objects.toString(req.getHeader("X-Demo-Role"),""));
          String user=Objects.toString(req.getHeader("X-Demo-User"),"demo-vecino");
          if(!r.isEmpty() && user.matches("[A-Za-z0-9._-]{1,64}"))
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of(new SimpleGrantedAuthority("ROLE_"+r))));
          chain.doFilter(req,res);
        }
      },UsernamePasswordAuthenticationFilter.class);
      http.exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.sendError(401)));
    } else if ("entra".equals(mode)) {
      NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
      OAuth2TokenValidator<Jwt> audienceCheck=token-> token.getAudience().contains(audience)
          ? OAuth2TokenValidatorResult.success() : invalid("Audience inválida");
      OAuth2TokenValidator<Jwt> scopeCheck=token-> Arrays.asList(Objects.toString(token.getClaimAsString("scp"),"").split(" ")).contains(scope)
          ? OAuth2TokenValidatorResult.success() : invalid("Scope requerido ausente");
      decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),audienceCheck,scopeCheck));
      JwtAuthenticationConverter converter=new JwtAuthenticationConverter();
      converter.setJwtGrantedAuthoritiesConverter(jwt->{
        var authorities=new ArrayList<org.springframework.security.core.GrantedAuthority>(new JwtGrantedAuthoritiesConverter().convert(jwt));
        List<String> roles=jwt.getClaimAsStringList("roles");
        if(roles!=null) roles.stream().map(SecurityConfig::role).filter(r->!r.isEmpty()).distinct().forEach(r->authorities.add(new SimpleGrantedAuthority("ROLE_"+r)));
        return authorities;
      });
      http.oauth2ResourceServer(o->o.jwt(j->j.decoder(decoder).jwtAuthenticationConverter(converter)));
    } else throw new IllegalStateException("AUTH_MODE debe ser entra o demo");
    return http.build();
  }
  private static OAuth2TokenValidatorResult invalid(String description) {
    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",description,null));
  }
}
