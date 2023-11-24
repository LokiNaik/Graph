package com.graph.service.wt;

import java.io.IOException;
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
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.GraphMessage;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.GraphRequest;
import com.graph.model.wt.LockParam;
import com.graph.model.wt.Message;
import com.graph.model.wt.RecentGraphSaveModel;
import com.graph.model.wt.RegularGraphModel;
import com.graph.model.wt.SaveAutoGraph;
import com.graph.model.wt.SavedAnnotation;
import com.graph.model.wt.SavedAnnotationModel;
import com.graph.model.wt.SortParam;
import com.graph.model.wt.WaferIdModel;
import com.graph.model.wt.XAxisModel;
import com.graph.model.wt.YAxisModel;
import com.graph.model.wt.ZAxisModel;
import com.graph.repository.wt.AutoGraphRepository;
import com.graph.repository.wt.RecentGraphRepository;
import com.graph.repository.wt.RegularGraphRepository;
import com.graph.repository.wt.SavedAnnotationRepository;
import com.graph.repository.wt.WaferIdRepository;
import com.graph.utils.DateUtil;
import com.graph.utils.ValidationUtil;
import com.graph.utils.CriteriaUtil;


	
	@Service
	public class CommonGraphService implements ICommonGraphService {

		
		@Autowired
	    @Qualifier(value = "primaryMongoTemplate")
	    private MongoTemplate mongoTemplate;
		 @Autowired
		    private RegularGraphRepository regularGraphRepository;
		    @Autowired
		    private RecentGraphRepository recentGraphRepository;
		    @Autowired
		    private WaferIdRepository waferIdRepository;
		    @Autowired
		    private AutoGraphRepository autoGraphRepository;
		    @Autowired
		    private SavedAnnotationRepository savedAnnotationRepository;
		    @Value("${currentlocale}")
		    private String currentLocale;		   	 
	   
	    @Value("${settings.path}")
	    private String filePath;
	    @Value("${settings.folder}")
	    private String settingsFolder;
	    @Value("${maximumLimitBoxPlot}")
	    private String maximumLimitBox;

	    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	 /*
     * List Regular Graph
     */
    @Override
    public DataModel getRegularGraphs(GraphModelJSON search) throws IOException {
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
		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams,CollectionName));
	   }
	   Criteria commonCriteria = null;

	   commonCriteria = Criteria.where("deleteFlag").ne(true).and("category").is("Regular");

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
		  Sort sort = Sort.by(output + "graphName").ascending();
		  FacetOperation unwindSortFacet = Aggregation
				.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
		  facets.add(unwindSortFacet);
		  output += "output.";
	   }
	   FacetOperation unWindAndProjectFieldsOperation = Aggregation.facet(Aggregation.unwind("$output", false),
			 new SkipOperation(pageable.getPageNumber() * pageable.getPageSize()),
			 Aggregation.limit(pageable.getPageSize()),
			 Aggregation.project().and("$" + output + "_id").as("_id").and("$" + output + "lastUpdatedDate")
				    .as("accessTime").and("$" + output + "graphType").as("graphType")
				    .and("$" + output + "graphName").as("graphName").and("$" + output + "accessType")
				    .as("accessType").and("$" + output + "xAxis.axisParameter").as("xAxis.axisParameter")
				    .and("$" + output + "yAxis.axisParameter").as("yAxis.axisParameter")
				    .and("$" + output + "zAxis.axisParameter").as("zAxis.axisParameter")
				    .and("$" + output + "category").as("category").and("$" + output + "grouping").as("grouping"))
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
	   List<Document> result = mongoTemplate.aggregate(aggregation, "GraphTemplates", Document.class)
			 .getMappedResults();
	   List<Document> count = mongoTemplate.aggregate(countAggregation, "GraphTemplates", Document.class)
			 .getMappedResults();
	   result = ValidationUtil.documentIdConverter((List<Document>) result.get(0).get("output"));
	   result = ValidationUtil.axisConverter(result, "xAxis");
	   result = ValidationUtil.axisConverter(result, "yAxis");
	   result = ValidationUtil.axisConverter(result, "zAxis");
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
     * List SavedGraph
     */
    @Override
    public DataModel getSavedGraphs(GraphModelJSON search) throws IOException {

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

	   commonCriteria = Criteria.where("deleteFlag").ne(true).and("category").is("Saved");
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
		  Sort sort = Sort.by(output + "lastUpdatedDate").descending();
		  FacetOperation unwindSortFacet = Aggregation
				.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
		  facets.add(unwindSortFacet);
		  output += "output.";
	   }
	   FacetOperation unWindAndProjectFieldsOperation = Aggregation.facet(Aggregation.unwind("$output", false),
			 new SkipOperation(pageable.getPageNumber() * pageable.getPageSize()),
			 Aggregation.limit(pageable.getPageSize()),
			 Aggregation.project().and("$" + output + "_id").as("_id").and("$" + output + "lastUpdatedDate")
				    .as("lastUpdatedDate").and("$" + output + "graphType").as("graphType")
				    .and("$" + output + "graphName").as("graphName").and("$" + output + "accessType")
				    .as("accessType").and("$" + output + "xAxis.axisParameter").as("xAxis.axisParameter")
				    .and("$" + output + "yAxis.axisParameter").as("yAxis.axisParameter")
				    .and("$" + output + "zAxis.axisParameter").as("zAxis.axisParameter")
				    .and("$" + output + "grouping").as("grouping"))
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

	   List<Document> result = mongoTemplate.aggregate(aggregation, "GraphTemplates", Document.class)
			 .getMappedResults();
	   List<Document> count = mongoTemplate.aggregate(countAggregation, "GraphTemplates", Document.class)
			 .getMappedResults();
	   result = ValidationUtil.documentIdConverter((List<Document>) result.get(0).get("output"));
	   result = ValidationUtil.axisConverter(result, "xAxis");
	   result = ValidationUtil.axisConverter(result, "yAxis");
	   result = ValidationUtil.axisConverter(result, "zAxis");
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
     * List RecentGraph
     */
    @Override
    public DataModel getRecentGraphs(GraphModelJSON search) throws IOException {
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
		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams,CollectionName));
	   }
	   Criteria commonCriteria = null;
	   commonCriteria = Criteria.where("deleteFlag").ne(true);
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
		  Sort sort = Sort.by(output + "accessTime").descending();
		  FacetOperation unwindSortFacet = Aggregation
				.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
		  facets.add(unwindSortFacet);
		  output += "output.";
	   }
	   FacetOperation unWindAndProjectFieldsOperation = Aggregation.facet(Aggregation.unwind("$output", false),
			 new SkipOperation(pageable.getPageNumber() * pageable.getPageSize()),
			 Aggregation.limit(pageable.getPageSize()),
			 Aggregation.project().and("$" + output + "_id").as("_id").and("$" + output + "accessTime")
				    .as("accessTime").and("$" + output + "graphType").as("graphType")
				    .and("$" + output + "graphName").as("graphName").and("$" + output + "accessType")
				    .as("accessType").and("$" + output + "xAxis").as("xAxis").and("$" + output + "category")
				    .as("category").and("$" + output + "yAxis").as("yAxis").and("$" + output + "zAxis").as("zAxis")
				    .and("$" + output + "grouping").as("grouping"))
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
	   List<Document> result = mongoTemplate.aggregate(aggregation, "RecentGraphs", Document.class).getMappedResults();
	   List<Document> count = mongoTemplate.aggregate(countAggregation, "RecentGraphs", Document.class)
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
     * View RecentGraph
     */

    @Override
    public DataListModel specificRecentGraphs(List<String> regularGraphIds) {
	   MatchOperation matchGraphId = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
	   ProjectionOperation projectGraphId = Aggregation.project().and("$graphId").as("graphId");
	   Aggregation aggregationId = Aggregation.newAggregation(matchGraphId, projectGraphId);
	   List<Document> result = mongoTemplate.aggregate(aggregationId, "RecentGraphs", Document.class)
			 .getMappedResults();
	   MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("_id").in(result.get(0).get("graphId")));
	   MatchOperation deleteFlag = Aggregation.match(Criteria.where("deleteFlag").is(false));

	   ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
			 .and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
			 .and("$graphName").as("graphName").and("$graphType").as("graphType").and("$legendList").as("legendList")
			 .and("$category").as("category").and("$accessType").as("accessType").and("$grouping").as("grouping")
			 .and("$xAxis").as("xAxis").and("$yAxis").as("yAxis").and("$zAxis").as("zAxis")
			 .and("$isHistogramValEnabled").as("isHistogramValEnabled").and("$graphBarWidth").as("graphBarWidth");
	   Aggregation aggregation = Aggregation.newAggregation(matchRegularGraphId, deleteFlag,
			 projectFieldSelectionOperation, Aggregation.sort(Sort.Direction.ASC, "graphName"));
	   List<GraphRequest> results = mongoTemplate.aggregate(aggregation, "GraphTemplates", GraphRequest.class)
			 .getMappedResults();
	   for (GraphRequest obj : results) {
		  ObjectId objectId = new ObjectId(obj.get_id());
		  obj.set_id(objectId.toHexString());
	   }
	   for (GraphRequest graphRequest : results) {
		  ObjectId obj = new ObjectId(graphRequest.get_id().toString());
		  MatchOperation matchId = Aggregation.match(Criteria.where("graphId").in(obj));
		  projectGraphId = Aggregation.project().and("$waferIds").as("waferIds");
		  aggregationId = Aggregation.newAggregation(matchId, projectGraphId);
		  List<WaferIdModel> waferIdList = mongoTemplate
				.aggregate(aggregationId, "SavedGraphWaferIds", WaferIdModel.class).getMappedResults();
		  List<String> waferList = new ArrayList<String>();
		  if (waferIdList.size() > 0) {
			 for (WaferIdModel waferModel : waferIdList) {
				List<String> objId = waferModel.getWaferIds();
				for (String hexaObj : objId) {
				    ObjectId ids = new ObjectId(hexaObj);
				    waferList.add(ids.toHexString());
				}
			 }
			 graphRequest.setWaferIds(waferList);
		  }
		  graphRequest.setSavedAnnotation(getSavedAnnotations(obj));
	   }

	   DataListModel testListModel = new DataListModel();
	   Message message = new Message();
	   if (results.size() > 0) {
		  saveGraphDetails(results.get(0));
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
     * View SavedGraph
     */
    @Override
    public DataListModel specificSavedGraphs(List<String> regularGraphIds) {
	   MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
	   ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
			 .and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
			 .and("$graphName").as("graphName").and("$graphType").as("graphType").and("$legendList").as("legendList")
			 .and("$category").as("category").and("$accessType").as("accessType").and("$grouping").as("grouping")
			 .and("$xAxis").as("xAxis").and("$yAxis").as("yAxis").and("$zAxis").as("zAxis")
			 .and("$isHistogramValEnabled").as("isHistogramValEnabled").and("$graphBarWidth").as("graphBarWidth");
	   Aggregation aggregation = Aggregation.newAggregation(matchRegularGraphId, projectFieldSelectionOperation,
			 Aggregation.sort(Sort.Direction.ASC, "graphName"));
	   List<GraphRequest> results = mongoTemplate.aggregate(aggregation, "GraphTemplates", GraphRequest.class)
			 .getMappedResults();
	   ObjectId id = new ObjectId(results.get(0).get_id());
	   results.get(0).setGraphId(id);
	   saveRecentGraph(results.get(0), "Saved");
	   for (GraphRequest obj : results) {
		  ObjectId objectId = new ObjectId(obj.get_id());
		  obj.set_id(objectId.toHexString());
	   }
	   for (GraphRequest graphRequest : results) {
		  ObjectId obj = new ObjectId(graphRequest.get_id().toString());
		  MatchOperation matchId = Aggregation.match(Criteria.where("graphId").in(obj));
		  ProjectionOperation projectGraphId = Aggregation.project().and("$waferIds").as("waferIds");
		  Aggregation aggregationId = Aggregation.newAggregation(matchId, projectGraphId);
		  List<WaferIdModel> waferIdList = mongoTemplate
				.aggregate(aggregationId, "SavedGraphWaferIds", WaferIdModel.class).getMappedResults();
		  List<String> waferList = new ArrayList<String>();
		  if (waferIdList.size() > 0) {
			 for (WaferIdModel waferModel : waferIdList) {
				List<String> objId = waferModel.getWaferIds();
				for (String hexaObj : objId) {
				    ObjectId ids = new ObjectId(hexaObj);
				    waferList.add(ids.toHexString());
				}
			 }
			 graphRequest.setWaferIds(waferList);
		  }
		  graphRequest.setSavedAnnotation(getSavedAnnotations(obj));
	   }
	   DataListModel testListModel = new DataListModel();
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

    /*
     * View RegularGraph
     */
    @Override
    public DataListModel specificRegularGraphs(List<String> regularGraphIds) {
	   MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
	   ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
			 .and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
			 .and("$graphName").as("graphName").and("$category").as("category").and("$legendList").as("legendList")
			 .and("$graphType").as("graphType").and("$accessType").as("accessType").and("$grouping").as("grouping")
			 .and("$xAxis").as("xAxis").and("$yAxis").as("yAxis").and("$zAxis").as("zAxis")
			 .and("$isHistogramValEnabled").as("isHistogramValEnabled").and("$graphBarWidth").as("graphBarWidth");
	   Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectFieldSelectionOperation,
			 Aggregation.sort(Sort.Direction.ASC, "graphName"));
	   List<GraphRequest> results = mongoTemplate.aggregate(aggregation, "GraphTemplates", GraphRequest.class)
			 .getMappedResults();

	   for (GraphRequest obj : results) {
		  ObjectId objectId = new ObjectId(obj.get_id());
		  obj.set_id(objectId.toHexString());
	   }
	   ObjectId id = new ObjectId(results.get(0).get_id());
	   results.get(0).setGraphId(id);
	   saveRecentGraph(results.get(0), "Regular");
	   DataListModel testListModel = new DataListModel();
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

    /*
     * 
     */
    private void saveRecentGraph(GraphRequest graphRequest, String category) {
	   RecentGraphSaveModel recentGraph = new RecentGraphSaveModel();
	   
	   if (graphRequest.getGraphName() == null) {
		  recentGraph.setGraphName("-");
	   }
	   else {
		  recentGraph.setGraphName(graphRequest.getGraphName());

	   }
	   if (graphRequest.getAccessType() == null) {
		  recentGraph.setAccessType("-");
	   }
	   else {
		  recentGraph.setAccessType(graphRequest.getAccessType());

	   }
	   if (graphRequest.getCategory() == null) {
		  recentGraph.setCategory("-");
	   }
	   else {
		  recentGraph.setCategory(category);

	   }
	   recentGraph.setLegendList(graphRequest.getLegendList());
	   recentGraph.setGraphId(graphRequest.getGraphId());
	   recentGraph.setApplicationName(graphRequest.getApplicationName());
	   recentGraph.setAccessTime(DateUtil.getCurrentDateInUTC());
	   recentGraph.setGraphType(graphRequest.getGraphType());
	   if (graphRequest.getGrouping().size() > 0) {
		  recentGraph.setGrouping(graphRequest.getGrouping().get(0).getAxisParameter());
	   }
	   else {
		  recentGraph.setGrouping("");
	   }
	   List<String> xAxisList = new ArrayList<>();
	   for (XAxisModel xAxis : graphRequest.getxAxis()) {
		  xAxisList.add(xAxis.getAxisParameter());
	   }
	   recentGraph.setxAxis(xAxisList);
	   List<String> yAxisList = new ArrayList<>();
	   for (YAxisModel yAxis : graphRequest.getyAxis()) {
		  yAxisList.add(yAxis.getAxisParameter());
	   }
	   recentGraph.setyAxis(yAxisList);
	   List<String> zAxisList = new ArrayList<>();
	   for (ZAxisModel zAxis : graphRequest.getzAxis()) {
		  zAxisList.add(zAxis.getAxisParameter());
	   }
	   recentGraph.setzAxis(zAxisList);
	   recentGraph = recentGraphRepository.insert((RecentGraphSaveModel) recentGraph);
    }
    
    /*
     * Rename
     */
    @Override
    public Message renameGraph(String id, String graphName) {
	   RecentGraphSaveModel recentGraphModels = null;
	   SaveAutoGraph autoGraphModels = null;
	   Message message = new Message();
	   List<Document> recentGraphId = null;
	   List<Document> autoGraphId = checkAutoGraphExists(id, "AutoGraphs");
	   if (autoGraphId.size() == 0) {
		  recentGraphId = checkRecentGraphExists(id, "RecentGraphs");
	   }
	   if (autoGraphId.size() > 0) {

		  Optional<SaveAutoGraph> optional = autoGraphRepository.findById(id);
		  autoGraphModels = optional.get();
		  MatchOperation matchGraphName = Aggregation.match(Criteria.where("autographName").in(graphName));
		  MatchOperation matchAccessType = Aggregation
				.match(Criteria.where("accessType").in(autoGraphModels.getAccessType()));
		  ProjectionOperation projectGraphId = Aggregation.project().and("$autographName").as("autographName");
		  Aggregation aggregationId = Aggregation.newAggregation(matchGraphName, matchAccessType, projectGraphId);
		  List<Document> recentList = mongoTemplate.aggregate(aggregationId, "AutoGraphs", Document.class)
				.getMappedResults();
		  if (recentList.size() == 0) {
			 autoGraphModels.setAutographName(graphName);
			 autoGraphRepository.save((SaveAutoGraph) autoGraphModels);
			 message.setCode(202);
			 message.setSuccess(true);
		  }
		  else {
			 message.setCode(302);
			 message.setSuccess(false);
		  }
		  renameAuto(autoGraphId.get(0).get("_id").toString(), graphName, message, autoGraphId);
	   }
	   else if (recentGraphId.size() > 0) {
		  rename(recentGraphId.get(0).get("graphId").toString(), graphName, message, recentGraphId);
		  Optional<RecentGraphSaveModel> optional = recentGraphRepository.findById(id);
		  recentGraphModels = optional.get();
		  MatchOperation matchGraphName = Aggregation.match(Criteria.where("graphName").in(graphName));
		  MatchOperation matchAccessType = Aggregation
				.match(Criteria.where("accessType").in(recentGraphModels.getAccessType()));
		  ProjectionOperation projectGraphId = Aggregation.project().and("$graphName").as("graphName");
		  Aggregation aggregationId = Aggregation.newAggregation(matchGraphName, matchAccessType, projectGraphId);
		  List<Document> recentList = mongoTemplate.aggregate(aggregationId, "RecentGraphs", Document.class)
				.getMappedResults();
		  if (recentList.size() == 0) {
			 recentGraphModels.setGraphName(graphName);
			 recentGraphRepository.save((RecentGraphSaveModel) recentGraphModels);
			 message.setCode(202);
			 message.setSuccess(true);
		  }
		  else {
			 message.setCode(302);
			 message.setSuccess(false);
		  }
	   }
	   else {
		  rename(id, graphName, message, recentGraphId);
		  recentGraphId = checkRecentGraphExists(id, "GraphTemplates");
		  if (recentGraphId.size() > 0) {
			 for (Document document : recentGraphId) {
				Optional<RecentGraphSaveModel> optional = recentGraphRepository
					   .findById(document.get("_id").toString());
				recentGraphModels = optional.get();
				recentGraphModels.setGraphName(graphName);
				recentGraphRepository.save((RecentGraphSaveModel) recentGraphModels);
			 }
		  }
	   }
	   return message;
    }
    /*
     * deleteGraph
     */
    @Override
    public GraphMessage deleteGraph(String id) {
	   RegularGraphModel regularGraphModels = null;
	   GraphMessage message = new GraphMessage();
	   List<Document> recentGraphId = null;
	   List<Document> autoGraphId = null;
	   autoGraphId = checkAutoGraphExists(id, "AutoGraphs");

	   if (autoGraphId.size() == 0) {
		  recentGraphId = checkRecentGraphExists(id, "RecentGraphs");
	   }
	   if (autoGraphId.size() > 0) {
		  SaveAutoGraph autoGraphModel = null;
		  Optional<SaveAutoGraph> autoOptional = autoGraphRepository.findById(id);
		  autoGraphModel = autoOptional.get();
		  autoGraphModel.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
		  autoGraphModel.setDeleteFlag(true);
		  autoGraphRepository.save((SaveAutoGraph) autoGraphModel);
		  message.setStatus("AutoGraph deleted Successfully");
	   }
	   else if (recentGraphId.size() > 0) {
		  RecentGraphSaveModel recentGraphModel = null;
		  Optional<RecentGraphSaveModel> recentOptional = recentGraphRepository.findById(id);
		  recentGraphModel = recentOptional.get();
		  recentGraphModel.setAccessTime(DateUtil.getCurrentDateInUTC());
		  recentGraphModel.setDeleteFlag(true);
		  recentGraphRepository.save((RecentGraphSaveModel) recentGraphModel);
		  message.setStatus("Recent Graph deleted Successfully");
	   }
	   else {
		  recentGraphId = checkRecentGraphExists(id, "GraphTemplates");
		  Optional<RegularGraphModel> optionalObj = regularGraphRepository.findById(id);
		  regularGraphModels = optionalObj.get();
		  regularGraphModels.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
		  regularGraphModels.setUpdatedBy("User1");
		  regularGraphModels.setDeleteFlag(true);
		  regularGraphRepository.save((RegularGraphModel) regularGraphModels);
		  message.setStatus("Regular Graph deleted Successfully");
	   }
	   return message;
    }
    /*
     * Method for Saving GraphDetails
     */
    @Override
    public DataListModel saveGraphDetails(GraphRequest regularGraphInfo) {
    	DataListModel datalist = new DataListModel();
	   Message message = new Message();
	   RegularGraphModel regularGraphModel = null;
	   List<String> regularGraphModelDetails = new ArrayList<String>();
	   
	  
	   try {
		  if (regularGraphInfo.get_id() == null) {
			 if (regularGraphInfo.getGraphName() != null) {
				regularGraphModel = regularGraphRepository.findByGraphDetails(regularGraphInfo.getGraphName(),
					   regularGraphInfo.getAccessType(), regularGraphInfo.getCategory());
			 }
			 if (regularGraphModel == null) {
				RegularGraphModel regularGraphModelSave = new RegularGraphModel();
				regularGraphModelSave.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
				setParamToModel(regularGraphInfo, regularGraphModelSave);
				regularGraphModelSave = regularGraphRepository.insert((RegularGraphModel) regularGraphModelSave);
				ObjectId id = new ObjectId(regularGraphModelSave.get_id());
				regularGraphInfo.setGraphId(id);
				regularGraphModelDetails.add(regularGraphModelSave.get_id());
				datalist.setDataList(regularGraphModelDetails);
				if (regularGraphInfo.getCategory() != null
					   && regularGraphInfo.getCategory().equalsIgnoreCase("Saved")) {
				    if (regularGraphInfo.getIsAnnotation()) {
					   saveAnnotations(regularGraphInfo);
				    }
				    setSaveWaferIds(regularGraphInfo, regularGraphModelSave);
				}
				saveRecentGraph(regularGraphInfo, regularGraphInfo.getCategory());
				message.setCode(200);
				message.setSuccess(true);
			 }
			 else {
				message.setCode(302);
				message.setSuccess(false);
			 }
		  }
		  else if (regularGraphInfo.get_id() != null) { 
			 RegularGraphModel regularGraphDetails = null;
			 Optional<RegularGraphModel> optionalObj = regularGraphRepository
				    .findById(regularGraphInfo.get_id().toString());
			 regularGraphModel = optionalObj.get();
			 
			 if(regularGraphModel.getGraphType()!=null && regularGraphInfo.getGraphType()!=null
					 &&!regularGraphInfo.getGraphType().equals(regularGraphModel.getGraphType())) {
				 regularGraphInfo.setIsAnnotation(true);
			 }
			 
			 if(regularGraphModel.getGraphName().equals(regularGraphInfo.getGraphName()) 
					 && regularGraphModel.getAccessType().equals(regularGraphInfo.getAccessType())
					 && regularGraphModel.getCategory().equals(regularGraphInfo.getCategory()) ) {
				 
					setParamToModel(regularGraphInfo, regularGraphModel);
					regularGraphModel.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
					regularGraphModel = regularGraphRepository.save((RegularGraphModel) regularGraphModel);
					ObjectId id = new ObjectId(regularGraphModel.get_id());
    				regularGraphInfo.setGraphId(id);
    				regularGraphModelDetails.add(regularGraphModel.get_id());
    				datalist.setDataList(regularGraphModelDetails);
    				
    				if (regularGraphInfo.getCategory() != null
    						   && regularGraphInfo.getCategory().equalsIgnoreCase("Saved")) {
    					    if (regularGraphInfo.getIsAnnotation() != null && regularGraphInfo.getIsAnnotation()) {
    						   updateAnnotations(regularGraphInfo);
    					    }
    					    setUpdatedWaferIds(regularGraphInfo);
    					}
    					saveRecentGraph(regularGraphInfo, regularGraphInfo.getCategory());
    					message.setCode(202);
    					message.setSuccess(true);
			 }
			 else {
				 regularGraphDetails = regularGraphRepository.findByGraphDetails(
						   regularGraphInfo.getGraphName(),
						   regularGraphInfo.getAccessType(), regularGraphInfo.getCategory());
				 
				 if(regularGraphDetails == null) {
				 setParamToModel(regularGraphInfo, regularGraphModel);
					regularGraphModel.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
					regularGraphModel = regularGraphRepository.save((RegularGraphModel) regularGraphModel);
					ObjectId id = new ObjectId(regularGraphModel.get_id());
 				regularGraphInfo.setGraphId(id);
 				regularGraphModelDetails.add(regularGraphModel.get_id());
 				datalist.setDataList(regularGraphModelDetails);
 				
 				if (regularGraphInfo.getCategory() != null
 						   && regularGraphInfo.getCategory().equalsIgnoreCase("Saved")) {
 					    if (regularGraphInfo.getIsAnnotation() != null && regularGraphInfo.getIsAnnotation()) {
 						   updateAnnotations(regularGraphInfo);
 					    }
 					    setUpdatedWaferIds(regularGraphInfo);
 					}
 					saveRecentGraph(regularGraphInfo, regularGraphInfo.getCategory());
 					message.setCode(202);
 					message.setSuccess(true);
				 }
				 else {
					 message.setCode(302);
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
     * To get saved annotations
     */
    public SavedAnnotation getSavedAnnotations(ObjectId graphId) {
	   MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("graphId").in(graphId));
	   ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id").and("$graphId")
			 .as("graphId").and("$width").as("width").and("$height").as("height").and("$markers").as("markers");
	   Aggregation aggregation = Aggregation.newAggregation(matchRegularGraphId, projectFieldSelectionOperation);
	   List<SavedAnnotation> results = mongoTemplate
			 .aggregate(aggregation, "SavedGraphAnnotations", SavedAnnotation.class).getMappedResults();
	   if (results.size() > 0) {
		  return results.get(0);
	   }
	   else {
		  return null;
	   }
    }
    /*
     * checkRecentGraphExists
     */
    private List<Document> checkRecentGraphExists(String id, String category) {
	   MatchOperation matchGraphId = null;
	   if (category.equalsIgnoreCase("GraphTemplates")) {
		  ObjectId obj = new ObjectId(id);
		  matchGraphId = Aggregation.match(Criteria.where("graphId").in(obj));
	   }
	   else {
		  matchGraphId = Aggregation.match(Criteria.where("_id").in(id));
	   }
	   ProjectionOperation projectGraphId = Aggregation.project().and("$graphId").as("graphId");
	   Aggregation aggregationId = Aggregation.newAggregation(matchGraphId, projectGraphId);
	   List<Document> recentGraphId = mongoTemplate.aggregate(aggregationId, "RecentGraphs", Document.class)
			 .getMappedResults();
	   return recentGraphId;
    }

    /*
     * checkAutoGraphExists
     */
    private List<Document> checkAutoGraphExists(String id, String category) {
	   MatchOperation matchGraphId = null;
	   if (category.equalsIgnoreCase("AutoGraphs")) {
		  String obj = new String(id);
		  matchGraphId = Aggregation.match(Criteria.where("_id").in(obj));
	   }
	   ProjectionOperation projectGraphId = Aggregation.project().and("$_Id").as("graphId");
	   Aggregation aggregationId = Aggregation.newAggregation(matchGraphId, projectGraphId);
	   List<Document> autoGraphId = mongoTemplate.aggregate(aggregationId, "AutoGraphs", Document.class)
			 .getMappedResults();
	   return autoGraphId;
    }
    /*
     * rename AutoGraph
     */
    private void renameAuto(String id, String graphName, Message message, List<Document> recentGraphId) {
	   SaveAutoGraph autoGraphModels;
	   Optional<SaveAutoGraph> optionalObj = autoGraphRepository.findById(id);
	   autoGraphModels = optionalObj.get();
	   SaveAutoGraph autoGraphForName = autoGraphRepository.findByAutoGraph(id, graphName,
			 autoGraphModels.getAccessType(), autoGraphModels.getCategory());
	   if (autoGraphForName == null) {
		  autoGraphModels.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
		  autoGraphModels.setUpdatedBy("User1");
		  autoGraphModels.setAutographName(graphName);
		  autoGraphRepository.save((SaveAutoGraph) autoGraphModels);
		  message.setCode(202);
		  message.setSuccess(true);
	   }
	   else {
		  message.setCode(302);
		  message.setSuccess(false);
	   }
    }
    /*
     * rename Regular Graph
     */
    private void rename(String id, String graphName, Message message, List<Document> recentGraphId) {
	   RegularGraphModel regularGraphModels;
	   Optional<RegularGraphModel> optionalObj = regularGraphRepository.findById(id);
	   regularGraphModels = optionalObj.get();
	   RegularGraphModel regularGraphForName = regularGraphRepository.findByGraphAccessTypeCategory(id, graphName,
			 regularGraphModels.getAccessType(), regularGraphModels.getCategory());
	   if (regularGraphForName == null) {
		  regularGraphModels.setLastUpdatedDate(DateUtil.getCurrentDateInUTC());
		  regularGraphModels.setUpdatedBy("User1");
		  regularGraphModels.setGraphName(graphName);
		  regularGraphRepository.save((RegularGraphModel) regularGraphModels);
		  message.setCode(202);
		  message.setSuccess(true);
	   }
	   else {
		  message.setCode(302);
		  message.setSuccess(false);
	   }
    }
    /*
     * Binding Payload Save Data to Database collection
     */
    private void setParamToModel(GraphRequest regularGraphInfo, RegularGraphModel regularGraphModels) {
	   regularGraphModels.setCreatedBy("User1");
	   regularGraphModels.setDeleteFlag(false);
	   regularGraphModels.setUpdatedBy("User1");
	   if (regularGraphInfo.getGraphName() == null) {
		  regularGraphModels.setGraphName("-");
	   }
	   else {
		  regularGraphModels.setGraphName(regularGraphInfo.getGraphName());
	   }
	   if (regularGraphInfo.getAccessType() == null) {
		  regularGraphModels.setAccessType("-");
	   }
	   else {
		  regularGraphModels.setAccessType(regularGraphInfo.getAccessType());
	   }
	   if (regularGraphInfo.getCategory() == null) {
		  regularGraphModels.setCategory("-");
	   }
	   else {
		  regularGraphModels.setCategory(regularGraphInfo.getCategory());
	   }
	   if(regularGraphModels.getCategory().equals("Regular")) {
		   regularGraphModels.setIsHistogramValEnabled(regularGraphInfo.getIsHistogramValEnabled());
		   regularGraphModels.setGraphBarWidth(regularGraphInfo.getGraphBarWidth()); 
	   }else {
		   regularGraphModels.setIsHistogramValEnabled(true);
		   regularGraphModels.setGraphBarWidth(0.5); 
	   }
	  
	   regularGraphModels.setLegendList(regularGraphInfo.getLegendList());
	   regularGraphModels.setDeleteFlag(regularGraphInfo.isDeleteFlag());
	   regularGraphModels.setGraphType(regularGraphInfo.getGraphType());
	   regularGraphModels.setxAxis(regularGraphInfo.getxAxis());
	   regularGraphModels.setyAxis(regularGraphInfo.getyAxis());
	   regularGraphModels.setzAxis(regularGraphInfo.getzAxis());
	   regularGraphModels.setGrouping(regularGraphInfo.getGrouping());
    }
    /*
     * setSaveWaferIds
     */
    private void setSaveWaferIds(GraphRequest regularGraphInfo, RegularGraphModel regularGraphModels) {
	   WaferIdModel waferIdModel = new WaferIdModel();
	   ObjectId _id = new ObjectId(regularGraphModels.get_id());
	   waferIdModel.setGraphId(_id);
	   waferIdModel.setWaferIds(regularGraphInfo.getWaferIds());
	   waferIdModel = waferIdRepository.insert((WaferIdModel) waferIdModel);
    }
    /*
     * saveAnnotations
     */
    public void saveAnnotations(GraphRequest graphRequest) {
	   SavedAnnotationModel savedAnnotationModel = new SavedAnnotationModel();
	   savedAnnotationModel.setGraphId(graphRequest.getGraphId());
	   savedAnnotationModel.setWidth(graphRequest.getSavedAnnotation().getWidth());
	   savedAnnotationModel.setHeight(graphRequest.getSavedAnnotation().getHeight());
	   savedAnnotationModel.setMarkers(graphRequest.getSavedAnnotation().getMarkers());
	   savedAnnotationRepository.insert(savedAnnotationModel);
    }
    /*
     * setUpdatedWaferIds
     */
    private void setUpdatedWaferIds(GraphRequest regularGraphInfo) {
	   List<WaferIdModel> waferIdModels = null;
	   WaferIdModel waferIdModel = new WaferIdModel();
	   ObjectId objectId = new ObjectId(regularGraphInfo.get_id());
	   MatchOperation id = Aggregation.match(Criteria.where("graphId").in(objectId));
	   ProjectionOperation projectGraph = Aggregation.project().and("$_id").as("id").and("$waferIds").as("waferIds");
	   Aggregation aggregation = Aggregation.newAggregation(id, projectGraph);
	   waferIdModels = mongoTemplate.aggregate(aggregation, "SavedGraphWaferIds", WaferIdModel.class)
			 .getMappedResults();
	   if (waferIdModels.size() > 0) {
		  Optional<WaferIdModel> optionalObj = waferIdRepository.findById(waferIdModels.get(0).get_id().toString());
		  waferIdModel = optionalObj.get();
		  waferIdModel.setWaferIds(regularGraphInfo.getWaferIds());
		  waferIdRepository.save((WaferIdModel) waferIdModel);
	   }
	   else {
		  waferIdModel.setGraphId(objectId);
		  waferIdModel.setWaferIds(regularGraphInfo.getWaferIds());
		  waferIdModel = waferIdRepository.insert((WaferIdModel) waferIdModel);
	   }
    }

	/*
	 * updateAnnotations
	 */
	public void updateAnnotations(GraphRequest graphRequest) {
		MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("graphId").in(graphRequest.getGraphId()));
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id");
		Aggregation aggregation = Aggregation.newAggregation(matchRegularGraphId, projectFieldSelectionOperation);
		List<SavedAnnotation> results = mongoTemplate
				.aggregate(aggregation, "SavedGraphAnnotations", SavedAnnotation.class).getMappedResults();
		if (results.size() > 0) {
			Optional<SavedAnnotationModel> optionalObj = savedAnnotationRepository.findById(results.get(0).get_id());
			SavedAnnotationModel savedAnnotationModel = optionalObj.get();
			savedAnnotationModel.setWidth(graphRequest.getSavedAnnotation().getWidth());
			savedAnnotationModel.setHeight(graphRequest.getSavedAnnotation().getHeight());
			savedAnnotationModel.setMarkers(graphRequest.getSavedAnnotation().getMarkers());
			savedAnnotationRepository.save(savedAnnotationModel);
		} else {
			saveAnnotations(graphRequest);
		}
	}


}
