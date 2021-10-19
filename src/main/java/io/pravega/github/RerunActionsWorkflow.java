package io.pravega.github;

import java.io.IOException;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GHWorkflowRun.Conclusion;
import org.kohsuke.github.GHWorkflowRun.Status;
import org.kohsuke.github.PagedIterator;

import io.quarkiverse.githubapp.event.IssueComment;

public class RerunActionsWorkflow {

	void issueComment(@IssueComment.Created GHEventPayload.IssueComment comment) throws IOException {
		if (comment.getComment().getBody().equals("!rerun") && comment.getIssue().isPullRequest()) {
			GHIssue issue = comment.getIssue();
			GHPullRequest pr = comment.getRepository().getPullRequest(issue.getNumber());
			PagedIterator<GHWorkflowRun> itor = comment.getRepository()
					.queryWorkflowRuns()
					.branch(pr.getHead().getRef())
					.list()
					.iterator();
			while (itor.hasNext()) {
				GHWorkflowRun wfRun = itor.next();
				if ("build".equals(wfRun.getName())) {
					if (wfRun.getStatus() == Status.COMPLETED && wfRun.getConclusion() != Conclusion.SUCCESS) {
						wfRun.rerun();
						issue.comment("Rerunning failed workflow...");
					} else {
						issue.comment("Latest workflow run is in an invalid status. Status must be Completed and Conclusion must not be Success.\n\n"
								     +"Status: " + wfRun.getStatus().name() + ", Conclusion: " + wfRun.getConclusion().name() + "\n"
					                 +"Workflow Run: " + wfRun.getHtmlUrl());
					}
					return;
				}
			}
			issue.comment("No workflow runs found...");			
		}
	}

}
