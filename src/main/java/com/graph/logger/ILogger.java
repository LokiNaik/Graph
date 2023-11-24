package com.graph.logger;

public interface ILogger {

    public void info(String message);

    public void warn(String message);

    public void trace(String message);

    public void error(String message, Exception e);
}