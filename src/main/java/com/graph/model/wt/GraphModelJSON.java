package com.graph.model.wt;

import java.util.List;

public class GraphModelJSON {

	private String applicationName;
	private List<SortParam> sort;
	private List<FilterParam> filter;
	private LockParam lock;
	private String pageNo;
	private String pageSize;
	private String userId;
	

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public List<SortParam> getSort() {
		return sort;
	}

	public void setSort(List<SortParam> sort) {
		this.sort = sort;
	}

	public List<FilterParam> getFilter() {
		return filter;
	}

	public void setFilter(List<FilterParam> filter) {
		this.filter = filter;
	}

	public LockParam getLock() {
		return lock;
	}

	public void setLock(LockParam lock) {
		this.lock = lock;
	}

	public String getPageSize() {
		return pageSize;
	}

	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}

	public String getPageNo() {
		return pageNo;
	}

	public void setPageNo(String pageNo) {
		this.pageNo = pageNo;
	}

	
}
