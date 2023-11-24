package com.graph.model.wt;

public class Hyperlink {

	private String[] pages;
	private String defaultNavigateTo;
	
	public Hyperlink(String[] pages, String defaultNavigateTo) {
		super();
		this.pages = pages;
		this.defaultNavigateTo = defaultNavigateTo;
	}

	public String[] getPages() {
		return pages;
	}

	public void setPages(String[] pages) {
		this.pages = pages;
	}

	public String getDefaultNavigateTo() {
		return defaultNavigateTo;
	}

	public void setDefualtNavigateTo(String defaultNavigateTo) {
		this.defaultNavigateTo = defaultNavigateTo;
	}
	
	
	
	
}
