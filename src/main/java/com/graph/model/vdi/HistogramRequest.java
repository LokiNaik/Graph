package com.graph.model.vdi;

import java.util.List;
import org.bson.types.ObjectId;
import com.graph.model.wt.GroupingDetails;

public class HistogramRequest {

	private XaxisDetailsHisto xAxis;
	private YaxisDetailsHisto yAxis;
	public List<ObjectId> waferID;
	private GroupingDetails grouping;
	public ScatterPlotRequestFilter filter;
	private List<Area> areas;
	private List<Area> discardedAreas;
	private List<String> elements;

	public List<String> getElements() {
		return elements;
	}

	public ScatterPlotRequestFilter getFilter() {
		return filter;
	}

	public XaxisDetailsHisto getxAxis() {
		return xAxis;
	}

	public YaxisDetailsHisto getyAxis() {
		return yAxis;
	}

	public List<ObjectId> getWaferID() {
		return waferID;
	}

	public GroupingDetails getGrouping() {
		return grouping;
	}
	public List<Area> getAreas() {
		return areas ;
	}

	public List<Area> getDiscardedAreas() {
		return discardedAreas;
	}


}
