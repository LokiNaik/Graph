package com.graph.repository.vdi;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.SavedAnnotationModel;


public interface SavedRepository extends MongoRepository<SavedAnnotationModel, String> {
	@Query(value = "{ 'graphId' : {$ne : ?0} }")
	SavedRepository findByGraphId(ObjectId graphId);
}
