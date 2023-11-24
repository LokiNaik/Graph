package com.graph.model.wt;

import java.util.List;

public class HyperLinkModel {

	private String createdBy;
	private List<SettingsHyperlink> settings;
	
	
	public HyperLinkModel() {
		super();
		// TODO Auto-generated constructor stub
	}


	public HyperLinkModel(String createdBy, List<SettingsHyperlink> settings) {
		super();
		this.createdBy = createdBy;
		this.settings = settings;
	}


	public String getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}


	public List<SettingsHyperlink> getSettings() {
		return settings;
	}


	public void setSettings(List<SettingsHyperlink> settings) {
		this.settings = settings;
	}


	
	
	
}
