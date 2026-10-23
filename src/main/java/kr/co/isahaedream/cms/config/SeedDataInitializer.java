package kr.co.isahaedream.cms.config;

import kr.co.isahaedream.cms.domain.*;
import kr.co.isahaedream.cms.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SeedDataInitializer implements ApplicationRunner {
    private final RegionRepository regionRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final MovingServiceRepository movingServiceRepository;
    private final SiteSettingsRepository siteSettingsRepository;

    public SeedDataInitializer(RegionRepository regionRepository,
                               NeighborhoodRepository neighborhoodRepository,
                               MovingServiceRepository movingServiceRepository,
                               SiteSettingsRepository siteSettingsRepository) {
        this.regionRepository = regionRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.movingServiceRepository = movingServiceRepository;
        this.siteSettingsRepository = siteSettingsRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedServices();
        seedRegions();
        if (!siteSettingsRepository.existsById(1L)) {
            siteSettingsRepository.save(new SiteSettings());
        }
    }

    private void seedServices() {
        if (movingServiceRepository.count() > 0) return;
        movingServiceRepository.saveAll(List.of(
                new MovingService("포장이사", "packing-move", "포장부터 운송, 정리까지 한 번에 진행합니다.", 1),
                new MovingService("보관이사", "storage-move", "입주 일정에 맞춰 안전하게 보관하고 운송합니다.", 2),
                new MovingService("입주이사", "move-in", "새 보금자리 일정과 동선에 맞춘 이사 서비스입니다.", 3),
                new MovingService("사무실이사", "office-move", "업무 공백을 줄이도록 계획적으로 이전합니다.", 4),
                new MovingService("아파트이사", "apartment-move", "엘리베이터와 사다리차 동선을 고려해 진행합니다.", 5),
                new MovingService("이삿짐센터", "moving-center", "지역 여건에 맞는 차량과 인력을 상담합니다.", 6)
        ));
    }

    private void seedRegions() {
        if (regionRepository.count() > 0) return;
        List<RegionSeed> seeds = List.of(
                region("중랑구", "jungnang-gu", "면목동:myeonmok-dong", "묵동:muk-dong", "신내동:sinnae-dong", "중화동:junghwa-dong", "상봉동:sangbong-dong", "망우동:mangu-dong"),
                region("광진구", "gwangjin-gu", "중곡동:junggok-dong", "구의동:guui-dong", "자양동:jay ang-dong", "군자동:gunja-dong", "능동:neung-dong", "광장동:gwangjang-dong", "화양동:hwayang-dong"),
                region("성동구", "seongdong-gu", "마장동:majang-dong", "금호동:geumho-dong", "용답동:yongdap-dong", "응봉동:eungbong-dong", "홍익동:hongik-dong", "성수동:seongsu-dong", "행당동:haengdang-dong", "옥수동:oksu-dong", "송정동:songjeong-dong", "사근동:sageun-dong", "왕십리동:wangsimni-dong"),
                region("구리", "guri", "갈매동:galmae-dong", "사노동:sano-dong", "인창동:inchang-dong", "교문동:gyomun-dong", "수택동:sutaek-dong", "아천동:acheon-dong", "토평동:topyeong-dong", "동구동:donggu-dong"),
                region("남양주", "namyangju", "평내동:pyeongnae-dong", "일패동:ilpae-dong", "금곡동:geumgok-dong", "삼패동:sampae-dong", "이패동:ipae-dong", "가운동:gaun-dong", "다산동:dasan-dong", "도농동:donong-dong", "지금동:jigeum-dong", "호평동:hopyeong-dong", "별내동:byeollae-dong", "진접읍:jinjeop-eup", "와부읍:wabu-eup", "진건읍:jingeon-eup", "화도읍:hwado-eup", "오남읍:onam-eup"),
                region("노원구", "nowon-gu", "월계동:Wolgye-dong", "공릉동:gongneung-dong", "하계동:hagye-dong", "상계동:sanggye-dong", "중계동:junggye-dong"),
                region("강동구", "gangdong-gu", "길동:gil-dong", "고덕동:godeok-dong", "상일동:sangil-dong", "천호동:cheonho-dong", "강일동:gangil-dong", "성내동:seongnae-dong", "암사동:amsa-dong", "둔촌동:dunchon-dong", "명일동:myeongil-dong"),
                region("송파구", "songpa-gu", "잠실동:jamsil-dong", "신천동:sincheon-dong", "풍납동:pungnap-dong", "송파동:songpa-dong", "석촌동:seokchon-dong", "삼전동:samjeon-dong", "가락동:garak-dong", "위례동:wirye-dong", "마천동:macheon-dong", "거여동:geoyeo-dong", "오금동:ogeum-dong", "방이동:bangi-dong", "장지동:jangji-dong", "문정동:munjeong-dong"),
                region("동대문구", "dongdaemun-gu", "용두동:yongdu-dong", "전농동:jeonnong-dong", "장안동:jangan-dong", "회기동:hoegi-dong", "이문동:imun-dong", "용신동:yongsin-dong", "휘경동:hwigyeong-dong", "청량리동:cheongnyangni-dong", "답십리동:dapsimni-dong", "제기동:jegi-dong", "신설동:sinseol-dong"),
                region("의정부", "uijeongbu", "호원동:howon-dong", "신곡동:singok-dong", "자금동:jageum-dong", "녹양동:nogyang-dong", "가능동:ganeung-dong", "송산동:songsan-dong", "장암동:jangam-dong", "의정부동:uijeongbu-dong"),
                region("도봉구", "dobong-gu", "쌍문동:ssangmun-dong", "방학동:banghak-dong", "창동:chang-dong", "도봉동:dobong-dong"),
                region("강북구", "gangbuk-gu", "번동:beon-dong", "미아동:mia-dong", "수유동:suyu-dong", "우이동:ui-dong", "삼양동:samyang-dong", "송중동:songjung-dong", "송천동:songcheon-dong", "인수동:insu-dong", "삼각산동:samgaksan-dong"),
                region("성북구", "seongbuk-gu", "성북동:seongbuk-dong", "돈암동:donam-dong", "동소문동:dongsomun-dong", "삼선동:samseon-dong", "동선동:dongseon-dong", "안암동:anam-dong", "보문동:bomun-dong", "정릉동:jeongneung-dong", "길음동:gireum-dong", "종암동:jongam-dong", "하월곡동:hawolgok-dong", "상월곡동:sangwolgok-dong", "장위동:jangwi-dong", "석관동:seokgwan-dong", "월곡동:wolgok-dong"),
                region("하남시", "hanam", "하남:hanam", "덕풍동:deokpung-dong", "미사동:misa-dong", "위례동:wirye-dong", "감일동:gamil-dong", "초이동:choi-dong", "감북동:gambuk-dong", "풍산동:pungsan-dong", "신장동:sinjang-dong")
        );

        int regionOrder = 1;
        for (RegionSeed seed : seeds) {
            Region region = regionRepository.save(new Region(seed.name(), seed.slug(), regionOrder++));
            int neighborhoodOrder = 1;
            for (NameSlug neighborhood : seed.neighborhoods()) {
                neighborhoodRepository.save(new Neighborhood(region, neighborhood.name(), neighborhood.slug(), neighborhoodOrder++));
            }
        }
    }

    private static RegionSeed region(String name, String slug, String... neighborhoods) {
        return new RegionSeed(name, slug, java.util.Arrays.stream(neighborhoods)
                .map(item -> item.split(":", 2))
                .map(parts -> new NameSlug(parts[0], parts[1].replace(" ", "" ).toLowerCase()))
                .toList());
    }

    private record NameSlug(String name, String slug) {}
    private record RegionSeed(String name, String slug, List<NameSlug> neighborhoods) {}
}
