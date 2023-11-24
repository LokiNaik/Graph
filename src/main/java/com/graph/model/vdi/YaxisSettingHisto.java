package com.graph.model.vdi;

public class YaxisSettingHisto {
	public String level;
	public String name;
	public String aggregator;
	Object min;
	Object max;
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

	public Object getMin() {
		return min;
	}

	public void setMin(Object min) {
		this.min = min;
	}

	public Object getMax() {
		return max;
	}

	public void setMax(Object max) {
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
