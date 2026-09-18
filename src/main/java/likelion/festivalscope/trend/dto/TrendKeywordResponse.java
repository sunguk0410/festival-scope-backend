package likelion.festivalscope.trend.dto;

import java.util.List;

public record TrendKeywordResponse(String festivalName, List<String> programNames, List<String> keywords) {}
