package mes.domain.repository.bogo;

import mes.domain.entity.bogo.tb_as012;
import org.springframework.data.jpa.repository.JpaRepository;

public interface tb_as012Repository extends JpaRepository<tb_as012,Integer> {

  void deleteByRepid(Integer id);
}
