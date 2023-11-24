package com.graph.model.wt;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("SavedGraphAnnotations")
public class SavedAnnotationModel {
	private String _id;
	private ObjectId graphId;
	private Integer width;
	private Integer height;
	private List<?> markers;
	
	
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
	public List<?> getMarkers() {
		return markers;
	}
	public void setMarkers(List<?> markers) {
		this.markers = markers;
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
	
}
