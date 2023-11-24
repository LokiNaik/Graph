package com.graph.model.wt;

import java.util.List;

public class YaxisDetails {
	public int getAxisCount() {
		return axisCount;
	}
	public void setAxisCount(int axisCount) {
		this.axisCount = axisCount;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<YaxisSetting> getSettings() {
		return settings;
	}
	public void setSettings(List<YaxisSetting> settings) {
		this.settings = settings;
	}
	private int axisCount;
	private String type;
	private List<YaxisSetting> settings;
}
