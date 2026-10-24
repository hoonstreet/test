package kr.co.isahaedream.cms.config;

/** 승인 지역 안에서만 비공개 검수 초안을 만드는 공통 문안 생성기입니다. */
public final class ApprovedDraftFactory {
    private ApprovedDraftFactory() {}

    public static Draft create(String areaName, String areaContext, String accessFocus,
                               String serviceSlug, String serviceName) {
        ServiceCopy copy = serviceCopy(serviceSlug);
        String seoTitle = areaName + " " + serviceName + " 준비 안내 | 이사해드림";
        String metaDescription = areaName + " " + serviceName
                + " 상담 전 차량 접근, 주차 위치, 층수와 엘리베이터, 작업 범위와 일정을 확인하는 방법을 안내합니다.";
        String headline = areaName + " " + serviceName + ", " + copy.headlineTail();
        String introduction = areaName + " " + serviceName + " 상담에서는 " + copy.introduction()
                + " 주소와 사진을 바탕으로 필요한 차량, 인원과 작업 순서를 확인합니다.";
        String body = areaContext + " " + accessFocus + "\n\n"
                + copy.guidance() + "\n\n"
                + areaName + " " + serviceName + " 견적을 요청할 때는 출발지와 도착지 주소, 날짜, 층수, "
                + "엘리베이터 유무와 큰 짐 목록을 같은 조건으로 전달하세요. 계약 전에는 차량과 인원, "
                + "포함 작업, 현장에서 달라질 수 있는 조건과 결제 방법을 문서로 확인합니다. 주소별 현장 조건을 "
                + "사진이나 방문으로 확인한 뒤 최종 작업 방법을 정하며, 확인되지 않은 최저가나 과장된 표현은 사용하지 않습니다.";
        return new Draft(seoTitle, metaDescription, headline, introduction, body);
    }

    private static ServiceCopy serviceCopy(String serviceSlug) {
        return switch (serviceSlug) {
            case "packing-move" -> new ServiceCopy(
                    "포장 범위와 운반 동선을 함께 맞춥니다",
                    "고객이 직접 포장할 물품과 업체가 포장할 물품, 분해가 필요한 큰 가구를 먼저 구분해야 합니다.",
                    "포장이사는 방과 주방, 베란다의 짐을 사진으로 확인하고 장롱·침대·냉장고처럼 별도 보호가 필요한 물품을 구분합니다. 버릴 물건과 새집으로 가져갈 물건을 나누면 작업량을 정확히 계산하기 쉽습니다. 사다리차는 층수만으로 결정하지 않고 창문 방향, 전선과 가로수, 차량 통행 공간과 건물 규정을 확인합니다. 사용할 수 없다면 계단 또는 엘리베이터 운반에 필요한 인원과 시간을 다시 계산합니다.");
            case "storage-move" -> new ServiceCopy(
                    "반출일과 재입고일을 따로 준비합니다",
                    "퇴거일과 입주일 사이의 기간, 보관품 특성과 두 차례 운송 조건을 각각 확인해야 합니다.",
                    "보관이사 견적은 첫 반출, 보관 장소까지의 운송, 보관 기간, 새집으로 옮기는 두 번째 운송을 구분합니다. 냉장고와 세탁기는 내용물과 물기를 정리하고 충분히 건조해야 하며 침구·의류·책·서류는 포장 방식과 보관 환경을 확인합니다. 현금, 귀중품, 중요 문서와 의약품은 고객이 직접 관리할 목록으로 분리하고 보관품 목록과 사진을 남겨 재입고 때 확인합니다.");
            case "move-in" -> new ServiceCopy(
                    "새집의 작업 순서를 먼저 정합니다",
                    "입주 가능 시간, 바닥 보호, 큰 가구 배치와 별도 가전 설치 일정을 미리 맞추는 것이 중요합니다.",
                    "입주이사 전에는 새집의 방별 가구 배치를 메모나 사진으로 준비합니다. 침대·장롱·소파·냉장고처럼 큰 물품은 문과 통로를 통과할 수 있는지 확인하고 위치를 표시해 재이동을 줄입니다. 공동주택은 이사 가능 시간, 엘리베이터 사용 방법, 보양재 기준과 차량 위치를 확인하고 저층 건물은 계단 폭과 현관 진입 조건을 살펴봅니다. 작업 후에는 가구 위치와 가전 외관, 남은 포장재를 함께 점검합니다.");
            case "office-move" -> new ServiceCopy(
                    "업무 재개 시간부터 역산해 계획합니다",
                    "집기, 전산 장비, 중요 문서와 폐기 물품을 구분하고 새 사무실 배치 순서를 정해야 합니다.",
                    "사무실이사는 좌석 배치와 통신·전기 공사 일정을 먼저 확인합니다. 책상, 의자, 서랍과 모니터에 번호를 붙이고 새 자리 도면에도 표시하면 배치 시간을 줄일 수 있습니다. 서버와 저장장치, 개인정보 문서는 담당자가 직접 인계하거나 별도 관리 방법을 정합니다. 양쪽 건물의 화물 엘리베이터, 출입 등록, 보양 작업, 주차장 높이와 상하차 구역을 각각 확인하고 업무 재개에 필요한 장비를 먼저 풀도록 표시합니다.");
            case "apartment-move" -> new ServiceCopy(
                    "단지 규정과 차량 동선을 먼저 확인합니다",
                    "출발지와 도착지의 엘리베이터 예약, 보양 기준과 차량 출입 절차를 각각 확인해야 합니다.",
                    "아파트이사는 양쪽 단지의 예약 여부, 엘리베이터 사용 방법, 차량 출입과 작업 가능 시간을 확인합니다. 차량이 동 앞까지 들어갈 수 있는지, 지하주차장을 이용해야 하는지, 높이 제한이 있는지도 살펴봅니다. 사다리차는 작업 창 방향과 주변 공간, 단지 규정을 함께 보고 결정합니다. 실내에서는 냉장고장, 세탁실과 현관 중문의 통과 폭을 확인하고 입주 청소나 가전 설치와 작업 시간이 겹치지 않도록 순서를 정합니다.");
            case "moving-center" -> new ServiceCopy(
                    "같은 작업 조건으로 견적을 비교합니다",
                    "총액만 보지 말고 차량, 인원, 포장 범위와 추가 작업이 같은 조건인지 확인해야 합니다.",
                    "이삿짐센터 상담 전에는 거실과 각 방, 주방과 베란다 사진을 준비하고 큰 가구와 가전 수량을 알려줍니다. 버릴 물건은 운반 물량과 분리하고 폐기 방법과 비용은 관할 기준에 따라 따로 확인합니다. 견적서에는 차량 크기와 대수, 작업 인원, 포장과 정리 범위, 계단 또는 사다리차 작업, 벽걸이 제품 분리·설치 포함 여부를 구분합니다. 비용이 달라질 수 있는 조건과 파손 발생 시 처리 절차도 문서로 확인합니다.");
            default -> throw new IllegalArgumentException("승인되지 않은 서비스입니다: " + serviceSlug);
        };
    }

    private record ServiceCopy(String headlineTail, String introduction, String guidance) {}
    public record Draft(String seoTitle, String metaDescription, String headline,
                        String introduction, String body) {}
}
