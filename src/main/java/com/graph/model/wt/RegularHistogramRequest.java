package com.graph.model.wt;

public class RegularHistogramRequest {
    private GroupingDetails grouping;
    private String dataQueryId;
	public String getDataQueryId() {
        return dataQueryId;
    }
    public void setDataQueryId(String dataQueryId) {
        this.dataQueryId = dataQueryId;
    }
    private YaxisTestDetails xAxis;
	private String[] waferID;
    public GroupingDetails getGrouping() {
        return grouping;
    }
    public void setGrouping(GroupingDetails grouping) {
        this.grouping = grouping;
    }
    public YaxisTestDetails getxAxis() {
        return xAxis;
    }
    public void setxAxis(YaxisTestDetails xAxis) {
        this.xAxis = xAxis;
    }
    public String[] getWaferID() {
        return waferID;
    }
    public void setWaferID(String[] waferID) {
        this.waferID = waferID;
    }
   
}
