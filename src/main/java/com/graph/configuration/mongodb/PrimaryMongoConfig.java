package com.graph.configuration.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration

@EnableMongoRepositories(basePackages = 
{"com.graph.repository.wt","com.graph.model.wt"},
    mongoTemplateRef = "primaryMongoTemplate")

 public class PrimaryMongoConfig {

 }