package com.graph.model.wt;

public class YaxisSetting {
	
	private String level;

	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
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
	private String name;
	private String aggregator;
	private double min;
	private double max;
	private int selectedBin;
	private SelectedTestDetails selectedTest;
	

}
