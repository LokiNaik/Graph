package com.graph.model.wt;

public class Message {

	public boolean success;
	public int code;
	public Message() {
	
    }
	public Message(boolean success, int  code ,String status) {
		this.success = success;
		this.code = code;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
