package com.graph.model.wt;

import java.util.ArrayList;
import java.util.List;


public class AutoGraphAnnotationModel {

	
	private Integer width;
	private Integer height;
	private List<MarkersAnnotation> markers = new ArrayList<>();
	
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
