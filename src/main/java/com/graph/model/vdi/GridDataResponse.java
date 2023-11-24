package com.graph.model.vdi;

import java.util.List;

import org.bson.Document;

public class GridDataResponse {
	private List<Document> data;
	private long count;
	public List<Document> getData() {
		return data;
	}
	public void setData(List<Document> data) {
		this.data = data;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	public GridDataResponse() {
		
	}
}