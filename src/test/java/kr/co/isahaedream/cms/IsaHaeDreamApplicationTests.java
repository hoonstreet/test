package kr.co.isahaedream.cms;

import kr.co.isahaedream.cms.config.ApprovedLocationCatalog;
import kr.co.isahaedream.cms.repository.*;
import kr.co.isahaedream.cms.service.LandingPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IsaHaeDreamApplicationTests {
    @Autowired MockMvc mockMvc;
    @Autowired RegionRepository regionRepository;
    @Autowired NeighborhoodRepository neighborhoodRepository;
    @Autowired MovingServiceRepository movingServiceRepository;
    @Autowired LandingContentRepository landingContentRepository;

    @Test
    void seedsExpectedLocationAndServiceData() {
        assertThat(regionRepository.count()).isEqualTo(14);
        assertThat(neighborhoodRepository.count()).isEqualTo(132);
        assertThat(movingServiceRepository.count()).isEqualTo(6);
        assertThat((regionRepository.count() + neighborhoodRepository.count()) * movingServiceRepository.count())
                .isEqualTo(876);
        assertThat(ApprovedLocationCatalog.regions()).hasSize(14);
        assertThat(ApprovedLocationCatalog.neighborhoodCount()).isEqualTo(132);
        assertThat(ApprovedLocationCatalog.keywordCount(6)).isEqualTo(876);
    }

    @Test
    @Transactional
    void approvedCatalogExactlyMatchesStoredLocationsAndAll876DraftTargets() {
        var actualRegions = regionRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        assertThat(actualRegions)
                .extracting(region -> region.getName() + "|" + region.getSlug())
                .containsExactlyElementsOf(ApprovedLocationCatalog.regions().stream()
                        .map(region -> region.name() + "|" + region.slug())
                        .toList());

        Set<String> expectedTargets = new LinkedHashSet<>();
        var services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        for (var approvedRegion : ApprovedLocationCatalog.regions()) {
            var storedRegion = regionRepository.findBySlugAndEnabledTrue(approvedRegion.slug()).orElseThrow();
            assertThat(neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(storedRegion))
                    .extracting(neighborhood -> neighborhood.getName() + "|" + neighborhood.getSlug())
                    .containsExactlyElementsOf(approvedRegion.neighborhoods().stream()
                            .map(neighborhood -> neighborhood.name() + "|" + neighborhood.slug())
                            .toList());

            for (var service : services) {
                expectedTargets.add(targetKey(approvedRegion.slug(), null, service.getSlug()));
                for (var neighborhood : approvedRegion.neighborhoods()) {
                    expectedTargets.add(targetKey(approvedRegion.slug(), neighborhood.slug(), service.getSlug()));
                }
            }
        }

        var drafts = landingContentRepository.findAll();
        var actualTargets = drafts.stream()
                .map(content -> targetKey(content.getRegion().getSlug(),
                        content.getNeighborhood() == null ? null : content.getNeighborhood().getSlug(),
                        content.getMovingService().getSlug()))
                .toList();

        assertThat(expectedTargets).hasSize(876);
        assertThat(actualTargets).hasSize(876).doesNotHaveDuplicates();
        assertThat(actualTargets).containsExactlyInAnyOrderElementsOf(expectedTargets);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getMetaDescription()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getHeadline()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getIntroduction()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
    }

    @Test
    @Transactional
    void representativeDraftsCoverRootDongEupAndDuplicatePlaceNames() {
        assertDraftContains("jungnang-gu", null, "packing-move", "중랑구 포장이사");
        assertDraftContains("namyangju", "jinjeop-eup", "storage-move", "진접읍 보관이사");
        assertDraftContains("songpa-gu", "wirye-dong", "office-move", "송파구 위례동 사무실이사");
        assertDraftContains("hanam", "wirye-dong", "apartment-move", "하남시 위례동 아파트이사");
        assertDraftContains("hanam", "hanam", "moving-center", "하남 이삿짐센터");
    }

    @Test
    void seedsSixJunggokPilotPagesAsUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
        var neighborhood = neighborhoodRepository
                .findByRegionAndSlugAndEnabledTrue(region, "junggok-dong").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region).stream()
                .filter(content -> content.getNeighborhood() != null
                        && content.getNeighborhood().getId().equals(neighborhood.getId()))
                .toList();

        assertThat(drafts).hasSize(6);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).allMatch(content -> !containsUnapprovedJunggokKeyword(content.getSeoTitle())
                && !containsUnapprovedJunggokKeyword(content.getMetaDescription())
                && !containsUnapprovedJunggokKeyword(content.getHeadline()));
        assertThat(neighborhoodRepository.findAllByRegionAndEnabledTrueOrderByDisplayOrderAsc(region))
                .extracting(neighborhoodItem -> neighborhoodItem.getName())
                .doesNotContain("중곡1동", "중곡2동", "중곡3동", "중곡4동",
                        "중곡제1동", "중곡제2동", "중곡제3동", "중곡제4동");
    }

    @Test
    void seedsAllFortyEightGwangjinTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(48);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedGwangjinSubKeyword(content.getSeoTitle())
                && !containsUnapprovedGwangjinSubKeyword(content.getMetaDescription())
                && !containsUnapprovedGwangjinSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllFortyTwoJungnangTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("jungnang-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(42);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedNumberedDong(content.getSeoTitle())
                && !containsUnapprovedNumberedDong(content.getMetaDescription())
                && !containsUnapprovedNumberedDong(content.getHeadline()));
    }

    @Test
    void seedsAllSeventyTwoSeongdongTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("seongdong-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(72);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedSeongdongSubKeyword(content.getSeoTitle())
                && !containsUnapprovedSeongdongSubKeyword(content.getMetaDescription())
                && !containsUnapprovedSeongdongSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllFiftyFourGuriTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("guri").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(54);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedGuriSubKeyword(content.getSeoTitle())
                && !containsUnapprovedGuriSubKeyword(content.getMetaDescription())
                && !containsUnapprovedGuriSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllOneHundredTwoNamyangjuTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("namyangju").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(102);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedNamyangjuSubKeyword(content.getSeoTitle())
                && !containsUnapprovedNamyangjuSubKeyword(content.getMetaDescription())
                && !containsUnapprovedNamyangjuSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllThirtySixNowonTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("nowon-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(36);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedNowonSubKeyword(content.getSeoTitle())
                && !containsUnapprovedNowonSubKeyword(content.getMetaDescription())
                && !containsUnapprovedNowonSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllSixtyGangdongTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("gangdong-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(60);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedGangdongSubKeyword(content.getSeoTitle())
                && !containsUnapprovedGangdongSubKeyword(content.getMetaDescription())
                && !containsUnapprovedGangdongSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllNinetySongpaTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("songpa-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(90);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedSongpaSubKeyword(content.getSeoTitle())
                && !containsUnapprovedSongpaSubKeyword(content.getMetaDescription())
                && !containsUnapprovedSongpaSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllSeventyTwoDongdaemunTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("dongdaemun-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(72);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedDongdaemunSubKeyword(content.getSeoTitle())
                && !containsUnapprovedDongdaemunSubKeyword(content.getMetaDescription())
                && !containsUnapprovedDongdaemunSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllFiftyFourUijeongbuTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("uijeongbu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(54);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedUijeongbuSubKeyword(content.getSeoTitle())
                && !containsUnapprovedUijeongbuSubKeyword(content.getMetaDescription())
                && !containsUnapprovedUijeongbuSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllThirtyDobongTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("dobong-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(30);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedDobongSubKeyword(content.getSeoTitle())
                && !containsUnapprovedDobongSubKeyword(content.getMetaDescription())
                && !containsUnapprovedDobongSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllSixtyGangbukTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("gangbuk-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(60);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedGangbukSubKeyword(content.getSeoTitle())
                && !containsUnapprovedGangbukSubKeyword(content.getMetaDescription())
                && !containsUnapprovedGangbukSubKeyword(content.getHeadline()));
    }

    @Test
    void seedsAllNinetySixSeongbukTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("seongbuk-gu").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(96);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedSeongbukSubKeyword(content.getSeoTitle())
                && !containsUnapprovedSeongbukSubKeyword(content.getMetaDescription())
                && !containsUnapprovedSeongbukSubKeyword(content.getHeadline()));
        assertThat(landingContentRepository.count()).isEqualTo(876);
    }

    @Test
    void seedsAllSixtyHanamTargetsAsDistinctUnpublishedDrafts() {
        var region = regionRepository.findBySlugAndEnabledTrue("hanam").orElseThrow();
        var drafts = landingContentRepository.findAllByRegion(region);

        assertThat(drafts).hasSize(60);
        assertThat(drafts).allMatch(content -> !content.isPublished());
        assertThat(drafts).allMatch(content -> LandingPageService.qualityProblems(
                content.getSeoTitle(), content.getMetaDescription(), content.getHeadline(),
                content.getIntroduction(), content.getBody()).isEmpty());
        assertThat(drafts).extracting(content -> content.getSeoTitle()).doesNotHaveDuplicates();
        assertThat(drafts).extracting(content -> content.getBody()).doesNotHaveDuplicates();
        assertThat(drafts).allMatch(content -> !containsUnapprovedHanamSubKeyword(content.getSeoTitle())
                && !containsUnapprovedHanamSubKeyword(content.getMetaDescription())
                && !containsUnapprovedHanamSubKeyword(content.getHeadline()));
        assertThat(landingContentRepository.count()).isEqualTo(876);
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
        mockMvc.perform(get("/regions/gwangjin-gu/junggok-dong"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("중곡동 이사 서비스")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("중곡동 포장이사")));
        mockMvc.perform(get("/regions/jungnang-gu/packing-move"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("중랑구 포장이사")));
        mockMvc.perform(get("/regions/jungnang-gu/myeonmok-dong/packing-move"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("면목동 포장이사")));
    }

    @Test
    void all876ApprovedLandingUrlsRenderWhileUnapprovedSubAreasStayUnavailable() throws Exception {
        var services = movingServiceRepository.findAllByEnabledTrueOrderByDisplayOrderAsc();
        int rendered = 0;
        for (var approvedRegion : ApprovedLocationCatalog.regions()) {
            for (var service : services) {
                mockMvc.perform(get("/regions/{region}/{service}",
                                approvedRegion.slug(), service.getSlug()))
                        .andExpect(status().isOk())
                        .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"));
                rendered++;
                for (var neighborhood : approvedRegion.neighborhoods()) {
                    mockMvc.perform(get("/regions/{region}/{neighborhood}/{service}",
                                    approvedRegion.slug(), neighborhood.slug(), service.getSlug()))
                            .andExpect(status().isOk())
                            .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"));
                    rendered++;
                }
            }
        }

        assertThat(rendered).isEqualTo(876);
        mockMvc.perform(get("/regions/gwangjin-gu/junggok-1-dong/packing-move"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/regions/hanam/misa-1-dong/packing-move"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/regions/hanam/cheonhyeon-dong/packing-move"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    @Test
    void unpublishedLandingPagesAreExcludedFromSitemap() throws Exception {
        var sitemap = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/regions/jungnang-gu</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/regions/jungnang-gu/packing-move</loc>"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/regions/gwangjin-gu/junggok-dong</loc>"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(sitemap.split("<loc>", -1).length - 1).isEqualTo(15);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminScreensRenderForAuthenticatedAdministrator() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("콘텐츠 현황")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("876")));
        mockMvc.perform(get("/admin/content"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("876개 페이지 관리")));
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("010-5297-2459")));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "ADMIN")
    void naverVerificationContentCanBeSavedAndRenderedInTheHead() throws Exception {
        String verification = "naver-verification_test_123";

        mockMvc.perform(post("/admin/settings")
                        .with(csrf())
                        .param("businessName", "이사해드림")
                        .param("phoneDisplay", "010-5297-2459")
                        .param("phoneHref", "01052972459")
                        .param("kakaoUrl", "https://pf.kakao.com/_iITad")
                        .param("tagline", "이사 고민부터 마무리까지, 이사해드림")
                        .param("naverSiteVerification", verification))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "name=\"naver-site-verification\" content=\"" + verification + "\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDraftPreviewShowsSavedCopyWhilePublicPageKeepsFallbackCopy() throws Exception {
        var region = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
        var neighborhood = neighborhoodRepository
                .findByRegionAndSlugAndEnabledTrue(region, "junggok-dong").orElseThrow();
        var service = movingServiceRepository.findBySlugAndEnabledTrue("packing-move").orElseThrow();
        var draft = landingContentRepository
                .findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service).orElseThrow();

        mockMvc.perform(get("/admin/content")
                        .param("regionId", region.getId().toString())
                        .param("neighborhoodId", neighborhood.getId().toString())
                        .param("serviceId", service.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("자동 품질 검사 통과")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("공개 전 수동 확인")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("초안 미리보기")));

        mockMvc.perform(get("/admin/content/preview")
                        .param("regionId", region.getId().toString())
                        .param("neighborhoodId", neighborhood.getId().toString())
                        .param("serviceId", service.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 초안 미리보기")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(draft.getHeadline())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(draft.getIntroduction())));

        mockMvc.perform(get("/regions/gwangjin-gu/junggok-dong/packing-move"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(draft.getHeadline()))));
    }

    @Test
    @Transactional
    @WithMockUser(roles = "ADMIN")
    void publishingRequiresExplicitPreviewConfirmation() throws Exception {
        var region = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
        var neighborhood = neighborhoodRepository
                .findByRegionAndSlugAndEnabledTrue(region, "junggok-dong").orElseThrow();
        var service = movingServiceRepository.findBySlugAndEnabledTrue("packing-move").orElseThrow();
        var draft = landingContentRepository
                .findByRegionAndNeighborhoodAndMovingService(region, neighborhood, service).orElseThrow();

        mockMvc.perform(post("/admin/content").with(csrf())
                        .param("regionId", region.getId().toString())
                        .param("neighborhoodId", neighborhood.getId().toString())
                        .param("serviceId", service.getId().toString())
                        .param("seoTitle", draft.getSeoTitle())
                        .param("metaDescription", draft.getMetaDescription())
                        .param("headline", draft.getHeadline())
                        .param("introduction", draft.getIntroduction())
                        .param("body", draft.getBody())
                        .param("published", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("관리자 미리보기")));
        assertThat(draft.isPublished()).isFalse();

        mockMvc.perform(post("/admin/content").with(csrf())
                        .param("regionId", region.getId().toString())
                        .param("neighborhoodId", neighborhood.getId().toString())
                        .param("serviceId", service.getId().toString())
                        .param("seoTitle", draft.getSeoTitle())
                        .param("metaDescription", draft.getMetaDescription())
                        .param("headline", draft.getHeadline())
                        .param("introduction", draft.getIntroduction())
                        .param("body", draft.getBody())
                        .param("published", "true")
                        .param("reviewConfirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("saved", "저장하고 공개했습니다."));
        assertThat(draft.isPublished()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShowsRegionalContentProgressTable() throws Exception {
        var gwangjin = regionRepository.findBySlugAndEnabledTrue("gwangjin-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", gwangjin.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 48")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var jungnang = regionRepository.findBySlugAndEnabledTrue("jungnang-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", jungnang.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 42")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var seongdong = regionRepository.findBySlugAndEnabledTrue("seongdong-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", seongdong.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 72")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var guri = regionRepository.findBySlugAndEnabledTrue("guri").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", guri.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 54")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var namyangju = regionRepository.findBySlugAndEnabledTrue("namyangju").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", namyangju.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 102")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var nowon = regionRepository.findBySlugAndEnabledTrue("nowon-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", nowon.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 36")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var gangdong = regionRepository.findBySlugAndEnabledTrue("gangdong-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", gangdong.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var songpa = regionRepository.findBySlugAndEnabledTrue("songpa-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", songpa.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 90")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var dongdaemun = regionRepository.findBySlugAndEnabledTrue("dongdaemun-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", dongdaemun.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 72")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var uijeongbu = regionRepository.findBySlugAndEnabledTrue("uijeongbu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", uijeongbu.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 54")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var dobong = regionRepository.findBySlugAndEnabledTrue("dobong-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", dobong.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var gangbuk = regionRepository.findBySlugAndEnabledTrue("gangbuk-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", gangbuk.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var seongbuk = regionRepository.findBySlugAndEnabledTrue("seongbuk-gu").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", seongbuk.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 96")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));

        var hanam = regionRepository.findBySlugAndEnabledTrue("hanam").orElseThrow();
        mockMvc.perform(get("/admin/content").param("regionId", hanam.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("하남시 콘텐츠 현황")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("공개 0 / 전체 60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("작성중 60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("미작성 0")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approvedKeywordCsvContainsExactly876TargetsAndNoJunggokSubKeywords() throws Exception {
        var response = mockMvc.perform(get("/admin/keywords.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=approved-keywords-876.csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(response.lines()).hasSize(877);
        assertThat(response).contains("중곡동 포장이사");
        assertThat(response).doesNotContain("중곡1동", "중곡2동", "중곡3동", "중곡4동",
                "중곡제1동", "중곡제2동", "중곡제3동", "중곡제4동");
    }

    @Test
    @Transactional
    @WithMockUser(roles = "ADMIN")
    void contentThatMissesQualityRulesIsSavedAsDraftInsteadOfPublished() throws Exception {
        var region = regionRepository.findBySlugAndEnabledTrue("gangdong-gu").orElseThrow();
        var service = movingServiceRepository.findBySlugAndEnabledTrue("packing-move").orElseThrow();

        mockMvc.perform(post("/admin/content").with(csrf())
                        .param("regionId", region.getId().toString())
                        .param("serviceId", service.getId().toString())
                        .param("seoTitle", "짧은 제목")
                        .param("metaDescription", "짧은 설명")
                        .param("headline", "짧은 제목")
                        .param("introduction", "짧은 소개")
                        .param("body", "짧은 본문")
                        .param("published", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        var saved = landingContentRepository
                .findByRegionAndNeighborhoodIsNullAndMovingService(region, service)
                .orElseThrow();
        assertThat(saved.isPublished()).isFalse();
    }

    private void assertDraftContains(String regionSlug, String neighborhoodSlug,
                                     String serviceSlug, String expectedPhrase) {
        var region = regionRepository.findBySlugAndEnabledTrue(regionSlug).orElseThrow();
        var service = movingServiceRepository.findBySlugAndEnabledTrue(serviceSlug).orElseThrow();
        var content = neighborhoodSlug == null
                ? landingContentRepository
                        .findByRegionAndNeighborhoodIsNullAndMovingService(region, service).orElseThrow()
                : landingContentRepository.findByRegionAndNeighborhoodAndMovingService(
                        region,
                        neighborhoodRepository.findByRegionAndSlugAndEnabledTrue(region, neighborhoodSlug)
                                .orElseThrow(),
                        service).orElseThrow();

        assertThat(content.isPublished()).isFalse();
        assertThat(content.getSeoTitle()).contains(expectedPhrase);
        assertThat(content.getHeadline()).contains(expectedPhrase);
        assertThat(content.getBody()).contains(expectedPhrase);
    }

    private static String targetKey(String regionSlug, String neighborhoodSlug, String serviceSlug) {
        return regionSlug + "/" + (neighborhoodSlug == null ? "@region" : neighborhoodSlug)
                + "/" + serviceSlug;
    }

    private static boolean containsUnapprovedJunggokKeyword(String value) {
        return value != null && value.matches(".*중곡(?:제)?[1-4]동.*");
    }

    private static boolean containsUnapprovedGwangjinSubKeyword(String value) {
        return value != null && value.matches(".*(?:중곡(?:제)?[1-4]동|구의[1-3]동|자양[1-4]동).*");
    }

    private static boolean containsUnapprovedNumberedDong(String value) {
        return value != null && value.matches(".*(?:본동|제?[0-9]+동).*");
    }

    private static boolean containsUnapprovedSeongdongSubKeyword(String value) {
        return value != null && value.matches(".*(?:왕십리도선동|왕십리제2동|성수동[12]가|금호동[1-4]가|행당제?[12]동).*");
    }

    private static boolean containsUnapprovedGuriSubKeyword(String value) {
        return value != null && value.matches(".*(?:교문제?[12]동|수택제?[1-3]동).*");
    }

    private static boolean containsUnapprovedNamyangjuSubKeyword(String value) {
        return value != null && value.matches(".*(?:다산제?[12]동|일패[1-5]통|이패[1-4]통|삼패[1-3]통).*");
    }

    private static boolean containsUnapprovedNowonSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:월계[1-3]동|공릉[1-2]동|하계[1-2]동|중계본동|중계[1-4]동|상계(?:10|[1-9])동).*");
    }

    private static boolean containsUnapprovedGangdongSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:상일[12]동|명일[12]동|고덕[12]동|암사[1-3]동|천호[1-3]동|성내[1-3]동|둔촌[12]동).*");
    }

    private static boolean containsUnapprovedSongpaSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:잠실(?:본|[1-9])동|풍납[12]동|송파[12]동|가락(?:본|[12])동|마천[12]동|거여[12]동|방이[12]동|문정[12]동|오륜동).*");
    }

    private static boolean containsUnapprovedDongdaemunSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:전농[12]동|답십리[12]동|장안[12]동|휘경[12]동|이문[12]동|제기[12]동).*");
    }

    private static boolean containsUnapprovedUijeongbuSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:의정부[12]동|호원[12]동|신곡[12]동|송산[1-3]동|흥선동|고산동).*");
    }

    private static boolean containsUnapprovedDobongSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:쌍문[1-4]동|방학[1-3]동|창[1-5]동|도봉[12]동).*");
    }

    private static boolean containsUnapprovedGangbukSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:번(?:제)?[1-3]동|수유(?:제)?[1-6]동|미아(?:제)?[1-9]동).*");
    }

    private static boolean containsUnapprovedSeongbukSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:돈암[12]동|정릉[1-4]동|길음[12]동|월곡(?:제)?[12]동|장위[1-3]동).*");
    }

    private static boolean containsUnapprovedHanamSubKeyword(String value) {
        return value != null && value.matches(
                ".*(?:신장[12]동|덕풍[1-3]동|미사[1-3]동|천현동|춘궁동).*"
        );
    }
}
