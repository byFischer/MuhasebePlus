package com.MuhasebePlus.demo.common.sanitizer;

import java.util.regex.Pattern;

public final class XssSanitizer {

    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("\\bon\\w+\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern JAVASCRIPT_PATTERN =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);

    private XssSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String cleaned = input;

        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EVENT_HANDLER_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PATTERN.matcher(cleaned).replaceAll("");

        cleaned = cleaned.replace("<script", "")
                .replace("</script>", "")
                .replace("<iframe", "")
                .replace("</iframe>", "")
                .replace("<object", "")
                .replace("</object>", "")
                .replace("<embed", "")
                .replace("</embed>", "")
                .replace("<link", "")
                .replace("<style", "")
                .replace("</style>", "");

        return cleaned.equals(input) ? input : cleaned;
    }
}
