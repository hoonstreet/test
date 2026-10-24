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

/** 광진구 승인 키워드 48개 중 중곡동 견본 6개를 제외한 42개 비공개 초안을 준비합니다. */
@Component
@Order(30)
public class GwangjinDraftInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public GwangjinDraftInitializer(RegionRepository regionRepository,
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
        Region region = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
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
            Draft draft = draft(profile, service.getSlug(), service.getName());
            landingPageService.save(region, neighborhood, service, draft.seoTitle(), draft.metaDescription(),
                    draft.headline(), draft.introduction(), draft.body(), false);
        }
    }

    private static List<AreaProfile> profiles() {
        return List.of(
                new AreaProfile("광진구", null,
                        "광진구는 서울시 생활권계획에서 중곡, 화양·능동, 자양, 구의 생활권으로 나누어 살펴보는 지역입니다.",
                        "같은 구 안에서도 건물 형태와 차량 접근 조건이 달라질 수 있어 출발지와 도착지의 주소별 확인이 필요합니다."),
                new AreaProfile("구의동", "guui-dong",
                        "구의동은 서울시 생활권계획에서 구의 생활권으로 다뤄지고, 광진구 공식 안내에는 구의1동부터 구의3동까지 주민센터가 구분되어 있습니다.",
                        "세부 위치에 따라 공동 출입구, 엘리베이터 예약, 건물 앞 상하차 가능 지점을 각각 확인해야 합니다."),
                new AreaProfile("자양동", "jayang-dong",
                        "자양동은 서울시 생활권계획에서 자양 생활권으로 다뤄지고, 광진구 공식 안내에는 자양1동부터 자양4동까지 주민센터가 구분되어 있습니다.",
                        "출발지와 도착지의 관리 규정, 주차장 진입 높이, 도로변 정차 가능 여부를 이사 전에 따로 확인하는 것이 좋습니다."),
                new AreaProfile("군자동", "gunja-dong",
                        "군자동은 광진구가 공식 주민센터를 운영하는 승인 지역이며 생활도로와 간선도로가 만나는 위치에 따라 접근 방식이 달라질 수 있습니다.",
                        "건물 앞 작업 공간과 차량 통행을 방해하지 않는 상하차 시간, 계단과 엘리베이터 운반 조건을 함께 확인합니다."),
                new AreaProfile("능동", "neung-dong",
                        "능동은 서울시 생활권계획에서 화양·능동 생활권으로 함께 다뤄지고 광진구가 단일 행정동 주민센터를 운영하는 지역입니다.",
                        "주소별 골목 폭과 주차 위치, 저층 건물의 계단 폭, 공동주택의 엘리베이터 사용 조건을 현장 기준으로 확인합니다."),
                new AreaProfile("광장동", "gwangjang-dong",
                        "광장동은 광진구가 공식 주민센터를 운영하는 단일 행정동이며 서울시 생활권계획에서는 구의 생활권과 함께 검토됩니다.",
                        "단지와 건물마다 차량 출입 절차와 작업 가능 시간이 다를 수 있으므로 관리 주체의 안내를 먼저 확인합니다."),
                new AreaProfile("화양동", "hwayang-dong",
                        "화양동은 서울시 생활권계획에서 화양·능동 생활권으로 다뤄지고 광진구가 단일 행정동 주민센터를 운영하는 지역입니다.",
                        "주거 건물과 근린시설이 이어지는 구간에서는 보행과 영업에 방해되지 않는 차량 위치와 작업 시간을 확인해야 합니다.")
        );
    }

    private static Draft draft(AreaProfile area, String serviceSlug, String serviceName) {
        String seoTitle = area.name() + " " + serviceName + " 준비 안내 | 이사해드림";
        String metaDescription = area.name() + " " + serviceName
                + " 상담 전 차량 접근, 주차 위치, 층수와 엘리베이터, 작업 범위와 일정을 확인하는 방법을 안내합니다.";
        return switch (serviceSlug) {
            case "packing-move" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 포장이사, 포장 범위와 운반 동선을 맞춥니다",
                    area.name() + " 포장이사는 짐의 양뿐 아니라 건물 출입 조건과 고객이 원하는 포장 범위를 함께 확인해야 합니다. 큰 가구와 가전, 계단과 엘리베이터, 차량을 세울 위치를 미리 알려주시면 필요한 인원과 작업 순서를 구체적으로 상담할 수 있습니다.",
                    common(area) + """

                    포장이사 상담에서는 방과 주방, 베란다의 짐을 사진으로 확인하고 장롱·침대·냉장고처럼 분해나 별도 보호가 필요한 물품을 구분합니다. 고객이 직접 포장할 물품과 업체가 포장할 물품, 버릴 물건과 새집으로 가져갈 물건을 나누면 견적 조건을 비교하기 쉽습니다.

                    사다리차 사용은 층수만으로 정하지 않습니다. 작업 창의 방향, 전선과 가로수, 차량 통행 공간, 건물 또는 단지 규정을 확인해야 합니다. 사용할 수 없다면 계단이나 엘리베이터 운반에 필요한 인원과 시간을 다시 계산합니다. 계약 전에는 차량과 인원, 포장 범위, 추가될 수 있는 작업을 견적서에서 확인합니다.
                    """);
            case "storage-move" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 보관이사, 반출일과 재입고일을 나눠 준비합니다",
                    area.name() + " 보관이사는 퇴거일과 입주일 사이의 기간뿐 아니라 짐을 싣는 날과 다시 들이는 날의 조건을 각각 확인해야 합니다. 보관 기간, 물품 특성, 두 장소의 층수와 차량 접근 방법을 나누어 상담합니다.",
                    common(area) + """

                    보관이사 견적은 첫 반출 작업, 보관 장소까지의 운송, 보관 기간, 새집으로 옮기는 두 번째 운송을 구분해 확인합니다. 입주 일정이 바뀔 가능성이 있다면 연장 단위와 재입고 날짜 변경 조건도 계약 전에 살펴보세요.

                    냉장고와 세탁기는 내용물과 물기를 정리하고 충분히 건조해야 합니다. 침구·의류·책·서류처럼 습기에 민감한 짐은 포장 방식과 보관 환경을 확인합니다. 현금, 귀중품, 중요 문서와 의약품은 고객이 직접 관리할 목록으로 분리하고, 보관품 목록과 사진을 남겨 두면 재입고 때 확인하기 좋습니다.
                    """);
            case "move-in" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 입주이사, 새집의 작업 순서를 먼저 정합니다",
                    area.name() + " 입주이사는 짐을 옮기는 속도보다 새집의 입주 가능 시간과 작업 순서를 맞추는 일이 중요합니다. 엘리베이터 예약, 바닥 보호, 큰 가구 배치와 가전 설치 일정을 먼저 확인합니다.",
                    common(area) + """

                    새집의 방별 가구 배치를 간단한 메모나 사진으로 준비해 주세요. 침대·장롱·소파·냉장고처럼 큰 물품은 문과 통로를 통과할 수 있는지 확인하고 어느 방에 둘지 표시하면 불필요한 재이동을 줄일 수 있습니다. 별도 설치 기사가 방문하는 가전은 이사 작업과 시간이 겹치지 않게 조정합니다.

                    공동주택은 관리사무소나 관리인에게 이사 가능 시간, 엘리베이터 사용 방법, 보양재 기준과 차량 위치를 확인합니다. 저층 건물은 계단 폭과 현관 진입 조건을 살펴봅니다. 작업 후에는 가구 위치, 가전 외관, 남은 포장재와 차량에 남은 짐이 없는지 함께 점검합니다.
                    """);
            case "office-move" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 사무실이사, 업무 재개 시간부터 계획합니다",
                    area.name() + " 사무실이사는 정상 업무를 다시 시작할 시간을 먼저 정하고 집기, 전산 장비, 중요 문서와 폐기 물품을 구분해야 합니다. 출발지와 새 사무실의 작업 가능 시간도 각각 확인합니다.",
                    common(area) + """

                    새 사무실의 좌석 배치와 통신·전기 공사 일정을 먼저 확인합니다. 책상, 의자, 서랍, 모니터에 같은 번호를 붙이고 새 자리 도면에도 표시하면 배치 시간을 줄일 수 있습니다. 서버와 저장장치, 계약서와 개인정보 문서는 담당자가 직접 인계하거나 별도 관리 방법을 정합니다.

                    양쪽 건물의 화물 엘리베이터, 출입 등록, 보양 작업, 주차장 높이와 상하차 구역을 따로 확인해야 합니다. 상담 전에는 사무실 면적만 전달하지 말고 집기 수량, 복합기와 금고 같은 중량물, 컴퓨터 수량과 폐기 물품 목록을 준비해 주세요. 업무 재개에 필요한 장비는 먼저 풀 수 있도록 표시합니다.
                    """);
            case "apartment-move" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 아파트이사, 단지 규정부터 확인합니다",
                    area.name() + " 아파트이사는 단지마다 엘리베이터 예약, 보양 기준과 차량 출입 절차가 다를 수 있습니다. 출발지와 도착지 관리사무소의 안내를 각각 확인한 뒤 운반 방법을 정합니다.",
                    common(area) + """

                    양쪽 단지의 이사 예약 여부, 엘리베이터 사용 방법, 차량 출입 절차와 작업 가능 시간을 확인하세요. 이삿짐 차량이 동 앞까지 들어갈 수 있는지, 지하주차장을 이용해야 하는지, 높이 제한이 있는지도 필요합니다. 사다리차는 작업 창 방향과 주변 공간, 단지 규정을 함께 보고 결정합니다.

                    실내에서는 냉장고장, 세탁실, 현관 중문처럼 큰 물품이 지나갈 공간을 미리 확인합니다. 붙박이 가전의 분리와 재설치, 돌침대나 대형 식탁처럼 별도 장비가 필요한 짐도 상담 때 알려주세요. 입주 청소나 가전 설치와 이사 시간이 겹치지 않도록 순서를 정하고, 작업 후 공용부와 포장 폐기물 정리 상태를 확인합니다.
                    """);
            case "moving-center" -> new Draft(seoTitle, metaDescription,
                    area.name() + " 이삿짐센터, 같은 작업 조건으로 비교합니다",
                    area.name() + " 이삿짐센터를 선택할 때는 총액만 비교하지 말고 차량, 인원, 포장 범위, 계단이나 사다리차 작업 등 같은 조건이 포함됐는지 확인해야 합니다.",
                    common(area) + """

                    상담 전 출발지와 도착지 주소, 날짜, 층수, 엘리베이터 유무를 정리해 주세요. 거실과 각 방, 주방과 베란다 사진을 준비하고 큰 가구와 가전 수량을 알려주면 견적 조건을 맞추기 쉽습니다. 버릴 물건은 운반 물량과 분리하고 폐기 방법과 비용은 관할 기준에 따라 별도로 확인합니다.

                    견적서에는 차량 크기와 대수, 작업 인원, 포장과 정리 범위, 계단 또는 사다리차 작업, 벽걸이 제품의 분리·설치 포함 여부를 구분합니다. 현장에서 비용이 달라질 수 있는 조건과 결제 방법, 파손 발생 시 처리 절차도 문서로 확인하세요. 주소별 접근 조건은 사진이나 방문 확인을 거친 뒤 확정하는 것이 안전합니다.
                    """);
            default -> throw new IllegalArgumentException("승인되지 않은 서비스입니다: " + serviceSlug);
        };
    }

    private static String common(AreaProfile area) {
        return area.context() + " " + area.accessFocus();
    }

    private record AreaProfile(String name, String slug, String context, String accessFocus) {}
    private record Draft(String seoTitle, String metaDescription, String headline,
                         String introduction, String body) {}
}
