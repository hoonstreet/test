package kr.co.isahaedream.cms.controller;

import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import kr.co.isahaedream.cms.service.LandingPageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PublicController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;

    public PublicController(RegionRepository regionRepository,
                            NeighborhoodRepository neighborhoodRepository,
                            MovingServiceRepository movingServiceRepository,
                            LandingPageService landingPageService) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingPageService = landingPageService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("regions", regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("pageTitle", "이사해드림 | 지역별 포장이사 상담");
        model.addAttribute("pageDescription", "포장이사, 보관이사, 입주이사, 사무실이사, 아파트이사를 전화와 카카오톡으로 상담하세요.");
        return "home";
    }

    @GetMapping("/regions/{regionSlug}")
    public String region(@PathVariable String regionSlug, Model model) {
        Region region = region(regionSlug);
        model.addAttribute("region", region);
        model.addAttribute("neighborhoods", neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region));
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("pageTitle", region.getName() + " 이사 서비스 | 이사해드림");
        model.addAttribute("pageDescription", region.getName() + "의 지역별 포장이사 및 이사 서비스 상담 안내입니다.");
        return "region";
    }

    @GetMapping("/regions/{regionSlug}/{serviceSlug}")
    public String regionService(@PathVariable String regionSlug, @PathVariable String serviceSlug, Model model) {
        return landing(region(regionSlug), null, service(serviceSlug), model);
    }

    @GetMapping("/regions/{regionSlug}/{neighborhoodSlug}/{serviceSlug}")
    public String neighborhoodService(@PathVariable String regionSlug,
                                      @PathVariable String neighborhoodSlug,
                                      @PathVariable String serviceSlug,
                                      Model model) {
        Region region = region(regionSlug);
        Neighborhood neighborhood = neighborhoodRepository.findByRegionAndSlugAndEnabledTrue(region, neighborhoodSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return landing(region, neighborhood, service(serviceSlug), model);
    }

    private String landing(Region region, Neighborhood neighborhood, MovingService service, Model model) {
        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("movingService", service);
        model.addAttribute("content", landingPageService.view(region, neighborhood, service));
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("neighborhoods", neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region));
        return "landing";
    }

    private Region region(String slug) {
        return regionRepository.findBySlugAndEnabledTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private MovingService service(String slug) {
        return movingServiceRepository.findBySlugAndEnabledTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
