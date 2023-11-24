package com.graph.model.wt;

import java.util.ArrayList;

public class HistoTestValueGrouing {
	private int count;
	private ArrayList<String> waferID;
	
	
	
	public ArrayList<String> getWaferID() {
		return waferID;
	}
	public void setWaferID(ArrayList<String> waferID) {
		this.waferID = waferID;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public Double getRangeStart() {
		return rangeStart;
	}
	public void setRangeStart(Double rangeStart) {
		this.rangeStart = rangeStart;
	}
	public Double getRangeEnd() {
		return rangeEnd;
	}
	public void setRangeEnd(Double rangeEnd) {
		this.rangeEnd = rangeEnd;
	}
	private Double rangeStart;
	private Double rangeEnd;
}
