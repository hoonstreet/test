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

/** 동대문구 승인 키워드 72개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(110)
public class DongdaemunDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public DongdaemunDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("dongdaemun-gu").orElseThrow();
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
        String sharedAccess = "동대문구 공식 지역특성 자료처럼 철도·간선도로 주변, 시장과 상가, 단독주택과 공동주택이 함께 분포하므로, 이사 차량 진입과 주차 위치, 도로 폭, 경사, 엘리베이터와 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("동대문구", null,
                        "동대문구는 배봉산과 회기동 북쪽 산지를 제외하면 대체로 낮은 지형이며 중랑천·정릉천·청계천과 교통·상업·교육 시설이 어우러진 지역입니다. 검색 키워드는 사용자가 승인한 11개 하위 지역명만 사용합니다.", sharedAccess),
                new AreaProfile("용두동", "yongdu-dong",
                        "용두동은 지하철과 왕산로, 청량리역세권에 인접하고 일반주택과 상업시설이 함께 있는 것으로 안내되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("전농동", "jeonnong-dong",
                        "전농동은 동대문구 공식 지역특성에서 배봉산 주변 지형이 언급되는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("장안동", "jangan-dong",
                        "장안동은 동대문구 동쪽 생활권에 자리하며 주택가와 간선도로 주변 환경을 주소별로 살펴야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("회기동", "hoegi-dong",
                        "회기동은 동대문구 공식 기본현황에서 북쪽 산지가 언급되며 철도와 교육시설 주변 조건을 함께 확인해야 하는 승인 지역입니다.", sharedAccess),
                new AreaProfile("이문동", "imun-dong",
                        "이문동은 공식 지역 안내에서 단독주택과 공동주택이 함께 있고 중랑천과 동부간선도로에 인접한 것으로 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("용신동", "yongsin-dong",
                        "용신동은 행정구역 개편 여부와 관계없이 사용자가 고정 승인한 서비스 지역 키워드이며 다른 지역명으로 자동 변경하지 않습니다.", sharedAccess),
                new AreaProfile("휘경동", "hwigyeong-dong",
                        "휘경동은 공식 지역 안내에서 철도와 동부간선도로가 지나고 소규모 상가·원룸과 공동주택이 함께 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("청량리동", "cheongnyangni-dong",
                        "청량리동은 공식 안내에서 청량리역세권과 시장·상가, 아파트와 일반주택이 함께 있는 것으로 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("답십리동", "dapsimni-dong",
                        "답십리동은 주거지와 상업시설, 공동주택 단지의 작업 조건을 주소별로 확인해야 하는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("제기동", "jegi-dong",
                        "제기동은 시장과 주거지가 함께 있는 생활환경을 고려해 차량 진입과 상하차 위치를 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("신설동", "sinseol-dong",
                        "신설동은 지하철 환승과 업무시설·시장 주변 유동이 확인되는 사용자의 승인 서비스 지역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
