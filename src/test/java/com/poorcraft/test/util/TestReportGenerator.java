package com.poorcraft.test.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Collects per-test results and produces Markdown/HTML/console summaries.
 * This class is intentionally lightweight so it can be used from any test
 * without pulling in reporting frameworks.
 */
public class TestReportGenerator {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final Path REPORT_DIRECTORY = Paths.get("target", "test-reports");
    private static final Map<Pattern, String> SUGGESTED_FIXES;

    private final Map<String, List<TestResult>> resultsByCategory = new LinkedHashMap<>();
    private final Map<String, String> systemInfo = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private Path reportDirectory = REPORT_DIRECTORY;

    public TestReportGenerator() {
    }

    static {
        Map<Pattern, String> fixes = new LinkedHashMap<>();
        fixes.put(Pattern.compile("(?i)failed to initialize glfw"),
            "Ensure GLFW initialisation happens once per JVM and that all contexts are properly destroyed.");
        fixes.put(Pattern.compile("(?i)resource(?:\\s|_)not(?:\\s|_)found"),
            "Verify resource paths and include required assets on the classpath or resource manifest.");
        fixes.put(Pattern.compile("(?i)timeout|keep-alive"),
            "Consider increasing networking timeouts or ensuring keep-alive handlers respond within the allotted window.");
        SUGGESTED_FIXES = Collections.unmodifiableMap(fixes);
    }

