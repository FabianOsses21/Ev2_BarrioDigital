package cl.barriodigital.catalog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import cl.barriodigital.common.PlatformConfig;

@SpringBootApplication
@Import({PlatformConfig.class})
public class Application { public static void main(String[] args) { SpringApplication.run(Application.class,args); } }
