package kr.co.isahaedream.cms.service;

import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.LandingContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LandingPageService {
    private final LandingContentRepository repository;

    public LandingPageService(LandingContentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LandingPageView view(Region region, Neighborhood neighborhood, MovingService service) {
        return buildView(region, neighborhood, service, false);
    }

    /** 관리자 검수 화면에서만 비공개 초안을 포함해 보여줍니다. */
    @Transactional(readOnly = true)
    public LandingPageView preview(Region region, Neighborhood neighborhood, MovingService service) {
        return buildView(region, neighborhood, service, true);
    }

    private LandingPageView buildView(Region region, Neighborhood neighborhood, MovingService service,
                                      boolean includeDraft) {
        Optional<LandingContent> custom = neighborhood == null
                ? repository.findByRegionAndNeighborhoodIsNullAndMovingService(region, service)
                : repository.findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service);

        String area = neighborhood == null ? region.getName() : neighborhood.getName();
        String title = area + " " + service.getName() + " | 이사해드림";
        String headline = area + " " + service.getName() + ", 이사해드림과 상담하세요";
        String description = area + " 지역의 " + service.getName() + " 상담 안내입니다. 이사 일정과 현장 여건을 확인해 필요한 차량과 작업 방식을 안내합니다.";
        String introduction = area + "의 건물 형태와 이동 동선을 고려해 상담합니다. 정확한 작업 범위는 전화 또는 카카오톡 상담 후 결정됩니다.";
        String body = "이사 날짜, 출발지와 도착지, 짐의 양, 엘리베이터와 주차 가능 여부를 알려주시면 상담이 더 빠릅니다.\n\n" +
                "과장된 최저가보다 필요한 작업 범위를 먼저 확인하고, 현장 조건에 맞는 방법을 안내합니다.";

        if (custom.isPresent() && (custom.get().isPublished() || includeDraft)) {
            LandingContent c = custom.get();
            title = valueOr(c.getSeoTitle(), title);
            headline = valueOr(c.getHeadline(), headline);
            description = valueOr(c.getMetaDescription(), description);
            introduction = valueOr(c.getIntroduction(), introduction);
            body = valueOr(c.getBody(), body);
        }
        boolean publishedPublicly = !includeDraft && custom.map(LandingContent::isPublished).orElse(false);
        return new LandingPageView(title, description, headline, introduction, body, publishedPublicly);
    }

    @Transactional
    public LandingContent save(Region region, Neighborhood neighborhood, MovingService service,
                               String seoTitle, String metaDescription, String headline,
                               String introduction, String body, boolean published) {
        LandingContent content = (neighborhood == null
                ? repository.findByRegionAndNeighborhoodIsNullAndMovingService(region, service)
                : repository.findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service))
                .orElseGet(() -> new LandingContent(region, neighborhood, service));
        String cleanSeoTitle = trimToNull(seoTitle);
        String cleanMetaDescription = trimToNull(metaDescription);
        String cleanHeadline = trimToNull(headline);
        String cleanIntroduction = trimToNull(introduction);
        String cleanBody = trimToNull(body);

        if (published) {
            List<String> problems = qualityProblems(cleanSeoTitle, cleanMetaDescription, cleanHeadline,
                    cleanIntroduction, cleanBody);
            if (!problems.isEmpty()) {
                throw new IllegalArgumentException("공개할 수 없습니다: " + String.join(", ", problems));
            }
        }

        content.setSeoTitle(cleanSeoTitle);
        content.setMetaDescription(cleanMetaDescription);
        content.setHeadline(cleanHeadline);
        content.setIntroduction(cleanIntroduction);
        content.setBody(cleanBody);
        content.setPublished(published);
        return repository.save(content);
    }

    @Transactional(readOnly = true)
    public Optional<LandingContent> find(Region region, Neighborhood neighborhood, MovingService service) {
        return neighborhood == null
                ? repository.findByRegionAndNeighborhoodIsNullAndMovingService(region, service)
                : repository.findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service);
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static List<String> qualityProblems(String seoTitle, String metaDescription, String headline,
                                               String introduction, String body) {
        var problems = new java.util.ArrayList<String>();
        requireLength(problems, "SEO 제목 10자 이상", seoTitle, 10);
        requireLength(problems, "검색 설명 40자 이상", metaDescription, 40);
        requireLength(problems, "페이지 제목 10자 이상", headline, 10);
        requireLength(problems, "소개 문구 80자 이상", introduction, 80);
        requireLength(problems, "상세 본문 300자 이상", body, 300);
        return List.copyOf(problems);
    }

    private static void requireLength(List<String> problems, String label, String value, int minimum) {
        if (value == null || value.length() < minimum) problems.add(label);
    }

    public record LandingPageView(String title, String description, String headline,
                                  String introduction, String body, boolean customPublished) {}
}
