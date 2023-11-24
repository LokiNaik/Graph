package com.graph.model.vdi;

import java.util.List;

import org.bson.types.ObjectId;

public class GraphParams {
	public List<ObjectId> waferIds;
	public List<String> xAxes;
	public String yAxis;
	public String aggregationType;

	public String getAggregationType() {
		return aggregationType;
	}

	public List<ObjectId> getWaferIds() {
		return waferIds;
	}

	public List<String> getxAxes() {
		return xAxes;
	}

	public String getyAxis() {
		return yAxis;
	}

}
