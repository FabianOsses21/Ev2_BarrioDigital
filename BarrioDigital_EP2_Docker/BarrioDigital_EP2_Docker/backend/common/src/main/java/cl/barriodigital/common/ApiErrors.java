package cl.barriodigital.common;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.amqp.AmqpException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    var fields=new LinkedHashMap<String,String>();
    e.getBindingResult().getFieldErrors().forEach(f->fields.put(f.getField(),f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(Map.of("message","Revisa los campos obligatorios","details",fields));
  }
  @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
  ResponseEntity<?> bad(Exception e) {return error(400,"Datos de entrada inválidos");}
  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<?> conflict(IllegalStateException e) {return error(409,e.getMessage());}
  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<?> missing(Exception e) {return error(404,"Recurso no encontrado");}
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {return error(e.getStatusCode().value(),Objects.toString(e.getReason(),"Operación rechazada"));}
  @ExceptionHandler({RestClientException.class,AmqpException.class})
  ResponseEntity<?> unavailable(Exception e) {return error(503,"Servicio dependiente no disponible. Intenta nuevamente.");}
  private ResponseEntity<?> error(int status,String message) {return ResponseEntity.status(status).body(Map.of("status",status,"message",message));}
}
