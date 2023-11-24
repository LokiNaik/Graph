package com.graph.repository.wt;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.GraphTypeModel;

public interface GraphTypeRepository extends MongoRepository<GraphTypeModel, String>{

	@Query("{graphType:?0}")
	GraphTypeModel findGraphType(String graphType);
}
