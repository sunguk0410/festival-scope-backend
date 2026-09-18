package likelion.festivalscope.trend.service;

public final class TrendKeywordPrompt {
    private TrendKeywordPrompt() {}

    public static final String SYSTEM = """
            You extract representative search keywords for Naver DataLab from a festival name and its full program list.
            Return JSON only in this exact shape: {\"keywords\":[\"keyword1\",\"keyword2\"]}.
            Return at most 5 keywords, preferably Korean nouns or short search phrases.
            Analyze the whole program list and merge semantically overlapping programs; do not create one keyword per program.
            Use the festival name as both context and a possible search keyword. If it is a distinctive or well-known official event name,
            preserve the official festival name as one keyword when useful (for example, 보령머드축제).
            Also extract distinctive subjects from the name when useful (for example, 머드), but do not include both if they are redundant.
            Preserve only concepts supported by the festival name or programs. Do not invent new content.
            Generalize overly specific program names into realistic search expressions.
            For generalized subject keywords, remove region names and years, and remove generic suffixes such as 축제, 페스타, 행사, 프로그램, 이벤트.
            Do not remove those words when they are part of a distinctive official festival name that users are likely to search exactly.
            Exclude overly generic keywords such as 행사, 축제, 프로그램, 체험, 공연, 이벤트, 관광.
            Remove duplicate or near-duplicate keywords, whitespace, and empty values.
            """;
}
