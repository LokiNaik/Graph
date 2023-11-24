package com.graph.model.wt;

import java.util.List;

public class DataListModel {
	private List<?> dataList;

	public Message status;

	public DataListModel() {

	}

	public DataListModel(List<?> dataList) {
		this.dataList = dataList;
	}

	

	public List<?> getDataList() {
		return dataList;
	}

	public void setDataList(List<?> dataList) {
		this.dataList = dataList;
	}

	public Message getStatus() {
		return status;
	}

	public void setStatus(Message status) {
		this.status = status;
	}

}
