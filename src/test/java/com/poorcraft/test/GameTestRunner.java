package com.poorcraft.test;

import com.poorcraft.test.util.TestReportGenerator;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.TagFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Programmatic JUnit Platform runner that executes the PoorCraft automated test suite
 * and produces aggregated Markdown/HTML reports.
 */
public final class GameTestRunner {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path REPORT_DIR = Paths.get("target", "test-reports");

    private GameTestRunner() {
    }

    public static void main(String[] args) throws IOException {
        RunnerOptions options = RunnerOptions.parse(args);
        ensureReportDirectory();

        TestReportGenerator reportGenerator = new TestReportGenerator();
        options.outputDirectory.ifPresent(reportGenerator::setReportDirectory);
        reportGenerator.addSystemInfo("Generated", TS.format(LocalDateTime.now()));
        reportGenerator.addSystemInfo("Java", System.getProperty("java.version", "unknown"));
        reportGenerator.addSystemInfo("OS", System.getProperty("os.name", "unknown"));
        if (options.skipNetworking) {
            reportGenerator.addWarning("Networking tests skipped via --skip-networking flag");
        }
        if (options.skipRendering) {
            reportGenerator.addWarning("Rendering tests skipped via --skip-rendering flag");
        }

        List<String> testClasses = discoverTestClasses(options);
        if (options.verbose) {
            System.out.println("[GameTestRunner] Selected test classes: " + (testClasses.isEmpty() ? "<package scan>" : testClasses));
            System.out.println("[GameTestRunner] Options: skipNetworking=" + options.skipNetworking
                + ", skipRendering=" + options.skipRendering
                + ", classNameFilter=" + options.classNameFilter.orElse("<none>")
                + ", selectors=" + options.selectorFilters);
            options.outputDirectory.ifPresent(path -> System.out.println("[GameTestRunner] Report directory: " + path.toAbsolutePath()));
        }

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = buildRequest(testClasses, options);
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        int status = processSummary(summary, reportGenerator);

        writeConsoleSummary(summary);
        reportGenerator.generateConsoleReport();
        reportGenerator.generateMarkdownReport();
        reportGenerator.generateHtmlReport();

        if (summary.getFailures().isEmpty()) {
            Files.writeString(REPORT_DIR.resolve("latest-success.txt"),
                "All tests passed at " + TS.format(LocalDateTime.now()), StandardCharsets.UTF_8);
        } else {
            Files.writeString(REPORT_DIR.resolve("latest-failure.txt"),
                summary.getFailures().stream()
                    .map(failure -> failure.getTestIdentifier().getDisplayName() + " - " + failure.getException())
                    .collect(Collectors.joining(System.lineSeparator())), StandardCharsets.UTF_8);
        }

        System.exit(status);
    }

    private static LauncherDiscoveryRequest buildRequest(List<String> testClasses, RunnerOptions options) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        if (testClasses.isEmpty()) {
            builder.selectors(DiscoverySelectors.selectPackage("com.poorcraft.test"));
        } else {
            List<DiscoverySelector> selectors = testClasses.stream()
                .map(DiscoverySelectors::selectClass)
                .collect(Collectors.toList());
            builder.selectors(selectors);
        }

