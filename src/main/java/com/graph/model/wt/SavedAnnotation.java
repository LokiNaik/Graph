package com.graph.model.wt;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

public class SavedAnnotation {
	private String _id;
	private Integer width;
	private Integer height;
	private List<MarkersAnnotation> markers=new ArrayList<MarkersAnnotation>();
	private ObjectId graphId;
	public ObjectId getGraphId() {
		return graphId;
	}
	public void setGraphId(ObjectId graphId) {
		this.graphId = graphId;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public Integer getWidth() {
		return width;
	}
	public void setWidth(Integer width) {
		this.width = width;
	}
	public Integer getHeight() {
		return height;
	}
	public void setHeight(Integer height) {
		this.height = height;
	}

	public List<MarkersAnnotation> getMarkers() {
		return markers;
	}
	public void setMarkers(List<MarkersAnnotation> markers) {
		this.markers = markers;
	}
}
