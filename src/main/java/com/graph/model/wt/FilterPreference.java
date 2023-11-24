package com.graph.model.wt;

public class FilterPreference {

	private String columnId;
	private String operator;
	private String value1;
	private String value2;
	private boolean exclude;

	public void setColumnId(String columnId) {
		this.columnId = columnId;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public void setValue1(String value1) {
		this.value1 = value1;
	}

	public void setValue2(String value2) {
		this.value2 = value2;
	}

	public void setExclude(boolean exclude) {
		this.exclude = exclude;
	}

	
	public String getColumnId() {
		return columnId;
	}

	public String getOperator() {
		return operator;
	}

	public String getValue1() {
		return value1;
	}

	public String getValue2() {
		return value2;
	}

	public boolean isExclude() {
		return exclude;
	}
}
