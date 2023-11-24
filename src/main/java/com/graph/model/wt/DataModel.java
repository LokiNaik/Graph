package com.graph.model.wt;

import java.util.List;

public class DataModel {
	
	private List<?> data;
	private long count;

	public DataModel() {
		
	}
	public DataModel(List<?> data,long count) {
		this.data = data;
		this.count = count;
	}
	
	public List<?> getData() {
		return data;
	}

	public void setData(List<?> data) {
		this.data = data;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

}
