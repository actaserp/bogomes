package mes.domain.repository;

import mes.domain.entity.Routing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoutingRepository extends JpaRepository<Routing, Integer>{

	Optional<Routing> findByName(String name);

	Routing getRoutingById(Integer id);
}
