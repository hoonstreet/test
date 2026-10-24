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

/** 구리 승인 키워드 54개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(60)
public class GuriDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public GuriDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("guri").orElseThrow();
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
        String sharedAccess = "구리시 공식 하천 자료에서 지역별 하천과 구간이 서로 다르게 확인되는 만큼, 이사 차량의 진입로와 주차 위치, 건물 경사, 엘리베이터 조건은 실제 주소를 기준으로 확인해야 합니다.";
        return List.of(
                new AreaProfile("구리", null,
                        "구리시는 8개 행정복지센터를 운영하며, 이사해드림은 사용자가 승인한 구리와 8개 하위 지역만 서비스 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("갈매동", "galmae-dong",
                        "갈매동은 구리시 공식 행정복지센터가 운영되는 지역이며 공식 소하천 현황에서도 갈매동 구간이 확인됩니다.", sharedAccess),
                new AreaProfile("사노동", "sano-dong",
                        "사노동은 구리시 공식 소하천 현황에 지역명이 기재된 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("인창동", "inchang-dong",
                        "인창동은 구리시 공식 행정복지센터가 운영되는 지역이며 공식 하천 현황에도 인창동 구간이 안내됩니다.", sharedAccess),
                new AreaProfile("교문동", "gyomun-dong",
                        "교문동은 공식 행정 안내에서 두 행정복지센터가 관할하는 권역이지만 검색용 지역명은 사용자가 승인한 교문동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("수택동", "sutaek-dong",
                        "수택동은 공식 행정 안내에서 세 행정복지센터가 관할하는 권역이지만 검색용 지역명은 사용자가 승인한 수택동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("아천동", "acheon-dong",
                        "아천동은 구리시 공식 소하천 현황에 지역명이 기재된 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("토평동", "topyeong-dong",
                        "토평동은 구리시 공식 소하천 현황에 지역명이 기재되어 있으며 주소별 현장 조건을 확인해야 하는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("동구동", "donggu-dong",
                        "동구동은 구리시 공식 행정복지센터가 운영되는 지역이며 공식 하천 현황에도 동구동 구간이 안내됩니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
