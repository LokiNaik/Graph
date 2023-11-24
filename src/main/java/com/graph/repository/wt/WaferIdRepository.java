package com.graph.repository.wt;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.WaferIdModel;


public interface WaferIdRepository extends MongoRepository<WaferIdModel, String> {
    @Query(value = "{ 'graphId' : {$ne : ?0} }")
    WaferIdModel findByGraphId(ObjectId graphId);
}
