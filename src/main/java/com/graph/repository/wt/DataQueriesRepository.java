package com.graph.repository.wt;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.QueryList;
import com.graph.model.wt.SaveDataQueries;


public interface DataQueriesRepository extends MongoRepository<SaveDataQueries, String> {

	@Query(value = "{'queryName' : ?0,'deleteFlag' : false}")
	SaveDataQueries findByDataQuery(String queryName );
	
	@Query(value = "{ 'queryName' : ?0,'accessType' : ?1,'deleteFlag' : false}")
	SaveDataQueries findByData(String queryName ,String accessType);
}
