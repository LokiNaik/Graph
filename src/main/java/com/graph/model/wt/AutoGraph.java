package com.graph.model.wt;

import java.util.Date;
import java.util.List;

public class AutoGraph {
	 private String _id;
	 private String category;
	 private String autographName;
	 private String accessType;
	 private List<AutoSlides> autoSlides;
	 
	
	public AutoGraph(String _id, String category, String graphName, String autographName, String accessType,
			List<AutoSlides> autoSlides) {
		super();
		this._id = _id;
		this.category = category;
		this.autographName = autographName;
		this.accessType = accessType;
		this.autoSlides = autoSlides;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getAutographName() {
		return autographName;
	}
	public void setAutographName(String autographName) {
		this.autographName = autographName;
	}
	public String getAccessType() {
		return accessType;
	}
	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}
	public List<AutoSlides> getAutoSlides() {
		return autoSlides;
	}
	public void setAutoSlides(List<AutoSlides> autoSlides) {
		this.autoSlides = autoSlides;
	}
	
	
	
	 
	 
	
}