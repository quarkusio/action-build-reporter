package io.quarkus.bot.action.buildreporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterActionHandler;
import io.quarkus.bot.buildreporter.githubactions.BuildReporterConfig;

public class BuildReporterAction {

    private static final Logger LOG = Logger.getLogger(BuildReporterAction.class);

    private static final String BUILD_REPORTS_ARTIFACTS_PATH_INPUT_NAME = "build-reports-artifacts-path";
    private static final String JOB_NAME_INITIAL_JDK_PREFIX = "Initial JDK ";

    @Inject
    BuildReporterActionHandler buildReporterActionHandler;

    @Action
    void postJobSummary(Inputs inputs, Commands commands, Context context, GitHub gitHub) throws IOException {
        Path buildReportsArtifactsPath = Path.of(inputs.getRequired(BUILD_REPORTS_ARTIFACTS_PATH_INPUT_NAME));

        if (!Files.exists(buildReportsArtifactsPath)) {
            LOG.error("Unable to find the directory containing the build reports: " + buildReportsArtifactsPath);

            return;
        }

        BuildReporterConfig buildReporterConfig = new BuildReporterConfig.Builder()
                .createCheckRun(false)
                .workflowJobComparator(QuarkusWorkflowJobComparator.INSTANCE)
                .build();

        GHWorkflowRun workflowRun = gitHub.getRepository(context.getGitHubRepository())
                .getWorkflowRun(context.getGitHubRunId());

        Optional<String> report = buildReporterActionHandler.generateReport(workflowRun, buildReportsArtifactsPath,
                buildReporterConfig);

        if (report.isEmpty()) {
            return;
        }

        commands.jobSummary(report.get());
    }

    private final static class QuarkusWorkflowJobComparator implements Comparator<GHWorkflowJob> {

        private static final QuarkusWorkflowJobComparator INSTANCE = new QuarkusWorkflowJobComparator();

        @Override
        public int compare(GHWorkflowJob o1, GHWorkflowJob o2) {
            if (o1.getName().startsWith(JOB_NAME_INITIAL_JDK_PREFIX)
                    && !o2.getName().startsWith(JOB_NAME_INITIAL_JDK_PREFIX)) {
                return -1;
            }
            if (!o1.getName().startsWith(JOB_NAME_INITIAL_JDK_PREFIX)
                    && o2.getName().startsWith(JOB_NAME_INITIAL_JDK_PREFIX)) {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }
    }
}
