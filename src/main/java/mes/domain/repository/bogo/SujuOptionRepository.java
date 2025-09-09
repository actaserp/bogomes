package mes.domain.repository.bogo;

import mes.domain.entity.bogo.suju_option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SujuOptionRepository extends JpaRepository<suju_option, Integer> {
}
