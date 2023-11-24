package com.graph.model.wt;

import java.util.List;

import org.bson.Document;

public class RegularHistoTestValue {
	private List<XaxisResponseDetails> xAxisDetails;
	private groupResponseDetails groupDetails;
	private List<Document> data;
	private int xAxisCount;
	private double minValue;
	private double MaxValue;
	private double classValue;
	private double scaleValue;
	private WaferDateTime waferDateTime ;
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
    public double getMinValue() {
        return minValue;
    }
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }
    public double getMaxValue() {
        return MaxValue;
    }
    public void setMaxValue(double maxValue) {
        MaxValue = maxValue;
    }
    public double getClassValue() {
        return classValue;
    }
    public void setClassValue(double classValue) {
        this.classValue = classValue;
    }
    public double getScaleValue() {
        return scaleValue;
    }
    public void setScaleValue(double scaleValue) {
        this.scaleValue = scaleValue;
    }
	
}
