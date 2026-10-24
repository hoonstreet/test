package kr.co.isahaedream.cms.config;

import kr.co.isahaedream.cms.domain.MovingService;
import kr.co.isahaedream.cms.domain.Neighborhood;
import kr.co.isahaedream.cms.domain.Region;
import kr.co.isahaedream.cms.domain.SiteSettings;
import kr.co.isahaedream.cms.repository.MovingServiceRepository;
import kr.co.isahaedream.cms.repository.NeighborhoodRepository;
import kr.co.isahaedream.cms.repository.RegionRepository;
import kr.co.isahaedream.cms.repository.SiteSettingsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(10)
public class SeedDataInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final SiteSettingsRepository siteSettingsRepository;

    public SeedDataInitializer(RegionRepository regionRepository,
                               NeighborhoodRepository neighborhoodRepository,
                               MovingServiceRepository movingServiceRepository,
                               SiteSettingsRepository siteSettingsRepository) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.siteSettingsRepository = siteSettingsRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedServices();
        seedRegions();
        if (!siteSettingsRepository.existsById(1L)) {
            siteSettingsRepository.save(new SiteSettings());
        }
    }

    private void seedServices() {
        if (movingServiceRepository.count() > 0) return;
        movingServiceRepository.saveAll(List.of(
                new MovingService("포장이사", "packing-move", "포장부터 운송, 정리까지 한 번에 진행합니다.", 1),
                new MovingService("보관이사", "storage-move", "입주 일정에 맞춰 안전하게 보관하고 운송합니다.", 2),
                new MovingService("입주이사", "move-in", "새 보금자리 일정과 동선에 맞춘 이사 서비스입니다.", 3),
                new MovingService("사무실이사", "office-move", "업무 공백을 줄이도록 계획적으로 이전합니다.", 4),
                new MovingService("아파트이사", "apartment-move", "엘리베이터와 사다리차 동선을 고려해 진행합니다.", 5),
                new MovingService("이삿짐센터", "moving-center", "지역 여건에 맞는 차량과 인력을 상담합니다.", 6)
        ));
    }

    private void seedRegions() {
        if (regionRepository.count() > 0) return;
        int regionOrder = 1;
        for (ApprovedLocationCatalog.RegionSeed seed : ApprovedLocationCatalog.regions()) {
            Region region = regionRepository.save(new Region(seed.name(), seed.slug(), regionOrder++));
            int neighborhoodOrder = 1;
            for (ApprovedLocationCatalog.NameSlug neighborhood : seed.neighborhoods()) {
                neighborhoodRepository.save(new Neighborhood(
                        region, neighborhood.name(), neighborhood.slug(), neighborhoodOrder++));
            }
        }
    }
}
