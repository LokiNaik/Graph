package com.graph.model.vdi;

import java.util.List;

import org.bson.types.ObjectId;

public class LineGraphRequest {
	public String group;
	public XaxisDetails xAxis;
	public YaxisDetails yAxis;
	public List<ObjectId> waferID;
	public LineGraphRequestFilter filter;
	private List<Area> areas;
	private List<Area> discardedAreas;
	private SelectionType selectionType;
	private List<String> elements;

	
	public LineGraphRequestFilter getFilter() {
		return filter;
	}

	public String getGroup() {
		return group;
	}

	public XaxisDetails getxAxis() {
		return xAxis;
	}

	public YaxisDetails getyAxis() {
		return yAxis;
	}

	public List<ObjectId> getWaferID() {
		return waferID;
	}

	public List<Area> getAreas() {
		return areas;
	}

	public void setAreas(List<Area> areas) {
		this.areas = areas;
	}

	public SelectionType getSelectionType() {
		return selectionType;
	}

	public void setSelectionType(SelectionType selectionType) {
		this.selectionType = selectionType;
	}

	public List<Area> getDiscardedAreas() {
		return discardedAreas;
	}

	public void setDiscardedAreas(List<Area> discardedAreas) {
		this.discardedAreas = discardedAreas;
	}

	public List<String> getElements() {
		return elements;
	}
	

}
