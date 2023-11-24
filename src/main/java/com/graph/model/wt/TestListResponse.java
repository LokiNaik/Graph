package com.graph.model.wt;

import java.util.List;

import org.json.simple.JSONObject;

public class TestListResponse {
	public List<JSONObject> getTestList() {
		return testList;
	}

	public void setTestList(List<JSONObject> testList) {
		this.testList = testList;
	}

	private List<JSONObject> testList;
}
