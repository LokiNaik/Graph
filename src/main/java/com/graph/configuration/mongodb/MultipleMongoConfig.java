package com.graph.configuration.mongodb;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MultipleMongoConfig {

    @Primary
    @Bean(name = "primary")
    @ConfigurationProperties(prefix = "spring.data.mongodb")
    public MongoProperties getPrimary() {
	   return new MongoProperties();
    }

    @Bean(name = "secondary")
    @ConfigurationProperties(prefix = "mongodb")
    public MongoProperties getSecondary() {
	   return new MongoProperties();
    }

    @Primary
    @Bean(name = "primaryMongoTemplate")
    public MongoTemplate primaryMongoTemplate() throws Exception {
	   return new MongoTemplate(primaryFactory(getPrimary()));
    }

    @Bean(name = "secondaryMongoTemplate")
    public MongoTemplate secondaryMongoTemplate() throws Exception {
	   return new MongoTemplate(secondaryFactory(getSecondary()));
    }

    @Primary
    @Bean
    public MongoDatabaseFactory primaryFactory(MongoProperties mongo) throws Exception {
	   return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }

    @Bean
    public MongoDatabaseFactory secondaryFactory(MongoProperties mongo) throws Exception {
	   return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }

}
