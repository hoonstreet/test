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

/** 강북구 승인 키워드 60개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(140)
public class GangbukDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public GangbukDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("gangbuk-gu").orElseThrow();
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
        String sharedAccess = "강북구는 북한산 인접 지역과 경사진 주택가, 공동주택과 간선도로 주변의 조건이 서로 다르므로, 이사 차량 진입과 주차 위치, 도로 폭, 경사, 엘리베이터와 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("강북구", null,
                        "이사해드림은 행정구역의 세부 분류와 관계없이 사용자가 승인한 강북구와 9개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("번동", "beon-dong",
                        "번동은 공동주택과 주택가, 간선도로 주변의 차량 정차와 건물 진입 조건을 주소별로 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("미아동", "mia-dong",
                        "미아동은 경사진 주택가와 공동주택의 접근 방식이 달라 차량 위치와 운반 동선을 현장별로 살펴야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("수유동", "suyu-dong",
                        "수유동은 상업지역과 주택가, 북한산 방향의 지형 차이를 고려해 정차 공간과 경사를 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("우이동", "ui-dong",
                        "우이동은 공식 안내에서 북한산과 계곡에 인접한 주택지역으로 소개되며 경사와 차량 진입로 확인이 중요한 승인 지역입니다.", sharedAccess),
                new AreaProfile("삼양동", "samyang-dong",
                        "삼양동은 경사진 주택가와 골목, 공동주택의 운반 조건을 실제 주소로 확인해야 하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("송중동", "songjung-dong",
                        "송중동은 주택가와 공동주택 단지의 도로 폭, 주차 위치와 엘리베이터 조건을 함께 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("송천동", "songcheon-dong",
                        "송천동은 주소별 주거 형태와 경사, 차량 접근 조건을 확인한 뒤 작업 방법을 정해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("인수동", "insu-dong",
                        "인수동은 북한산 인접 지형과 주택가의 도로 조건을 고려해 차량 진입과 운반 동선을 점검해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("삼각산동", "samgaksan-dong",
                        "삼각산동은 공동주택 단지와 경사진 주택가의 엘리베이터·차량 동선을 주소별로 확인해야 하는 승인 서비스 지역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
