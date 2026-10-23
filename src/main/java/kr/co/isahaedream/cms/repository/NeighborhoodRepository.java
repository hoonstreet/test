package kr.co.isahaedream.cms.repository;

import kr.co.isahaedream.cms.domain.Neighborhood;
import kr.co.isahaedream.cms.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long> {
    List<Neighborhood> findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(Region region);
    Optional<Neighborhood> findByRegionAndSlugAndEnabledTrue(Region region, String slug);
}
