package likelion.festivalscope.plan.service;

import java.util.Map;

public final class FestivalThemeCatalog {
    private FestivalThemeCatalog() {}

    private record Metadata(String category, String name) {}

    private static final Map<String, Metadata> THEMES = Map.ofEntries(
            entry("CA01", "문화예술", "음악·공연"), entry("CA02", "문화예술", "미술·공예·디자인"),
            entry("CA03", "문화예술", "빛·미디어아트"), entry("CA04", "문화예술", "영화·영상·콘텐츠"),
            entry("CA05", "문화예술", "문학·책"), entry("CA06", "문화예술", "복합문화예술"),
            entry("NE01", "자연생태", "꽃·식물"), entry("NE02", "자연생태", "숲·산·걷기"),
            entry("NE03", "자연생태", "강·바다·수변"), entry("NE04", "자연생태", "계절·자연경관"),
            entry("NE05", "자연생태", "생태·환경"), entry("CC01", "주민화합", "주민화합·마을"),
            entry("CC02", "주민화합", "먹거리·야시장"), entry("CC03", "주민화합", "스포츠·레저"),
            entry("CC04", "주민화합", "가족·어린이"), entry("CC05", "주민화합", "지역상권·마켓"),
            entry("HT01", "전통역사", "역사인물·사건"), entry("HT02", "전통역사", "전통문화·민속"),
            entry("HT03", "전통역사", "문화유산"), entry("HT04", "전통역사", "전통공연·무형유산"),
            entry("HT05", "전통역사", "전통의례·제례"), entry("LS01", "지역특산물", "농산물·과일"),
            entry("LS02", "지역특산물", "수산물·해산물"), entry("LS03", "지역특산물", "축산물"),
            entry("LS04", "지역특산물", "음식·향토먹거리"), entry("LS05", "지역특산물", "주류·차·음료"),
            entry("LS06", "지역특산물", "특산품·공예")
    );

    private static Map.Entry<String, Metadata> entry(String code, String category, String name) {
        return Map.entry(code, new Metadata(category, name));
    }

    public static ParsedTheme toParsedTheme(String code) {
        Metadata metadata = THEMES.get(code);
        return metadata == null ? null : new ParsedTheme(code, metadata.category(), metadata.name());
    }

    public record ParsedTheme(String code, String category, String name) {}
}
