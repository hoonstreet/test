package kr.co.isahaedream.cms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.isahaedream.cms.service.VisitAnalyticsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VisitAnalyticsFilter extends OncePerRequestFilter {
    private static final Pattern REGION_PATH = Pattern.compile("^/regions/([^/]+)(?:/.*)?$");
    private static final Pattern BOT = Pattern.compile(
            "bot|crawler|spider|slurp|yeti|google|bing|baidu|facebookexternalhit|preview",
            Pattern.CASE_INSENSITIVE);

    private final VisitAnalyticsService analytics;

    public VisitAnalyticsFilter(VisitAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !analytics.isEnabled()
                || !"GET".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/admin")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.svg")
                || path.equals("/robots.txt")
                || path.equals("/sitemap.xml")
                || isBot(request.getHeader("User-Agent"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getSession(true).getId();
        filterChain.doFilter(request, response);
        String contentType = response.getContentType();
        if (response.getStatus() >= 200 && response.getStatus() < 300
                && contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html")) {
            Matcher matcher = REGION_PATH.matcher(request.getRequestURI());
            analytics.record(matcher.matches() ? matcher.group(1) : null, sessionId);
        }
    }

    private static boolean isBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT.matcher(userAgent).find();
    }
}
