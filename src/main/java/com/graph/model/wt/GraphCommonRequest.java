package com.graph.model.wt;

public class GraphCommonRequest {
	private GroupingDetails grouping;
	private String dataQueryId;
	public String getDataQueryId() {
		return dataQueryId;
	}
	public void setDataQueryId(String dataQueryId) {
		this.dataQueryId = dataQueryId;
	}
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
	private XaxisDetails xAxis;
	private YaxisDetails yAxis;
	private String[] waferID;

}
