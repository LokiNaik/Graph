package com.graph.model.wt;

public class MESTestAvgMedRequest {
     private String[] waferID;
     private String dataQueryId;
	private int testNumber;
	private String testName;
	private String dimension;
	private String dimension2X;
	private String granularity;
	private double min;
	private double max;
    public String[] getWaferID() {
        return waferID;
    }
    public String getDataQueryId() {
        return dataQueryId;
    }
    public void setDataQueryId(String dataQueryId) {
        this.dataQueryId = dataQueryId;
    }
    public void setWaferID(String[] waferID) {
        this.waferID = waferID;
    }
    public int getTestNumber() {
        return testNumber;
    }
    public void setTestNumber(int testNumber) {
        this.testNumber = testNumber;
    }
    public String getTestName() {
        return testName;
    }
    public void setTestName(String testName) {
        this.testName = testName;
    }
    public String getDimension() {
        return dimension;
    }
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }
    public String getDimension2X() {
        return dimension2X;
    }
    public void setDimension2X(String dimension2x) {
        dimension2X = dimension2x;
    }
    public String getGranularity() {
        return granularity;
    }
    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }
    public double getMin() {
        return min;
    }
    public void setMin(double min) {
        this.min = min;
    }
    public double getMax() {
        return max;
    }
    public void setMax(double max) {
        this.max = max;
    }
	
	
   
}
