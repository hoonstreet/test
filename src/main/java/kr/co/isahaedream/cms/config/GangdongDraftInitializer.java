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

/** 강동구 승인 키워드 60개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(90)
public class GangdongDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public GangdongDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("gangdong-gu").orElseThrow();
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
        String sharedAccess = "강동구 공식 생활권 안내에서도 여러 권역으로 나누어 검토하는 만큼, 공동주택과 주택가의 차량 진입, 상하차 위치, 주차장 높이, 경사와 엘리베이터 조건을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("강동구", null,
                        "강동구는 공식 주민센터 안내에서 19개 행정동을 운영하며, 이사해드림은 강동구와 사용자가 승인한 9개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("길동", "gil-dong",
                        "길동은 강동구 공식 주민센터 안내에서 같은 이름의 주민센터가 운영되는 승인 서비스 지역이며 길동·둔촌 생활권에 포함됩니다.", sharedAccess),
                new AreaProfile("고덕동", "godeok-dong",
                        "고덕동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 고덕동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("상일동", "sangil-dong",
                        "상일동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 상일동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("천호동", "cheonho-dong",
                        "천호동은 공식 행정 안내에서 세 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 천호동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("강일동", "gangil-dong",
                        "강일동은 강동구 공식 주민센터 안내에서 같은 이름의 주민센터가 운영되며 강일·상일 생활권에 포함되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("성내동", "seongnae-dong",
                        "성내동은 공식 행정 안내에서 세 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 성내동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("암사동", "amsa-dong",
                        "암사동은 공식 행정 안내에서 세 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 암사동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("둔촌동", "dunchon-dong",
                        "둔촌동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 둔촌동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("명일동", "myeongil-dong",
                        "명일동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 명일동 하나로만 관리합니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
