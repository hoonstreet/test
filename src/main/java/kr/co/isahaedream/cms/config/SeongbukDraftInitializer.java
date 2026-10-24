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

/** 성북구 승인 키워드 96개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(150)
public class SeongbukDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public SeongbukDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("seongbuk-gu").orElseThrow();
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
        String sharedAccess = "성북구는 구릉지 주택가와 골목, 공동주택 단지와 대학·상업시설 주변의 조건이 서로 다르므로, 이사 차량 진입과 주차 위치, 도로 폭, 경사, 엘리베이터와 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("성북구", null,
                        "이사해드림은 행정구역의 세부 분류와 관계없이 사용자가 승인한 성북구와 15개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("성북동", "seongbuk-dong",
                        "성북동은 구릉지 주택가와 골목의 경사, 차량 진입과 정차 가능 위치를 주소별로 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("돈암동", "donam-dong",
                        "돈암동은 공동주택과 주택가, 간선도로 주변의 운반 동선과 엘리베이터 조건을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("동소문동", "dongsomun-dong",
                        "동소문동은 상업시설과 주택이 함께 있는 도심 환경에서 차량 정차와 건물 진입 시간을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("삼선동", "samseon-dong",
                        "삼선동은 대학가와 주택가, 경사진 골목의 차량 접근과 상하차 조건을 주소별로 살펴야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("동선동", "dongseon-dong",
                        "동선동은 상가와 주거지가 함께 있는 지역으로 작업 차량 위치와 건물별 운반 동선을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("안암동", "anam-dong",
                        "안암동은 대학과 의료시설 주변, 주택가의 교통과 주차 조건을 고려해 작업 시간을 정해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("보문동", "bomun-dong",
                        "보문동은 주택가와 간선도로 주변의 차량 진입, 정차 위치와 건물 계단 조건을 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("정릉동", "jeongneung-dong",
                        "정릉동은 북한산 방향의 경사진 주택가와 공동주택 단지 조건을 주소별로 점검해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("길음동", "gireum-dong",
                        "길음동은 대규모 공동주택과 기존 주택가의 엘리베이터 예약, 차량 진입 조건을 각각 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("종암동", "jongam-dong",
                        "종암동은 공동주택과 주택가, 간선도로 주변의 상하차 위치와 건물 진입 조건을 살펴야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("하월곡동", "hawolgok-dong",
                        "하월곡동은 사용자가 별도로 승인한 지역명으로 주택가와 공동주택의 도로·주차 조건을 주소별로 확인합니다.", sharedAccess),
                new AreaProfile("상월곡동", "sangwolgok-dong",
                        "상월곡동은 사용자가 별도로 승인한 지역명으로 경사와 골목 폭, 차량 정차 위치를 현장별로 확인합니다.", sharedAccess),
                new AreaProfile("장위동", "jangwi-dong",
                        "장위동은 정비사업 지역과 기존 주택가, 공동주택의 접근 조건이 달라 현장 확인이 필요한 승인 지역입니다.", sharedAccess),
                new AreaProfile("석관동", "seokgwan-dong",
                        "석관동은 철도와 간선도로 주변, 주택가의 차량 진입과 상하차 동선을 주소별로 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("월곡동", "wolgok-dong",
                        "월곡동은 하월곡동·상월곡동과 별개로 사용자가 승인한 지역명이며 실제 주소를 기준으로 작업 조건을 확인합니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
