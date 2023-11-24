package com.graph.repository.vdi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.RecentGraphSaveModel;
import com.graph.model.wt.RegularGraphModel;

public interface RecentRepository extends MongoRepository<RecentGraphSaveModel, String> {
	@Query(value = "{ '_id' : {$ne : ?0},'graphName' : ?1,'accessType' : ?2,'deleteFlag' : false}")
	RecentGraphSaveModel findByGraphAndAccessType(String id,String graphName, String accessType);
	
	 @Query(value = "{ '_id' : {$ne : ?0},'graphName' : ?1,'accessType' : ?2,'category' : ?3,'deleteFlag' : false}")
	 RecentGraphSaveModel findByGraphAccessTypeCategory(String id,String graphName, String accessType,String category);
	 
	 @Query(value = "{'graphName' : ?0,'accessType' : ?1,'category' : ?2,'deleteFlag' : false}")
	 RecentGraphSaveModel findByGraphDetails(String graphName, String accessType, String graphType);
}
