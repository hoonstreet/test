package kr.co.isahaedream.cms.controller;

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

import java.util.List;

@Controller
public class PublicController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingContentRepository landingContentRepository;
    private final LandingPageService landingPageService;
    private final String baseUrl;

    public PublicController(RegionRepository regionRepository,
                            NeighborhoodRepository neighborhoodRepository,
                            MovingServiceRepository movingServiceRepository,
                            LandingContentRepository landingContentRepository,
                            LandingPageService landingPageService,
                            @Value("${app.base-url}") String baseUrl) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingContentRepository = landingContentRepository;
        this.landingPageService = landingPageService;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("regions", approvedRegions());
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("pageTitle", "이사해드림 | 지역별 포장이사 상담");
        model.addAttribute("pageDescription", "포장이사, 보관이사, 입주이사, 사무실이사, 아파트이사를 전화와 카카오톡으로 상담하세요.");
        model.addAttribute("canonicalUrl", baseUrl + "/");
        return "home";
    }

    @GetMapping("/regions/{regionSlug}")
    public String region(@PathVariable String regionSlug, Model model) {
        Region region = region(regionSlug);
        model.addAttribute("region", region);
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("pageTitle", region.getName() + " 이사 서비스 | 이사해드림");
        model.addAttribute("pageDescription", region.getName() + "의 지역별 포장이사 및 이사 서비스 상담 안내입니다.");
        model.addAttribute("canonicalUrl", baseUrl + "/regions/" + region.getSlug());
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
        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("movingService", service);
        model.addAttribute("content", landingPageService.view(region, neighborhood, service));
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        String path = "/regions/" + region.getSlug() + "/";
        if (neighborhood != null) path += neighborhood.getSlug() + "/";
        path += service.getSlug();
        model.addAttribute("canonicalUrl", baseUrl + path);
        return "landing";
    }

    private String neighborhoodHub(Region region, Neighborhood neighborhood, Model model) {
        if (!ApprovedLocationCatalog.isApprovedNeighborhood(region.getSlug(), neighborhood.getSlug())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        model.addAttribute("pageTitle", neighborhood.getName() + " 이사 서비스 | " + region.getName() + " 이사해드림");
        model.addAttribute("pageDescription", region.getName() + " " + neighborhood.getName() + "의 포장이사, 보관이사, 입주이사, 사무실이사, 아파트이사 상담 안내입니다.");
        model.addAttribute("hubPublished", landingContentRepository.existsByNeighborhoodAndPublishedTrue(neighborhood));
        model.addAttribute("canonicalUrl", baseUrl + "/regions/" + region.getSlug() + "/" + neighborhood.getSlug());
        return "neighborhood";
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
