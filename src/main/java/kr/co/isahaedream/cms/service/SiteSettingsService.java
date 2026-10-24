package kr.co.isahaedream.cms.service;

import kr.co.isahaedream.cms.domain.SiteSettings;
import kr.co.isahaedream.cms.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettingsService {
    private final SiteSettingsRepository repository;

    public SiteSettingsService(SiteSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SiteSettings get() {
        return repository.findById(1L).orElseGet(SiteSettings::new);
    }

    @Transactional
    public SiteSettings save(String businessName, String phoneDisplay, String phoneHref,
                             String kakaoUrl, String tagline, String naverSiteVerification) {
        SiteSettings settings = repository.findById(1L).orElseGet(SiteSettings::new);
        settings.setBusinessName(businessName.trim());
        settings.setPhoneDisplay(phoneDisplay.trim());
        settings.setPhoneHref(phoneHref.replaceAll("[^0-9]", ""));
        settings.setKakaoUrl(kakaoUrl.trim());
        settings.setTagline(tagline.trim());
        settings.setNaverSiteVerification(naverSiteVerification == null ? "" : naverSiteVerification.trim());
        return repository.save(settings);
    }
}
