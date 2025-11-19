package mes.domain.repository;

import mes.domain.entity.WorkCategory;
import mes.domain.entity.WorkCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface WorkCategoryRepository extends JpaRepository<WorkCategory, WorkCategoryId> {
    List<WorkCategory> findByIdJobResId(int jobResId);
    boolean existsById(WorkCategoryId id);

    void deleteByJobResId(int jobResId);

}