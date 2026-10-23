package kr.co.isahaedream.cms.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "moving_services", uniqueConstraints = @UniqueConstraint(name = "uk_service_slug", columnNames = "slug"))
public class MovingService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false, length = 180)
    private String shortDescription;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean enabled = true;

    protected MovingService() {}

    public MovingService(String name, String slug, String shortDescription, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getShortDescription() { return shortDescription; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isEnabled() { return enabled; }
    public void setName(String name) { this.name = name; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
