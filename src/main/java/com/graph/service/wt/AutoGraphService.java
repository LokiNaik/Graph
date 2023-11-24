package com.graph.service.wt;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.constants.wt.AdvancedGraphConstants;
import com.graph.model.wt.AutoGraph;
import com.graph.model.wt.AutoSlides;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.DataQueries;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.LockParam;
import com.graph.model.wt.Message;
import com.graph.model.wt.QueryList;
import com.graph.model.wt.SaveAutoGraph;
import com.graph.model.wt.SaveDataQueries;
import com.graph.model.wt.SortParam;
import com.graph.repository.wt.AutoGraphRepository;
import com.graph.repository.wt.DataQueriesRepository;
import com.graph.utils.CriteriaUtil;
import com.graph.utils.DateUtil;
import com.graph.utils.ValidationUtil;


	
	@Service
	public class AutoGraphService implements IAutographService {
		@Autowired
		@Qualifier(value = "primaryMongoTemplate")
		private MongoTemplate mongoTemplate;
		@Value("${currentlocale}")
		private String currentLocale;
		@Autowired
		private DataQueriesRepository dataQueriesRepository;
		@Autowired
		private AutoGraphRepository autoGraphRepository;
		@Value("${settings.path}")
		private String filePath;
		@Value("${settings.folder}")
		private String settingsFolder;
		@Value("${maximumLimitBoxPlot}")
		private String maximumLimitBox;
	
		private final Logger logger = LoggerFactory.getLogger(getClass());

	/*
     * List AutoGraph
     */
    @Override
    public DataModel getAutoGraphs(GraphModelJSON search) throws IOException {

    	String CollectionName = null;
	   List<SortParam> sortParams = search.getSort();
	   List<FilterParam> filterParams = search.getFilter();
	   LockParam lock = search.getLock();
	   List<Criteria> criteriaList = new ArrayList<>();
	   Pageable pageable = null;
	   int pageSize = 0;
	   int pageNo = 0;
	   if ((search.getPageSize() != null || search.getPageNo() != null) && search.getPageSize() != ""
			 && search.getPageNo() != "") {
		  pageSize = Integer.parseInt(search.getPageSize().trim());
		  pageNo = Integer.parseInt(search.getPageNo().trim());
	   }
	   if (pageSize >= 1) {
		  pageable = PageRequest.of(pageNo, pageSize);
	   }
	   else {
		  pageable = PageRequest.of(0, 25);
	   }
	   if (filterParams != null && filterParams.size() >= 1) {
		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
	   }
	   Criteria commonCriteria = null;
	   commonCriteria = Criteria.where("deleteFlag").ne(true).and("category").is("Auto");

	   AggregationOptions options = AggregationOptions.builder()
			 .collation(Collation.of(currentLocale).numericOrdering(true)).build();
	   MatchOperation matchApplication = Aggregation.match(commonCriteria);
	   MatchOperation matchFilter = Aggregation
			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

	   List<FacetOperation> facets = new ArrayList<>();
	   if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
		  facets.add(Aggregation.facet(matchApplication, matchFilter).as("output"));
	   }
	   else {
		  facets.add(Aggregation.facet(matchApplication).as("output"));
	   }
	   String output = "output.";
	   List<SortParam> sortParmsList = Collections.synchronizedList(sortParams);
	   Collections.reverse(sortParmsList);
	   if (sortParams != null && sortParams.size() >= 1) {
		  for (SortParam sortParam : sortParmsList) {
			 Sort sort = Sort.by(output + sortParam.getField());
			 output += "output.";
			 if (sortParam.isAsc()) {
				sort = sort.ascending();
			 }
			 else {
				sort = sort.descending();
			 }
			 FacetOperation unwindSortFacet = Aggregation
				    .facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
			 facets.add(unwindSortFacet);
		  }
	   }
	   else {
		  Sort sort = Sort.by(output + "autographName").ascending();
		  FacetOperation unwindSortFacet = Aggregation
				.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
		  facets.add(unwindSortFacet);
		  output += "output.";
	   }
	   FacetOperation unWindAndProjectFieldsOperation = Aggregation.facet(Aggregation.unwind("$output", false),
			 new SkipOperation(pageable.getPageNumber() * pageable.getPageSize()),
			 Aggregation.limit(pageable.getPageSize()),
			 Aggregation.project().and("$" + output + "_id").as("_id").and("$" + output + "autographName")
				    .as("autographName").and("$" + output + "createdAt").as("createdAt")
				    .and("$" + output + "lastUpdatedDate").as("lastUpdatedDate").and("$" + output + "accessType")
				    .as("accessType"))
			 .as("output");

	   facets.add(unWindAndProjectFieldsOperation);
	   Aggregation aggregation = Aggregation.newAggregation(facets).withOptions(options);
	   Aggregation countAggregation = null;
	   if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
		  countAggregation = Aggregation.newAggregation(matchApplication, matchFilter,
				Aggregation.count().as("count"));
	   }
	   else {
		  countAggregation = Aggregation.newAggregation(matchApplication, Aggregation.count().as("count"));
	   }
	   List<Document> result = mongoTemplate.aggregate(aggregation, "AutoGraphs", Document.class).getMappedResults();
	   List<Document> count = mongoTemplate.aggregate(countAggregation, "AutoGraphs", Document.class)
			 .getMappedResults();
	   result = ValidationUtil.documentIdConverter((List<Document>) result.get(0).get("output"));

	   if (sortParams.size() > 0 && sortParams != null) {
		  result = CriteriaUtil.sortAxisParameters(result, sortParams);
	   }

	   DataModel response = new DataModel();
	   response.setData(result);
	   if (!count.isEmpty()) {
		  response.setCount((int) count.get(0).get("count"));
	   }
	   return response;
    }
    
    /*
     * getLoadQueryList
     */
    
    public List<Document> getLoadQueryList() {
	   ProjectionOperation projectQueryNames = Aggregation.project().and("$_id").as(AdvancedGraphConstants.QUERY_ID).and("$queryName")
			 .as("queryName").andExclude("_id");
	   Aggregation aggregation = Aggregation.newAggregation(projectQueryNames);
	   List<Document> results = mongoTemplate.aggregate(aggregation, "DataQueries", Document.class).getMappedResults();
	   for (Document obj : results) {
		  obj.put(AdvancedGraphConstants.QUERY_ID, new ObjectId(obj.get(AdvancedGraphConstants.QUERY_ID).toString()).toHexString());
	   }
	   return results;
    }
    
    /*
     * getDataQuery
     */
    public List<Document> getDataQuery(DataQueries dataQueries) {
 	   MatchOperation matchQueryName = Aggregation.match(Criteria.where("queryName").in(dataQueries.getQueryName()));
 	   MatchOperation matchId = Aggregation.match(Criteria.where("_id").in(dataQueries.get_id()));
 	   ProjectionOperation projectQueryNames = Aggregation.project().and("$queryList").as("queryList").and("$_id")
 			 .as(AdvancedGraphConstants.DATA_QUERY_ID).and("accessType").as("accessType").andExclude("_id");
 	   Aggregation aggregation = Aggregation.newAggregation(matchId, matchQueryName, projectQueryNames);
 	   List<Document> results = mongoTemplate.aggregate(aggregation, "DataQueries", Document.class).getMappedResults();
 	   for (Document obj : results) {
 		  obj.put(AdvancedGraphConstants.DATA_QUERY_ID, new ObjectId(obj.get(AdvancedGraphConstants.DATA_QUERY_ID).toString()).toHexString());
 	   }
 	   return results;
     }

    /*
     * saveDataQuery
     */
    
    @Override
    public DataListModel saveDataQuery(DataQueries request) {
	   DataListModel datalist = new DataListModel();
	   Message message = new Message();
	   SaveDataQueries saveQueryModel = null;
	   SaveDataQueries saveQueryModelName = null;
	   SaveDataQueries saveDataQueries = new SaveDataQueries();
	   List<FilterParam> filterParams = null;
	   String value;
	   String field;
	   String operator;
	   List<Document> docList = new ArrayList<Document>();
	   List<String> saveDataQueryDetails = new ArrayList<String>();

	   try {
		  if (request.get_id() == null) {
			 if (request.getQueryName() != null) {
				validateDateFormat(request, message, docList);
				if (message.getCode() != 422) {

				    saveQueryModel = dataQueriesRepository.findByData(request.getQueryName(),
						  request.getAccessType());

				    if (saveQueryModel == null) {
					   setParamTosaveQuery(request, saveDataQueries);
					   saveDataQueries.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
					   dataQueriesRepository.insert(saveDataQueries);
					   saveDataQueryDetails.add(saveDataQueries.get_id());
					   saveDataQueryDetails.add(saveDataQueries.getQueryName());
					   datalist.setDataList(saveDataQueryDetails);
					   message.setCode(200);
					   message.setSuccess(true);

				    }
				    else {
					   message.setCode(601); // QueryName repetition
					   message.setSuccess(false);
				    }
				}
				else {
				    message.setCode(422); // entered DateFormat error
				    message.setSuccess(false);
				}
			 }
			 else {
				message.setCode(602); // No QueryName
				message.setSuccess(false);
			 }
		  }
		  else {
			 message.setCode(302);
			 message.setSuccess(false);
		  }
	   }
	   catch (Exception e) {

			 logger.error("Save Data Query Error : " + e);
	   }
	   datalist.setStatus(message);
	   return datalist;
    }
    /*
     * setParamTosaveQuery
     */
    private void setParamTosaveQuery(DataQueries request, SaveDataQueries saveDataQueries) {
 	   if (request.getQueryName() == null) {
 		  saveDataQueries.setQueryName("-");
 	   }
 	   else {
 		  saveDataQueries.setQueryName(request.getQueryName());
 	   }
 	   if (request.getAccessType() == null) {
 		  saveDataQueries.setAccessType("-");
 	   }
 	   else {
 		  saveDataQueries.setAccessType(request.getAccessType());
 	   }

 	   saveDataQueries.setQueryList(request.getQueryList());
 	   saveDataQueries.setCreatedBy(AdvancedGraphConstants.USER_1);
 	   saveDataQueries.setUpdatedBy(AdvancedGraphConstants.USER_1);
 	   saveDataQueries.setDeleteFlag(false);
 	   saveDataQueries.setApplicationName("WT");

     }
    
    /*
     * saveAutoGraph
     */
    @Override
    public DataListModel saveAutoGraph(AutoGraph request) {
	   DataListModel datalist = new DataListModel();
	   Message message = new Message();
	   SaveAutoGraph saveAutoModel = null;
	   SaveAutoGraph saveAutoGraph = new SaveAutoGraph();
	   List<String> saveAutoGraphDetails = new ArrayList<String>();

	   try {
		  if (request.get_id() == null) { // first save when Id is null
			 if (request.getAutographName() != null) {

				saveAutoModel = autoGraphRepository.findByAutoGraphName(request.getAutographName(),
					   request.getAccessType(), request.getCategory());

				if (saveAutoModel == null) {
				    setParamTosaveAutoGraph(request, saveAutoGraph);
				    saveAutoGraph.setCreatedAt(DateUtil.getCurrentDateInUTC());
				    saveAutoGraph.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
				    autoGraphRepository.insert(saveAutoGraph);
				    saveAutoGraphDetails.add(saveAutoGraph.get_id());
				    datalist.setDataList(saveAutoGraphDetails);
				    message.setCode(200);
				    message.setSuccess(true);
				}
				else {
				    message.setCode(601);
				    message.setSuccess(false);// Name Duplication
				}
			 }
			 else {
				message.setCode(302);
				message.setSuccess(false);// no name
			 }
		  }
		  else if (request.get_id() != null) {// repeated save

			 SaveAutoGraph saveAutoGraphModel = null;
			 Optional<SaveAutoGraph> optionalObj = autoGraphRepository.findById(request.get_id().toString());
			 saveAutoGraph = optionalObj.get();

			 if (saveAutoGraph.getAutographName().equals(request.getAutographName())
				    && saveAutoGraph.getAccessType().equals(request.getAccessType())) {// same name & access type

				setParamTosaveAutoGraph(request, saveAutoGraph);
				saveAutoGraph.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
				autoGraphRepository.save(saveAutoGraph);
				message.setCode(202);
				message.setSuccess(true);
				saveAutoGraphDetails.add(saveAutoGraph.get_id());
				datalist.setDataList(saveAutoGraphDetails);

			 }
			 else {
				saveAutoGraphModel = autoGraphRepository.findByAutoGraphName(request.getAutographName(),
					   request.getAccessType(), request.getCategory());
				if (saveAutoGraphModel == null) {
				    setParamTosaveAutoGraph(request, saveAutoGraph);
				    saveAutoGraph.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
				    autoGraphRepository.save(saveAutoGraph);
				    message.setCode(202);
				    message.setSuccess(true);
				    saveAutoGraphDetails.add(saveAutoGraph.get_id());
				    datalist.setDataList(saveAutoGraphDetails);
				}
				else {
				    message.setCode(601);// Name duplication with same access type
				    message.setSuccess(false);
				}

			 }
		  }
	   }
	   catch (Exception e) {
			 logger.error("Save graph error : " + e);
	   }
	   datalist.setStatus(message);
	   return datalist;
    }
