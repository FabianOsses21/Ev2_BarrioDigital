package cl.barriodigital.common;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@Configuration
@Import({SecurityConfig.class,ApiErrors.class,Identity.class,CorrelationFilter.class,HttpClients.class})
public class PlatformConfig {}
