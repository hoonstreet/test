package kr.co.isahaedream.cms.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "landing_contents", indexes = {
        @Index(name = "idx_landing_region_service", columnList = "region_id,service_id"),
        @Index(name = "idx_landing_neighborhood", columnList = "neighborhood_id")
})
public class LandingContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MovingService movingService;

    @Column(length = 120)
    private String seoTitle;

    @Column(length = 200)
    private String metaDescription;

    @Column(length = 300)
    private String headline;

    @Column(length = 1200)
    private String introduction;

    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private Instant updatedAt;

    protected LandingContent() {}

    public LandingContent(Region region, Neighborhood neighborhood, MovingService movingService) {
        this.region = region;
        this.neighborhood = neighborhood;
        this.movingService = movingService;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Region getRegion() { return region; }
    public Neighborhood getNeighborhood() { return neighborhood; }
    public MovingService getMovingService() { return movingService; }
    public String getSeoTitle() { return seoTitle; }
    public String getMetaDescription() { return metaDescription; }
    public String getHeadline() { return headline; }
    public String getIntroduction() { return introduction; }
    public String getBody() { return body; }
    public boolean isPublished() { return published; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public void setHeadline(String headline) { this.headline = headline; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public void setBody(String body) { this.body = body; }
    public void setPublished(boolean published) { this.published = published; }
}
