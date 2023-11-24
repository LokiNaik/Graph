package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("RecentGraphs")
public class RecentGraphSaveModel {
	private ObjectId _id;
	private String applicationName;
	private String graphName;
	private String accessType;
	private String grouping;
	private List<?> legendList;
	private List<?> xAxis;
	private List<?> yAxis;
	private List<?> zAxis;
	private ObjectId graphId;
	private String category;
	private Date accessTime;
	private String graphType;
	private boolean deleteFlag;
	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<?> getLegendList() {
		return legendList;
	}

	public void setLegendList(List<?> legendList) {
		this.legendList = legendList;
	}

	public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
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

	public String getGrouping() {
		return grouping;
	}

	public void setGrouping(String grouping) {
		this.grouping = grouping;
	}

	public List<?> getxAxis() {
		return xAxis;
	}

	public void setxAxis(List<?> xAxis) {
		this.xAxis = xAxis;
	}

	public List<?> getyAxis() {
		return yAxis;
	}

	public void setyAxis(List<?> yAxis) {
		this.yAxis = yAxis;
	}

	public List<?> getzAxis() {
		return zAxis;
	}

	public void setzAxis(List<?> zAxis) {
		this.zAxis = zAxis;
	}

	public ObjectId get_id() {
		return _id;
	}

	public void set_id(ObjectId _id) {
		this._id = _id;
	}

	public ObjectId getGraphId() {
		return graphId;
	}

	public void setGraphId(ObjectId graphId) {
		this.graphId = graphId;
	}

	public String getCategory() {
		return category;
	}

	public String getGraphType() {
		return graphType;
	}

	public void setGraphType(String graphType) {
		this.graphType = graphType;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Date getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(Date accessTime) {
		this.accessTime = accessTime;
	}
}
