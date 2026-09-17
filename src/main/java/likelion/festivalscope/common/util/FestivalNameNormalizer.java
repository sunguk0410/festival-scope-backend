package likelion.festivalscope.common.util;

import java.util.Locale;

public final class FestivalNameNormalizer {
    private FestivalNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\b\\d{4}\\s*년?\\b", "");
        normalized = normalized.replaceAll("제\\s*\\d+\\s*회", "");
        normalized = normalized.replaceAll("\\b\\d+\\s*회\\b", "");
        return normalized.replaceAll("[^\\p{L}\\p{N}]", "");
    }

    public static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    public static double similarity(String left, String right) {
        String first = normalize(left);
        String second = normalize(right);
        if (first.equals(second)) return 1.0;
        if (first.isEmpty() || second.isEmpty()) return 0.0;

        int distance = levenshteinDistance(first, second);
        return 1.0 - (double) distance / Math.max(first.length(), second.length());
    }

    private static int levenshteinDistance(String first, String second) {
        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];
        for (int j = 0; j <= second.length(); j++) previous[j] = j;

        for (int i = 1; i <= first.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int substitution = previous[j - 1] + (first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[second.length()];
    }
}
