package com.template.usermanagement.wilayah.validation;

/**
 * Shared utilities for validation providers: Levenshtein similarity and
 * the VALID / PARTIAL_ZIP / PARTIAL_NAME / INVALID status matrix.
 */
abstract class AbstractValidationProvider implements WilayahValidationProvider {

    protected static final double SIMILARITY_THRESHOLD = 0.80;

    // ─── Status helpers ────────────────────────────────────────────────────────

    protected String determineStatus(boolean nameValid, boolean hasLocalZip, boolean zipMatch) {
        if (nameValid && (!hasLocalZip || zipMatch)) return "VALID";
        if (nameValid)                               return "PARTIAL_ZIP";
        if (zipMatch)                                return "PARTIAL_NAME";
        return "INVALID";
    }

    // ─── Levenshtein similarity ────────────────────────────────────────────────

    /** Returns similarity in [0.0, 1.0] — 1.0 = identical. */
    public static double computeSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        String s1 = a.toLowerCase().trim();
        String s2 = b.toLowerCase().trim();
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(s1, s2) / maxLen;
    }

    private static int levenshtein(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }

    // ─── String helpers ────────────────────────────────────────────────────────

    protected static String str(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    protected static Double parseDouble(Object val) {
        try { return val != null ? Double.parseDouble(val.toString()) : null; }
        catch (NumberFormatException e) { return null; }
    }

    protected static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
