package com.graph.model.wt;

import java.util.List;

public class GroupingDetails {
    private String axisParameter;
    private List<?> searchValues;
    private Boolean isSearchValueExcluded;

    public List<?> getSearchValues() {
	   return searchValues;
    }

    public void setSearchValues(List<?> searchValues) {
	   this.searchValues = searchValues;
    }

    public String getAxisParameter() {
	   return axisParameter;
    }

    public void setAxisParameter(String axisParameter) {
	   this.axisParameter = axisParameter;
    }

    public Boolean getIsSearchValueExcluded() {
        return isSearchValueExcluded;
    }

    public void setIsSearchValueExcluded(Boolean isSearchValueExcluded) {
        this.isSearchValueExcluded = isSearchValueExcluded;
    }
}
