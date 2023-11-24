package com.graph.logger;

import com.graph.logger.factories.Log4jLogger;

public class Logger {
    public ILogger getLogger(){
        // read from config for logger selection
        return new Log4jLogger();
    }
}