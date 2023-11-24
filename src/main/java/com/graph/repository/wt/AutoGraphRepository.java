package com.graph.repository.wt;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.AutoSlides;
import com.graph.model.wt.SaveAutoGraph;


public interface AutoGraphRepository extends MongoRepository<SaveAutoGraph, String> {

	@Query(value = "{ 'autographName' : ?0,'accessType' : ?1, 'category' :?2, 'deleteFlag' : false}")
	SaveAutoGraph findByAutoGraphName(String autographName, String accessType, String category);
	
	@Query(value = "{ '_id' : {$ne : ?0},'autographName' : ?1,'accessType' : ?2,'category' : ?3,'deleteFlag' : false}")
	SaveAutoGraph findByAutoGraph(String id, String autographName, String accessType, String category);
}
