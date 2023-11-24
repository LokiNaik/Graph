package com.graph.model.wt;

import org.bson.types.ObjectId;

public class TestStatusDetails {

	private int testNumber;
	private String testName;
	private ObjectId[] waferIds;
	public ObjectId[] getWaferIds() {
		return waferIds;
	}
	public void setWaferIds(ObjectId[] waferIds) {
		this.waferIds = waferIds;
	}
	
	public int getTestNumber() {
		return testNumber;
	}
	public void setTestNumber(int testNumber) {
		this.testNumber = testNumber;
	}
	public String getTestName() {
		return testName;
	}
	public void setTestName(String testName) {
		this.testName = testName;
	}


}
