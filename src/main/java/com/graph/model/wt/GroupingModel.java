package com.graph.model.wt;

import java.util.List;

public class GroupingModel {
	private String axisParameter;
	private List<String> searchValues;
	private Boolean isSearchValueExcluded;
	
	public List<String> getSearchValues() {
        return searchValues;
    }

    public void setSearchValues(List<String> searchValues) {
        this.searchValues = searchValues;
    }

    public Boolean getIsSearchValueExcluded() {
        return isSearchValueExcluded;
    }

    public void setIsSearchValueExcluded(Boolean isSearchValueExcluded) {
        this.isSearchValueExcluded = isSearchValueExcluded;
    }

    public String getAxisParameter() {
		return axisParameter;
	}

	public void setAxisParameter(String axisParameter) {
		this.axisParameter = axisParameter;
	}
	
}
