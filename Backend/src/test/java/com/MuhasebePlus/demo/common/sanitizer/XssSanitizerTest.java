package com.MuhasebePlus.demo.common.sanitizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class XssSanitizerTest {

    // ── Null / blank koruma ───────────────────────────────────────────────────

    @Test
    void sanitize_whenNull_returnsNull() {
        assertThat(XssSanitizer.sanitize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void sanitize_whenBlank_returnsSameInput(String blank) {
        assertThat(XssSanitizer.sanitize(blank)).isEqualTo(blank);
    }

    // ── Script tag temizleme ──────────────────────────────────────────────────

    @Test
    void sanitize_scriptTagWithContent_removesScriptBlock() {
        String input = "<script>alert('xss')</script>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<script");
        assertThat(result).doesNotContain("alert('xss')");
    }

    @Test
    void sanitize_uppercaseScript_removesScriptBlock() {
        String input = "<SCRIPT>alert(1)</SCRIPT>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<script");
    }

    @Test
    void sanitize_mixedCaseScript_removesScriptBlock() {
        String input = "<ScRiPt>kötüKod()</ScRiPt>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<script");
    }

    // ── Event handler temizleme ───────────────────────────────────────────────

    @Test
    void sanitize_onclickEventHandler_removed() {
        String input = "<img src='x' onclick=\"alert(1)\">";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("onclick");
    }

    @Test
    void sanitize_onerrorEventHandler_removed() {
        String input = "<img src='invalid' onerror=\"alert(1)\">";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContain("onerror");
    }

    // ── javascript: protokolü ─────────────────────────────────────────────────

    @Test
    void sanitize_javascriptProtocol_removed() {
        String input = "<a href=\"javascript:alert(1)\">tikla</a>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("javascript:");
    }

    @Test
    void sanitize_javascriptWithSpaces_removed() {
        String input = "javascript  :  alert(1)";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("javascript");
    }

    // ── Tehlikeli tag'ler ─────────────────────────────────────────────────────

    @Test
    void sanitize_iframeTag_removed() {
        String input = "<iframe src=\"http://evil.com\"></iframe>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<iframe");
    }

    @Test
    void sanitize_objectTag_removed() {
        String input = "<object data=\"evil.swf\"></object>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<object");
    }

    @Test
    void sanitize_embedTag_removed() {
        String input = "<embed src=\"evil.swf\">";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<embed");
    }

    @Test
    void sanitize_styleTag_removed() {
        String input = "<style>body{display:none}</style>";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("<style");
    }

    // ── Temiz metin korunur ───────────────────────────────────────────────────

    @Test
    void sanitize_cleanText_returnsSameReference() {
        String clean = "Merhaba Dunya, bu guvenli bir metindir.";

        String result = XssSanitizer.sanitize(clean);

        assertThat(result).isSameAs(clean);
    }

    @Test
    void sanitize_textWithNumbers_unchanged() {
        String input = "Fatura No: 2026-0042, Tutar: 1.250,00 TL";

        assertThat(XssSanitizer.sanitize(input)).isEqualTo(input);
    }

    @Test
    void sanitize_combinedAttack_removesBothScriptAndEventHandler() {
        String input = "<script>attack(1)</script><img onerror=\"badCode()\">";

        String result = XssSanitizer.sanitize(input);

        assertThat(result).doesNotContainIgnoringCase("script");
        assertThat(result).doesNotContain("onerror");
    }
}
