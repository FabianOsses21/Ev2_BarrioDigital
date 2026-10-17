package cl.barriodigital.requests;
import jakarta.persistence.*;
@Entity @Table(name="tramite_write_lock")
public class DomainLock {
  @Id public Long id;
  protected DomainLock(){}
  public DomainLock(long id){this.id=id;}
}
