package mes.domain.repository.commute;

import mes.domain.entity.commute.TB_PB201;
import mes.domain.entity.commute.TB_PB201_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TB_PB201Repository extends JpaRepository<TB_PB201, Long> {

    int countByJobResIdAndWorkcd(Integer JobResId, String workcd);

}