/*
 * setParamTosaveAutoGraph
 */
    private void setParamTosaveAutoGraph(AutoGraph request,SaveAutoGraph saveAutoGraph) {

 	   if (request.getCategory() == null) {
 		  saveAutoGraph.setCategory("-");
 	   }
 	   else {
 		  saveAutoGraph.setCategory(request.getCategory());
 	   }
 	   if (request.getAutographName() == null) {
 		  saveAutoGraph.setAutographName("-");
 	   }
 	   else {
 		  saveAutoGraph.setAutographName(request.getAutographName());
 	   }
 	   if (request.getAccessType() == null) {
 		  saveAutoGraph.setAccessType("-");
 	   }
 	   else {
 		  saveAutoGraph.setAccessType(request.getAccessType());
 	   }
 	   saveAutoGraph.setAutoSlides(request.getAutoSlides());
 	   saveAutoGraph.setCreatedBy(AdvancedGraphConstants.USER_1);
 	   saveAutoGraph.setUpdatedBy(AdvancedGraphConstants.USER_1);
 	   saveAutoGraph.setDeleteFlag(false);

     }

     /*
      * View AutoGraph
      */
     @Override
     public DataListModel specificAutoGraphs(List<String> autoGraphIds) {
 	   MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(autoGraphIds));
 	   MatchOperation deleteFlag = Aggregation.match(Criteria.where("deleteFlag").is(false));
 	   ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
 			 .and("$lastUpdatedDate").as("lastUpdatedDate").and("$autographName").as("autographName")
 			 .and("$category").as("category").and("$accessType").as("accessType").and("$autoSlides").as("autoSlides")
 			 .and("$createdBy").as("createdBy").and("$updatedBy").as("updatedBy").and("$createdAt").as("createdAt")
 			 .and("$_class").as("_class");
 	   Aggregation aggregation = Aggregation.newAggregation(matchOperation, deleteFlag, projectFieldSelectionOperation,
 			 Aggregation.sort(Sort.Direction.ASC, "autographName"));
 	   List<SaveAutoGraph> results = mongoTemplate.aggregate(aggregation, "AutoGraphs", SaveAutoGraph.class)
 			 .getMappedResults();

 	   DataListModel testListModel = new DataListModel();
 	   Message message = new Message();
 	   if (!results.isEmpty()) {
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
     /*
	  * validateDateFormat
	 */
	    private Message validateDateFormat(DataQueries request, Message message, List<Document> queryList) {
	 	   List<QueryList> results = request.getQueryList();
	 	   for (QueryList document : results) {
	 		  Document queryListDocument = new Document();
	 		  queryListDocument.put(AdvancedGraphConstants.FIELD_VAL, document.getField());
	 		  queryListDocument.put(AdvancedGraphConstants.OPERATOR_VAL, document.getOperator());
	 		  queryListDocument.put(AdvancedGraphConstants.VALUE_IN, document.getValue());

	 		  queryList.add(queryListDocument);
	 	   }
	 	   List<FilterParam> filterValidations = null;
	 	   Document queryDocument = new Document();
	 	   queryDocument.put("queryList", queryList);
	 	   filterValidations = CriteriaUtil.getDataFromQueryList(queryDocument);
	 	   for (FilterParam output : filterValidations) {
	 		  if (output.getField().endsWith("Time")) {

	 			 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	 			 try {
	 				LocalDate localTo = LocalDate.parse(output.getValueFirst(), formatter);
	 			 }
	 			 catch (Exception e) {
	 				message.setCode(422); // entered DateFormat error
	 				message.setSuccess(false);

	 			 }

	 		  }
	 	   }
	 	   return message;
	     }

	    public Aggregation getAutoGraphCompHistoTestValue1X(int testNumber, String testName, MatchOperation matchOperation,
				AggregationOperation projectionOperation, final Document projectFetchDatas) {
			Aggregation aggregationMean;
			aggregationMean = Aggregation.newAggregation(productLookUp ->new Document(AdvancedGraphConstants.LOOK_UP, 
					    new Document("from", AdvancedGraphConstants.PRODUCT_INFO)
			            .append(AdvancedGraphConstants.LOCAL_FIELD, "mesWafer.productId")
			            .append(AdvancedGraphConstants.FOREIGN_FIELD, "_id")
			            .append("as", AdvancedGraphConstants.PRODUCT_INFO)),
					  unwindproduct->new Document(AdvancedGraphConstants.UNWIND,
							  new Document("path",  AdvancedGraphConstants.DOLLAR_PRODUCT_INFO).append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)),lookUpTestValue->new Document(AdvancedGraphConstants.LOOK_UP, 
									    new Document("from", "TestValueSummary")
							            .append("let", 
							    new Document("businessId", "$mesWafer._id"))
							            .append("pipeline", Arrays.asList(new Document(AdvancedGraphConstants.MATCH_AGG, 
							                new Document("$expr", 
							                new Document("$and", Arrays.asList(new Document("$eq", Arrays.asList("$wd", "$$businessId")), 
							                                new Document("$eq", Arrays.asList("$tn", testNumber)), 
							                                new Document("$eq", Arrays.asList("$tm", testName))))))))
							            .append("as", "testValueDetails")), 
							    unwindTestValue->new Document(AdvancedGraphConstants.UNWIND,new Document("path", "$testValueDetails")
							            .append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)), 
							    matchOperation,
							    projectionOperation,
							    addFields -> new Document(AdvancedGraphConstants.ADD_FIELDS, 
							            	    new Document("rangeValues", AdvancedGraphConstants.RANGE_KEY)),
							            	    
							     projectFetching->new Document(AdvancedGraphConstants.PROJECT_AGG, 
							    		    new Document("Group", 1)
							                .append(AdvancedGraphConstants.DIMENSION_KEY, 1)
							                .append(AdvancedGraphConstants.RANGE_VAL, 1)
							                .append(AdvancedGraphConstants.COUNT_VAL, 1)
							                .append("equalRange", 
							        new Document("$cond", 
							        new Document("if", 
							        new Document("$eq", Arrays.asList(AdvancedGraphConstants.RANGE_KEY, "$rangeValues")))
							                        .append("then", 1)
							                        .append("else", 0))).append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
					                .append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)),
							     
							     addFieldsTotalOperations ->new Document(AdvancedGraphConstants.ADD_FIELDS, 
							    		    new Document("TotalCount", 
							    		    new Document(AdvancedGraphConstants.MULTIPLY_AGG, Arrays.asList("$equalRange", "$Count")))),
							    		    
							    		 groupOperation->new Document(AdvancedGraphConstants.GROUP_AGG, 
							    				    new Document("_id", 
							    				    	    new Document(AdvancedGraphConstants.DIMENSION_KEY, AdvancedGraphConstants.DOLLAR_DIMENSION)
							    				    	                .append(AdvancedGraphConstants.RANGE_VAL, AdvancedGraphConstants.RANGE_KEY))
							    				    	            .append(AdvancedGraphConstants.COUNT_VAL, 
							    				    	    new Document("$sum", "$TotalCount")).append("StartTime",
					    				    	            		new Document("$min",AdvancedGraphConstants.DOLLAR_WAFER_START_TIME))
					    				    	            .append("EndTime",
					    				    	            		new Document("$max",AdvancedGraphConstants.DOLLAR_WAFER_END_TIME))
					    				    	            .append("WaferId", new Document("$addToSet", "$Group"))),
							    		 groupOperationTest->
							    		 new Document(AdvancedGraphConstants.GROUP_AGG, 
							    				    new Document("_id", 
							    				    new Document(AdvancedGraphConstants.DIMENSION_KEY, "$_id.Dimension")
							    				                .append("Group", "$_id.Group"))
							    				            .append("yAxis", 
							    				    new Document("$addToSet", 
							    				    new Document(AdvancedGraphConstants.RANGE_VAL, "$_id.range")
							    				                    .append(AdvancedGraphConstants.COUNT_VAL, "$Count")
							    				                    .append("WaferId", "$WaferId"))).append("StartTime",
							    				    	            		new Document("$min","$StartTime"))
							    				    	            .append("EndTime",
							    				    	            		new Document("$max","$EndTime"))),
							    		 projectFetchingData->projectFetchDatas,
							    		 projectExcludeIds->new Document(AdvancedGraphConstants.PROJECT_AGG, 
							    				    new Document("_id", 0))
							    		 
							    		 
					  ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
			return aggregationMean;
		}
	    
	    
	    public Aggregation getAutoGraphCompHistoTestValue2X(String Dimension, List<Criteria> criteriaList,
				final Document projectFilteringMinMax) {
			Aggregation aggregationval;
			MatchOperation match = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
			aggregationval = Aggregation.newAggregation(
					lookup -> new Document(AdvancedGraphConstants.LOOK_UP,
							new Document("from", AdvancedGraphConstants.TEST_HISTORY_KEY).append(AdvancedGraphConstants.LOCAL_FIELD, AdvancedGraphConstants.WAFER_ID)
									.append(AdvancedGraphConstants.FOREIGN_FIELD, "mesWafer._id").append("as", AdvancedGraphConstants.TEST_HISTORY_KEY)),

					unwindTesthistory -> new Document(AdvancedGraphConstants.UNWIND,
							new Document("path", AdvancedGraphConstants.DOLLAR_TEST_HISTORY).append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)),
					lookup -> new Document(AdvancedGraphConstants.LOOK_UP,
							new Document("from", AdvancedGraphConstants.PRODUCT_INFO).append(AdvancedGraphConstants.LOCAL_FIELD, "TestHistory.mesWafer.productId")
									.append(AdvancedGraphConstants.FOREIGN_FIELD, "_id").append("as", AdvancedGraphConstants.PRODUCT_INFO)),

					unwindTesthistory -> new Document(AdvancedGraphConstants.UNWIND,
							new Document("path",  AdvancedGraphConstants.DOLLAR_PRODUCT_INFO).append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)),
					match,
					addFields -> new Document(AdvancedGraphConstants.ADD_FIELDS, new Document("GroupingField", "$TestHistory." + Dimension)),

					groupfield -> new Document(AdvancedGraphConstants.GROUP_AGG,
							new Document("_id", "$GroupingField").append(AdvancedGraphConstants.VALUE_IN, new Document("$push", "$mv"))
							.append(AdvancedGraphConstants.WAFER_START_TIME, new Document("$min", "$TestHistory.mesWafer.WaferStartTime"))
							.append(AdvancedGraphConstants.WAFER_END_TIME, new Document("$min", "$TestHistory.mesWafer.WaferEndTime"))
							),

					projectField -> new Document(AdvancedGraphConstants.PROJECT_AGG,
							new Document("testvalue", new Document("$reduce",
									new Document("input", "$value").append("initialValue", new ArrayList<>()).append("in",
											new Document("$concatArrays", Arrays.asList("$$value", "$$this")))))
													.append(AdvancedGraphConstants.DIMENSION_KEY, "$_id").append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
													.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)),

					projectFiel -> projectFilteringMinMax,
					matchZero -> new Document(AdvancedGraphConstants.MATCH_AGG, new Document("testvalue", new Document("$gt", 0))),
					soreField -> new Document(AdvancedGraphConstants.PROJECT_AGG,
							new Document("testvalue", new Document("$function",
									new Document("body", "function(mv){return mv.sort(function(a, b){return a - b})}")
											.append("args", Arrays.asList("$testvalue")).append("lang", "js")))
													.append(AdvancedGraphConstants.DIMENSION_KEY, "$_id").append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
													.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)),
					boxPlotCal -> new Document(AdvancedGraphConstants.PROJECT_AGG, new Document(AdvancedGraphConstants.DIMENSION_KEY, AdvancedGraphConstants.DOLLAR_DIMENSION)
							.append("Max", new Document("$max", Arrays.asList("$testvalue")))
							.append("Min", new Document("$min", Arrays.asList("$testvalue")))
							.append("Q1",
									new Document("$arrayElemAt",
											Arrays.asList("$testvalue", new Document("$floor",
													new Document("$divide",
															Arrays.asList(new Document("$size", "$testvalue"), 4))))))
							.append("median",
									new Document("$arrayElemAt",
											Arrays.asList("$testvalue", new Document("$floor",
													new Document("$divide",
															Arrays.asList(new Document("$size", "$testvalue"), 2))))))
							.append("Q3", new Document("$arrayElemAt", Arrays.asList("$testvalue", new Document("$floor",
									new Document(AdvancedGraphConstants.MULTIPLY_AGG, Arrays.asList(new Document("$size", "$testvalue"), 0.75))))))
							.append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
							.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)

					), projectExcludeId -> new Document(AdvancedGraphConstants.PROJECT_AGG, new Document("_id", 0))
					).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
			return aggregationval;
		}
	        
	    public Aggregation getAutoCompTestMinMaxAgg(String testName, int testNumber, List<Criteria> criteriaList) {
			Aggregation aggregationMean;
			MatchOperation matchCriteria = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
			   LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfo = Aggregation.unwind( AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				ProjectionOperation projectFetchData = Aggregation.project().and("$mesWafer._id").as("waferId")
						   .andExclude("_id");
			   Aggregation aggregationResults = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					   matchCriteria, projectFetchData);
				List<Document> wafers = mongoTemplate.aggregate(aggregationResults, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
				ArrayList<String> wafersnew = new ArrayList<>();
				
				
				for(Document doc:wafers) {
					wafersnew.add(doc.get("waferId").toString());
				}
				ObjectId[] waferIdsnew = new ObjectId[wafersnew.size()];
				for (int i = 0; i < wafersnew.size(); i++) {
					waferIdsnew[i] = new ObjectId(wafersnew.get(i));
				}
				
				aggregationMean = Aggregation.newAggregation(
				        unwind-> new Document(AdvancedGraphConstants.UNWIND,"$TestBoundSummary"),
				        match-> new Document(AdvancedGraphConstants.MATCH_AGG,
				                new Document("$and",Arrays.asList(
						                new Document(AdvancedGraphConstants.WAFER_ID,new Document("$in",Arrays.asList(waferIdsnew))), 
						                new Document("TestBoundSummary.tn", testNumber), 
						                new Document("TestBoundSummary.tm", testName)
						               	    
						               	    )
				        )),
				        grouping-> new Document(AdvancedGraphConstants.GROUP_AGG, 
							    new Document("_id", 
							    new Document("min", "$TestBoundSummary.min")
							                .append("max", "$TestBoundSummary.max"))
							            .append("minVal", 
							    new Document("$min", "$TestBoundSummary.min"))
							            .append("maxVal",
							    new Document("$max","$TestBoundSummary.max")))
				        
				        
				        
					 ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
			return aggregationMean;
		}

	    public List<Criteria> getWafersFromQueryCriteria(List<Criteria> criteriaListforwafers1,
				LookupOperation lookupProductTestPass, UnwindOperation unwindProductTestPass,
				MatchOperation matchCriteriaTestPass, ProjectionOperation projectFetchWaferTestPass) {
			Aggregation aggregationResults = Aggregation.newAggregation(lookupProductTestPass, unwindProductTestPass, 
					matchCriteriaTestPass, projectFetchWaferTestPass);
			List<Document> wafers = mongoTemplate.aggregate(aggregationResults,AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
					.getMappedResults();
			ArrayList<String> wafersnew = new ArrayList<String>();
			
			
			for(Document doc:wafers) {
				wafersnew.add(doc.get("waferId").toString());
			}
			ObjectId[] waferIdsnew = new ObjectId[wafersnew.size()];
			for (int i = 0; i < wafersnew.size(); i++) {
				waferIdsnew[i] = new ObjectId(wafersnew.get(i));
			}
			criteriaListforwafers1.add(Criteria.where("wd").in(waferIdsnew));
			return criteriaListforwafers1;
		}

	   @Override
	   public Aggregation getAutoGraphBoxPlotTestValue2X(String dimension, String dimension2x,
			 List<Criteria> criteriaList, String dimensionName, String dimensionName2X,
			 Document projectFilteringMinMax) {
			Aggregation aggregationval;
			MatchOperation match = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
			aggregationval = Aggregation.newAggregation(
					lookup -> new Document(AdvancedGraphConstants.LOOK_UP,
							new Document("from",AdvancedGraphConstants.TEST_HISTORY_KEY).append(AdvancedGraphConstants.LOCAL_FIELD, AdvancedGraphConstants.WAFER_ID)
									.append(AdvancedGraphConstants.FOREIGN_FIELD, "mesWafer._id").append("as", AdvancedGraphConstants.TEST_HISTORY_KEY)),

					unwindTesthistory -> new Document(AdvancedGraphConstants.UNWIND,
							new Document("path", AdvancedGraphConstants.DOLLAR_TEST_HISTORY).append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)),
					lookup -> new Document(AdvancedGraphConstants.LOOK_UP,
							new Document("from", AdvancedGraphConstants.PRODUCT_INFO).append(AdvancedGraphConstants.LOCAL_FIELD, "TestHistory.mesWafer.productId")
									.append(AdvancedGraphConstants.FOREIGN_FIELD, "_id").append("as", AdvancedGraphConstants.PRODUCT_INFO)),

					unwindTesthistory -> new Document(AdvancedGraphConstants.UNWIND,
							new Document("path", AdvancedGraphConstants.DOLLAR_PRODUCT_INFO).append(AdvancedGraphConstants.PRESERVE_NULL_EMPTY_ARRAYS, true)),
					match,
					addFields -> new Document(AdvancedGraphConstants.ADD_FIELDS, new Document("GroupingField", "$TestHistory." + dimension)
						   .append("GroupingField2X",  "$TestHistory." + dimension2x)),

					groupfield -> new Document(AdvancedGraphConstants.GROUP_AGG,
						   new Document("_id",
								 new Document(dimensionName, "$GroupingField").append(dimensionName2X,
									    "$GroupingField2X")).append(AdvancedGraphConstants.VALUE_IN, new Document("$push", "$mv"))
							.append(AdvancedGraphConstants.WAFER_START_TIME, new Document("$min", "$TestHistory.mesWafer.WaferStartTime"))
							.append(AdvancedGraphConstants.WAFER_END_TIME, new Document("$min", "$TestHistory.mesWafer.WaferEndTime"))
							),
					projectFieldDimension -> new Document(AdvancedGraphConstants.ADD_FIELDS,
						   new Document("xAxis", new Document("$concatArrays",
								 Arrays.asList(Arrays.asList("$_id." + dimensionName, "$_id." + dimensionName2X))))),
					
					projectField -> new Document(AdvancedGraphConstants.PROJECT_AGG,
							new Document("testvalue", new Document("$reduce",
									new Document("input", "$value").append("initialValue", new ArrayList<>()).append("in",
											new Document("$concatArrays", Arrays.asList("$$value", "$$this")))))
													.append(AdvancedGraphConstants.DIMENSION_KEY, "$xAxis").append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
													.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)),

					projectFiel -> projectFilteringMinMax,
					matchZero -> new Document(AdvancedGraphConstants.MATCH_AGG, new Document("testvalue", new Document("$gt", 0))),
					soreField -> new Document(AdvancedGraphConstants.PROJECT_AGG,
							new Document("testvalue", new Document("$function",
									new Document("body", "function(mv){return mv.sort(function(a, b){return a - b})}")
											.append("args", Arrays.asList("$testvalue")).append("lang", "js")))
													.append(AdvancedGraphConstants.DIMENSION_KEY, AdvancedGraphConstants.DOLLAR_DIMENSION).append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
													.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)),
					boxPlotCal -> new Document(AdvancedGraphConstants.PROJECT_AGG, new Document(AdvancedGraphConstants.DIMENSION_KEY, AdvancedGraphConstants.DOLLAR_DIMENSION)
							.append("Max", new Document("$max", Arrays.asList("$testvalue")))
							.append("Min", new Document("$min", Arrays.asList("$testvalue")))
							.append("Q1",
									new Document("$arrayElemAt",
											Arrays.asList("$testvalue", new Document("$floor",
													new Document("$divide",
															Arrays.asList(new Document("$size", "$testvalue"), 4))))))
							.append("median",
									new Document("$arrayElemAt",
											Arrays.asList("$testvalue", new Document("$floor",
													new Document("$divide",
															Arrays.asList(new Document("$size", "$testvalue"), 2))))))
							.append("Q3", new Document("$arrayElemAt", Arrays.asList("$testvalue", new Document("$floor",
									new Document(AdvancedGraphConstants.MULTIPLY_AGG, Arrays.asList(new Document("$size", "$testvalue"), 0.75))))))
							.append(AdvancedGraphConstants.WAFER_START_TIME, AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
							.append(AdvancedGraphConstants.WAFER_END_TIME, AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)

					), projectExcludeId -> new Document(AdvancedGraphConstants.PROJECT_AGG, new Document("_id", 0))
					).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
			return aggregationval;
		}
	    
}
