package cl.barriodigital.requests;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface DomainLockRepository extends JpaRepository<DomainLock,Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from DomainLock l where l.id=1")
  DomainLock acquire();
}
