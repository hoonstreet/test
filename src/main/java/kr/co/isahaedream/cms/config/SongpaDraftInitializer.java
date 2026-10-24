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

/** 송파구 승인 키워드 90개를 모두 비공개 검수 초안으로 준비합니다. */
@Component
@Order(100)
public class SongpaDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public SongpaDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("songpa-gu").orElseThrow();
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
        String sharedAccess = "송파구 공식 생활권 안내처럼 대단지 공동주택, 주택가와 업무·상업지역의 조건이 서로 달라, 이사 차량 진입과 주차 위치, 지하주차장 높이, 엘리베이터 예약과 상하차 시간을 실제 주소별로 확인해야 합니다.";
        return List.of(
                new AreaProfile("송파구", null,
                        "송파구는 공식 안내에서 27개 동주민센터를 운영하며, 이사해드림은 송파구와 사용자가 승인한 14개 하위 지역명만 검색 키워드로 관리합니다.", sharedAccess),
                new AreaProfile("잠실동", "jamsil-dong",
                        "잠실동은 공식 행정 안내에서 본동과 여러 숫자 행정동으로 나뉘지만 검색용 지역명은 사용자가 승인한 잠실동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("신천동", "sincheon-dong",
                        "신천동은 송파구의 공식 주소와 지역 자료에서 확인되는 사용자의 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("풍납동", "pungnap-dong",
                        "풍납동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 풍납동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("송파동", "songpa-dong",
                        "송파동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 송파동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("석촌동", "seokchon-dong",
                        "석촌동은 송파구 공식 동주민센터와 석촌 생활권 자료에서 같은 이름으로 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("삼전동", "samjeon-dong",
                        "삼전동은 송파구 공식 동주민센터와 석촌 생활권 자료에서 같은 이름으로 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("가락동", "garak-dong",
                        "가락동은 공식 행정 안내에서 본동과 숫자 행정동으로 나뉘지만 검색용 지역명은 사용자가 승인한 가락동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("송파구 위례동", "wirye-dong",
                        "위례동은 송파구 공식 동주민센터와 문정·위례 생활권 자료에서 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("마천동", "macheon-dong",
                        "마천동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 마천동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("거여동", "geoyeo-dong",
                        "거여동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 거여동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("오금동", "ogeum-dong",
                        "오금동은 송파구 공식 동주민센터와 가락 생활권 자료에서 같은 이름으로 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("방이동", "bangi-dong",
                        "방이동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 방이동 하나로만 관리합니다.", sharedAccess),
                new AreaProfile("장지동", "jangji-dong",
                        "장지동은 송파구 공식 동주민센터와 문정·위례 생활권 자료에서 같은 이름으로 확인되는 승인 서비스 지역입니다.", sharedAccess),
                new AreaProfile("문정동", "munjeong-dong",
                        "문정동은 공식 행정 안내에서 두 주민센터가 나누어 담당하지만 검색용 지역명은 사용자가 승인한 문정동 하나로만 관리합니다.", sharedAccess)
        );
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
}
