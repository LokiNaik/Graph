package com.graph.model.wt;

import java.util.Date;
import java.util.List;

public class SettingsHyperlink {

	private AppHyperLink app;
	private Hyperlink hyperlink;
	private String LastUpdateddate;
	
	
	public SettingsHyperlink() {
		super();
		// TODO Auto-generated constructor stub
	}


	public SettingsHyperlink(AppHyperLink app, Hyperlink hyperlink, String lastUpdateddate) {
		super();
		this.app = app;
		this.hyperlink = hyperlink;
		LastUpdateddate = lastUpdateddate;
	}


	public AppHyperLink getApp() {
		return app;
	}


	public void setApp(AppHyperLink app) {
		this.app = app;
	}


	public Hyperlink getHyperlink() {
		return hyperlink;
	}


	public void setHyperlink(Hyperlink hyperlink) {
		this.hyperlink = hyperlink;
	}


	public String getLastUpdateddate() {
		return LastUpdateddate;
	}


	public void setLastUpdateddate(String lastUpdateddate) {
		LastUpdateddate = lastUpdateddate;
	}

	
}
