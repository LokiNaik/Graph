package com.graph.model.wt;

import java.util.List;
import java.util.Map;

import org.bson.Document;

public class DataListTestModel {

	private Map<String,  List<Document>> testResult;
	private Message status;
	public Map<String, List<Document>> getTestResult() {
		return testResult;
	}
	public void setTestResult(Map<String, List<Document>> testResult) {
		this.testResult = testResult;
	}
	public Message getStatus() {
		return status;
	}
	public void setStatus(Message status) {
		this.status = status;
	}
	
}
