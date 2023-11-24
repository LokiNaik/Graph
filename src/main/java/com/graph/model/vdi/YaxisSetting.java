package com.graph.model.vdi;

public class YaxisSetting {
	
	public String level;
	public String name;
	public String aggregator;
	public double min;
	public double max;
	public int selectedBin;
	public SelectedTestDetails selectedTest;
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
}
