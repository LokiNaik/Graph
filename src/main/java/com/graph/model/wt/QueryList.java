package com.graph.model.wt;

public class QueryList {
	private String field;
	private String operator;
	private String value;
	private String position;
	
	public QueryList(String field, String operator, String value, String position) {
		super();
		this.field = field;
		this.operator = operator;
		this.value = value;
		
		this.position = position;
	}
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
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	
}
