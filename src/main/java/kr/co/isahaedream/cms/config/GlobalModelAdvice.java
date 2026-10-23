package kr.co.isahaedream.cms.config;

import kr.co.isahaedream.cms.domain.SiteSettings;
import kr.co.isahaedream.cms.service.SiteSettingsService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {
    private final SiteSettingsService settingsService;

    public GlobalModelAdvice(SiteSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @ModelAttribute("site")
    public SiteSettings siteSettings() {
        return settingsService.get();
    }
}
