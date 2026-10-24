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

/** 중랑구 승인 키워드 42개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(40)
public class JungnangDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public JungnangDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("jungnang-gu").orElseThrow();
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
        String sharedAccess = "중랑구 공식 자료에서도 동별 여건이 구분되므로 같은 법정동 안에서도 건물 앞 주차, 골목 폭, 계단과 엘리베이터 조건을 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("중랑구", null,
                        "중랑구는 면목동, 상봉동, 중화동, 묵동, 망우동, 신내동의 승인된 6개 법정동으로 구성되고 서울시 생활권계획에서는 면목, 중화·묵동, 신내·망우 생활권으로 구분됩니다.", sharedAccess),
                new AreaProfile("면목동", "myeonmok-dong",
                        "면목동은 서울시 생활권계획에서 면목 생활권으로 구분되며, 공식 자료상 여러 행정동을 포함하는 하나의 승인 법정동입니다.", sharedAccess),
                new AreaProfile("묵동", "muk-dong",
                        "묵동은 서울시 생활권계획에서 중화·묵동 생활권으로 함께 검토되며, 공식 지역 목록에 포함된 승인 법정동입니다.", sharedAccess),
                new AreaProfile("신내동", "sinnae-dong",
                        "신내동은 서울시 생활권계획에서 신내·망우 생활권으로 함께 검토되며, 공식 지역 목록에 포함된 승인 법정동입니다.", sharedAccess),
                new AreaProfile("중화동", "junghwa-dong",
                        "중화동은 서울시 생활권계획에서 중화·묵동 생활권으로 함께 검토되며, 공식 지역 목록에 포함된 승인 법정동입니다.", sharedAccess),
                new AreaProfile("상봉동", "sangbong-dong",
                        "상봉동은 중랑구를 이루는 6개 승인 법정동 가운데 하나로, 세부 주소와 건물 관리 방식에 따라 이사 작업 조건을 따로 확인해야 하는 지역입니다.", sharedAccess),
                new AreaProfile("망우동", "mangu-dong",
                        "망우동은 서울시 생활권계획에서 신내·망우 생활권으로 함께 검토되며, 공식 지역 목록에 포함된 승인 법정동입니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
