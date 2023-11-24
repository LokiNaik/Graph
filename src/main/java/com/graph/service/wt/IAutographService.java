package com.graph.service.wt;

import java.io.IOException;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import com.graph.model.wt.AutoGraph;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.DataQueries;
import com.graph.model.wt.GraphModelJSON;

public interface IAutographService {

	DataModel getAutoGraphs(GraphModelJSON search) throws IOException;
	 public List<Document> getLoadQueryList();

	    public List<Document> getDataQuery(DataQueries request);

	    public DataListModel saveDataQuery(DataQueries request);

	    public DataListModel saveAutoGraph(AutoGraph request);
	    
	    DataListModel specificAutoGraphs(List<String> autoGraphIds);
	    
		Aggregation getAutoGraphCompHistoTestValue2X(String dimension, List<Criteria> criteriaList,
				Document projectFilteringMinMax);
		
		Aggregation getAutoGraphCompHistoTestValue1X(int testNumber, String testName, MatchOperation matchOperation,
				AggregationOperation projectionOperation, Document projectFetchDatas);
		
		Aggregation getAutoCompTestMinMaxAgg(String testName, int testNumber, List<Criteria> criteriaList);
		
		List<Criteria> getWafersFromQueryCriteria(List<Criteria> criteriaListforwafers,
				LookupOperation lookupProductOperation, UnwindOperation unwindProductInfo, MatchOperation match,
				ProjectionOperation projectFetchData);
	   Aggregation getAutoGraphBoxPlotTestValue2X(String dimension, String dimension2x, List<Criteria> criteriaList,
			 String dimensionName, String dimensionName2X, Document projectFilteringMinMax);
}
