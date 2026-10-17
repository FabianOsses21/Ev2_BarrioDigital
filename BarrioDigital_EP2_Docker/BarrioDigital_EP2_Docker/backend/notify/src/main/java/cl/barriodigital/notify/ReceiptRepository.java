package cl.barriodigital.notify;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceiptRepository extends JpaRepository<Receipt,String>{List<Receipt> findTop100ByOrderByProcessedAtDesc();}
