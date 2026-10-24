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

/** 도봉구 승인 키워드 30개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(130)
public class DobongDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public DobongDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("dobong-gu").orElseThrow();
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
        String sharedAccess = "도봉구는 산지와 하천 인접 지역, 공동주택과 다세대·일반주택이 함께 분포하므로, 이사 차량 진입과 주차 위치, 도로 폭, 경사, 엘리베이터와 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("도봉구", null,
                        "도봉구 공식 현황에서는 녹지지역과 주거지역이 큰 비중을 차지하며, 이사해드림은 사용자가 승인한 도봉구와 4개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("쌍문동", "ssangmun-dong",
                        "쌍문동은 주택가와 공동주택의 진입 도로, 정차 공간과 건물 운반 조건을 주소별로 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("방학동", "banghak-dong",
                        "방학동은 도봉구청 소재지를 포함하며 산지 인접 주택가와 공동주택의 작업 조건을 현장별로 살펴야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("창동", "chang-dong",
                        "창동은 철도와 간선도로 주변, 공동주택 단지와 주택가의 차량 접근 조건을 각각 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("도봉동", "dobong-dong",
                        "도봉동은 도봉산과 중랑천 방향의 지형 차이를 고려해 경사와 진입로, 주차 위치를 확인해야 하는 승인 서비스 지역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
