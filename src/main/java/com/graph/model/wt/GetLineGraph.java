package com.graph.model.wt;

import java.util.ArrayList;

public class GetLineGraph {

	private String combinationKey;
	private double aggregationValue;
	private ArrayList<WaferListLineGraph> lists;
	
	public String getCombinationKey() {
		return combinationKey;
	}
	public void setCombinationKey(String combinationKey) {
		this.combinationKey = combinationKey;
	}
	
	public double getAggregationValue() {
		return aggregationValue;
	}
	public void setAggregationValue(double aggregationValue) {
		this.aggregationValue = aggregationValue;
	}
	public ArrayList<WaferListLineGraph> getLists() {
		return lists;
	}
	public void setLists(ArrayList<WaferListLineGraph> lists) {
		this.lists = lists;
	}
	
	
	
	
}
