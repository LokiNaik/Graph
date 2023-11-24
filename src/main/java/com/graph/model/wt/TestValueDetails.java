package com.graph.model.wt;

import org.bson.types.ObjectId;

public class TestValueDetails {
	private ObjectId[] waferID;
	private Double testValue;
	private String testName;

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public int getTestNumber() {
		return testNumber;
	}

	public void setTestNumber(int testNumber) {
		this.testNumber = testNumber;
	}

	private int testNumber;

	public Double getTestValue() {
		return testValue;
	}

	public void setTestValue(Double testValue) {
		this.testValue = testValue;
	}

	public ObjectId[] getWaferID() {
		return waferID;
	}

	public void setWaferID(ObjectId[] waferID) {
		this.waferID = waferID;
	}

}
