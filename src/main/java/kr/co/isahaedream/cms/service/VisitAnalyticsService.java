package kr.co.isahaedream.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class VisitAnalyticsService {
    private static final String SITE_SCOPE = "site";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M월 d일");

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public VisitAnalyticsService(JdbcTemplate jdbcTemplate,
                                 @Value("${app.analytics.enabled:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Transactional
    public void record(String regionSlug, String sessionId) {
        if (!enabled || sessionId == null || sessionId.isBlank()) return;
        LocalDate today = LocalDate.now(SEOUL);
        String visitorHash = hash(today + ":" + sessionId);
        recordScope(today, SITE_SCOPE, visitorHash);
        if (regionSlug != null && !regionSlug.isBlank()) {
            recordScope(today, regionSlug, visitorHash);
        }
    }

    public VisitSummary summary() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate start = today.minusDays(29);
        long[] todayValues = totals(today, today, SITE_SCOPE);
        long[] periodValues = totals(start, today, SITE_SCOPE);
        return new VisitSummary(todayValues[0], todayValues[1], periodValues[0], periodValues[1]);
    }

    public List<RegionVisitRow> regionRows() {
        LocalDate start = LocalDate.now(SEOUL).minusDays(29);
        return jdbcTemplate.query("""
                select r.name, coalesce(sum(s.page_views), 0), coalesce(sum(s.unique_visitors), 0)
                from regions r
                left join daily_visit_stats s on s.scope = r.slug and s.visit_date >= ?
                where r.enabled = true
                group by r.id, r.name, r.display_order
                order by coalesce(sum(s.page_views), 0) desc, r.display_order
                """, (rs, rowNum) -> new RegionVisitRow(
                rs.getString(1), rs.getLong(2), rs.getLong(3)), start);
    }

    public List<DailyVisitRow> dailyRows() {
        LocalDate start = LocalDate.now(SEOUL).minusDays(13);
        return jdbcTemplate.query("""
                select visit_date, page_views, unique_visitors
                from daily_visit_stats
                where scope = ? and visit_date >= ?
                order by visit_date desc
                """, (rs, rowNum) -> {
            LocalDate date = rs.getObject(1, LocalDate.class);
            return new DailyVisitRow(date, DAY_LABEL.format(date), rs.getLong(2), rs.getLong(3));
        }, SITE_SCOPE, start);
    }

    private void recordScope(LocalDate date, String scope, String visitorHash) {
        int inserted = jdbcTemplate.update("""
                insert into daily_visit_uniques (visit_date, scope, visitor_hash)
                values (?, ?, ?)
                on conflict (visit_date, scope, visitor_hash) do nothing
                """, date, scope, visitorHash);
        jdbcTemplate.update("""
                insert into daily_visit_stats (visit_date, scope, page_views, unique_visitors)
                values (?, ?, 1, ?)
                on conflict (visit_date, scope) do update
                set page_views = daily_visit_stats.page_views + 1,
                    unique_visitors = daily_visit_stats.unique_visitors + excluded.unique_visitors
                """, date, scope, inserted);
    }

    private long[] totals(LocalDate start, LocalDate end, String scope) {
        return jdbcTemplate.queryForObject("""
                select coalesce(sum(page_views), 0), coalesce(sum(unique_visitors), 0)
                from daily_visit_stats
                where scope = ? and visit_date between ? and ?
                """, (rs, rowNum) -> new long[]{rs.getLong(1), rs.getLong(2)}, scope, start, end);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public record VisitSummary(long todayPageViews, long todayVisitors,
                               long last30PageViews, long last30Visitors) {}

    public record RegionVisitRow(String regionName, long pageViews, long visitors) {}

    public record DailyVisitRow(LocalDate date, String dateLabel, long pageViews, long visitors) {}
}
