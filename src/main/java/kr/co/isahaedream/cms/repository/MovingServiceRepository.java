package kr.co.isahaedream.cms.repository;

import kr.co.isahaedream.cms.domain.MovingService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MovingServiceRepository extends JpaRepository<MovingService, Long> {
    List<MovingService> findAllByEnabledTrueOrderByDisplayOrderAsc();
    Optional<MovingService> findBySlugAndEnabledTrue(String slug);
}
