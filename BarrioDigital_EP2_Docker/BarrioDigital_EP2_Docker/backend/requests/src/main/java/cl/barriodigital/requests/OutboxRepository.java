package cl.barriodigital.requests;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.*;
public interface OutboxRepository extends JpaRepository<OutboxMessage,String> {
  List<OutboxMessage> findTop20BySentAtIsNullAndNextAttemptLessThanEqualOrderByCreatedAtAsc(Instant now);
  long countBySentAtIsNull();
  long deleteBySentAtBefore(Instant cutoff);
}
