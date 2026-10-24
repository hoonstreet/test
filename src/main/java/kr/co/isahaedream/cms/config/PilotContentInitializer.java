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

/**
 * 첫 콘텐츠 검수용 중곡동 6개 서비스 초안입니다.
 * 자동 공개하지 않으며 관리자가 확인하고 수정한 뒤에만 공개할 수 있습니다.
 * 지역 근거: 광진구 중곡1~4동 주민센터 안내와 광진구의회 주차환경 자료.
 */
@Component
@Order(20)
public class PilotContentInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public PilotContentInitializer(RegionRepository regionRepository,
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
        Neighborhood neighborhood = neighborhoodRepository
                .findByRegionAndSlugAndEnabledTrue(region, "junggok-dong").orElseThrow();

        for (PilotDraft draft : drafts()) {
            MovingService service = movingServiceRepository
                    .findBySlugAndEnabledTrue(draft.serviceSlug()).orElseThrow();
            if (landingPageService.find(region, neighborhood, service).isEmpty()) {
                landingPageService.save(region, neighborhood, service, draft.seoTitle(), draft.metaDescription(),
                        draft.headline(), draft.introduction(), draft.body(), false);
            }
        }
    }

    private static List<PilotDraft> drafts() {
        return List.of(
                new PilotDraft(
                        "packing-move",
                        "중곡동 포장이사 준비와 현장 확인 | 이사해드림",
                        "광진구 중곡동 포장이사를 준비할 때 차량 진입, 주차 위치, 계단과 엘리베이터, 포장 범위를 먼저 확인하는 상담 안내입니다.",
                        "중곡동 포장이사, 집 구조와 운반 동선부터 확인합니다",
                        "중곡동은 중곡1동부터 중곡4동까지 생활권이 이어지고 주택 형태와 골목 여건이 주소마다 다를 수 있습니다. 포장이사 상담에서는 짐의 양만 묻지 않고 출발지와 도착지의 층수, 엘리베이터 사용 여부, 차량을 세울 위치와 작업 가능한 시간을 함께 확인합니다.",
                        """
                        포장이사는 상자 수보다 실제 작업 범위를 정확히 나누는 일이 먼저입니다. 상담할 때 큰 가구와 가전의 종류, 붙박이장처럼 분해가 필요한 물품, 버릴 물건과 새집으로 가져갈 물건을 구분해 알려주세요. 냉장고 정리, 의류 포장, 주방용품 포장처럼 고객이 원하는 범위도 미리 정하면 당일 혼선을 줄일 수 있습니다.

                        중곡동에서는 같은 동 안에서도 건물 앞 도로 폭과 주차 가능 위치가 달라질 수 있습니다. 사다리차 사용 여부를 주소만으로 단정하지 않고 창문 방향, 전선과 가로수, 차량 통행, 관리 규정을 확인합니다. 사다리차를 쓰지 못하는 곳이라면 계단 또는 엘리베이터 운반에 필요한 인원과 시간을 다시 계산해야 합니다.

                        이사 날짜와 양쪽 주소, 층수, 엘리베이터 유무, 주차 가능 여부, 큰 짐 사진을 보내주시면 상담이 빨라집니다. 계약 전에는 차량 크기와 작업 인원, 포장 범위, 추가될 수 있는 작업을 견적서에서 확인하고 진행합니다.
                        """),
                new PilotDraft(
                        "storage-move",
                        "중곡동 보관이사 일정과 보관품 점검 | 이사해드림",
                        "중곡동 보관이사는 퇴거일과 입주일 사이의 기간, 보관할 짐의 특성, 두 번의 운송 동선을 나누어 확인하는 것이 중요합니다.",
                        "중곡동 보관이사, 두 번의 운송 일정을 따로 계획합니다",
                        "퇴거일과 새집 입주일이 맞지 않는 중곡동 보관이사는 짐을 싣는 날과 다시 들이는 날을 각각 준비해야 합니다. 보관 기간과 장소만 정하는 것이 아니라 습기에 민감한 물품, 냉장 가전, 서류와 귀중품처럼 별도 관리가 필요한 짐부터 구분합니다.",
                        """
                        보관이사 견적은 첫 번째 집에서 짐을 반출하는 작업, 보관 장소로 운송하는 작업, 보관 기간, 새집으로 다시 운송하고 배치하는 작업을 나누어 확인해야 합니다. 입주 날짜가 바뀔 가능성이 있다면 보관 연장 단위와 두 번째 운송 일정의 변경 조건도 계약 전에 확인하는 편이 안전합니다.

                        냉장고와 세탁기 같은 가전은 물기와 내부 내용물을 정리하고 충분히 건조해야 합니다. 침구와 의류, 책, 서류는 습기에 영향을 받을 수 있으므로 포장 방식과 보관 환경을 상담합니다. 현금, 귀금속, 중요 계약서, 의약품처럼 장기 보관에 적합하지 않은 물품은 고객이 직접 보관할 목록으로 분리하세요.

                        중곡동 출발지와 새집 도착지의 차량 접근 조건은 서로 다를 수 있습니다. 양쪽 주소의 층수, 엘리베이터 예약, 주차 위치를 각각 알려주시면 두 차례 작업에 필요한 차량과 인원을 따로 계산할 수 있습니다. 보관품 목록과 사진을 남겨두면 재입고할 때 확인하기도 수월합니다.
                        """),
                new PilotDraft(
                        "move-in",
                        "중곡동 입주이사 체크사항과 배치 준비 | 이사해드림",
                        "광진구 중곡동 새집 입주이사 전 엘리베이터 예약, 바닥 보호, 가구 배치, 가전 설치 순서를 확인하는 안내입니다.",
                        "중곡동 입주이사, 새집 작업 순서를 먼저 맞춥니다",
                        "중곡동 새집으로 들어가는 날에는 짐을 옮기는 속도만큼 작업 순서가 중요합니다. 입주 가능 시간, 엘리베이터 예약, 관리 주체의 안내, 바닥과 벽 보호 범위를 먼저 확인하고 큰 가구와 가전의 위치를 정해 두면 재이동을 줄일 수 있습니다.",
                        """
                        입주이사 전에는 새집의 방별 가구 배치를 간단한 메모나 사진으로 준비해 주세요. 침대, 장롱, 소파, 냉장고처럼 큰 물품은 통로 폭과 문 크기를 먼저 확인하고 어느 방에 둘지 표시하면 작업이 수월합니다. 설치 기사가 별도로 방문하는 정수기, 에어컨, 벽걸이 제품은 이사 작업과 시간이 겹치지 않도록 순서를 조정합니다.

                        공동주택이라면 관리사무소 또는 건물 관리인에게 이사 가능 시간, 엘리베이터 사용 방법, 보양재 기준, 차량 주차 위치를 확인해야 합니다. 저층 주택이나 빌라라면 계단 폭, 현관 진입, 건물 앞 정차 가능 여부를 살펴봅니다. 주소가 같아도 현장 조건에 따라 필요한 장비와 인원이 달라질 수 있습니다.

                        상담 시 입주 날짜와 가능 시간, 새집 층수, 엘리베이터 유무, 큰 가구의 크기와 배치 희망 위치를 알려주세요. 작업이 끝난 뒤에는 가구 흔들림, 가전 외관, 남은 포장재, 차량에 남은 짐이 없는지를 함께 확인합니다.
                        """),
                new PilotDraft(
                        "office-move",
                        "중곡동 사무실이사 업무 공백 줄이기 | 이사해드림",
                        "중곡동 사무실이사는 집기 목록, 전산 장비, 문서 보안, 건물 작업 시간과 차량 동선을 구분해 이전 계획을 세웁니다.",
                        "중곡동 사무실이사, 업무 재개 시점부터 역산합니다",
                        "중곡동 사무실이사는 책상과 의자를 옮기는 것에서 끝나지 않습니다. 정상 업무를 다시 시작할 시간을 먼저 정하고 부서별 집기, 전산 장비, 중요 문서, 폐기 예정 물품을 구분해 포장과 운송 순서를 설계합니다.",
                        """
                        이전 준비는 새 사무실의 좌석 배치와 통신·전기 공사 일정을 확인하는 것부터 시작합니다. 각 책상과 서랍, 의자, 모니터에 같은 번호를 붙이고 새 자리 도면에도 번호를 표시하면 현장에서 위치를 다시 묻는 시간을 줄일 수 있습니다. 서버, 저장장치, 계약서와 개인정보 문서는 담당자가 직접 인계하거나 별도 관리 방법을 정해야 합니다.

                        출발지와 도착지 건물에는 화물 엘리베이터 사용 시간, 출입 등록, 보양 작업, 주차장 높이, 상하차 가능 구역을 각각 확인합니다. 영업 중인 상가나 주거 건물과 함께 사용하는 공간이라면 소음과 통행에 영향을 덜 주는 작업 시간도 협의가 필요합니다.

                        상담 전 사무실 면적만 알려주기보다 책상·의자·캐비닛 수량, 복합기와 금고 같은 중량물, 컴퓨터 수량, 폐기 물품을 목록으로 준비해 주세요. 이전 당일 담당자와 최종 배치 확인자를 정하고, 업무 재개에 꼭 필요한 장비부터 먼저 풀 수 있도록 박스에 우선순위를 표시합니다.
                        """),
                new PilotDraft(
                        "apartment-move",
                        "중곡동 아파트이사 관리규정 확인 | 이사해드림",
                        "중곡동 아파트이사 전 관리사무소 예약, 엘리베이터와 사다리차 사용 조건, 주차와 보양 기준을 확인하는 상담 안내입니다.",
                        "중곡동 아파트이사, 단지 규정과 작업 동선을 함께 봅니다",
                        "중곡동 아파트이사는 단지와 동 위치에 따라 작업 차량의 진입, 지하주차장 높이, 엘리베이터 예약 방식이 달라질 수 있습니다. 이사 날짜를 정한 뒤 관리사무소에 가능한 시간과 보양 기준을 확인하고 출발지와 도착지를 각각 점검합니다.",
                        """
                        먼저 양쪽 단지의 이사 예약 여부와 엘리베이터 사용료, 보양재 설치 범위, 차량 출입 절차를 확인하세요. 이삿짐 차량이 동 앞까지 들어갈 수 있는지, 지하주차장을 이용해야 하는지, 출입 높이 제한이 있는지도 필요합니다. 사다리차는 층수만으로 결정하지 않고 작업 창의 방향과 주변 공간, 단지 규정을 함께 확인합니다.

                        아파트 내부에서는 냉장고장, 세탁실, 현관 중문처럼 큰 가전과 가구가 지나갈 공간을 미리 재는 것이 좋습니다. 붙박이 가전의 분리와 재설치가 필요한지, 돌침대나 대형 식탁처럼 별도 장비가 필요한 물품이 있는지도 상담 때 알려주세요. 바닥과 모서리 보호 범위도 견적서에서 확인합니다.

                        입주 시간과 청소·가전 설치 일정이 겹치면 작업 동선이 혼잡해질 수 있습니다. 가능하면 작업 순서를 나누고, 새집의 방별 배치표를 준비하세요. 작업 후에는 가구 위치, 가전 외관, 공용부 보양재와 포장 폐기물 정리 상태를 고객과 함께 점검합니다.
                        """),
                new PilotDraft(
                        "moving-center",
                        "중곡동 이삿짐센터 견적 비교 기준 | 이사해드림",
                        "중곡동 이삿짐센터 상담 시 차량과 인원, 포장 범위, 추가 작업, 포함·제외 항목을 같은 조건으로 비교하는 방법을 안내합니다.",
                        "중곡동 이삿짐센터, 같은 조건으로 견적을 비교하세요",
                        "중곡동 이삿짐센터를 알아볼 때는 총액만 비교하기보다 각 견적이 같은 작업을 포함하는지 확인해야 합니다. 짐의 양과 거리 외에도 층수, 계단과 엘리베이터, 주차 위치, 포장 범위가 달라지면 필요한 차량과 인원이 달라질 수 있습니다.",
                        """
                        상담을 요청하기 전에 출발지와 도착지 주소, 이사 날짜, 층수, 엘리베이터 유무를 정리해 주세요. 거실과 각 방, 주방, 베란다의 짐을 사진으로 보내고 장롱·침대·냉장고·세탁기처럼 큰 물품의 수량을 알려주면 견적 조건을 맞추기 쉽습니다. 버릴 물건은 운반 물량에서 분리하되 폐기 방법과 비용은 관할 기준에 따라 별도로 확인해야 합니다.

                        견적서에는 차량 크기와 대수, 작업 인원, 포장과 정리 범위, 사다리차 또는 계단 작업, 에어컨과 벽걸이 제품의 분리·설치 포함 여부를 구분해 적는 것이 좋습니다. 주말이나 손 없는 날처럼 일정에 따른 조건, 현장에서 추가 비용이 생길 수 있는 경우도 계약 전에 질문하세요.

                        중곡동은 주소별로 건물 앞 정차와 골목 진입 조건이 다를 수 있어 전화 설명만으로 확정하기 어려운 현장이 있습니다. 사진이나 방문 확인을 통해 작업 위치를 확인하고, 계약한 작업 내용과 결제 조건, 파손 발생 시 처리 절차를 문서로 남긴 뒤 업체를 선택하세요.
                        """)
        );
    }

    private record PilotDraft(String serviceSlug, String seoTitle, String metaDescription,
                              String headline, String introduction, String body) {}
}
