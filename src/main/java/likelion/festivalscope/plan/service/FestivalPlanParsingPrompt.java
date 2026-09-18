package likelion.festivalscope.plan.service;

public final class FestivalPlanParsingPrompt {
    private FestivalPlanParsingPrompt() {}

    public static final String SYSTEM = """
            You extract structured festival plan data from the supplied Korean planning document.
            Return JSON only with these keys:
            planName, festivalName, festivalStatus, firstHeldYear, sido, sigungu, venueName, venueAddress,
            latitude, longitude, startDate, endDate, targetVisitorCount, venueType, capacity, themeCodes, programNames.
            Use null when a value is not explicitly present or cannot be determined with confidence.
            Use [] for themeCodes or programNames when no values are found.
            Do not guess latitude or longitude from an address. Use yyyy-MM-dd for dates and integer values for counts.
            festivalStatus must be EXISTING or NEW, and venueType must be INDOOR, OUTDOOR, or MIXED; otherwise use null.
            Extract programNames as the actual program names written in the document. Do not generalize them into trend keywords.
            themeCodes must contain only codes from this catalog:
            CA01 음악·공연, CA02 미술·공예·디자인, CA03 빛·미디어아트, CA04 영화·영상·콘텐츠, CA05 문학·책, CA06 복합문화예술;
            NE01 꽃·식물, NE02 숲·산·걷기, NE03 강·바다·수변, NE04 계절·자연경관, NE05 생태·환경;
            CC01 주민화합·마을, CC02 먹거리·야시장, CC03 스포츠·레저, CC04 가족·어린이, CC05 지역상권·마켓;
            HT01 역사인물·사건, HT02 전통문화·민속, HT03 문화유산, HT04 전통공연·무형유산, HT05 전통의례·제례;
            LS01 농산물·과일, LS02 수산물·해산물, LS03 축산물, LS04 음식·향토먹거리, LS05 주류·차·음료, LS06 특산품·공예.
            Extract only information explicitly stated or clearly supported by the document. Do not create missing information.
            """;
}
