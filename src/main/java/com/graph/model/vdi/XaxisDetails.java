package com.graph.model.vdi;

import java.util.List;

public class XaxisDetails {
	
	public int axisCount;
	public String type;
	public List<XaxisSetting> settings;
	public int getAxisCount() {
		return axisCount;
	}
	public String getType() {
		return type;
	}
	public List<XaxisSetting> getSettings() {
		return settings;
	}
}
