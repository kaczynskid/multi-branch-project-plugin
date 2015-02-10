package com.github.mjdetullio.jenkins.plugins.multibranch;

import java.lang.reflect.Field;

import jenkins.model.Jenkins;
import jenkins.triggers.ReverseBuildTrigger;

public class ReverseBuildTriggerUpdater {

    private static final Jenkins jenkins = Jenkins.getInstance();

    private final ReverseBuildTriggerWrapper trigger;

    public ReverseBuildTriggerUpdater(ReverseBuildTrigger trigger) {
        this.trigger = new ReverseBuildTriggerWrapper(trigger);
    }

    public void updateForBranch(String branchName) {
        String upstreamProjects = trigger.getUpstreamProjects();
        String newUpstreamProjects = "";

        for (String upstreamProjectName : upstreamProjects.split(",")) {
            if (isEmpty(upstreamProjectName = trim(upstreamProjectName))) {
                continue;
            }

            upstreamProjectName = asBranchSpecificProjectName(upstreamProjectName, branchName);

            if (isEmpty(upstreamProjectName = trim(upstreamProjectName))) {
                continue;
            }

            if (!isEmpty(newUpstreamProjects)) {
                newUpstreamProjects += ",";
            }
            newUpstreamProjects += upstreamProjectName;
        }

        if (!upstreamProjects.equals(newUpstreamProjects)) {
            trigger.setUpstreamProjects(newUpstreamProjects);
        }
    }

    private String asBranchSpecificProjectName(String projectName, String branchName) {
        FreeStyleMultiBranchProject project = findProjectByName(projectName);
        if (project == null) {
            return projectName;
        }

        String subProjectName = getBranchSubProjectName(project, branchName);
        if (isEmpty(subProjectName)) {
            subProjectName = getBranchSubProjectName(project, "develop");
        }

        return isEmpty(subProjectName) ? projectName : subProjectName;
    }

    private FreeStyleMultiBranchProject findProjectByName(String projectName) {
        for (FreeStyleMultiBranchProject project : jenkins.getAllItems(FreeStyleMultiBranchProject.class)) {
            if (project.getFullName().equals(projectName)) {
                return project;
            }
        }
        return null;
    }

    private String getBranchSubProjectName(FreeStyleMultiBranchProject project, String branchName) {
        FreeStyleBranchProject subProject = project.getBranch(branchName);
        return subProject == null ? null : subProject.getFullName();
    }

    private static class ReverseBuildTriggerWrapper {

        private static final Field upstreamProjectsField;

        static {
            try {
                upstreamProjectsField = ReverseBuildTrigger.class.getDeclaredField("upstreamProjects");
                upstreamProjectsField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Unexpected error!", e);
            }
        }

        private final ReverseBuildTrigger trigger;

        public ReverseBuildTriggerWrapper(ReverseBuildTrigger trigger) {
            this.trigger = trigger;
        }

        public String getUpstreamProjects() {
            try {
                return (String) upstreamProjectsField.get(trigger);
            } catch (Exception e) {
                return "";
            }
        }

        public void setUpstreamProjects(String newUpstreamProjects) {
            try {
                upstreamProjectsField.set(trigger, newUpstreamProjects);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error!", e);
            }
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
