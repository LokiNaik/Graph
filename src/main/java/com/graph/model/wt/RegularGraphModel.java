package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("GraphTemplates")
public class RegularGraphModel {

    private String _id;
    private String applicationName;
    private String category;
    private String graphType;
    private String graphName;
    private String accessType;
    private String normalization;
    private String gridLines;
    private String line;
    private String dataLabel;
    private List<?> legendList;
    private List<?> xAxis;
    private List<?> yAxis;
    private List<?> grouping;
    private List<?> zAxis;
    private Boolean isHistogramValEnabled;
	private double graphBarWidth;
    private String createdBy;
    private String updatedBy;
    private boolean deleteFlag;
    private Date lastUpdatedDate;
    private String userId;

    public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public List<?> getLegendList() {
	   return legendList;
    }

    public void setLegendList(List<?> legendList) {
	   this.legendList = legendList;
    }

    public String get_id() {
	   return _id;
    }

    public void set_id(String _id) {
	   this._id = _id;
    }

    public String getGraphType() {
	   return graphType;
    }

    public void setGraphType(String graphType) {
	   this.graphType = graphType;
    }

    public List<?> getGrouping() {
	   return grouping;
    }

    public List<?> getxAxis() {
	   return xAxis;
    }

    public List<?> getyAxis() {
	   return yAxis;
    }

    public List<?> getzAxis() {
	   return zAxis;
    }

    public void setGrouping(List<?> grouping) {
	   this.grouping = grouping;
    }

    public String getCategory() {
	   return category;
    }

    public void setCategory(String category) {
	   this.category = category;
    }

    public String getNormalization() {
	   return normalization;
    }

    public void setNormalization(String normalization) {
	   this.normalization = normalization;
    }

    public String getGridLines() {
	   return gridLines;
    }

    public void setGridLines(String gridLines) {
	   this.gridLines = gridLines;
    }

    public String getLine() {
	   return line;
    }

    public void setLine(String line) {
	   this.line = line;
    }

    public String getDataLabel() {
	   return dataLabel;
    }

    public void setDataLabel(String dataLabel) {
	   this.dataLabel = dataLabel;
    }

    public void setxAxis(List<?> xAxis) {
	   this.xAxis = xAxis;
    }

    public void setyAxis(List<?> yAxis) {
	   this.yAxis = yAxis;
    }

    public void setzAxis(List<?> zAxis) {
	   this.zAxis = zAxis;
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

}
