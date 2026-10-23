package kr.co.isahaedream.cms;

import kr.co.isahaedream.cms.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IsaHaeDreamApplicationTests {
    @Autowired MockMvc mockMvc;
    @Autowired RegionRepository regionRepository;
    @Autowired NeighborhoodRepository neighborhoodRepository;
    @Autowired MovingServiceRepository movingServiceRepository;

    @Test
    void seedsExpectedLocationAndServiceData() {
        assertThat(regionRepository.count()).isEqualTo(14);
        assertThat(neighborhoodRepository.count()).isEqualTo(132);
        assertThat(movingServiceRepository.count()).isEqualTo(6);
    }

    @Test
    void homeRendersAndBlocksIndexingBeforeLaunch() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("010-5297-2459")));
    }

    @Test
    void regionAndNeighborhoodLandingPagesRender() throws Exception {
        mockMvc.perform(get("/regions/jungnang-gu/packing-move"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("중랑구 포장이사")));
        mockMvc.perform(get("/regions/jungnang-gu/myeonmok-dong/packing-move"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("면목동 포장이사")));
    }

    @Test
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void unpublishedLandingPagesAreExcludedFromSitemap() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/regions/jungnang-gu</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/regions/jungnang-gu/packing-move</loc>"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminScreensRenderForAuthenticatedAdministrator() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("콘텐츠 현황")));
        mockMvc.perform(get("/admin/content"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("지역 페이지 편집")));
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("010-5297-2459")));
    }
}
