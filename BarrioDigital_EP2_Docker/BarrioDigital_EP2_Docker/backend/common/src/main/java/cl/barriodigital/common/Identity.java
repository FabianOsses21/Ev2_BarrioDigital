package cl.barriodigital.common;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
public class Identity {
  public String id() {
    var a=SecurityContextHolder.getContext().getAuthentication();
    if(a.getPrincipal() instanceof Jwt jwt) return jwt.hasClaim("oid")?jwt.getClaimAsString("oid"):jwt.getSubject();
    return a.getName();
  }
  public String name() {
    var a=SecurityContextHolder.getContext().getAuthentication();
    if(a.getPrincipal() instanceof Jwt jwt && jwt.hasClaim("name")) return jwt.getClaimAsString("name");
    return id();
  }
  public boolean staff() { return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")||a.getAuthority().equals("ROLE_FUNCIONARIO")); }
}