        options.classNameFilter.ifPresent(pattern ->
            builder.filters(ClassNameFilter.includeClassNamePatterns(pattern))
        );
        if (options.skipNetworking) {
            builder.filters(TagFilter.excludeTags("networking"));
        }
        if (options.skipRendering) {
            builder.filters(TagFilter.excludeTags("rendering"));
        }
        return builder.build();
    }

    private static int processSummary(TestExecutionSummary summary, TestReportGenerator reportGenerator) {
        summary.getFailures().forEach(failure ->
            reportGenerator.addError("Failures", failure.getTestIdentifier().getDisplayName(), failure.getException()));

        long testsFound = summary.getTestsFoundCount();
        long testsSucceeded = summary.getTestsSucceededCount();
        long testsFailed = summary.getTestsFailedCount();
        long testsSkipped = summary.getTestsSkippedCount();

        reportGenerator.addSystemInfo("Tests Found", String.valueOf(testsFound));
        reportGenerator.addSystemInfo("Tests Passed", String.valueOf(testsSucceeded));
        reportGenerator.addSystemInfo("Tests Failed", String.valueOf(testsFailed));
        reportGenerator.addSystemInfo("Tests Skipped", String.valueOf(testsSkipped));

        summary.getFailures().forEach(failure ->
            reportGenerator.addTestResult("Failures", failure.getTestIdentifier().getDisplayName(), false,
                failure.getException().getMessage()));

        boolean success = testsFailed == 0 && summary.getFailures().isEmpty();
        reportGenerator.addTestResult("Summary", "Overall", success,
            success ? "All tests passed" : "Some tests failed");
        return success ? 0 : 1;
    }

    private static void writeConsoleSummary(TestExecutionSummary summary) {
        System.out.println("\n===== PoorCraft Test Summary =====");
        System.out.println("Tests found: " + summary.getTestsFoundCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests failed: " + summary.getTestsFailedCount());
        System.out.println("Tests skipped: " + summary.getTestsSkippedCount());
        summary.getFailures().forEach(failure -> {
            System.out.println("\nFailure: " + failure.getTestIdentifier().getDisplayName());
            failure.getException().printStackTrace(System.out);
        });
    }

    private static void ensureReportDirectory() throws IOException {
        Files.createDirectories(REPORT_DIR);
    }

    private static List<String> discoverTestClasses(RunnerOptions options) throws IOException {
        if (!options.selectorFilters.isEmpty()) {
            return options.selectorFilters;
        }
        Path testRoot = Paths.get("src", "test", "java");
        if (!Files.exists(testRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(testRoot)) {
            return stream.filter(path -> path.toString().endsWith("Test.java"))
                .map(testRoot::relativize)
                .map(Path::toString)
                .map(path -> path.replace('/', '.').replace('\\', '.'))
                .map(name -> name.substring(0, name.length() - ".java".length()))
                .collect(Collectors.toList());
        }
    }

    private static final class RunnerOptions {
        private final List<String> selectorFilters;
        private final Optional<String> classNameFilter;
        private final Optional<Path> outputDirectory;
        private final boolean skipNetworking;
        private final boolean skipRendering;
        private final boolean verbose;

        private RunnerOptions(List<String> selectorFilters, Optional<String> classNameFilter,
                              Optional<Path> outputDirectory, boolean skipNetworking,
                              boolean skipRendering, boolean verbose) {
            this.selectorFilters = selectorFilters;
            this.classNameFilter = classNameFilter;
            this.outputDirectory = outputDirectory;
            this.skipNetworking = skipNetworking;
            this.skipRendering = skipRendering;
            this.verbose = verbose;
        }

        static RunnerOptions parse(String[] args) {
            List<String> selectors = new ArrayList<>();
            Optional<String> classNamePattern = Optional.empty();
            Optional<Path> outputDir = Optional.empty();
            boolean skipNetworking = false;
            boolean skipRendering = false;
            boolean verbose = false;

            for (int i = 0; i < args.length; i++) {
                String rawArg = args[i];
                String arg = rawArg.toLowerCase(Locale.ROOT);
                switch (arg) {
                    case "--test", "-t" -> {
                        if (i + 1 < args.length) {
                            selectors.add(args[++i]);
                        }
                    }
                    case "--filter", "-f" -> {
                        if (i + 1 < args.length) {
                            classNamePattern = java.util.Optional.of(args[++i]);
                        }
                    }
                    case "--skip-networking" -> skipNetworking = true;
                    case "--skip-rendering" -> skipRendering = true;
                    case "--verbose", "-v" -> verbose = true;
                    case "--output-dir" -> {
                        if (i + 1 < args.length) {
                            outputDir = Optional.of(Paths.get(args[++i]));
                        }
                    }
                    default -> {
                    }
                }
            }
            return new RunnerOptions(selectors, classNamePattern, outputDir, skipNetworking, skipRendering, verbose);
        }
    }
}
