package org.jmonkeyengine.screenshottests.testframework.protoreport;

import java.util.ArrayList;
import java.util.List;

public class ProtoReport {
    private String reportTitle;

    private List<ProtoReportTestItem> testResults = new ArrayList<>();

    public void addTest(ProtoReportTestItem testInProgress) {
        testResults.add(testInProgress);
    }

    public List<ProtoReportTestItem> getTestResults() {
        return testResults;
    }

    public ProtoReport(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void setTestResults(List<ProtoReportTestItem> testResults) {
        this.testResults = testResults;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }
}
