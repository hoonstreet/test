package kr.co.isahaedream.cms.service;

import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.LandingContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class LandingPageService {
    private final LandingContentRepository repository;

    public LandingPageService(LandingContentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LandingPageView view(Region region, Neighborhood neighborhood, MovingService service) {
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

        if (custom.isPresent() && custom.get().isPublished()) {
            LandingContent c = custom.get();
            title = valueOr(c.getSeoTitle(), title);
            headline = valueOr(c.getHeadline(), headline);
            description = valueOr(c.getMetaDescription(), description);
            introduction = valueOr(c.getIntroduction(), introduction);
            body = valueOr(c.getBody(), body);
        }
        return new LandingPageView(title, description, headline, introduction, body, custom.map(LandingContent::isPublished).orElse(false));
    }

    @Transactional
    public LandingContent save(Region region, Neighborhood neighborhood, MovingService service,
                               String seoTitle, String metaDescription, String headline,
                               String introduction, String body, boolean published) {
        LandingContent content = (neighborhood == null
                ? repository.findByRegionAndNeighborhoodIsNullAndMovingService(region, service)
                : repository.findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service))
                .orElseGet(() -> new LandingContent(region, neighborhood, service));
        content.setSeoTitle(trimToNull(seoTitle));
        content.setMetaDescription(trimToNull(metaDescription));
        content.setHeadline(trimToNull(headline));
        content.setIntroduction(trimToNull(introduction));
        content.setBody(trimToNull(body));
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

    public record LandingPageView(String title, String description, String headline,
                                  String introduction, String body, boolean customPublished) {}
}
