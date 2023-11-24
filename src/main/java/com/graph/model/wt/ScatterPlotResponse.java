package com.graph.model.wt;

import java.util.List;

public class ScatterPlotResponse {
	
	private List data;
	private int yAxisCount;
	private XaxisResponseDetails xAxisDetails;
	private YaxisResponseDetails yAxisDetails;
	private groupResponseDetails groupDetails;
	private int xAxisCount;
	private WaferDateTime waferDateTime ;
	public WaferDateTime getWaferDateTime() {
		return waferDateTime;
	}
	public void setWaferDateTime(WaferDateTime waferDateTime) {
		this.waferDateTime = waferDateTime;
	}
	public List getData() {
		return data;
	}
	public void setData(List data) {
		this.data = data;
	}
	public int getyAxisCount() {
		return yAxisCount;
	}
	public void setyAxisCount(int yAxisCount) {
		this.yAxisCount = yAxisCount;
	}
	public XaxisResponseDetails getxAxisDetails() {
		return xAxisDetails;
	}
	public void setxAxisDetails(XaxisResponseDetails xAxisDetails) {
		this.xAxisDetails = xAxisDetails;
	}
	public YaxisResponseDetails getyAxisDetails() {
		return yAxisDetails;
	}
	public void setyAxisDetails(YaxisResponseDetails yAxisDetails) {
		this.yAxisDetails = yAxisDetails;
	}
	public groupResponseDetails getGroupDetails() {
		return groupDetails;
	}
	public void setGroupDetails(groupResponseDetails groupDetails) {
		this.groupDetails = groupDetails;
	}
	public int getxAxisCount() {
		return xAxisCount;
	}
	public void setxAxisCount(int xAxisCount) {
		this.xAxisCount = xAxisCount;
	}

}
