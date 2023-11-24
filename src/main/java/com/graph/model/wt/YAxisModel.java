package com.graph.model.wt;



public class YAxisModel {
	private String axisParameter;
	private String aggregator;
	private RangeModel range;
	private ScaleModel scale;
	private ClassModel classObj;
	private String level;
	private SelectedBin selectedBin;
	private SelectedTest selectedTest;
	
	
	public SelectedBin getSelectedBin() {
        return selectedBin;
    }
    public void setSelectedBin(SelectedBin selectedBin) {
        this.selectedBin = selectedBin;
    }
    public SelectedTest getSelectedTest() {
        return selectedTest;
    }
    public void setSelectedTest(SelectedTest selectedTest) {
        this.selectedTest = selectedTest;
    }
    public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }
    public ClassModel getClassObj() {
		return classObj;
	}
	public void setClassObj(ClassModel classObj) {
		this.classObj = classObj;
	}
	public String getAxisParameter() {
		return axisParameter;
	}
	public void setAxisParameter(String axisParameter) {
		this.axisParameter = axisParameter;
	}

	public String getAggregator() {
		return aggregator;
	}
	public void setAggregator(String aggregator) {
		this.aggregator = aggregator;
	}
	public RangeModel getRange() {
		return range;
	}
	public void setRange(RangeModel range) {
		this.range= range;
	}
	public ScaleModel getScale() {
		return scale;
	}
	public void setScale(ScaleModel scale) {
		this.scale = scale;
	}
	
}
