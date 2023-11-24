package com.graph.model.wt;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;


public class DataQueries {

	private String _id;
	private String queryName;
	private String accessType;
	private List<QueryList> queryList;
	
	public DataQueries(String _id, String queryName, String accessType, List<QueryList> queryList) {
		super();
		this._id = _id;
		this.queryName = queryName;
		this.accessType = accessType;
		this.queryList = queryList;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public List<QueryList> getQueryList() {
		return queryList;
	}

	public void setQueryList(List<QueryList> queryList) {
		this.queryList = queryList;
	}
	
	
	
	
	
}
