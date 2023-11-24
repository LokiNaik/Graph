package com.graph.model.wt;

public class XAxisModel {
	private String axisParameter;
	private String sort;
	//private String search;
	private boolean exclude;
	private ScaleModel scale;
	private RangeModel range;
	private String granularity;
	private ClassModel classObj;
	private SelectedBin selectedBin;
	private String level;
	private SelectedTest selectedTest;
	
	public SelectedTest getSelectedTest() {
		return selectedTest;
	}
	public void setSelectedTest(SelectedTest selectedTest) {
		this.selectedTest = selectedTest;
	}
	public SelectedBin getSelectedBin() {
		return selectedBin;
	}
	public void setSelectedBin(SelectedBin selectedBin) {
		this.selectedBin = selectedBin;
	}
	public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }
    public String getGranularity() {
        return granularity;
    }
    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }
    public RangeModel getRange() {
		return range;
	}
	public void setRange(RangeModel range) {
		this.range = range;
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
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
	}
	/*public String getSearch() {
		return search;
	}
	public void setSearch(String search) {
		this.search = search;
	}*/
	public boolean isExclude() {
		return exclude;
	}
	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}
	public ScaleModel getScale() {
		return scale;
	}
	public void setScale(ScaleModel scale) {
		this.scale = scale;
	}
	
}
