package kr.co.isahaedream.cms.controller;

import kr.co.isahaedream.cms.config.ApprovedLocationCatalog;
import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class SeoController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final LandingContentRepository landingContentRepository;
    private final String baseUrl;
    private final boolean indexingEnabled;

    public SeoController(RegionRepository regionRepository,
                         NeighborhoodRepository neighborhoodRepository,
                         LandingContentRepository landingContentRepository,
                         @Value("${app.base-url}") String baseUrl,
                         @Value("${app.search-index-enabled}") boolean indexingEnabled) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.landingContentRepository = landingContentRepository;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.indexingEnabled = indexingEnabled;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return indexingEnabled
                ? "User-agent: *\nAllow: /\nSitemap: " + baseUrl + "/sitemap.xml\n"
                : "User-agent: *\nDisallow: /\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        var publishedContents = landingContentRepository.findAllByPublishedTrue();
        Set<Long> publishedNeighborhoodIds = publishedContents.stream()
                .filter(content -> content.getNeighborhood() != null)
                .filter(content -> ApprovedLocationCatalog.isApprovedNeighborhood(
                        content.getRegion().getSlug(), content.getNeighborhood().getSlug()))
                .map(content -> content.getNeighborhood().getId())
                .collect(Collectors.toSet());
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                .append(url("/"));
        for (Region region : regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc()) {
            if (!ApprovedLocationCatalog.isApprovedRegion(region.getSlug())) continue;
            xml.append(url("/regions/" + region.getSlug()));
            for (Neighborhood neighborhood : neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region)) {
                if (ApprovedLocationCatalog.isApprovedNeighborhood(region.getSlug(), neighborhood.getSlug())
                        && publishedNeighborhoodIds.contains(neighborhood.getId())) {
                    xml.append(url("/regions/" + region.getSlug() + "/" + neighborhood.getSlug()));
                }
            }
        }
        for (LandingContent content : publishedContents) {
            if (!ApprovedLocationCatalog.isApprovedRegion(content.getRegion().getSlug())) continue;
            if (content.getNeighborhood() != null && !ApprovedLocationCatalog.isApprovedNeighborhood(
                    content.getRegion().getSlug(), content.getNeighborhood().getSlug())) continue;
            String path = "/regions/" + content.getRegion().getSlug() + "/";
            if (content.getNeighborhood() != null) {
                path += content.getNeighborhood().getSlug() + "/";
            }
            path += content.getMovingService().getSlug();
            xml.append(url(path));
        }
        return xml.append("</urlset>").toString();
    }

    private String url(String path) {
        return "<url><loc>" + baseUrl + path + "</loc></url>";
    }
}
