package com.graph.model.wt;

import org.springframework.data.mongodb.core.mapping.Document;

@Document("GraphTypeSetting")
public class GraphTypeModel {
	
	private String graphType;
	private String xAxisParameterType;
	private String yAxisParameterType;
	private String zAxisParameterType;
	private Integer xAxisCount;
	private Integer yAxisCount;
	private Integer zAxisCount;
	private Integer groupingCount;
	public String getGraphType() {
		return graphType;
	}
	public void setGraphType(String graphType) {
		this.graphType = graphType;
	}
	public String getxAxisParameterType() {
		return xAxisParameterType;
	}
	public void setxAxisParameterType(String xAxisParameterType) {
		this.xAxisParameterType = xAxisParameterType;
	}
	public String getyAxisParameterType() {
		return yAxisParameterType;
	}
	public void setyAxisParameterType(String yAxisParameterType) {
		this.yAxisParameterType = yAxisParameterType;
	}
	public String getzAxisParameterType() {
		return zAxisParameterType;
	}
	public void setzAxisParameterType(String zAxisParameterType) {
		this.zAxisParameterType = zAxisParameterType;
	}
	public Integer getxAxisCount() {
		return xAxisCount;
	}
	public void setxAxisCount(Integer xAxisCount) {
		this.xAxisCount = xAxisCount;
	}
	public Integer getyAxisCount() {
		return yAxisCount;
	}
	public void setyAxisCount(Integer yAxisCount) {
		this.yAxisCount = yAxisCount;
	}
	public Integer getzAxisCount() {
		return zAxisCount;
	}
	public void setzAxisCount(Integer zAxisCount) {
		this.zAxisCount = zAxisCount;
	}
	public Integer getGroupingCount() {
		return groupingCount;
	}
	public void setGroupingCount(Integer groupingCount) {
		this.groupingCount = groupingCount;
	}
		
}
