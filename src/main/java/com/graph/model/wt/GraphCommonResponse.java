package com.graph.model.wt;

import java.util.List;

import org.bson.Document;

public class GraphCommonResponse {
private int xAxisCount;
private WaferDateTime waferDateTime ;
public WaferDateTime getWaferDateTime() {
	return waferDateTime;
}
public void setWaferDateTime(WaferDateTime waferDateTime) {
	this.waferDateTime = waferDateTime;
}
public int getxAxisCount() {
	return xAxisCount;
}
public void setxAxisCount(int xAxisCount) {
	this.xAxisCount = xAxisCount;
}
public int getyAxisCount() {
	return yAxisCount;
}
public void setyAxisCount(int yAxisCount) {
	this.yAxisCount = yAxisCount;
}

public groupResponseDetails getGroupDetails() {
	return groupDetails;
}
public void setGroupDetails(groupResponseDetails groupDetails) {
	this.groupDetails = groupDetails;
}
public List<Document> getData() {
	return data;
}
public void setData(List<Document> data) {
	this.data = data;
}
private int yAxisCount;
private List<XaxisResponseDetails> xAxisDetails;
public List<XaxisResponseDetails> getxAxisDetails() {
	return xAxisDetails;
}
public void setxAxisDetails(List<XaxisResponseDetails> xAxisDetails) {
	this.xAxisDetails = xAxisDetails;
}
public List<YaxisResponseDetails> getyAxisDetails() {
	return yAxisDetails;
}
public void setyAxisDetails(List<YaxisResponseDetails> yAxisDetails) {
	this.yAxisDetails = yAxisDetails;
}
private List<YaxisResponseDetails> yAxisDetails;
private groupResponseDetails groupDetails;
private List<Document> data;


}
