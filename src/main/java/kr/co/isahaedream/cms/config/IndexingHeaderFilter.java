package kr.co.isahaedream.cms.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class IndexingHeaderFilter implements Filter {
    private final boolean indexingEnabled;

    public IndexingHeaderFilter(@Value("${app.search-index-enabled}") boolean indexingEnabled) {
        this.indexingEnabled = indexingEnabled;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!indexingEnabled && response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        }
        chain.doFilter(request, response);
    }
}
