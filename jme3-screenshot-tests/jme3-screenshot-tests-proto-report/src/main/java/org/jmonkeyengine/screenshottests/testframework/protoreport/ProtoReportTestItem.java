package org.jmonkeyengine.screenshottests.testframework.protoreport;

import java.util.ArrayList;
import java.util.List;

public class ProtoReportTestItem {
    private String methodName;
    private String classSimpleName;
    private List<String> logs = new ArrayList<>();
    private List<ReportEvent> reportEvents = new ArrayList<>();
    private List<ImageReference> attachedImages = new ArrayList<>();

    public ProtoReportTestItem() {
    }

    public ProtoReportTestItem(String methodName, String classSimpleName) {
        this.methodName = methodName;
        this.classSimpleName = classSimpleName;
    }

    public void addLogs(List<String> logs) {
        this.logs.addAll(logs);
    }
    public void addStatus(ReportStatus status, String message){
        reportEvents.add(new ReportEvent(status, message));
    }

    public void addImageReference(String title, String fileName) {
        attachedImages.add(new ImageReference(title, fileName));
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassSimpleName() {
        return classSimpleName;
    }

    public void setClassSimpleName(String classSimpleName) {
        this.classSimpleName = classSimpleName;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public List<ReportEvent> getReportEvents() {
        return reportEvents;
    }

    public void setReportEvents(List<ReportEvent> reportEvents) {
        this.reportEvents = reportEvents;
    }

    public List<ImageReference> getAttachedImages() {
        return attachedImages;
    }

    public void setAttachedImages(List<ImageReference> attachedImages) {
        this.attachedImages = attachedImages;
    }

    public static class ReportEvent{
        private ReportStatus status;
        private String message;

        public ReportEvent() {
        }

        public ReportEvent(ReportStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public ReportStatus getStatus() {
            return status;
        }

        public void setStatus(ReportStatus status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public enum ReportStatus{
        SKIPPED, PASSED, WARNING, FAILED
    }

    public static class ImageReference{
        private String title;
        private String relativeFileName;

        public ImageReference() {
        }

        public ImageReference(String title, String relativeFileName) {
            this.title = title;
            this.relativeFileName = relativeFileName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getRelativeFileName() {
            return relativeFileName;
        }

        public void setRelativeFileName(String relativeFileName) {
            this.relativeFileName = relativeFileName;
        }
    }
}
