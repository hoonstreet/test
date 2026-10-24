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

/** 의정부 승인 키워드 54개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(120)
public class UijeongbuDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public UijeongbuDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("uijeongbu").orElseThrow();
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
        String sharedAccess = "의정부는 공동주택 밀집지역과 주택가, 산지 인접 지역의 도로 조건이 서로 다를 수 있으므로, 이사 차량 진입과 주차 위치, 도로 폭, 경사, 엘리베이터와 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("의정부", null,
                        "이사해드림은 행정구역의 세부 분류와 관계없이 사용자가 승인한 의정부와 8개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("호원동", "howon-dong",
                        "호원동은 공동주택과 주택가의 차량 접근 조건을 출발지와 도착지 주소별로 살펴야 하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("신곡동", "singok-dong",
                        "신곡동은 단지별 차량 진입과 엘리베이터 사용 기준, 주택가 도로 폭을 함께 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("자금동", "jageum-dong",
                        "자금동은 주소에 따라 주거 형태와 접근 도로가 달라질 수 있어 현장 조건을 먼저 확인하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("녹양동", "nogyang-dong",
                        "녹양동은 의정부 북쪽에서 도시와 외곽 지역의 성격이 함께 나타나는 것으로 공식 안내되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("가능동", "ganeung-dong",
                        "가능동은 주택가와 간선도로 주변의 정차·상하차 조건을 실제 주소로 확인해야 하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("송산동", "songsan-dong",
                        "송산동은 행정 구분과 관계없이 사용자가 고정 승인한 지역명이며 공동주택과 주택가의 작업 조건을 주소별로 확인합니다.", sharedAccess),
                new AreaProfile("장암동", "jangam-dong",
                        "장암동은 산지와 간선도로, 공동주택 주변의 차량 접근 조건을 현장별로 살펴야 하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("의정부동", "uijeongbu-dong",
                        "의정부동은 상업·업무시설과 주거지가 함께 있는 도심 환경을 고려해 차량 정차와 건물 작업 시간을 확인해야 하는 승인 지역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
