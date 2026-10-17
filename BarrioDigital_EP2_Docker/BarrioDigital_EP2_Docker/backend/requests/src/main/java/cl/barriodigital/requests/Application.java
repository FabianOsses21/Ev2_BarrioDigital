package cl.barriodigital.requests;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import cl.barriodigital.common.PlatformConfig;
import cl.barriodigital.common.MessagingConfig;
@SpringBootApplication
@Import({PlatformConfig.class, MessagingConfig.class})
public class Application { public static void main(String[] args) { SpringApplication.run(Application.class,args); } }
