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

/** 남양주 승인 키워드 102개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(70)
public class NamyangjuDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public NamyangjuDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("namyangju").orElseThrow();
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
        String sharedAccess = "남양주시는 공식 통계상 면적이 넓고 읍·동별 면적과 생활권 구성이 서로 다르므로, 같은 지역명 안에서도 이사 차량 진입로, 주차 위치, 경사와 엘리베이터 조건을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("남양주", null,
                        "남양주시는 공식 현황에서 6읍·3면·7동과 8개 행정복지센터로 안내되며, 이사해드림은 사용자가 승인한 남양주와 16개 하위 지역만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("평내동", "pyeongnae-dong",
                        "평내동은 남양주시 공식 행정구역과 호평·평내 생활권에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("일패동", "ilpae-dong",
                        "일패동은 남양주시 양정동 행정복지센터가 안내하는 세 법정동 가운데 하나인 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("금곡동", "geumgok-dong",
                        "금곡동은 남양주시 공식 행정구역과 금곡·양정 생활권에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("삼패동", "sampae-dong",
                        "삼패동은 남양주시 양정동 행정복지센터가 안내하는 세 법정동 가운데 하나인 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("이패동", "ipae-dong",
                        "이패동은 남양주시 양정동 행정복지센터가 안내하는 세 법정동 가운데 하나이며 종합운동장 소재지로도 확인되는 승인 지역입니다.", sharedAccess),
                new AreaProfile("가운동", "gaun-dong",
                        "가운동은 남양주시 공식 지금·도농 재정비촉진지구 자료에서 확인되는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("다산동", "dasan-dong",
                        "다산동은 공식 행정 안내에서 두 행정동으로 나누어 관리되지만 검색용 지역명은 사용자가 승인한 다산동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("도농동", "donong-dong",
                        "도농동은 남양주시 공식 지금·도농 재정비촉진지구 자료에서 확인되는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("지금동", "jigeum-dong",
                        "지금동은 남양주시 공식 지금·도농 재정비촉진지구 자료와 공공시설 주소에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("호평동", "hopyeong-dong",
                        "호평동은 남양주시 공식 행정구역과 호평·평내 생활권에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("별내동", "byeollae-dong",
                        "별내동은 남양주시 공식 행정구역과 별내 생활권에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("진접읍", "jinjeop-eup",
                        "진접읍은 남양주시 공식 행정구역의 읍 지역이며 진접·오남 행정복지센터 생활권에 포함됩니다.", sharedAccess),
                new AreaProfile("와부읍", "wabu-eup",
                        "와부읍은 남양주시 공식 행정구역의 읍 지역이며 와부·조안 행정복지센터 생활권에 포함됩니다.", sharedAccess),
                new AreaProfile("진건읍", "jingeon-eup",
                        "진건읍은 남양주시 공식 행정구역의 읍 지역이며 진건·퇴계원 행정복지센터 생활권에 포함됩니다.", sharedAccess),
                new AreaProfile("화도읍", "hwado-eup",
                        "화도읍은 남양주시 공식 행정구역의 읍 지역이며 화도·수동 행정복지센터 생활권에 포함됩니다.", sharedAccess),
                new AreaProfile("오남읍", "onam-eup",
                        "오남읍은 남양주시 공식 행정구역의 읍 지역이며 진접·오남 행정복지센터 생활권에 포함됩니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
