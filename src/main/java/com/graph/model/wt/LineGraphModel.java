package com.graph.model.wt;

import java.util.List;

public class LineGraphModel {
	private List<?> data;

	public LineGraphModel() {

	}

	public LineGraphModel(List<?> data) {
		this.data = data;
	}

	public List<?> getData() {
		return data;
	}

	public void setData(List<?> data) {
		this.data = data;
	}
}
