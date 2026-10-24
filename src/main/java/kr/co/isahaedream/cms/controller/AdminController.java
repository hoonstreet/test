package kr.co.isahaedream.cms.controller;

import kr.co.isahaedream.cms.config.ApprovedLocationCatalog;
import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import kr.co.isahaedream.cms.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final LandingContentRepository landingContentRepository;
    private final LandingPageService landingPageService;
    private final SiteSettingsService settingsService;

    public AdminController(RegionRepository regionRepository,
                           NeighborhoodRepository neighborhoodRepository,
                           MovingServiceRepository movingServiceRepository,
                           LandingContentRepository landingContentRepository,
                           LandingPageService landingPageService,
                           SiteSettingsService settingsService) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.landingContentRepository = landingContentRepository;
        this.landingPageService = landingPageService;
        this.settingsService = settingsService;
    }

    @GetMapping("/login")
    public String login() { return "admin/login"; }

    @GetMapping
    public String dashboard(Model model) {
        long regionCount = regionRepository.count();
        long neighborhoodCount = neighborhoodRepository.count();
        long serviceCount = movingServiceRepository.count();
        model.addAttribute("regionCount", regionCount);
        model.addAttribute("neighborhoodCount", neighborhoodCount);
        model.addAttribute("serviceCount", serviceCount);
        model.addAttribute("landingCount", (regionCount + neighborhoodCount) * serviceCount);
        model.addAttribute("publishedCount", landingContentRepository.countByPublishedTrue());
        model.addAttribute("regions", approvedRegions());
        return "admin/dashboard";
    }

    @GetMapping("/content")
    public String content(@RequestParam(required = false) Long regionId,
                          @RequestParam(required = false) Long neighborhoodId,
                          @RequestParam(required = false) Long serviceId,
                          @RequestParam(required = false) Long filterServiceId,
                          @RequestParam(defaultValue = "all") String status,
                          Model model) {
        List<Region> regions = approvedRegions();
        List<MovingService> services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        model.addAttribute("regions", regions);
        model.addAttribute("services", services);
        model.addAttribute("filterServiceId", filterServiceId);
        model.addAttribute("statusFilter", status);
        if (regionId != null) {
            Region region = regionRepository.findById(regionId).orElseThrow();
            requireApproved(region, null);
            List<Neighborhood> neighborhoods = approvedNeighborhoods(region);
            List<LandingContent> savedContents = landingContentRepository.findAllByRegion(region);
            model.addAttribute("selectedRegion", region);
            model.addAttribute("neighborhoods", neighborhoods);
            addContentRows(model, region, neighborhoods, services, savedContents, filterServiceId, status);
            if (serviceId != null) {
                MovingService service = movingServiceRepository.findById(serviceId).orElseThrow();
                Neighborhood neighborhood = neighborhoodId == null ? null : neighborhoodRepository.findById(neighborhoodId).orElseThrow();
                if (neighborhood != null && !neighborhood.getRegion().getId().equals(region.getId())) {
                    throw new IllegalArgumentException("선택한 동이 시·구와 일치하지 않습니다.");
                }
                model.addAttribute("selectedService", service);
                model.addAttribute("selectedNeighborhood", neighborhood);
                LandingContent selectedContent = landingPageService.find(region, neighborhood, service).orElse(null);
                model.addAttribute("content", selectedContent);
                model.addAttribute("preview", landingPageService.view(region, neighborhood, service));
                List<String> qualityProblems = selectedContent == null
                        ? List.of("초안이 아직 저장되지 않았습니다.")
                        : LandingPageService.qualityProblems(selectedContent.getSeoTitle(),
                                selectedContent.getMetaDescription(), selectedContent.getHeadline(),
                                selectedContent.getIntroduction(), selectedContent.getBody());
                model.addAttribute("qualityProblems", qualityProblems);
                model.addAttribute("qualityReady", qualityProblems.isEmpty());
            }
        }
        return "admin/content";
    }

    @GetMapping("/content/preview")
    public String previewContent(@RequestParam Long regionId,
                                 @RequestParam(required = false) Long neighborhoodId,
                                 @RequestParam Long serviceId,
                                 Model model) {
        Region region = regionRepository.findById(regionId).orElseThrow();
        Neighborhood neighborhood = neighborhoodId == null ? null : neighborhoodRepository.findById(neighborhoodId).orElseThrow();
        MovingService service = movingServiceRepository.findById(serviceId).orElseThrow();
        requireApproved(region, neighborhood);
        if (neighborhood != null && !neighborhood.getRegion().getId().equals(region.getId())) {
            throw new IllegalArgumentException("선택한 동이 시·구와 일치하지 않습니다.");
        }

        model.addAttribute("region", region);
        model.addAttribute("neighborhood", neighborhood);
        model.addAttribute("movingService", service);
        model.addAttribute("content", landingPageService.preview(region, neighborhood, service));
        model.addAttribute("services", movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc());
        model.addAttribute("neighborhoods", approvedNeighborhoods(region));
        model.addAttribute("canonicalUrl", publicPath(region, neighborhood, service));
        model.addAttribute("adminPreview", true);
        return "landing";
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
                              @RequestParam(defaultValue = "false") boolean reviewConfirmed,
                              @RequestParam(required = false) Long filterServiceId,
                              @RequestParam(defaultValue = "all") String status,
                              RedirectAttributes redirect) {
        Region region = regionRepository.findById(regionId).orElseThrow();
        Neighborhood neighborhood = neighborhoodId == null ? null : neighborhoodRepository.findById(neighborhoodId).orElseThrow();
        MovingService service = movingServiceRepository.findById(serviceId).orElseThrow();
        requireApproved(region, neighborhood);
        if (neighborhood != null && !neighborhood.getRegion().getId().equals(region.getId())) {
            throw new IllegalArgumentException("선택한 동이 시·구와 일치하지 않습니다.");
        }

        List<String> qualityProblems = LandingPageService.qualityProblems(
                stripToNull(seoTitle), stripToNull(metaDescription), stripToNull(headline),
                stripToNull(introduction), stripToNull(body));
        boolean canPublish = published && reviewConfirmed && qualityProblems.isEmpty();
        landingPageService.save(region, neighborhood, service, seoTitle, metaDescription, headline,
                introduction, body, canPublish);
        if (published && !reviewConfirmed) {
            redirect.addFlashAttribute("error", "초안은 저장했지만 공개하지 않았습니다. 관리자 미리보기를 확인한 뒤 검수 확인에 체크하세요.");
        } else if (published && !canPublish) {
            redirect.addFlashAttribute("error", "초안은 저장했지만 공개하지 않았습니다. " + String.join(", ", qualityProblems));
        } else {
            redirect.addFlashAttribute("saved", canPublish ? "저장하고 공개했습니다." : "초안으로 저장했습니다.");
        }
        redirect.addAttribute("regionId", regionId);
        redirect.addAttribute("serviceId", serviceId);
        if (neighborhoodId != null) redirect.addAttribute("neighborhoodId", neighborhoodId);
        if (filterServiceId != null) redirect.addAttribute("filterServiceId", filterServiceId);
        redirect.addAttribute("status", status);
        return "redirect:/admin/content";
    }

    @GetMapping(value = "/keywords.csv", produces = "text/csv;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> keywordCsv() {
        List<MovingService> services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        StringBuilder csv = new StringBuilder("\uFEFF구분,상위지역,하위지역,서비스,승인 키워드,URL\r\n");
        for (ApprovedLocationCatalog.RegionSeed region : ApprovedLocationCatalog.regions()) {
            for (MovingService service : services) {
                csv.append("상위지역,").append(region.name()).append(",,")
                        .append(service.getName()).append(',')
                        .append(region.name()).append(' ').append(service.getName()).append(',')
                        .append("/regions/").append(region.slug()).append('/').append(service.getSlug()).append("\r\n");
            }
            for (ApprovedLocationCatalog.NameSlug neighborhood : region.neighborhoods()) {
                for (MovingService service : services) {
                    csv.append("하위지역,").append(region.name()).append(',').append(neighborhood.name()).append(',')
                            .append(service.getName()).append(',')
                            .append(neighborhood.name()).append(' ').append(service.getName()).append(',')
                            .append("/regions/").append(region.slug()).append('/').append(neighborhood.slug())
                            .append('/').append(service.getSlug()).append("\r\n");
                }
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=approved-keywords-876.csv")
                .body(csv.toString());
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

    private void addContentRows(Model model, Region region, List<Neighborhood> neighborhoods,
                                List<MovingService> services, List<LandingContent> contents,
                                Long filterServiceId, String statusFilter) {
        Map<String, LandingContent> contentMap = new HashMap<>();
        for (LandingContent content : contents) {
            contentMap.put(contentKey(content.getNeighborhood(), content.getMovingService()), content);
        }

        List<ContentRow> allRows = new ArrayList<>();
        addAreaRows(allRows, region, null, services, contentMap);
        for (Neighborhood neighborhood : neighborhoods) {
            addAreaRows(allRows, region, neighborhood, services, contentMap);
        }

        long published = allRows.stream().filter(row -> row.state().equals("published")).count();
        long draft = allRows.stream().filter(row -> row.state().equals("draft")).count();
        long missing = allRows.size() - published - draft;
        List<ContentRow> filtered = allRows.stream()
                .filter(row -> filterServiceId == null || row.serviceId().equals(filterServiceId))
                .filter(row -> "all".equals(statusFilter) || row.state().equals(statusFilter))
                .toList();

        model.addAttribute("contentRows", filtered);
        model.addAttribute("regionTotalCount", allRows.size());
        model.addAttribute("regionPublishedCount", published);
        model.addAttribute("regionDraftCount", draft);
        model.addAttribute("regionMissingCount", missing);
        model.addAttribute("regionProgress", allRows.isEmpty() ? 0 : Math.round(published * 100.0 / allRows.size()));
    }

    private void addAreaRows(List<ContentRow> rows, Region region, Neighborhood neighborhood,
                             List<MovingService> services, Map<String, LandingContent> contentMap) {
        for (MovingService service : services) {
            LandingContent content = contentMap.get(contentKey(neighborhood, service));
            String state = content == null ? "missing" : content.isPublished() ? "published" : "draft";
            String areaName = neighborhood == null ? region.getName() + " 전체" : neighborhood.getName();
            String publicPath = neighborhood == null
                    ? "/regions/" + region.getSlug() + "/" + service.getSlug()
                    : "/regions/" + region.getSlug() + "/" + neighborhood.getSlug() + "/" + service.getSlug();
            rows.add(new ContentRow(neighborhood == null ? null : neighborhood.getId(), areaName,
                    service.getId(), service.getName(), state, content == null ? null : content.getUpdatedAt(), publicPath));
        }
    }

    private static String contentKey(Neighborhood neighborhood, MovingService service) {
        return (neighborhood == null ? "region" : neighborhood.getId()) + ":" + service.getId();
    }

    private static String publicPath(Region region, Neighborhood neighborhood, MovingService service) {
        return neighborhood == null
                ? "/regions/" + region.getSlug() + "/" + service.getSlug()
                : "/regions/" + region.getSlug() + "/" + neighborhood.getSlug() + "/" + service.getSlug();
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
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

    private static void requireApproved(Region region, Neighborhood neighborhood) {
        if (!ApprovedLocationCatalog.isApprovedRegion(region.getSlug())) {
            throw new IllegalArgumentException("승인되지 않은 지역입니다.");
        }
        if (neighborhood != null && !ApprovedLocationCatalog.isApprovedNeighborhood(
                region.getSlug(), neighborhood.getSlug())) {
            throw new IllegalArgumentException("승인되지 않은 하위 지역입니다.");
        }
    }

    public record ContentRow(Long neighborhoodId, String areaName, Long serviceId, String serviceName,
                             String state, Instant updatedAt, String publicPath) {
        private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of("Asia/Seoul"));

        public String stateLabel() {
            return switch (state) {
                case "published" -> "공개";
                case "draft" -> "작성중";
                default -> "미작성";
            };
        }

        public String updatedLabel() {
            return updatedAt == null ? "-" : UPDATED_FORMAT.format(updatedAt);
        }
    }
}
