package cl.barriodigital.notify;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/notifications") @PreAuthorize("hasRole('ADMIN')")
public class NotifyController {
  private final ReceiptRepository repo;
  public NotifyController(ReceiptRepository repo){this.repo=repo;}
  @GetMapping public List<Receipt> recent(){return repo.findTop100ByOrderByProcessedAtDesc();}
}
