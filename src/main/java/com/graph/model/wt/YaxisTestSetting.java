package com.graph.model.wt;

public class YaxisTestSetting {
	private String name;
	public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAggregator() {
        return aggregator;
    }
    public void setAggregator(String aggregator) {
        this.aggregator = aggregator;
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
    public double getScaleval() {
        return scaleval;
    }
    public void setScaleval(double scaleval) {
        this.scaleval = scaleval;
    }
    public double getClassval() {
        return classval;
    }
    public void setClassval(double classval) {
        this.classval = classval;
    }
    public int getSelectedBin() {
        return selectedBin;
    }
    public void setSelectedBin(int selectedBin) {
        this.selectedBin = selectedBin;
    }
    public SelectedTestDetails getSelectedTest() {
        return selectedTest;
    }
    public void setSelectedTest(SelectedTestDetails selectedTest) {
        this.selectedTest = selectedTest;
    }
    private String aggregator;
	private double min;
	private double max;
	private double scaleval;
	private double classval;
	private int selectedBin;
	private SelectedTestDetails selectedTest;
}
