package io.quarkus.bot.action.buildreporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.ConfigFile;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.quarkus.bot.action.buildreporter.config.QuarkusGitHubBotConfigFile;
import io.quarkus.bot.action.buildreporter.util.Strings;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterActionHandler;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterConfig;

public class BuildReporterAction {

    private static final Logger LOG = Logger.getLogger(BuildReporterAction.class);

    private static final String BUILD_REPORTS_ARTIFACTS_PATH_INPUT_NAME = "build-reports-artifacts-path";
    private static final String FORKS_ONLY_INPUT_NAME = "forks-only";
    private static final String DEVELOCITY_URL_INPUT_NAME = "develocity-url";

    @Inject
    BuildReporterActionHandler buildReporterActionHandler;

    @Action
    void postJobSummary(Inputs inputs, Commands commands, Context context, GitHub gitHub,
            @ConfigFile("quarkus-github-bot.yml") QuarkusGitHubBotConfigFile quarkusBotConfigFile) throws IOException {
        boolean forksOnly = inputs.getBoolean(FORKS_ONLY_INPUT_NAME).orElse(Boolean.FALSE);

        GHWorkflowRun workflowRun = gitHub.getRepository(context.getGitHubRepository())
                .getWorkflowRun(context.getGitHubRunId());

        if (forksOnly && !workflowRun.getRepository().isFork()) {
            return;
        }

        Path buildReportsArtifactsPath = Path.of(inputs.getRequired(BUILD_REPORTS_ARTIFACTS_PATH_INPUT_NAME));

        if (!Files.exists(buildReportsArtifactsPath)) {
            LOG.error("Unable to find the directory containing the build reports: " + buildReportsArtifactsPath);

            return;
        }

        Comparator<GHWorkflowJob> workflowJobComparator;

        switch (context.getGitHubRepository()) {
            case "quarkusio/quarkus":
                workflowJobComparator = QuarkusWorkflowJobComparator.INSTANCE;
                break;
            case "quarkiverse/quarkus-langchain4j":
                workflowJobComparator = QuarkusLangChain4jWorkflowJobComparator.INSTANCE;
                break;
            default:
                // it will use the default ones
                workflowJobComparator = null;
        }

        BuildReporterConfig.Builder buildReporterConfigBuilder = new BuildReporterConfig.Builder()
                // only create check run and annotation if this is a fork
                .createCheckRun(workflowRun.getRepository().isFork())
                .workflowJobComparator(workflowJobComparator);

        Optional<String> develocityUrl = inputs.get(DEVELOCITY_URL_INPUT_NAME);
        if (develocityUrl.isPresent()) {
            buildReporterConfigBuilder.enableDevelocity(true).develocityUrl(develocityUrl.get());
        } else if (quarkusBotConfigFile != null && quarkusBotConfigFile.develocity.enabled
                && Strings.isNotBlank(quarkusBotConfigFile.develocity.url)) {
            buildReporterConfigBuilder.enableDevelocity(true).develocityUrl(quarkusBotConfigFile.develocity.url);
        }

        if (quarkusBotConfigFile != null) {
            buildReporterConfigBuilder.ignoredFlakyTests(quarkusBotConfigFile.workflowRunAnalysis.ignoredFlakyTests);
        }

        BuildReporterConfig buildReporterConfig = buildReporterConfigBuilder.build();

        Optional<String> report = buildReporterActionHandler.generateReport(context.getGitHubWorkflow(), workflowRun,
                buildReportsArtifactsPath, buildReporterConfig);

        if (report.isEmpty()) {
            return;
        }

        commands.appendJobSummary(report.get());
    }

    private final static class QuarkusWorkflowJobComparator implements Comparator<GHWorkflowJob> {

        private static final QuarkusWorkflowJobComparator INSTANCE = new QuarkusWorkflowJobComparator();

        @Override
        public int compare(GHWorkflowJob o1, GHWorkflowJob o2) {
            int order1 = getOrder(o1.getName());
            int order2 = getOrder(o2.getName());

            if (order1 == order2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }

            return order1 - order2;
        }

        private static int getOrder(String jobName) {
            if (jobName.startsWith("Initial JDK")) {
                return 1;
            }
            if (jobName.startsWith("Calculate Test Jobs")) {
                return 2;
            }
            if (jobName.startsWith("JVM Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 12;
                }
                return 11;
            }
            if (jobName.startsWith("Maven Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 22;
                }
                return 21;
            }
            if (jobName.startsWith("Gradle Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 32;
                }
                return 31;
            }
            if (jobName.startsWith("Devtools Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 42;
                }
                return 41;
            }
            if (jobName.startsWith("Kubernetes Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 52;
                }
                return 51;
            }
            if (jobName.startsWith("Quickstarts Compilation")) {
                return 61;
            }
            if (jobName.startsWith("MicroProfile TCKs Tests")) {
                return 71;
            }
            if (jobName.startsWith("Native Tests - ")) {
                if (jobName.contains("Windows")) {
                    return 82;
                }
                return 81;
            }

            return 200;
        }
    }

    private final static class QuarkusLangChain4jWorkflowJobComparator implements Comparator<GHWorkflowJob> {

        private static final QuarkusLangChain4jWorkflowJobComparator INSTANCE = new QuarkusLangChain4jWorkflowJobComparator();

        @Override
        public int compare(GHWorkflowJob o1, GHWorkflowJob o2) {
            int order1 = getOrder(o1.getName());
            int order2 = getOrder(o2.getName());

            if (order1 == order2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }

            return order1 - order2;
        }

        private static int getOrder(String jobName) {
            if (jobName.startsWith("Quick Build")) {
                return 1;
            }
            if (jobName.startsWith("JVM tests - ")) {
                return 2;
            }
            if (jobName.startsWith("Native tests - ")) {
                return 12;
            }
            if (jobName.startsWith("In process embedding model tests - ")) {
                return 22;
            }

            return 200;
        }
    }
}
