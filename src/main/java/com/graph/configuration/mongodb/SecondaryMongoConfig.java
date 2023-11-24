package com.graph.configuration.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = 
{"com.graph.repository.vdi","com.graph.model.vdi"},
    mongoTemplateRef = "secondaryMongoTemplate")
public class SecondaryMongoConfig {
}