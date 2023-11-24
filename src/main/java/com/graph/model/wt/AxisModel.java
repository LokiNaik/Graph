package com.graph.model.wt;

import org.springframework.data.mongodb.core.mapping.Document;

@Document("AxisParameters")
public class AxisModel {

	private String applicationName;
	private String[] dimension;
	private String[] measure;
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public String[] getDimension() {
		return dimension;
	}
	public void setDimension(String[] dimension) {
		this.dimension = dimension;
	}
	public String[] getMeasure() {
		return measure;
	}
	public void setMeasure(String[] measure) {
		this.measure = measure;
	}
	
}