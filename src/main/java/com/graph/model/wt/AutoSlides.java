package com.graph.model.wt;

import java.util.List;

public class AutoSlides {
	
	private List<XAxisModel> xAxis;
    private List<YAxisModel> yAxis;
    private List<ZAxisModel> zAxis;
    private AutoGraphAnnotationModel savedAnnotation;
	private List<GroupingModel> grouping;
    private String position;
    private String slideName;
	private String graphName;
	private String graphId;
	private String graphType;
	private Boolean isHistogramValEnabled;
	private double graphBarWidth;
    private String thumbnailPath;
    private String queryId;
    private String queryName;

    
	
	public Boolean getIsHistogramValEnabled() {
		return isHistogramValEnabled;
	}
	public void setIsHistogramValEnabled(Boolean isHistogramValEnabled) {
		this.isHistogramValEnabled = isHistogramValEnabled;
	}
	public double getGraphBarWidth() {
		return graphBarWidth;
	}
	public void setGraphBarWidth(double graphBarWidth) {
		this.graphBarWidth = graphBarWidth;
	}
	public AutoGraphAnnotationModel getSavedAnnotation() {
		return savedAnnotation;
	}
	public void setAutoGraphAnnotations(AutoGraphAnnotationModel savedAnnotation) {
		this.savedAnnotation = savedAnnotation;
	}
	public String getPosition() {
		return position;
		
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getSlideName() {
		return slideName;
	}
	public void setSlideName(String slideName) {
		this.slideName = slideName;
	}
    public String getGraphName() {
		return graphName;
	}
	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}
	public String getGraphId() {
		return graphId;
	}
	public void setGraphId(String graphId) {
		this.graphId = graphId;
	}
	public String getGraphType() {
		return graphType;
	}
	public void setGraphType(String graphType) {
		this.graphType = graphType;
	}
	public String getThumbnailPath() {
		return thumbnailPath;
	}
	public void setThumbnailPath(String thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}
	public String getQueryId() {
		return queryId;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	
	public String getQueryName() {
		return queryName;
	}
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}
	public List<XAxisModel> getxAxis() {
		return xAxis;
	}
	public void setxAxis(List<XAxisModel> xAxis) {
		this.xAxis = xAxis;
	}
	public List<YAxisModel> getyAxis() {
		return yAxis;
	}
	public void setyAxis(List<YAxisModel> yAxis) {
		this.yAxis = yAxis;
	}
	public List<ZAxisModel> getzAxis() {
		return zAxis;
	}
	public void setzAxis(List<ZAxisModel> zAxis) {
		this.zAxis = zAxis;
	}
	public List<GroupingModel> getGrouping() {
		return grouping;
	}
	public void setGrouping(List<GroupingModel> grouping) {
		this.grouping = grouping;
	}
	
}
