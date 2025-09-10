package mes.domain.repository.bogo;

import mes.domain.entity.bogo.suju_remark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SujuRemarkRepository extends JpaRepository<suju_remark, Integer> {
  void deleteBySujuId(Integer id);
}
