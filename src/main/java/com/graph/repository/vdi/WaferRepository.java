package com.graph.repository.vdi;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.WaferIdModel;


public interface WaferRepository extends MongoRepository<WaferIdModel, String> {
    @Query(value = "{ 'graphId' : {$ne : ?0} }")
    WaferIdModel findByGraphId(ObjectId graphId);
}
