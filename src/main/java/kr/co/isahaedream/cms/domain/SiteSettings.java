package kr.co.isahaedream.cms.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "site_settings")
public class SiteSettings {
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 60)
    private String businessName = "이사해드림";

    @Column(nullable = false, length = 30)
    private String phoneDisplay = "010-5297-2459";

    @Column(nullable = false, length = 30)
    private String phoneHref = "01052972459";

    @Column(nullable = false, length = 200)
    private String kakaoUrl = "https://pf.kakao.com/_iITad";

    @Column(nullable = false, length = 100)
    private String tagline = "이사 고민부터 마무리까지, 이사해드림";

    @Column(nullable = false, length = 200)
    private String naverSiteVerification = "";

    public SiteSettings() {}

    public Long getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getPhoneDisplay() { return phoneDisplay; }
    public String getPhoneHref() { return phoneHref; }
    public String getKakaoUrl() { return kakaoUrl; }
    public String getTagline() { return tagline; }
    public String getNaverSiteVerification() { return naverSiteVerification; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setPhoneDisplay(String phoneDisplay) { this.phoneDisplay = phoneDisplay; }
    public void setPhoneHref(String phoneHref) { this.phoneHref = phoneHref; }
    public void setKakaoUrl(String kakaoUrl) { this.kakaoUrl = kakaoUrl; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public void setNaverSiteVerification(String naverSiteVerification) { this.naverSiteVerification = naverSiteVerification; }
}
