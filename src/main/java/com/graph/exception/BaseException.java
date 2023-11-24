package com.graph.exception;

public abstract class BaseException extends Exception{
	private static final long serialVersionUID = 1L;
    private final String message;
 
    protected BaseException(String msg)
    {
        this.message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
