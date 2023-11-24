package com.graph.model.common;

import java.util.List;

public class DataListExceptionModel {
	private List<?> data;

	public List<?> getData() {
		return data;
	}

	public void setData(List<?> data) {
		this.data = data;
	}

	public MessageadvGraph getMessage() {
		return message;
	}

	public void setMessage(MessageadvGraph message) {
		this.message = message;
	}

	public MessageadvGraph message;
}
