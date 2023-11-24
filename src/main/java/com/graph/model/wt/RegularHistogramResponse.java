package com.graph.model.wt;

import java.util.List;

import org.bson.Document;

public class RegularHistogramResponse {
	private List<XaxisResponseDetails> xAxisDetails;
	private groupResponseDetails groupDetails;
	private List<Document> data;
	private int xAxisCount;
	private WaferDateTime waferDateTime;
    public WaferDateTime getWaferDateTime() {
		return waferDateTime;
	}
	public void setWaferDateTime(WaferDateTime waferDateTime) {
		this.waferDateTime = waferDateTime;
	}
	public List<XaxisResponseDetails> getxAxisDetails() {
        return xAxisDetails;
    }
    public void setxAxisDetails(List<XaxisResponseDetails> xAxisDetails) {
        this.xAxisDetails = xAxisDetails;
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
    public int getxAxisCount() {
        return xAxisCount;
    }
    public void setxAxisCount(int xAxisCount) {
        this.xAxisCount = xAxisCount;
    }

    
}
