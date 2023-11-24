package com.graph.repository.wt;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graph.model.wt.AxisModel;

public interface AxisModelRepository extends MongoRepository<AxisModel, String>{

	@Query("{applicationName:?0}")
	AxisModel findAxisModel(String applicationName);	
	
	
}
