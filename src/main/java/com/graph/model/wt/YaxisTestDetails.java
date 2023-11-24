package com.graph.model.wt;

import java.util.List;

public class YaxisTestDetails {
	private int axisCount;
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
    public List<YaxisTestSetting> getSettings() {
        return settings;
    }
    public void setSettings(List<YaxisTestSetting> settings) {
        this.settings = settings;
    }
    private String type;
	private List<YaxisTestSetting> settings;
}
