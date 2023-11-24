package com.graph.model.wt;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import com.graph.model.vdi.LineGraphRequestFilter;
import com.graph.model.vdi.WaferArea;

@Document("SavedGraphWaferIds")
public class WaferIdModel {
	private String _id;
	private ObjectId graphId;
	private List<String> waferIds = new ArrayList<String>();
	private LineGraphRequestFilter filter;
	private WaferArea waferArea;
	private List<String> elements;
	

	public List<String> getElements() {
		return elements;
	}

	public void setElements(List<String> elements) {
		this.elements = elements;
	}

	public WaferArea getWaferArea() {
		return waferArea;
	}

	public void setWaferArea(WaferArea waferArea) {
		this.waferArea = waferArea;
	}

	public LineGraphRequestFilter getFilter() {
		return filter;
	}

	public void setFilter(LineGraphRequestFilter filter) {
		this.filter = filter;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public ObjectId getGraphId() {
		return graphId;
	}

	public void setGraphId(ObjectId graphId) {
		this.graphId = graphId;
	}

	public List<String> getWaferIds() {
		return waferIds;
	}

	public void setWaferIds(List<String> waferIds) {
		this.waferIds = waferIds;
	}

}
