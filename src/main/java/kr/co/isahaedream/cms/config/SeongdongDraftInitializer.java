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

/** 성동구 승인 키워드 72개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(50)
public class SeongdongDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public SeongdongDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("seongdong-gu").orElseThrow();
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
        String sharedAccess = "성동구 공식 주차 안내도 권역을 나누어 제공하므로 같은 지역명 안에서도 차량 진입, 상하차 위치, 경사와 엘리베이터 조건을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("성동구", null,
                        "성동구는 서울시 생활권계획에서 성수·송정, 왕십리·행당·응봉, 마장·사근·용답, 금호·옥수 권역으로 나누어 검토되는 지역입니다.", sharedAccess),
                new AreaProfile("마장동", "majang-dong",
                        "마장동은 서울시 생활권계획의 마장·사근·용답 생활권에 포함되고 성동구 공식 행정구역 안내에서도 같은 이름의 법정동과 행정동으로 확인됩니다.", sharedAccess),
                new AreaProfile("금호동", "geumho-dong",
                        "금호동은 서울시 생활권계획의 금호·옥수 생활권에 포함되며 공식 행정구역에서는 금호동1가부터 금호동4가까지 여러 법정동으로 나뉩니다.", sharedAccess),
                new AreaProfile("용답동", "yongdap-dong",
                        "용답동은 서울시 생활권계획의 마장·사근·용답 생활권에 포함되고 성동구 공식 행정구역 안내에 등록된 승인 법정동입니다.", sharedAccess),
                new AreaProfile("응봉동", "eungbong-dong",
                        "응봉동은 서울시 생활권계획의 왕십리·행당·응봉 생활권에 포함되고 성동구 공식 행정구역 안내에서 같은 이름으로 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("홍익동", "hongik-dong",
                        "홍익동은 성동구 공식 행정구역 안내에서 왕십리도선동 행정구역에 포함되는 법정동으로 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("성수동", "seongsu-dong",
                        "성수동은 서울시 생활권계획의 성수·송정 생활권에 포함되며 공식 행정구역에서는 성수동1가와 성수동2가로 나뉘는 승인 지역입니다.", sharedAccess),
                new AreaProfile("행당동", "haengdang-dong",
                        "행당동은 서울시 생활권계획의 왕십리·행당·응봉 생활권에 포함되고 여러 행정동에 걸쳐 관리되는 승인 법정동입니다.", sharedAccess),
                new AreaProfile("옥수동", "oksu-dong",
                        "옥수동은 서울시 생활권계획의 금호·옥수 생활권에 포함되고 성동구 공식 행정구역 안내에서 같은 이름으로 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("송정동", "songjeong-dong",
                        "송정동은 서울시 생활권계획의 성수·송정 생활권에 포함되고 성동구 공식 행정구역 안내에서 확인되는 승인 법정동입니다.", sharedAccess),
                new AreaProfile("사근동", "sageun-dong",
                        "사근동은 서울시 생활권계획의 마장·사근·용답 생활권에 포함되고 성동구 공식 행정구역 안내에서 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("왕십리동", "wangsimni-dong",
                        "왕십리동은 사용자가 승인한 서비스 지역 키워드이며, 공식 행정 안내에서는 왕십리도선동과 왕십리제2동 등으로 구분해 관리되는 권역입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
