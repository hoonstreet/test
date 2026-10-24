package kr.co.isahaedream.cms.config;

import kr.co.isahaedream.cms.domain.MovingService;
import kr.co.isahaedream.cms.domain.Neighborhood;
import kr.co.isahaedream.cms.domain.Region;
import kr.co.isahaedream.cms.repository.MovingServiceRepository;
import kr.co.isahaedream.cms.repository.NeighborhoodRepository;
import kr.co.isahaedream.cms.repository.RegionRepository;
import kr.co.isahaedream.cms.service.LandingPageService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 노원구 승인 키워드 36개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(80)
public class NowonDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public NowonDraftInitializer(RegionRepository regionRepository,
                                 NeighborhoodRepository neighborhoodRepository,
                                 MovingServiceRepository movingServiceRepository,
                                 LandingPageService landingPageService) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingPageService = landingPageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Region region = regionRepository.findBySlugAndEnabledTrue("nowon-gu").orElseThrow();
        seedArea(region, null, profiles().getFirst());
        for (AreaProfile profile : profiles().subList(1, profiles().size())) {
            Neighborhood neighborhood = neighborhoodRepository
                    .findByRegionAndSlugAndEnabledTrue(region, profile.slug()).orElseThrow();
            seedArea(region, neighborhood, profile);
        }
    }

    private void seedArea(Region region, Neighborhood neighborhood, AreaProfile profile) {
        for (MovingService service : movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc()) {
            if (landingPageService.find(region, neighborhood, service).isPresent()) continue;
            ApprovedDraftFactory.Draft draft = ApprovedDraftFactory.create(
                    profile.name(), profile.context(), profile.accessFocus(),
                    service.getSlug(), service.getName());
            landingPageService.save(region, neighborhood, service, draft.seoTitle(), draft.metaDescription(),
                    draft.headline(), draft.introduction(), draft.body(), false);
        }
    }

    private static List<AreaProfile> profiles() {
        String sharedAccess = "노원구 공식 지역특성 안내처럼 동쪽 산지와 서쪽 주거지역의 조건이 다르고 공동주택과 도로 여건도 주소마다 달라, 이사 차량 진입과 주차 위치, 경사, 엘리베이터 사용 기준을 현장별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("노원구", null,
                        "노원구는 공식 조직 안내에서 월계·공릉·하계·중계·상계 권역의 19개 동주민센터로 구성되며, 이사해드림은 노원구와 이 다섯 승인 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("월계동", "wolgye-dong",
                        "월계동은 노원구 공식 안내에서 세 동주민센터가 담당하지만 검색용 지역명은 사용자가 승인한 월계동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("공릉동", "gongneung-dong",
                        "공릉동은 노원구 공식 안내에서 두 동주민센터가 담당하지만 검색용 지역명은 사용자가 승인한 공릉동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("하계동", "hagye-dong",
                        "하계동은 노원구 공식 안내에서 두 동주민센터가 담당하지만 검색용 지역명은 사용자가 승인한 하계동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("상계동", "sanggye-dong",
                        "상계동은 노원구 공식 안내에서 여러 동주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 상계동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("중계동", "junggye-dong",
                        "중계동은 노원구 공식 안내에서 본동을 포함한 여러 동주민센터가 담당하지만 검색용 지역명은 사용자가 승인한 중계동 하나로만 관리합니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
