package com.graph.model.wt;

public class ComparisonHistogramTestValueRequest {
    private String dataQueryId;
	public String getDataQueryId() {
        return dataQueryId;
    }
    public void setDataQueryId(String dataQueryId) {
        this.dataQueryId = dataQueryId;
    }
    private GroupingDetails grouping;
	private XaxisDetails xAxis;
	private YaxisTestDetails yAxis;
	private String[] waferID;
    public GroupingDetails getGrouping() {
        return grouping;
    }
    public void setGrouping(GroupingDetails grouping) {
        this.grouping = grouping;
    }
    public XaxisDetails getxAxis() {
        return xAxis;
    }
    public void setxAxis(XaxisDetails xAxis) {
        this.xAxis = xAxis;
    }
    public YaxisTestDetails getyAxis() {
        return yAxis;
    }
    public void setyAxis(YaxisTestDetails yAxis) {
        this.yAxis = yAxis;
    }
    public String[] getWaferID() {
        return waferID;
    }
    public void setWaferID(String[] waferID) {
        this.waferID = waferID;
    }

}
