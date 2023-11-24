package com.graph.logger.factories;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.graph.logger.ILogger;


public class Log4jLogger implements ILogger {

    private final Logger logger;

    public Log4jLogger() {
        this.logger = LogManager.getLogger();
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void trace(String message) {
        logger.warn(message);
    }

    public void error(String message, Exception e) {
        logger.error(message, e);
    }
}
