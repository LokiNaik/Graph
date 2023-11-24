package com.graph.model.wt;

import java.util.List;

public class FilterParam {

	private String field;
	private String operator;
	private String valueFirst;
	private String valueSecond;
	private boolean exclude;
	private List<String> excludeValues;
	private String includeValue;
	
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public String getValueFirst() {
		return valueFirst;
	}
	public void setValueFirst(String valueFirst) {
		this.valueFirst = valueFirst;
	}
	public String getValueSecond() {
		return valueSecond;
	}
	public void setValueSecond(String valueSecond) {
		this.valueSecond = valueSecond;
	}
	public boolean isExclude() {
		return exclude;
	}
	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}
	public String getIncludeValue() {
		return includeValue;
	}
	public void setIncludeValue(String includeValue) {
		this.includeValue = includeValue;
	}
	public List<String> getExcludeValues() {
		return excludeValues;
	}
	public void setExcludeValues(List<String> excludeValues) {
		this.excludeValues = excludeValues;
	}

	
	
}
