package io.quarkus.bot.action.buildreporter.config;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A subset of the Quarkus GitHub Bot config file.
 * <p>
 * It should never be made mandatory to have this file around.
 */
public class QuarkusGitHubBotConfigFile {

    public WorkflowRunAnalysisConfig workflowRunAnalysis = new WorkflowRunAnalysisConfig();

    public Develocity develocity = new Develocity();

    public static class WorkflowRunAnalysisConfig {

        @JsonDeserialize(as = HashSet.class)
        public Set<String> ignoredFlakyTests = new HashSet<>();
    }

    public static class Develocity {

        public boolean enabled = false;

        public String url;
    }
}
