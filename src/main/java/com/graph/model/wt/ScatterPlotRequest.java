package com.graph.model.wt;

public class ScatterPlotRequest {
    private GroupingDetails grouping;
    private String dataQueryId;
	public String getDataQueryId() {
        return dataQueryId;
    }
    public void setDataQueryId(String dataQueryId) {
        this.dataQueryId = dataQueryId;
    }
    private YaxisDetails xAxis;
	private YaxisDetails yAxis;
	private String[] waferID;
	public YaxisDetails getxAxis() {
		return xAxis;
	}
	public void setxAxis(YaxisDetails xAxis) {
		this.xAxis = xAxis;
	}
	public YaxisDetails getyAxis() {
		return yAxis;
	}
	public void setyAxis(YaxisDetails yAxis) {
		this.yAxis = yAxis;
	}
	public String[] getWaferID() {
		return waferID;
	}
	public void setWaferID(String[] waferID) {
		this.waferID = waferID;
	}
    public GroupingDetails getGrouping() {
        return grouping;
    }
    public void setGrouping(GroupingDetails grouping) {
        this.grouping = grouping;
    }
}



