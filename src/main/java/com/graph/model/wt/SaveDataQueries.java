package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
@Document("DataQueries")
public class SaveDataQueries {

	private String _id;
    private String accessType;
	private String queryName;
	private List<QueryList> queryList;
	private String createdBy;
	private String updatedBy;
    private boolean deleteFlag;
    private String applicationName;
    private Date lastUpdatedDate;
	
	
	public SaveDataQueries() {
		super();
		// TODO Auto-generated constructor stub
	}


	public SaveDataQueries(String _id, String accessType, String queryName, List<QueryList> queryList, String createdBy,
			String updatedBy, boolean deleteFlag, String applicationName, Date lastUpdatedDate) {
		super();
		this._id = _id;
		this.accessType = accessType;
		this.queryName = queryName;
		this.queryList = queryList;
		this.createdBy = createdBy;
		this.updatedBy = updatedBy;
		this.deleteFlag = deleteFlag;
		this.applicationName = applicationName;
		this.lastUpdatedDate = lastUpdatedDate;
	}


	public String get_id() {
		return _id;
	}


	public void set_id(String _id) {
		this._id = _id;
	}


	public String getAccessType() {
		return accessType;
	}


	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}


	public String getQueryName() {
		return queryName;
	}


	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}


	public List<QueryList> getQueryList() {
		return queryList;
	}


	public void setQueryList(List<QueryList> queryList) {
		this.queryList = queryList;
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


	public String getApplicationName() {
		return applicationName;
	}


	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}


	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}


	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}
	
}
