package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.graph.model.vdi.LineGraphRequestFilter;
import com.graph.model.vdi.WaferArea;

public class GraphRequest {

	private String _id;
	private ObjectId graphId;
	private Boolean isAnnotation;
	private String graphType;
	private Date accessTime;
	private String applicationName;
	private String category;
	private String type;
	private String graphName;
	private String accessType;
	private List<String> waferIds;
	private List<XAxisModel> xAxis;
	private List<YAxisModel> yAxis;
	private List<ZAxisModel> zAxis;
	private List<GroupingModel> grouping;
	private List<LegendModel> legendList;
	private Boolean isHistogramValEnabled;
	private double graphBarWidth;
	private String groupingRecentGraph;
	private String updatedBy;
	private boolean deleteFlag;
	private Date lastUpdatedDate;
	private SavedAnnotation savedAnnotation;
	private LineGraphRequestFilter filter;
	private WaferArea waferArea;
	private List<String> elements;
	private String userId;
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getElements() {
		return elements;
	}

	public void setElements(List<String> elements) {
		this.elements = elements;
	}

	public WaferArea getWaferArea() {
		return waferArea;
	}

	public void setWaferArea(WaferArea waferArea) {
		this.waferArea = waferArea;
	}

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

	public LineGraphRequestFilter getFilter() {
		return filter;
	}

	public void setFilter(LineGraphRequestFilter filter) {
		this.filter = filter;
	}

	public List<LegendModel> getLegendList() {
		return legendList;
	}

	public void setLegendList(List<LegendModel> legendList) {
		this.legendList = legendList;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getIsAnnotation() {
		return isAnnotation;
	}

	public void setIsAnnotation(Boolean isAnnotation) {
		this.isAnnotation = isAnnotation;
	}

	public SavedAnnotation getSavedAnnotation() {
		return savedAnnotation;
	}

	public void setSavedAnnotation(SavedAnnotation savedAnnotation) {
		this.savedAnnotation = savedAnnotation;
	}

	public String get_id() {
		return _id;
	}

	public Date getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(Date accessTime) {
		this.accessTime = accessTime;
	}

	public String getGroupingRecentGraph() {
		return groupingRecentGraph;
	}

	public void setGroupingRecentGraph(String groupingRecentGraph) {
		this.groupingRecentGraph = groupingRecentGraph;
	}

	private String createdBy;

	public String getGraphType() {
		return graphType;
	}

	public void setGraphType(String graphType) {
		this.graphType = graphType;
	}

	public ObjectId getGraphId() {
		return graphId;
	}

	public void setGraphId(ObjectId graphId) {
		this.graphId = graphId;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<GroupingModel> getGrouping() {
		return grouping;
	}

	public void setGrouping(List<GroupingModel> grouping) {
		this.grouping = grouping;
	}

	public List<String> getWaferIds() {
		return waferIds;
	}

	public void setWaferIds(List<String> waferIds) {
		this.waferIds = waferIds;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}

	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getGraphName() {
		return graphName;
	}

	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
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

}
