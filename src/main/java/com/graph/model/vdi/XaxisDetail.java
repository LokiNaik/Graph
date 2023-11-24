package com.graph.model.vdi;

import java.util.List;

public class XaxisDetail {
	public int axisCount;
	public String type;
	public List<YaxisSetting> settings;

	public int getAxisCount() {
		return axisCount;
	}

	public String getType() {
		return type;
	}

	public List<YaxisSetting> getSettings() {
		return settings;
	}
}
