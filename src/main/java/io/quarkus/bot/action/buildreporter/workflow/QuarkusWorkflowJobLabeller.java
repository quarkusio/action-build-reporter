package io.quarkus.bot.action.buildreporter.workflow;

import jakarta.inject.Singleton;

import io.quarkus.bot.buildreporter.githubactions.WorkflowJobLabeller;

@Singleton
public class QuarkusWorkflowJobLabeller implements WorkflowJobLabeller {

    @Override
    public String label(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        StringBuilder label = new StringBuilder();
        String[] tokens = name.split(QuarkusWorkflowConstants.JOB_NAME_DELIMITER);

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith(QuarkusWorkflowConstants.JOB_NAME_JDK_PREFIX)
                    || tokens[i].startsWith(QuarkusWorkflowConstants.JOB_NAME_JAVA_PREFIX)) {
                break;
            }

            if (!label.isEmpty()) {
                label.append(QuarkusWorkflowConstants.JOB_NAME_DELIMITER);
            }
            label.append(tokens[i]);
        }

        return label.toString();
    }
}
