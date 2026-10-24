package kr.co.isahaedream.cms.repository;

import kr.co.isahaedream.cms.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface LandingContentRepository extends JpaRepository<LandingContent, Long> {
    Optional<LandingContent> findByRegionAndNeighborhoodIsNullAndMovingService(Region region, MovingService movingService);
    Optional<LandingContent> findByRegionAndNeighborhoodAndMovingService(Region region, Neighborhood neighborhood, MovingService movingService);

    @EntityGraph(attributePaths = {"region", "neighborhood", "movingService"})
    List<LandingContent> findAllByPublishedTrue();

    @EntityGraph(attributePaths = {"region", "neighborhood", "movingService"})
    List<LandingContent> findAllByRegion(Region region);

    long countByPublishedTrue();
    boolean existsByNeighborhoodAndPublishedTrue(Neighborhood neighborhood);
}
