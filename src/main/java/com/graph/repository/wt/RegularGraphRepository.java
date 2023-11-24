package com.graph.repository.wt;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.RegularGraphModel;


public interface RegularGraphRepository extends MongoRepository<RegularGraphModel, String> {
	@Query(value = "{'graphName' : ?0,'accessType' : ?1,'category' : ?2,'deleteFlag' : false}")
	RegularGraphModel findByGraphDetails(String graphName, String accessType, String graphType);
	
	@Query(value = "{ '_id' : {$ne : ?0},'graphName' : ?1,'accessType' : ?2,'deleteFlag' : false}")
	RegularGraphModel findByGraphAndAccessType(String id,String graphName, String accessType);
	
	@Query(value = "{ '_id' : {$ne : ?0},'graphName' : ?1,'accessType' : ?2,'category' : ?3,'deleteFlag' : false}")
	RegularGraphModel findByGraphAccessTypeCategory(String id,String graphName, String accessType,String category);
	RegularGraphModel insert(RegularGraphModel regularGraphInfo);	
}
