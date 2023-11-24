package com.graph.model.wt;

public class GraphInformationResponse {
	public RegularGraphModel regularGraphModel;
	public RecentGraphSaveModel recentGraphModel;
	public Message message;

	public GraphInformationResponse() {

	}

	public GraphInformationResponse(RegularGraphModel regularGraphModel,RecentGraphSaveModel recentGraphModel, Message message) {
		this.regularGraphModel = regularGraphModel;
		this.recentGraphModel = recentGraphModel;
		this.message = message;
	}

	

	public RecentGraphSaveModel getRecentGraphModel() {
		return recentGraphModel;
	}

	public void setRecentGraphModel(RecentGraphSaveModel recentGraphModel) {
		this.recentGraphModel = recentGraphModel;
	}

	public RegularGraphModel getRegularGraphModel() {
		return regularGraphModel;
	}

	public void setRegularGraphModel(RegularGraphModel regularGraphModel) {
		this.regularGraphModel = regularGraphModel;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

}
