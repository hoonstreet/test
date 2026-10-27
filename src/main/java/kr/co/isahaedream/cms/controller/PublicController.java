package kr.co.isahaedream.cms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.isahaedream.cms.config.ApprovedLocationCatalog;
import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import kr.co.isahaedream.cms.service.LandingPageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PublicController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingContentRepository landingContentRepository;
    private final LandingPageService landingPageService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public PublicController(RegionRepository regionRepository,
                            NeighborhoodRepository neighborhoodRepository,
                            MovingServiceRepository movingServiceRepository,
                            LandingContentRepository landingContentRepository,
                            LandingPageService landingPageService,
                            ObjectMapper objectMapper,
                            @Value("${app.base-url}") String baseUrl) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingContentRepository = landingContentRepository;
        this.landingPageService = landingPageService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/")
    public String home(Model model) {
        String pageTitle = "이사해드림 | 지역별 포장이사 상담";
        String pageDescription = "포장이사, 보관이사, 입주이사, 사무실이사, 아파트이사를 전화와 카카오톡으로 상담하세요.";
        String canonicalUrl = baseUrl + "/";
        model.addAttribute("regions", approvedRegions());
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("canonicalUrl", canonicalUrl);
        addImageSeo(model, pageTitle, pageDescription, canonicalUrl,
                baseUrl + "/images/services/packing-move.webp", null);
        return "home";
    }

    @GetMapping("/regions/{regionSlug}")
    public String region(@PathVariable String regionSlug, Model model) {
        Region region = region(regionSlug);
        List<MovingService> services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        String pageTitle = serviceKeywordTitle(region.getName(), services);
        String pageDescription = serviceKeywordDescription(region.getName(), services);
        String canonicalUrl = baseUrl + "/regions/" + region.getSlug();
        model.addAttribute("region", region);
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        model.addAttribute("services", services);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("canonicalUrl", canonicalUrl);
        addImageSeo(model, pageTitle, pageDescription, canonicalUrl,
                serviceImageUrl("packing-move"), serviceItems(region.getName(), canonicalUrl, services));
        return "region";
    }

    @GetMapping("/regions/{regionSlug}/{areaOrServiceSlug}")
    public String regionChild(@PathVariable String regionSlug,
                              @PathVariable String areaOrServiceSlug,
                              Model model) {
        Region region = region(regionSlug);
        return neighborhoodRepository.findByRegionAndSlugAndEnabledTrue(region, areaOrServiceSlug)
                .map(neighborhood -> neighborhoodHub(region, neighborhood, model))
                .orElseGet(() -> landing(region, null, service(areaOrServiceSlug), model));
    }

    @GetMapping("/regions/{regionSlug}/{neighborhoodSlug}/{serviceSlug}")
    public String neighborhoodService(@PathVariable String regionSlug,
                                      @PathVariable String neighborhoodSlug,
                                      @PathVariable String serviceSlug,
                                      Model model) {
        Region region = region(regionSlug);
        Neighborhood neighborhood = neighborhoodRepository.findByRegionAndSlugAndEnabledTrue(region, neighborhoodSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ApprovedLocationCatalog.isApprovedNeighborhood(regionSlug, neighborhoodSlug)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return landing(region, neighborhood, service(serviceSlug), model);
    }

    private String landing(Region region, Neighborhood neighborhood, MovingService service, Model model) {
        LandingPageService.LandingPageView content = landingPageService.view(region, neighborhood, service);
        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("movingService", service);
        model.addAttribute("content", content);
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        String path = "/regions/" + region.getSlug() + "/";
        if (neighborhood != null) path += neighborhood.getSlug() + "/";
        path += service.getSlug();
        String canonicalUrl = baseUrl + path;
        model.addAttribute("canonicalUrl", canonicalUrl);
        addImageSeo(model, content.title(), content.description(), canonicalUrl,
                serviceImageUrl(service.getSlug()), null);
        return "landing";
    }

    private String neighborhoodHub(Region region, Neighborhood neighborhood, Model model) {
        if (!ApprovedLocationCatalog.isApprovedNeighborhood(region.getSlug(), neighborhood.getSlug())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<MovingService> services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        String areaName = region.getName() + " " + neighborhood.getName();
        String pageTitle = serviceKeywordTitle(areaName, services);
        String pageDescription = serviceKeywordDescription(areaName, services);
        String canonicalUrl = baseUrl + "/regions/" + region.getSlug() + "/" + neighborhood.getSlug();
        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("services", services);
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("hubPublished", landingContentRepository.existsByNeighborhoodAndPublishedTrue(neighborhood));
        model.addAttribute("canonicalUrl", canonicalUrl);
        addImageSeo(model, pageTitle, pageDescription, canonicalUrl,
                serviceImageUrl("packing-move"), serviceItems(areaName, canonicalUrl, services));
        return "neighborhood";
    }

    private void addImageSeo(Model model, String title, String description, String canonicalUrl,
                             String imageUrl, List<Map<String, Object>> items) {
        model.addAttribute("ogImageUrl", imageUrl);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("@type", "WebPage");
        page.put("url", canonicalUrl);
        page.put("name", title);
        page.put("description", description);
        page.put("inLanguage", "ko-KR");
        page.put("primaryImageOfPage", Map.of("@type", "ImageObject", "url", imageUrl,
                "width", 1000, "height", 1000));

        List<Map<String, Object>> graph = new ArrayList<>();
        graph.add(page);
        if (items != null) {
            Map<String, Object> itemList = new LinkedHashMap<>();
            itemList.put("@type", "ItemList");
            itemList.put("numberOfItems", items.size());
            itemList.put("itemListElement", items);
            graph.add(itemList);
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "@context", "https://schema.org",
                    "@graph", graph));
            model.addAttribute("structuredDataJson", json.replace("</", "<\\/"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("검색 메타데이터를 만들 수 없습니다.", e);
        }
    }

    private List<Map<String, Object>> serviceItems(String areaName, String canonicalUrl,
                                                    List<MovingService> services) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int index = 0; index < services.size(); index++) {
            MovingService service = services.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", index + 1);
            item.put("name", areaName + " " + service.getName());
            item.put("url", canonicalUrl + "#" + service.getSlug());
            item.put("image", serviceImageUrl(service.getSlug()));
            items.add(item);
        }
        return items;
    }

    private String serviceKeywordTitle(String areaName, List<MovingService> services) {
        String keywords = services.stream()
                .limit(3)
                .map(MovingService::getName)
                .collect(Collectors.joining(" "));
        return areaName + " " + keywords + " | 이사해드림";
    }

    private String serviceKeywordDescription(String areaName, List<MovingService> services) {
        String keywords = services.stream()
                .map(MovingService::getName)
                .collect(Collectors.joining(", "));
        return areaName + "에서 이용할 수 있는 " + keywords + "의 준비사항과 상담 전 확인사항을 안내합니다.";
    }

    private String serviceImageUrl(String serviceSlug) {
        return baseUrl + "/images/services/" + serviceSlug + ".webp";
    }

    private Region region(String slug) {
        if (!ApprovedLocationCatalog.isApprovedRegion(slug)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return regionRepository.findBySlugAndEnabledTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private MovingService service(String slug) {
        return movingServiceRepository.findBySlugAndEnabledTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private List<Region> approvedRegions() {
        return regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc().stream()
                .filter(region -> ApprovedLocationCatalog.isApprovedRegion(region.getSlug()))
                .toList();
    }

    private List<Neighborhood> approvedNeighborhoods(Region region) {
        return neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region).stream()
                .filter(neighborhood -> ApprovedLocationCatalog.isApprovedNeighborhood(
                        region.getSlug(), neighborhood.getSlug()))
                .toList();
    }
}
