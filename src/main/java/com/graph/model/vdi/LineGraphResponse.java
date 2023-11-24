package com.graph.model.vdi;

import java.util.List;

import org.bson.Document;

import com.graph.model.wt.YaxisResponseDetails;
import com.graph.model.wt.groupResponseDetails;

public class LineGraphResponse {

public int xAxisCount;
public int yAxisCount;
public List<XaxisResponseDetails> xAxisDetails;
public YaxisResponseDetails yAxisDetails;
public groupResponseDetails groupDetails;
public List<Document> data;
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
public List<XaxisResponseDetails> getxAxisDetails() {
	return xAxisDetails;
}
public void setxAxisDetails(List<XaxisResponseDetails> xAxisDetails) {
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
public List<Document> getData() {
	return data;
}
public void setData(List<Document> data) {
	this.data = data;
}

}
