package io.quarkus.bot.action.buildreporter.workflow.report;

import java.util.Locale;

import jakarta.inject.Singleton;

import io.quarkus.bot.action.buildreporter.workflow.QuarkusWorkflowConstants;
import io.quarkus.bot.buildreporter.githubactions.WorkflowConstants;
import io.quarkus.bot.buildreporter.githubactions.report.WorkflowReport;
import io.quarkus.bot.buildreporter.githubactions.report.WorkflowReportJob;
import io.quarkus.bot.buildreporter.githubactions.report.WorkflowReportJobIncludeStrategy;

@Singleton
public class QuarkusWorkflowReportJobIncludeStrategy implements WorkflowReportJobIncludeStrategy {

    @Override
    public boolean include(WorkflowReport report, WorkflowReportJob job) {
        if (QuarkusWorkflowConstants.JOB_NAME_BUILD_REPORT.equals(job.getName())) {
            return false;
        }
        if (job.getName().startsWith(WorkflowConstants.BUILD_SUMMARY_CHECK_RUN_PREFIX)) {
            return false;
        }
        if (job.isFailing()) {
            return true;
        }

        // in this particular case, we exclude the Windows job as it does not run the containers job
        // (no Docker support on Windows) and thus does not provide a similar coverage as the Linux
        // jobs. Having it green does not mean that things were OK globally.
        if (isJvmTests(job)) {
            if (isWindows(job)) {
                return false;
            }

            return hasJobWithSameLabelFailing(report, job);
        }

        return hasJobWithSameLabelFailing(report, job);
    }

    private static boolean isJvmTests(WorkflowReportJob job) {
        return job.getName().toLowerCase(Locale.ROOT)
                .startsWith(QuarkusWorkflowConstants.JOB_NAME_JVM_TESTS_PREFIX.toLowerCase(Locale.ROOT));
    }

    private static boolean isWindows(WorkflowReportJob job) {
        return job.getName().contains(QuarkusWorkflowConstants.JOB_NAME_WINDOWS);
    }

    private static boolean hasJobWithSameLabelFailing(WorkflowReport report, WorkflowReportJob job) {
        return report.getJobs().stream()
                .filter(j -> j.getLabel().equals(job.getLabel()))
                .anyMatch(j -> j.isFailing());
    }
}