    public void addSystemInfo(String key, String value) {
        systemInfo.put(key, value);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void setReportDirectory(Path directory) {
        if (directory != null) {
            this.reportDirectory = directory;
        }
    }

    public void addTestResult(String category, String testName, boolean passed, String message) {
        resultsByCategory.computeIfAbsent(category, ignored -> new ArrayList<>())
            .add(new TestResult(testName, passed, Optional.empty(), message));
    }

    public void addError(String category, String testName, Throwable error) {
        resultsByCategory.computeIfAbsent(category, ignored -> new ArrayList<>())
            .add(new TestResult(testName, false, Optional.ofNullable(error), error.getMessage()));
    }

    public void generateMarkdownReport() {
        ensureReportDirectory(reportDirectory);
        Path target = reportDirectory.resolve("test-report-" + FILE_TS.format(LocalDateTime.now()) + ".md");
        StringBuilder builder = new StringBuilder();
        List<String> suggestions = collectSuggestedFixes();

        builder.append("# PoorCraft Automated Test Report\n\n");
        builder.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        appendSummary(builder);
        appendSystemInfo(builder);
        appendWarnings(builder);
        appendSuggestedFixesMarkdown(builder, suggestions);
        appendDetailedResults(builder);

        try {
            Files.writeString(target, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[TestReportGenerator] Failed to write Markdown report: " + e.getMessage());
        }
    }

    public void generateHtmlReport() {
        ensureReportDirectory(reportDirectory);
        Path target = reportDirectory.resolve("test-report-" + FILE_TS.format(LocalDateTime.now()) + ".html");
        List<String> suggestions = collectSuggestedFixes();

        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/><title>PoorCraft Test Report</title>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;padding:2rem;background:#1e1e1e;color:#e0e0e0;}")
            .append("h1{color:#6ee7b7;}")
            .append(".summary{background:#111827;padding:1rem;border-radius:8px;margin-bottom:1rem;}")
            .append(".category{margin-top:1.5rem;}")
            .append("table{width:100%;border-collapse:collapse;margin-top:0.5rem;}")
            .append("th,td{padding:0.5rem;border-bottom:1px solid #374151;text-align:left;}")
            .append(".pass{color:#22c55e;font-weight:bold;}")
            .append(".fail{color:#ef4444;font-weight:bold;}")
            .append(".warn{color:#f97316;font-weight:bold;}")
            .append(".stacktrace{background:#0f172a;padding:0.75rem;border-radius:6px;white-space:pre-wrap;}")
            .append("</style></head><body><h1>PoorCraft Automated Test Report</h1>");

        builder.append("<section class=\"summary\">");
        builder.append("<p><strong>Generated:</strong> ").append(LocalDateTime.now()).append("</p>");
        builder.append(buildSummaryHtml());
        builder.append("</section>");

        if (!systemInfo.isEmpty()) {
            builder.append("<section class=\"summary\"><h2>System Information</h2><ul>");
            systemInfo.forEach((key, value) -> builder.append("<li><strong>").append(key)
                .append(":</strong> ").append(value).append("</li>"));
            builder.append("</ul></section>");
        }

        if (!warnings.isEmpty()) {
            builder.append("<section class=\"summary\"><h2>Warnings</h2><ul>");
            warnings.forEach(warning -> builder.append("<li class=\"warn\">").append(warning).append("</li>"));
            builder.append("</ul></section>");
        }

        if (!suggestions.isEmpty()) {
            builder.append("<section class=\"summary\"><h2>Suggested Fixes</h2><ul>");
            suggestions.forEach(suggestion -> builder.append("<li>").append(escapeHtml(suggestion)).append("</li>"));
            builder.append("</ul></section>");
        }

        resultsByCategory.forEach((category, results) -> {
            builder.append("<section class=\"category\"><h2>").append(category).append("</h2>");
            builder.append("<table><thead><tr><th>Test</th><th>Status</th><th>Details</th></tr></thead><tbody>");
            for (TestResult result : results) {
                builder.append("<tr><td>").append(result.testName).append("</td>")
                    .append("<td class=\"").append(result.passed ? "pass" : "fail")
                    .append("\">")
                    .append(result.passed ? "Passed" : "Failed")
                    .append("</td><td>")
                    .append(result.message == null ? "" : escapeHtml(result.message));
                result.error.ifPresent(error -> builder.append("<div class=\"stacktrace\">")
                    .append(escapeHtml(stackTraceToString(error))).append("</div>"));
                builder.append("</td></tr>");
            }
            builder.append("</tbody></table></section>");
        });

        builder.append("</body></html>");

        try {
            Files.writeString(target, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[TestReportGenerator] Failed to write HTML report: " + e.getMessage());
        }
    }

    public void generateConsoleReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n======== PoorCraft Test Summary ========\n");
        appendSummary(builder);
        if (!warnings.isEmpty()) {
            builder.append("Warnings:\n");
            warnings.forEach(warning -> builder.append("  - ").append(warning).append('\n'));
        }
        List<String> suggestions = collectSuggestedFixes();
        if (!suggestions.isEmpty()) {
            builder.append("Suggested Fixes:\n");
            suggestions.forEach(suggestion -> builder.append("  - ").append(suggestion).append('\n'));
        }
        System.out.println(builder.toString());
    }

    private void appendSummary(StringBuilder builder) {
        int total = 0;
        int passed = 0;
        int failed = 0;

        for (List<TestResult> results : resultsByCategory.values()) {
            for (TestResult result : results) {
                total++;
                if (result.passed) {
                    passed++;
                } else {
                    failed++;
                }
            }
        }

        builder.append("## Summary\n");
        builder.append("- Total tests: ").append(total).append("\n");
        builder.append("- Passed: ").append(passed).append("\n");
        builder.append("- Failed: ").append(failed).append("\n\n");
    }

    private void appendSystemInfo(StringBuilder builder) {
        if (systemInfo.isEmpty()) {
            return;
        }
        builder.append("## System Information\n");
        systemInfo.forEach((key, value) -> builder.append("- ").append(key).append(": ").append(value).append("\n"));
        builder.append('\n');
    }

    private void appendWarnings(StringBuilder builder) {
        if (warnings.isEmpty()) {
            return;
        }
        builder.append("## Warnings\n");
        warnings.forEach(warning -> builder.append("- ").append(warning).append("\n"));
        builder.append('\n');
    }

    private void appendSuggestedFixesMarkdown(StringBuilder builder, List<String> suggestions) {
        if (suggestions.isEmpty()) {
            return;
        }
        builder.append("## Suggested Fixes\n");
        suggestions.forEach(suggestion -> builder.append("- ").append(suggestion).append("\n"));
        builder.append('\n');
    }

    private void appendDetailedResults(StringBuilder builder) {
        resultsByCategory.forEach((category, results) -> {
            builder.append("## ").append(category).append('\n');
            for (TestResult result : results) {
                builder.append("- ").append(result.passed ? "[PASS] " : "[FAIL] ")
                    .append(result.testName);
                if (result.message != null) {
                    builder.append(" - ").append(result.message);
                }
                builder.append('\n');
                result.error.ifPresent(error -> builder.append("  Stacktrace:\n")
                    .append("  ").append(stackTraceToString(error)).append('\n'));
            }
            builder.append('\n');
        });
    }

    private String buildSummaryHtml() {
        int total = 0;
        int passed = 0;
        int failed = 0;

        for (List<TestResult> results : resultsByCategory.values()) {
            for (TestResult result : results) {
                total++;
                if (result.passed) {
                    passed++;
                } else {
                    failed++;
                }
            }
        }

        return new StringBuilder()
            .append("<p><strong>Total:</strong> ").append(total)
            .append(" &nbsp; <span class=\"pass\">Passed: ").append(passed)
            .append("</span> &nbsp; <span class=\"fail\">Failed: ").append(failed)
            .append("</span></p>")
            .toString();
    }

    private void ensureReportDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println("[TestReportGenerator] Failed to create report directory: " + e.getMessage());
        }
    }

    private List<String> collectSuggestedFixes() {
        Set<String> suggestions = new LinkedHashSet<>();
        for (List<TestResult> results : resultsByCategory.values()) {
            for (TestResult result : results) {
                StringBuilder evidence = new StringBuilder();
                if (result.message != null) {
                    evidence.append(result.message).append('\n');
                }
                result.error.ifPresent(error -> evidence.append(stackTraceToString(error)));
                String text = evidence.toString();
                if (text.isEmpty()) {
                    continue;
                }
                for (Map.Entry<Pattern, String> entry : SUGGESTED_FIXES.entrySet()) {
                    if (entry.getKey().matcher(text).find()) {
                        suggestions.add(entry.getValue());
                    }
                }
            }
        }
        return new ArrayList<>(suggestions);
    }

    private String stackTraceToString(Throwable error) {
        StringBuilder builder = new StringBuilder();
        builder.append(error).append('\n');
        for (StackTraceElement element : error.getStackTrace()) {
            builder.append("    at ").append(element).append('\n');
        }
        return builder.toString();
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private record TestResult(String testName, boolean passed, Optional<Throwable> error, String message) {
    }
}
