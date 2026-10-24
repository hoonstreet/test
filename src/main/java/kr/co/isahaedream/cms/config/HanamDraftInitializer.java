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

/** 하남시 승인 키워드 60개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(160)
public class HanamDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public HanamDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("hanam").orElseThrow();
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
        String sharedAccess = "하남시는 원도심 주택가와 신도시 공동주택, 산업·외곽 지역의 도로 조건이 서로 다르므로, 이사 차량 진입과 주차 위치, 지하주차장 높이, 엘리베이터 예약과 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("하남시", null,
                        "이사해드림은 행정구역의 세부 분류와 관계없이 사용자가 승인한 하남시와 9개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("하남", "hanam",
                        "하남은 상위 지역명 하남시와 별개로 사용자가 승인한 하위 검색 키워드이며, 실제 주소를 기준으로 작업 조건을 확인합니다.", sharedAccess),
                new AreaProfile("덕풍동", "deokpung-dong",
                        "덕풍동은 기존 주택가와 공동주택, 상업시설 주변의 차량 진입과 정차 위치를 주소별로 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("미사동", "misa-dong",
                        "미사동은 대규모 공동주택과 상가 주변의 지하주차장 높이, 엘리베이터 예약과 상하차 동선을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("하남시 위례동", "wirye-dong",
                        "위례동은 공동주택 단지별 차량 출입 방법과 주차 관리, 엘리베이터 예약 시간을 미리 점검해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("감일동", "gamil-dong",
                        "감일동은 신축 공동주택과 주택가의 관리실 예약, 지하주차장 진입과 작업 차량 대기 위치를 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("초이동", "choi-dong",
                        "초이동은 주택과 사업장, 외곽 도로 주변의 주소별 진입로와 대형 차량 회차 가능 여부를 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("감북동", "gambuk-dong",
                        "감북동은 주택가와 외곽 도로의 폭, 작업 차량 정차 위치와 짐 운반 거리를 실제 주소별로 점검해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("풍산동", "pungsan-dong",
                        "풍산동은 공동주택과 산업·상업 시설 주변의 차량 진입 시간, 주차 규정과 운반 동선을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("신장동", "sinjang-dong",
                        "신장동은 원도심 상가와 주택, 공동주택의 혼잡 시간대와 차량 정차 위치, 건물 진입 조건을 확인해야 하는 승인 지역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
