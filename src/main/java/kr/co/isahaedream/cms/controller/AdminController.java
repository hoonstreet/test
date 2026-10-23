package kr.co.isahaedream.cms.controller;

import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import kr.co.isahaedream.cms.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingPageService landingPageService;
    private final SiteSettingsService settingsService;

    public AdminController(RegionRepository regionRepository,
                           NeighborhoodRepository neighborhoodRepository,
                           MovingServiceRepository movingServiceRepository,
                           LandingPageService landingPageService,
                           SiteSettingsService settingsService) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingPageService = landingPageService;
        this.settingsService = settingsService;
    }

    @GetMapping("/login")
    public String login() { return "admin/login"; }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("regionCount", regionRepository.count());
        model.addAttribute("neighborhoodCount", neighborhoodRepository.count());
        model.addAttribute("serviceCount", movingServiceRepository.count());
        model.addAttribute("regions", regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        return "admin/dashboard";
    }

    @GetMapping("/content")
    public String content(@RequestParam(required = false) Long regionId,
                          @RequestParam(required = false) Long neighborhoodId,
                          @RequestParam(required = false) Long serviceId,
                          Model model) {
        model.addAttribute("regions", regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        if (regionId != null) {
            Region region = regionRepository.findById(regionId).orElseThrow();
            model.addAttribute("selectedRegion", region);
            model.addAttribute("neighborhoods", neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region));
            if (serviceId != null) {
                MovingService service = movingServiceRepository.findById(serviceId).orElseThrow();
                Neighborhood neighborhood = neighborhoodId == null ? null : neighborhoodRepository.findById(neighborhoodId).orElseThrow();
                model.addAttribute("selectedService", service);
                model.addAttribute("selectedNeighborhood", neighborhood);
                model.addAttribute("content", landingPageService.find(region, neighborhood, service).orElse(null));
                model.addAttribute("preview", landingPageService.view(region, neighborhood, service));
            }
        }
        return "admin/content";
    }

    @PostMapping("/content")
    public String saveContent(@RequestParam Long regionId,
                              @RequestParam(required = false) Long neighborhoodId,
                              @RequestParam Long serviceId,
                              @RequestParam(defaultValue = "") String seoTitle,
                              @RequestParam(defaultValue = "") String metaDescription,
                              @RequestParam(defaultValue = "") String headline,
                              @RequestParam(defaultValue = "") String introduction,
                              @RequestParam(defaultValue = "") String body,
                              @RequestParam(defaultValue = "false") boolean published,
                              RedirectAttributes redirect) {
        Region region = regionRepository.findById(regionId).orElseThrow();
        Neighborhood neighborhood = neighborhoodId == null ? null : neighborhoodRepository.findById(neighborhoodId).orElseThrow();
        MovingService service = movingServiceRepository.findById(serviceId).orElseThrow();
        landingPageService.save(region, neighborhood, service, seoTitle, metaDescription, headline, introduction, body, published);
        redirect.addFlashAttribute("saved", true);
        redirect.addAttribute("regionId", regionId);
        redirect.addAttribute("serviceId", serviceId);
        if (neighborhoodId != null) redirect.addAttribute("neighborhoodId", neighborhoodId);
        return "redirect:/admin/content";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", settingsService.get());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String businessName,
                               @RequestParam String phoneDisplay,
                               @RequestParam String phoneHref,
                               @RequestParam String kakaoUrl,
                               @RequestParam String tagline,
                               RedirectAttributes redirect) {
        settingsService.save(businessName, phoneDisplay, phoneHref, kakaoUrl, tagline);
        redirect.addFlashAttribute("saved", true);
        return "redirect:/admin/settings";
    }
}
