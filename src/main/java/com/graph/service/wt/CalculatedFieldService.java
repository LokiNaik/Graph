package com.graph.service.wt;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.stereotype.Service;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.Message;


@Service
public class CalculatedFieldService implements ICalculatedFieldService {
	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;
	
	public DataListModel getCalculatedFields() {
		 DataListModel testListModel = new DataListModel();
		
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
				.and("$calculatedMeasureName").as("calculatedMeasureName");
		 Aggregation aggregation = Aggregation.newAggregation(projectFieldSelectionOperation);
		 List<Document> results = mongoTemplate.aggregate(aggregation, "CalculatedField", Document.class)
				 .getMappedResults();
		 for (Document obj : results) {
			  obj.put("_id", new ObjectId(obj.get("_id").toString()).toHexString());
		   }
		 
		   Message message = new Message();
		   if (results.size() > 0) {
			  testListModel.setDataList(results);
			  message.setSuccess(true);
			  message.setCode(200);
			  testListModel.setStatus(message);
			  return testListModel;
		   }
		   else {
			  testListModel.setDataList(results);
			  message.setSuccess(false);
			  message.setCode(204);
			  testListModel.setStatus(message);
			  return testListModel;
		   }

	
	}
}
