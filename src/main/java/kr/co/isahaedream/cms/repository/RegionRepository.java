package kr.co.isahaedream.cms.repository;

import kr.co.isahaedream.cms.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findAllByEnabledTrueOrderByDisplayOrderAsc();
    Optional<Region> findBySlugAndEnabledTrue(String slug);
}
