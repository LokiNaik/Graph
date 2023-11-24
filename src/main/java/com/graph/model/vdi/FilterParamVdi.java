package com.graph.model.vdi;
import java.util.List;

public class FilterParamVdi {
	private String field;
	private String operator;
	private String valueFirst;
	private String valueSecond;
	List<?> excludeValues;
	private String includeValue;
	List<?> firstValues;
	private int isAdvanceSearch;

	public FilterParamVdi() {
	}
	
	public int getIsAdvanceSearch() {
		return isAdvanceSearch;
	}

	public String getField() {
		return field;
	}

	public List<?> getFirstValues() {
		return firstValues;
	}

	public String getOperator() {
		return operator;
	}

	public String getValueFirst() {
		return valueFirst;
	}

	public String getValueSecond() {
		return valueSecond;
	}
	
	public List<?> getExcludeValues() {
		return excludeValues;
	}

	public String getIncludeValue() {
		return includeValue;
	}
}