package com.graph.repository.wt;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.SavedAnnotationModel;


public interface SavedAnnotationRepository extends MongoRepository<SavedAnnotationModel, String> {
	@Query(value = "{ 'graphId' : {$ne : ?0} }")
	SavedAnnotationRepository findByGraphId(ObjectId graphId);
}
