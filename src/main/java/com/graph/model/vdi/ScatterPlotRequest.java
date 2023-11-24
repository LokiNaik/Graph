package com.graph.model.vdi;

import java.util.List;

import org.bson.types.ObjectId;

import com.graph.model.wt.GroupingDetails;

public class ScatterPlotRequest {

	private XaxisDetail xAxis;
	private YaxisDetails yAxis;
	public List<ObjectId> waferID;
	private GroupingDetails grouping;

	public XaxisDetail getxAxis() {
		return xAxis;
	}

	public YaxisDetails getyAxis() {
		return yAxis;
	}

	public List<ObjectId> getWaferID() {
		return waferID;
	}

	public GroupingDetails getGrouping() {
		return grouping;
	}

	public ScatterPlotRequestFilter getFilter() {
		return filter;
	}

	public ScatterPlotRequestFilter filter;

}
