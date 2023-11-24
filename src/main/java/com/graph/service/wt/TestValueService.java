package com.graph.service.wt;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.BucketOperation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.ConcatArrays;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.HistoTestValueGrouing;
import com.graph.utils.CriteriaUtil;

@Service
public class TestValueService {


	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired(required = true)
	private IAutographService autoGraphService;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	/*
     * List Comparison Histogram One X TestValueFromCollection
     */
    public List<Document> gettingTestValueFromCollection(int testNumber, String testName, ObjectId[] waferIds,
		  double classValFinal, double maxValFinal, DecimalFormat df, double minVal, String DimensionName,
		  String group, String granuality, List<FilterParam> filterParams) throws IOException {

	   String CollectionName = null;
	   AddFieldsOperation addFieldsOperation = null;
	   Document projectFetchData = new Document();
	   logger.info("Inside function gettingTestValueFromCollection");
	   List<Criteria> criteriaList = new ArrayList<>();
	   if (waferIds.length == 0) {
		  if (filterParams != null && filterParams.size() >= 1) {
			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
		  }
	   }
	   else {
		  criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

	   }
	   MatchOperation matchOperation = Aggregation
			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

	   Aggregation aggregationMean = null;

	   StringBuilder s1 = new StringBuilder("");
	   for (double k = minVal; k <= maxValFinal; k = Double.valueOf(k + classValFinal)) {
		  String Cond = "cond((testValueDetails.mv > " + k + " && testValueDetails.mv <= " + (k + classValFinal)
				+ "), \"" + k + "-" + (k + classValFinal) + "\", \"\")";
		  if (s1 == null || s1.toString().equals("")) {
			 s1 = s1.append(Cond);
		  }
		  else {
			 s1 = s1.append("," + Cond);
		  }
	   }
	   String expString = "concat(" + s1 + ")";
	   String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
	   String groupingmeasure;
	   if (grouping == null) {
		  grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
	   }
	   final String groupingmeasures = grouping;
	   String Dimension = CriteriaUtil.generateFieldNameForCriteria(DimensionName);
	   if (Dimension == null) {
		  throw new IOException("Selected combination is not supported");
	   }
	   AggregationOperation projectionOperation = Aggregation.project().andExpression(expString).as("range")
			 .and(grouping).as("Group").and(Dimension).as("Dimension").and("$testValueDetails.mvCount").as("Count")
			 .and("$mesWafer.WaferStartTime").as("WaferStartTime").and("$mesWafer.WaferEndTime").as("WaferEndTime");
	   if (grouping.equals("mesWafer._id")) {
		  projectFetchData = new Document("$project",
				new Document("xAxis", Arrays.asList("$_id.Dimension")).append("yAxis", 1)
					   .append("WaferStartTime", Arrays.asList("$StartTime"))
					   .append("WaferEndTime", Arrays.asList("$EndTime")));

	   }
	   else {
		  projectFetchData = new Document("$project",
				new Document("xAxis", Arrays.asList("$_id.Dimension")).append("group", "$Group").append("yAxis", 1)
					   .append("WaferStartTime", Arrays.asList("$StartTime"))
					   .append("WaferEndTime", Arrays.asList("$EndTime")));
	   }
	   final Document projectFetchDatas = projectFetchData;
	   if (grouping.equals("mesWafer._id") || grouping.equals(Dimension)) {

		  if (granuality != null && granuality != "") {

			 String dateFormat = "";
			 switch (granuality) {
				case "Year":
				    dateFormat = "%Y";
				    break;
				case "Month":
				    dateFormat = "%Y/%m";
				    break;
				case "Day":
				    dateFormat = "%Y/%m/%d";
				    break;
				case "Hour":
				    dateFormat = "%Y/%m/%d %H:00:00";
				    break;
				case "Minute":
				    dateFormat = "%Y/%m/%d %H:%M:00";
				    break;
				case "Second":
				    dateFormat = "%Y/%m/%d %H:%M:%S";
				    break;
				default:
				    break;
			 }
			 Document addFieldsDateTime = new Document();
			 addFieldsDateTime = new Document("$addFields", new Document("DateField", new Document("$dateToString",
				    new Document("format", dateFormat).append("date", "$" + Dimension))));
			 final Document addFieldsOperationDateTimeMeasures = addFieldsDateTime;
			 /*
			  * AddFieldsOperation addFieldsOperationDateTimeMeasures =
			  * Aggregation.addFields() .addFieldWithValue("$DateField",
			  * DateOperators.dateOf("$" + Dimension).toString(dateFormat)) .build();
			  */
			 if (addFieldsOperationDateTimeMeasures != null) {
				AggregationOperation projectionOperationDate = Aggregation.project().andExpression(expString)
					   .as("range").and(grouping).as("Group").and("DateField").as("Dimension")
					   .and("$testValueDetails.mvCount").as("Count").and("$mesWafer.WaferStartTime")
					   .as("WaferStartTime").and("$mesWafer.WaferEndTime").as("WaferEndTime");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				addFieldsOperation = Aggregation.addFields().addFieldWithValue("rangeValues", "$range").build();
				aggregationMean = Aggregation.newAggregation(
					   lookUpTestValue -> new Document("$lookup",
							 new Document("from", "TestValueSummary")
								    .append("let", new Document("businessId", "$mesWafer._id"))
								    .append("pipeline", Arrays.asList(new Document("$match",
										  new Document("$expr", new Document("$and", Arrays.asList(
												new Document("$eq", Arrays.asList("$wd", "$$businessId")),
												new Document("$eq", Arrays.asList("$tn", testNumber)),
												new Document("$eq", Arrays.asList("$tm", testName))))))))
								    .append("as", "testValueDetails")),
					   unwindTestValue -> new Document("$unwind",
							 new Document("path", "$testValueDetails").append("preserveNullAndEmptyArrays",
								    true)),

					   matchOperation, addFieldsOperationDate -> addFieldsOperationDateTimeMeasures,
					   projectionOperationDate,
					   addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

					   projectFetching -> new Document("$project", new Document("Group", 1).append("Dimension", 1)
							 .append("range", 1).append("Count", 1)
							 .append("equalRange", new Document("$cond",
								    new Document("if",
										  new Document("$eq", Arrays.asList("$range", "$rangeValues")))
												.append("then", 1).append("else", 0)))
							 .append("WaferStartTime", "$WaferStartTime")
							 .append("WaferEndTime", "$WaferEndTime")),

					   addFieldsTotalOperations -> new Document("$addFields",
							 new Document("TotalCount",
								    new Document("$multiply", Arrays.asList("$equalRange", "$Count")))),

					   groupOperation -> new Document("$group",
							 new Document("_id",
								    new Document("Dimension", "$Dimension").append("range", "$range"))
										  .append("Count", new Document("$sum", "$TotalCount"))
										  .append("StartTime", new Document("$min", "$WaferStartTime"))
										  .append("EndTime", new Document("$max", "$WaferEndTime"))
										  .append("WaferId", new Document("$addToSet", "$Group"))),
					   groupOperationTest -> new Document("$group",
							 new Document("_id",
								    new Document("Dimension", "$_id.Dimension").append("Group", "$_id.Group"))
										  .append("yAxis",
												new Document("$addToSet",
													   new Document("range", "$_id.range").append("Count",
															 "$Count")
													   .append("WaferId", "$WaferId")))
										  .append("StartTime", new Document("$min", "$StartTime"))
										  .append("EndTime", new Document("$max", "$EndTime"))),
					   projectFetchingData -> projectFetchDatas,
					   projectExcludeIds -> new Document("$project", new Document("_id", 0))

				).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
			 }
		  }
		  else {
			 if (waferIds.length > 0) {
				if (DimensionName.equals("Device") || DimensionName.equals("deviceGroup")) {

				    aggregationMean = Aggregation.newAggregation(
						  productLookUp -> new Document("$lookup",
								new Document("from", "ProductInfo").append("localField", "mesWafer.productId")
									   .append("foreignField", "_id").append("as", "ProductInfo")),
						  unwindproduct -> new Document("$unwind",
								new Document("path", "$ProductInfo").append("preserveNullAndEmptyArrays",
									   true)),
						  lookUpTestValue -> new Document("$lookup", new Document("from", "TestValueSummary")
								.append("let", new Document("businessId", "$mesWafer._id"))
								.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr",
									   new Document("$and", Arrays.asList(
											 new Document("$eq", Arrays.asList("$wd", "$$businessId")),
											 new Document("$eq", Arrays.asList("$tn", testNumber)),
											 new Document("$eq", Arrays.asList("$tm", testName))))))))
								.append("as", "testValueDetails")),
						  unwindTestValue -> new Document("$unwind",
								new Document("path", "$testValueDetails").append("preserveNullAndEmptyArrays",
									   true)),
						  matchOperation, projectionOperation,
						  addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

						  projectFetching -> new Document("$project", new Document("Group", 1)
								.append("Dimension", 1).append("range", 1).append("Count", 1)
								.append("equalRange", new Document("$cond",
									   new Document("if",
											 new Document("$eq", Arrays.asList("$range", "$rangeValues")))
												    .append("then", 1).append("else", 0)))
								.append("WaferStartTime", "$WaferStartTime")
								.append("WaferEndTime", "$WaferEndTime")),

						  addFieldsTotalOperations -> new Document("$addFields",
								new Document("TotalCount",
									   new Document("$multiply", Arrays.asList("$equalRange", "$Count")))),

						  groupOperation -> new Document("$group",
								new Document("_id",
									   new Document("Dimension", "$Dimension").append("range", "$range"))
											 .append("Count", new Document("$sum", "$TotalCount"))
											 .append("StartTime", new Document("$min", "$WaferStartTime"))
											 .append("EndTime", new Document("$max", "$WaferEndTime"))
											 .append("WaferId", new Document("$addToSet", "$Group"))),
						  groupOperationTest -> new Document("$group",
								new Document("_id",
									   new Document("Dimension", "$_id.Dimension").append("Group",
											 "$_id.Group"))
												    .append("yAxis",
														  new Document("$addToSet",
																new Document("range", "$_id.range")
																	   .append("Count", "$Count")
																	   .append("WaferId", "$WaferId")))
												    .append("StartTime", new Document("$min", "$StartTime"))
												    .append("EndTime", new Document("$max", "$EndTime"))),
						  projectFetchingData -> projectFetchDatas,
						  projectExcludeIds -> new Document("$project", new Document("_id", 0))

				    ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
				}
				else {
				    aggregationMean = Aggregation
						  .newAggregation(
								lookUpTestValue -> new Document("$lookup",
									   new Document("from", "TestValueSummary")
											 .append("let", new Document("businessId", "$mesWafer._id"))
											 .append("pipeline", Arrays.asList(new Document("$match",
												    new Document("$expr",
														  new Document("$and", Arrays.asList(
																new Document("$eq",
																	   Arrays.asList("$wd",
																			 "$$businessId")),
																new Document("$eq",
																	   Arrays.asList("$tn",
																			 testNumber)),
																new Document("$eq",
																	   Arrays.asList("$tm",
																			 testName))))))))
											 .append("as", "testValueDetails")),
								unwindTestValue -> new Document("$unwind",
									   new Document("path", "$testValueDetails")
											 .append("preserveNullAndEmptyArrays", true)),
								matchOperation, projectionOperation,
								addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

								projectFetching -> new Document("$project",
									   new Document("Group", 1).append("Dimension", 1).append("range", 1)
											 .append("Count", 1)
											 .append("equalRange", new Document("$cond", new Document("if",
												    new Document("$eq",
														  Arrays.asList("$range", "$rangeValues")))
																.append("then", 1).append("else", 0)))
											 .append("WaferStartTime", "$WaferStartTime")
											 .append("WaferEndTime", "$WaferEndTime")),

								addFieldsTotalOperations -> new Document("$addFields",
									   new Document("TotalCount",
											 new Document("$multiply",
												    Arrays.asList("$equalRange", "$Count")))),

								groupOperation -> new Document("$group", new Document("_id",
									   new Document("Dimension", "$Dimension").append("range", "$range"))
											 .append("Count", new Document("$sum", "$TotalCount"))
											 .append("StartTime", new Document("$min", "$WaferStartTime"))
											 .append("EndTime", new Document("$max", "$WaferEndTime"))
											 .append("WaferId", new Document("$addToSet", "$Group"))),
								groupOperationTest -> new Document("$group",
									   new Document("_id",
											 new Document("Dimension", "$_id.Dimension").append("Group",
												    "$_id.Group")).append(
														  "yAxis",
														  new Document("$addToSet",
																new Document("range", "$_id.range")
																	   .append("Count", "$Count").append("WaferId", "$WaferId")))
														  .append("StartTime",
																new Document("$min", "$StartTime"))
														  .append("EndTime",
																new Document("$max", "$EndTime"))),
								projectFetchingData -> projectFetchDatas,
								projectExcludeIds -> new Document("$project", new Document("_id", 0))

						  ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
				}
			 }
			 else {
				aggregationMean = autoGraphService.getAutoGraphCompHistoTestValue1X(testNumber, testName,
					   matchOperation, projectionOperation, projectFetchDatas);
			 }
		  }
		  logger.info("Inside function getting Data from Collection" + aggregationMean);
		  List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
				.getMappedResults();
		  return testResults;
	   }
	   else {
		  throw new IOException("Selected combination is not supported");
	   }
    }
    /*
     * List Comparison Histogram Two X TestValueFromCollection
     */
    public List<Document> gettingTestValueFromCollectionFor2X(int testNumber, String testName, ObjectId[] waferIds,
  		  double classValFinal, double maxValFinal, DecimalFormat df, double minVal, String DimensionName,
  		  String group, String dimension2xName, List<FilterParam> filterParams) throws IOException {
  	   Aggregation aggregationMean = null;
  	   String CollectionName = null;
  	   AddFieldsOperation addFieldsOperation = null;
  	   // ProjectionOperation projectFetchData = null;
  	   Document projectFetchData = new Document();
  	   logger.info("Inside function gettingTestValueFromCollection");
  	   List<Criteria> criteriaList = new ArrayList<>();
  	   if (waferIds.length == 0) {
  		  if (filterParams != null && filterParams.size() >= 1) {
  			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
  		  }
  	   }
  	   else {
  		  criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

  	   }

  	   MatchOperation matchOperation = Aggregation
  			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

  	   StringBuilder s1 = new StringBuilder("");
  	   for (double k = minVal; k <= maxValFinal; k = Double.valueOf(k + classValFinal)) {
  		  String Cond = "cond((testValueDetails.mv > " + k + " && testValueDetails.mv <= " + (k + classValFinal)
  				+ "), \"" + k + "-" + (k + classValFinal) + "\", \"\")";
  		  if (s1 == null || s1.toString().equals("")) {
  			 s1 = s1.append(Cond);
  		  }
  		  else {
  			 s1 = s1.append("," + Cond);
  		  }
  	   }

  	   String expString = "concat(" + s1 + ")";
  	   String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
  	   if (grouping == null) {
  		  grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
  	   }
  	   String Dimension = CriteriaUtil.generateFieldNameForCriteria(DimensionName);
  	   if (Dimension == null) {
  		  throw new IOException("Selected combination is not supported");
  	   }
  	   String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(dimension2xName);
  	   if (Dimension2X == null) {
  		  throw new IOException("Selected combination is not supported");
  	   }
  	   if (grouping.equals("mesWafer._id") || grouping.equals(Dimension)) {
  		  List<String> DimensionExp = Arrays.asList(("$_id.Dimension"));
  		  List<String> Dimension2Exp = Arrays.asList(("$_id.Dimension2X"));
  		  List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
  				.collect(Collectors.toList());

  		  AddFieldsOperation addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
  				.withValue(ConcatArrays.arrayOf(newDimeList)).build();
  		  AggregationOperation projectionOperation = Aggregation.project().andExpression(expString).as("range")
  				.and(grouping).as("Group").and(Dimension).as("Dimension").and(Dimension2X).as("Dimension2X")
  				.and("$testValueDetails.mvCount").as("Count").and("$mesWafer.WaferStartTime").as("WaferStartTime")
  				.and("$mesWafer.WaferEndTime").as("WaferEndTime");
  		  if (grouping.equals("mesWafer._id")) {
  			 projectFetchData = new Document("$project",
  				    new Document("xAxis", 1).append("yAxis", 1)
  						  .append("WaferStartTime", Arrays.asList("$StartTime"))
  						  .append("WaferEndTime", Arrays.asList("$EndTime")));
  		  }
  		  else {
  			 projectFetchData = new Document("$project",
  				    new Document("xAxis", 1).append("group", "$Group").append("yAxis", 1)
  						  .append("WaferStartTime", Arrays.asList("$StartTime"))
  						  .append("WaferEndTime", Arrays.asList("$EndTime")));
  		  }
  		  final Document projectFetchDatas = projectFetchData;
  		  ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
  		  addFieldsOperation = Aggregation.addFields().addFieldWithValue("rangeValues", "$range").build();
  		  if (waferIds.length > 0) {
  			 if (DimensionName.equals("Device") || DimensionName.equals("deviceGroup")
  				    || dimension2xName.equals("Device") || dimension2xName.equals("deviceGroup")
  				    || DimensionName.equals("chipArea") || dimension2xName.equals("chipArea")) {
  				aggregationMean = Aggregation.newAggregation(
  					   productLookUp -> new Document("$lookup",
  							 new Document("from", "ProductInfo").append("localField", "mesWafer.productId")
  								    .append("foreignField", "_id").append("as", "ProductInfo")),
  					   unwindproduct -> new Document("$unwind",
  							 new Document("path", "$ProductInfo").append("preserveNullAndEmptyArrays", true)),
  					   lookUpTestValue -> new Document("$lookup",
  							 new Document("from", "TestValueSummary")
  								    .append("let", new Document("businessId", "$mesWafer._id"))
  								    .append("pipeline", Arrays.asList(new Document("$match",
  										  new Document("$expr", new Document("$and", Arrays.asList(
  												new Document("$eq", Arrays.asList("$wd", "$$businessId")),
  												new Document("$eq", Arrays.asList("$tn", testNumber)),
  												new Document("$eq", Arrays.asList("$tm", testName))))))))
  								    .append("as", "testValueDetails")),
  					   unwindTestValue -> new Document("$unwind",
  							 new Document("path", "$testValueDetails").append("preserveNullAndEmptyArrays",
  								    true)),
  					   matchOperation, projectionOperation,
  					   addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

  					   projectFetching -> new Document("$project", new Document("Group", 1).append("Dimension", 1)
  							 .append("Dimension2X", 1).append("range", 1).append("Count", 1)
  							 .append("equalRange", new Document("$cond",
  								    new Document("if",
  										  new Document("$eq", Arrays.asList("$range", "$rangeValues")))
  												.append("then", 1).append("else", 0)))
  							 .append("WaferStartTime", "$WaferStartTime")
  							 .append("WaferEndTime", "$WaferEndTime")),

  					   addFieldsTotalOperations -> new Document("$addFields",
  							 new Document("TotalCount",
  								    new Document("$multiply", Arrays.asList("$equalRange", "$Count")))),

  					   groupOperation -> new Document("$group",
  							 new Document("_id",
  								    new Document("Dimension", "$Dimension")
  										  .append("Dimension2X", "$Dimension2X").append("range", "$range"))
  												.append("Count", new Document("$sum", "$TotalCount"))
  												.append("StartTime",
  													   new Document("$min", "$WaferStartTime"))
  												.append("EndTime", new Document("$max", "$WaferEndTime"))
  												.append("WaferId", new Document("$addToSet", "$Group"))),
  					   groupOperationTest -> new Document("$group",
  							 new Document("_id", new Document("Dimension", "$_id.Dimension")
  								    .append("Dimension2X", "$_id.Dimension2X").append("Group", "$_id.Group"))
  										  .append("yAxis",
  												new Document("$addToSet",
  													   new Document("range", "$_id.range").append("Count",
  															 "$Count").append("WaferId", "$WaferId")))
  										  .append("StartTime", new Document("$min", "$StartTime"))
  										  .append("EndTime", new Document("$max", "$EndTime"))),
  					   addFieldsOperation2X -> new Document("$addFields",
  							 new Document("xAxis", new Document("$concatArrays", Arrays.asList(newDimeList)))),
  					   projectFetchingData -> projectFetchDatas,
  					   projectExcludeIds -> new Document("$project", new Document("_id", 0))

  				).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
  			 }
  			 else {
  				aggregationMean = Aggregation
  					   .newAggregation(
  							 lookUpTestValue -> new Document("$lookup", new Document("from", "TestValueSummary")
  								    .append("let", new Document("businessId", "$mesWafer._id"))
  								    .append("pipeline", Arrays.asList(new Document("$match",
  										  new Document("$expr", new Document("$and", Arrays.asList(
  												new Document("$eq", Arrays.asList("$wd", "$$businessId")),
  												new Document("$eq", Arrays.asList("$tn", testNumber)),
  												new Document("$eq", Arrays.asList("$tm", testName))))))))
  								    .append("as", "testValueDetails")),
  							 unwindTestValue -> new Document(
  								    "$unwind",
  								    new Document(
  										  "path", "$testValueDetails").append("preserveNullAndEmptyArrays",
  												true)),
  							 matchOperation, projectionOperation,
  							 addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

  							 projectFetching -> new Document("$project", new Document("Group", 1)
  								    .append("Dimension", 1).append("Dimension2X", 1).append("range", 1)
  								    .append("Count", 1)
  								    .append("equalRange", new Document("$cond",
  										  new Document("if",
  												new Document("$eq",
  													   Arrays.asList("$range", "$rangeValues")))
  															 .append("then", 1).append("else", 0)))
  								    .append("WaferStartTime", "$WaferStartTime")
  								    .append("WaferEndTime", "$WaferEndTime")),

  							 addFieldsTotalOperations -> new Document("$addFields",
  								    new Document("TotalCount",
  										  new Document("$multiply", Arrays.asList("$equalRange", "$Count")))),

  							 groupOperation -> new Document("$group",
  								    new Document("_id", new Document("Dimension", "$Dimension")
  										  .append("Dimension2X", "$Dimension2X").append("range", "$range"))
  												.append("Count", new Document("$sum", "$TotalCount"))
  												.append("StartTime",
  													   new Document("$min", "$WaferStartTime"))
  												.append("EndTime", new Document("$max", "$WaferEndTime"))
  												.append("WaferId", new Document("$addToSet", "$Group"))),
  							 groupOperationTest -> new Document("$group",
  								    new Document("_id",
  										  new Document("Dimension", "$_id.Dimension")
  												.append("Dimension2X", "$_id.Dimension2X")
  												.append("Group", "$_id.Group"))
  													   .append("yAxis",
  															 new Document("$addToSet",
  																    new Document("range", "$_id.range")
  																		  .append("Count", "$Count")
  																		.append("WaferId", "$WaferId")))
  													   .append("StartTime",
  															 new Document("$min", "$StartTime"))
  													   .append("EndTime",
  															 new Document("$max", "$EndTime"))),
  							 addFieldsOperation2X -> new Document("$addFields",
  								    new Document("xAxis",
  										  new Document("$concatArrays", Arrays.asList(newDimeList)))),
  							 projectFetchingData -> projectFetchDatas,
  							 projectExcludeIds -> new Document("$project", new Document("_id", 0)))
  					   .withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
  			 }
  		  }
  		  else {
  			 aggregationMean = Aggregation.newAggregation(
  				    productLookUp -> new Document("$lookup",
  						  new Document("from", "ProductInfo").append("localField", "mesWafer.productId")
  								.append("foreignField", "_id").append("as", "ProductInfo")),
  				    unwindproduct -> new Document("$unwind",
  						  new Document("path", "$ProductInfo").append("preserveNullAndEmptyArrays", true)),
  				    lookUpTestValue -> new Document("$lookup", new Document("from", "TestValueSummary")
  						  .append("let", new Document("businessId", "$mesWafer._id"))
  						  .append("pipeline",
  								Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
  									   Arrays.asList(new Document("$eq", Arrays.asList("$wd", "$$businessId")),
  											 new Document("$eq", Arrays.asList("$tn", testNumber)),
  											 new Document("$eq", Arrays.asList("$tm", testName))))))))
  						  .append("as", "testValueDetails")),
  				    unwindTestValue -> new Document("$unwind",
  						  new Document("path", "$testValueDetails").append("preserveNullAndEmptyArrays", true)),
  				    matchOperation, projectionOperation,
  				    addFields -> new Document("$addFields", new Document("rangeValues", "$range")),

  				    projectFetching -> new Document("$project", new Document("Group", 1).append("Dimension", 1)
  						  .append("Dimension2X", 1).append("range", 1).append("Count", 1)
  						  .append("equalRange", new Document("$cond",
  								new Document("if", new Document("$eq", Arrays.asList("$range", "$rangeValues")))
  									   .append("then", 1).append("else", 0)))
  						  .append("WaferStartTime", "$WaferStartTime").append("WaferEndTime", "$WaferEndTime")),

  				    addFieldsTotalOperations -> new Document("$addFields",
  						  new Document("TotalCount",
  								new Document("$multiply", Arrays.asList("$equalRange", "$Count")))),

  				    groupOperation -> new Document("$group",
  						  new Document("_id",
  								new Document("Dimension", "$Dimension").append("Dimension2X", "$Dimension2X")
  									   .append("range", "$range"))
  											 .append("Count", new Document("$sum", "$TotalCount"))
  											 .append("StartTime", new Document("$min", "$WaferStartTime"))
  											 .append("EndTime", new Document("$max", "$WaferEndTime"))
  											.append("WaferId", new Document("$addToSet", "$Group"))),
  				    groupOperationTest -> new Document("$group",
  						  new Document("_id", new Document("Dimension", "$_id.Dimension")
  								.append("Dimension2X", "$_id.Dimension2X").append("Group", "$_id.Group"))
  									   .append("yAxis",
  											 new Document("$addToSet",
  												    new Document("range", "$_id.range").append("Count",
  														  "$Count").append("WaferId", "$WaferId")))
  									   .append("StartTime", new Document("$min", "$StartTime"))
  									   .append("EndTime", new Document("$max", "$EndTime"))),
  				    addFieldsOperation2X -> new Document("$addFields",
  						  new Document("xAxis", new Document("$concatArrays", Arrays.asList(newDimeList)))),
  				    projectFetchingData -> projectFetchDatas,
  				    projectExcludeIds -> new Document("$project", new Document("_id", 0))

  			 ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());

  		  }
  		  logger.info("Inside function getting Data from Collection" + aggregationMean);
  		  List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
  				.getMappedResults();
  		  return testResults;
  	   }
  	   else {
  		  throw new IOException("Selected combination is not supported");
  	   }

      }
    
    public List<Document> GroupTestValueBasedOnYAxis(List<Document> GroupingtestValues) {
 	   List<Document> documentList = new ArrayList<Document>();
 	   for (Document document : GroupingtestValues) {
 		  Document documentNew = new Document();
 		  List<Object> MainArrayval = new ArrayList<Object>();
 		  if (document.containsKey("yAxis")) {
 			 List<Document> MainArray = (List<Document>) document.get("yAxis");
 			 for (Document obj : MainArray) {
 				HistoTestValueGrouing objHistoGrouping = new HistoTestValueGrouing();
 				String objId = null;
 				
 				ArrayList<ObjectId> wafersObj = new ArrayList<>();
 				if (obj.containsKey("range")) {
 				    objId = (String) obj.get("range");
 				   ArrayList<String> wafers = new ArrayList<>();
 				   wafersObj = (ArrayList<ObjectId>) obj.get("WaferId");
 				  for(ObjectId id:wafersObj) {
 					 
 					 wafers.add(id.toHexString()); 
 					
 				  }
 				    if (objId.length() > 0) {
 					   String[] SplitterobjId = objId.split("-");
 					   String SplitterobjIdpart1 = SplitterobjId[0];
 					   String SplitterobjIdpart2 = SplitterobjId[1];
 					   objHistoGrouping.setRangeStart(Double.parseDouble(SplitterobjIdpart1));
 					   objHistoGrouping.setRangeEnd(Double.parseDouble(SplitterobjIdpart2));
 					   if (obj.containsKey("Count")) {
 						  if (obj.get("Count") instanceof Integer) {
 							 Integer CountVal = (Integer) obj.get("Count");
 							 objHistoGrouping.setCount((int) CountVal);
 						  }
 						  else if (obj.get("Count") instanceof Double) {
 							 double data = (double) obj.get("Count");
 							 Double newData = new Double(data);
 							 int value = newData.intValue();
 							 objHistoGrouping.setCount((int) value);
 						  }
 						  else {
 							 long data = (long) obj.get("Count");
 							 Long newData = new Long(data);
 							 int value = newData.intValue();
 							 objHistoGrouping.setCount((int) value);
 						  }

 					   }
 					  objHistoGrouping.setWaferID(wafers);
 					   MainArrayval.add(objHistoGrouping);

 				    }
 				}
 			 }
 			 if (MainArrayval.size() > 0) {
 				documentNew.put("xAxis", (List<Document>) document.get("xAxis"));
 				documentNew.put("yAxis", MainArrayval);
 				documentNew.put("WaferStartTime", document.get("WaferStartTime"));
 				documentNew.put("WaferEndTime", document.get("WaferEndTime"));
 				documentList.add(documentNew);
 			 }
 		  }

 		  // document.put("yAxis", MainArrayval);
 	   }
 	   return documentList;
     }

/*
 * Regular Histogram TestValue
 */
    public List<Document> gettingRegularHistpTestFromCollection(int testNumber, String testName, ObjectId[] waferIds,
  		  double classValFinal, double maxValFinal, DecimalFormat df, double minVal, String group,
  		  List<FilterParam> filterParams) {

  	   String CollectionName = null;
  	   Aggregation aggregationMean = null;
  	   List<Double> RangeValue = new ArrayList<Double>();
  	   List<Criteria> criteriaList = new ArrayList<Criteria>();
  	   RangeValue.add(minVal);
  	   for (double k = minVal; k < maxValFinal; k = Double.valueOf(k + classValFinal)) {
  		  RangeValue.add(k + classValFinal);
  	   }

  	   logger.info("Inside Inside function gettingTestValueFromCollection");
  	   List<Criteria> criteriaListforwafers = new ArrayList<>();
  	   List<Criteria> criteriaListForMinMax = new ArrayList<>();
  	   if (waferIds.length == 0) {
  		  if (filterParams != null && filterParams.size() >= 1) {
  			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
  		  }
  	   }
  	   else {
  		  criteriaList.add(Criteria.where("wd").in(waferIds));

  	   }

  	   if (testNumber > 0) {
  		  Criteria testCriteria = (Criteria.where("tn").is(testNumber));
  		  criteriaListForMinMax.add(testCriteria);
  		  Criteria testNameCriteria = (Criteria.where("tm").is(testName));
  		  criteriaListForMinMax.add(testNameCriteria);
  	   }

  	   MatchOperation matchForMinMax = Aggregation.match(
  			 new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
  	   LookupOperation lookupTestHistory = LookupOperation.newLookup().from("TestHistory").localField("wd")
  			 .foreignField("mesWafer._id").as("TestHistory");
  	   UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", false);
  	   LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
  			 .localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
  	   UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
  	   MatchOperation matchTestOperation = Aggregation
  			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

  	   final BucketOperation bucketOperation = Aggregation.bucket("mv").withBoundaries(RangeValue.toArray())
  			 .withDefaultBucket("defaultBucket").andOutput("_id").count().as("count");

  	   final BucketOperation bucketOperationAutoGraph = Aggregation.bucket("mv").withBoundaries(RangeValue.toArray())
  			 .withDefaultBucket("defaultBucket").andOutput("_id").count().as("count")
  			 .andOutput("$TestHistory.mesWafer.WaferStartTime").min().as("WaferStartTime")
  			 .andOutput("$TestHistory.mesWafer.WaferEndTime").max().as("WaferEndTime");
  	   ProjectionOperation projectFetchData = Aggregation.project().and("$count").as("count")
  			 .andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime");

  	   if (waferIds.length > 0) {
  		  aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestOperation, bucketOperation);
  	   }
  	   else {

  		  ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
  				.andExclude("_id");
  		  List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
  				lookupProductOperation, unwindProductInfo, matchTestOperation, projectFetchWafers);

  		  MatchOperation match1 = Aggregation
  				.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
  		  aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupTestHistory, unwindTestHistory,
  				bucketOperationAutoGraph, projectFetchData);
  	   }
  	   List<Document> testValuesMap = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
  			 .getMappedResults();
  	   return testValuesMap;
      }
    
    public List<Document> GroupRegularHistoTestValueBasedOnYAxis(List<Document> groupingtestValues, double classVal) {
 	   final Double classValFinal = new Double(classVal);
 	   List<Object> MainArray = new ArrayList<Object>();
 	   List<Document> documentList = new ArrayList<Document>();

 	   Document documentNew = new Document();

 	   for (Document obj : groupingtestValues) {
 		  HistoTestValueGrouing objHistoGrouping = new HistoTestValueGrouing();
 		  Double objId = null;
 		  if (!(obj.get("_id").equals("defaultBucket"))) {
 			 objId = (Double) obj.get("_id");
 			 objHistoGrouping.setRangeStart(objId);
 			 objHistoGrouping.setRangeEnd(objId + classValFinal);

 			 if (obj.containsKey("count")) {
 				Integer CountVal = (Integer) obj.get("count");
 				objHistoGrouping.setCount((int) CountVal);
 			 }
 			 MainArray.add(objHistoGrouping);
 		  }
 		  documentNew.put("WaferStartTime", obj.get("WaferStartTime"));
 		  documentNew.put("WaferEndTime", obj.get("WaferEndTime"));
 	   }

 	   if (MainArray.size() > 0) {
 		  documentNew.put("xAxis", MainArray);
 		  documentList.add(documentNew);
 	   }

 	   return documentList;

     }

    
    /*
     * BoxPlot TestValue One X
     */
    public List<Document> gettingTestValueBoxPlot(int testNumber, String testName, ObjectId[] waferIds, double maxVal,
  		  double minVal, String dimensionName, String group, String granuality, List<FilterParam> filterParams)
  		  throws IOException {

  	   String Dimension;
  	   Aggregation aggregationval = null;
  	   String CollectionName = "TestHistory";
  	   List<Criteria> criteriaList = new ArrayList<>();
  	   if (waferIds.length == 0) {
  		  if (filterParams != null && filterParams.size() >= 1) {

  			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

  		  }
  	   }
  	   else {
  		  criteriaList.add(Criteria.where("wd").in(waferIds));

  	   }
  	   if (testNumber > 0) {
  		  Criteria testCriteria = (Criteria.where("tn").is(testNumber));
  		  criteriaList.add(testCriteria);
  		  Criteria testNameCriteria = (Criteria.where("tm").is(testName));
  		  criteriaList.add(testNameCriteria);
  	   }
  	   if (dimensionName.equals("deviceGroup") || dimensionName.equals("Device") || dimensionName.equals("chipArea")) {
  		  throw new IOException("Selected combination is not supported");
  	   }
  	   else {
  		  Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);

  	   }
  	   Document projectFieldval = new Document();
  	   if (waferIds.length > 0) {
  		  if (minVal > 0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)))))))
  									   .append("Dimension", "$_id").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal > 0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  									   .append("Dimension", "$_id").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project",
  				    new Document("testvalue",
  						  new Document("$filter",
  								new Document("input", "$testvalue").append("as", "filterValue").append("cond",
  									   new Document()))).append("Dimension", "$_id").append("WaferID",
  											 "$WaferId"));
  		  }
  		  else {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)),
  									   new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  											 .append("Dimension", "$_id").append("WaferID", "$WaferId"));
  		  }
  	   }
  	   else {
  		  if (minVal > 0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)))))))
  									   .append("Dimension", "$_id").append("WaferStartTime", "$WaferStartTime")
  									   .append("WaferEndTime", "$WaferEndTime").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal > 0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  									   .append("Dimension", "$_id").append("WaferStartTime", "$WaferStartTime")
  									   .append("WaferEndTime", "$WaferEndTime").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project",
  				    new Document("testvalue",
  						  new Document("$filter",
  								new Document("input", "$testvalue").append("as", "filterValue").append("cond",
  									   new Document()))).append("Dimension", "$_id")
  											 .append("WaferStartTime", "$WaferStartTime")
  											 .append("WaferEndTime", "$WaferEndTime")
  											 .append("WaferID", "$WaferId"));
  		  }
  		  else {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)),
  									   new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  											 .append("Dimension", "$_id")
  											 .append("WaferStartTime", "$WaferStartTime")
  											 .append("WaferEndTime", "$WaferEndTime")
  											 .append("WaferID", "$WaferId"));
  		  }
  	   }
  	   final Document projectFilteringMinMax = projectFieldval;
  	   if (waferIds.length > 0) {
  		  aggregationval = Aggregation.newAggregation(
  				match -> new Document("$match",
  					   new Document("$and",
  							 Arrays.asList(new Document("waferID", new Document("$in", Arrays.asList(waferIds))),
  								    new Document("tn", testNumber), new Document("tm", testName)))),
  				lookup -> new Document("$lookup",
  					   new Document("from", "TestHistory").append("localField", "waferID")
  							 .append("foreignField", "mesWafer._id").append("as", "TestHistory")),

  				unwindTesthistory -> new Document("$unwind",
  					   new Document("path", "$TestHistory").append("preserveNullAndEmptyArrays", true)),

  				addFields -> new Document("$addFields", new Document("GroupingField", "$TestHistory." + Dimension)),

  				groupfield -> new Document("$group",
  					   new Document("_id", "$GroupingField").append("value", new Document("$push", "$mv"))
  							 .append("wafer", new Document("$push", "$waferID"))),

  				projectField -> new Document("$project",
  					   new Document("testvalue", new Document("$reduce",
  							 new Document("input", "$value").append("initialValue", new ArrayList<>()).append(
  								    "in", new Document("$concatArrays", Arrays.asList("$$value", "$$this")))))
  										  .append("Dimension", "$_id").append("WaferId", "$wafer")),

  				projectFiel -> projectFilteringMinMax,
  				matchZero -> new Document("$match", new Document("testvalue", new Document("$gt", 0))),
  				soreField -> new Document("$project",
  					   new Document("testvalue", new Document("$function",
  							 new Document("body", "function(mv){return mv.sort(function(a, b){return a - b})}")
  								    .append("args", Arrays.asList("$testvalue")).append("lang", "js")))
  										  .append("Dimension", "$_id").append("Wafer", "$WaferID")),
  				BoxPlotcal -> new Document("$project",
  					   new Document("Dimension", "$Dimension").append("WaferID", "$Wafer")
  							 .append("Max", new Document("$max", Arrays.asList("$testvalue")))
  							 .append("Min", new Document("$min", Arrays.asList("$testvalue")))
  							 .append("Q1", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor",
  										  new Document("$divide",
  												Arrays.asList(new Document("$size", "$testvalue"), 4))))))
  							 .append("median", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor",
  										  new Document("$divide",
  												Arrays.asList(new Document("$size", "$testvalue"), 2))))))
  							 .append("Q3", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor", new Document("$multiply",
  										  Arrays.asList(new Document("$size", "$testvalue"), 0.75))))))

  				), projectExcludeId -> new Document("$project", new Document("_id", 0))

  		  ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
  	   }
  	   else {

  		  aggregationval = autoGraphService.getAutoGraphCompHistoTestValue2X(Dimension, criteriaList,
  				projectFilteringMinMax);
  	   }
  	   logger.info("Preparing Query gettingTestValueBoxPlot");
  	   List<Document> keyResultsTestValue = mongoTemplate.aggregate(aggregationval, "TestBoundValues", Document.class)
  			 .getMappedResults();
  	   List<ObjectId> listWafers=new ArrayList<>();	   
  	   if (waferIds.length > 0) {   
  	   for (Document obj : keyResultsTestValue) {
  		   List<String>waferIdList=new ArrayList<>();
  		   listWafers=(List<ObjectId>)obj.get("WaferID");
  			  for(ObjectId id:listWafers) {
  				  
  				  waferIdList.add(id.toHexString()); 
  			      obj.put("waferID", waferIdList); 
  			      obj.remove("WaferID");			     
  			  } 
  		   }
  	   }
  	   return keyResultsTestValue;
      }

    /*
     * BoxPlot TestValue two X
     */
    
    public List<Document> gettingTestValueBoxPlot2X(int testNumber, String testName, ObjectId[] waferIds,
  		  double maxVal, double minVal, String dimensionName, String dimensionName2X, String group, String granuality,
  		  String granuality2X, List<FilterParam> filterParams) throws IOException {

  	   String Dimension;
  	   String Dimension2X;
  	   Aggregation aggregationval = null;
  	   String CollectionName = "TestHistory";
  	   List<Criteria> criteriaList = new ArrayList<>();
  	   if (waferIds.length == 0) {
  		  if (filterParams != null && filterParams.size() >= 1) {

  			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

  		  }
  	   }
  	   else {
  		  criteriaList.add(Criteria.where("wd").in(waferIds));

  	   }
  	   if (testNumber > 0) {
  		  Criteria testCriteria = (Criteria.where("tn").is(testNumber));
  		  criteriaList.add(testCriteria);
  		  Criteria testNameCriteria = (Criteria.where("tm").is(testName));
  		  criteriaList.add(testNameCriteria);
  	   }

  	   if (granuality2X != null && granuality2X != "" || granuality != null && granuality != "") {
  		  throw new IOException("Selected combination is not supported");
  	   }

  	   if ((dimensionName.equals("deviceGroup") || (dimensionName2X.equals("deviceGroup")))
  			 || (dimensionName.equals("Device") || (dimensionName2X.equals("Device")))
  			 || dimensionName.equals("chipArea") || (dimensionName2X.equals("chipArea"))) {
  		  throw new IOException("Selected combination is not supported");
  	   }
  	   else {
  		  Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
  		  Dimension2X = CriteriaUtil.generateFieldNameForCriteria(dimensionName2X);

  	   }
  	   Document projectFieldval = new Document();
  	   if (waferIds.length > 0) {
  		  if (minVal > 0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)))))))
  									   .append("Dimension", "$Dimension").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal > 0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  									   .append("Dimension", "$Dimension").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project",
  				    new Document("testvalue",
  						  new Document("$filter",
  								new Document("input", "$testvalue").append("as", "filterValue").append("cond",
  									   new Document()))).append("Dimension", "$Dimension").append("WaferID",
  											 "$WaferId"));
  		  }
  		  else {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)),
  									   new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  											 .append("Dimension", "$Dimension")
  											 .append("WaferID", "$WaferId"));
  		  }
  	   }
  	   else {
  		  if (minVal > 0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)))))))
  									   .append("Dimension", "$Dimension")
  									   .append("WaferStartTime", "$WaferStartTime")
  									   .append("WaferEndTime", "$WaferEndTime").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal > 0) {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  									   .append("Dimension", "$Dimension")
  									   .append("WaferStartTime", "$WaferStartTime")
  									   .append("WaferEndTime", "$WaferEndTime").append("WaferID", "$WaferId"));
  		  }
  		  else if (minVal == 0.0 && maxVal == 0.0) {
  			 projectFieldval = new Document("$project",
  				    new Document("testvalue",
  						  new Document("$filter",
  								new Document("input", "$testvalue").append("as", "filterValue").append("cond",
  									   new Document()))).append("Dimension", "$Dimension")
  											 .append("WaferStartTime", "$WaferStartTime")
  											 .append("WaferEndTime", "$WaferEndTime")
  											 .append("WaferID", "$WaferId"));
  		  }
  		  else {
  			 projectFieldval = new Document("$project", new Document("testvalue",
  				    new Document("$filter", new Document("input", "$testvalue").append("as", "filterValue")
  						  .append("cond", new Document("$and",
  								Arrays.asList(new Document("$gte", Arrays.asList("$$filterValue", minVal)),
  									   new Document("$lte", Arrays.asList("$$filterValue", maxVal)))))))
  											 .append("Dimension", "$Dimension")
  											 .append("WaferStartTime", "$WaferStartTime")
  											 .append("WaferEndTime", "$WaferEndTime")
  											 .append("WaferID", "$WaferId"));
  		  }
  	   }
  	   final Document projectFilteringMinMax = projectFieldval;
  	   if (waferIds.length > 0) {
  		  aggregationval = Aggregation.newAggregation(
  				match -> new Document("$match",
  					   new Document("$and",
  							 Arrays.asList(new Document("waferID", new Document("$in", Arrays.asList(waferIds))),
  								    new Document("tn", testNumber), new Document("tm", testName)))),
  				lookup -> new Document("$lookup",
  					   new Document("from", "TestHistory").append("localField", "waferID")
  							 .append("foreignField", "mesWafer._id").append("as", "TestHistory")),

  				unwindTesthistory -> new Document("$unwind",
  					   new Document("path", "$TestHistory").append("preserveNullAndEmptyArrays", true)),

  				addFields -> new Document("$addFields",
  					   new Document("GroupingField", "$TestHistory." + Dimension).append("GroupingField2X",
  							 "$TestHistory." + Dimension2X)),

  				groupfield -> new Document("$group",
  					   new Document("_id",
  							 new Document(dimensionName, "$GroupingField").append(dimensionName2X,
  								    "$GroupingField2X")).append("value", new Document("$push", "$mv"))
  										  .append("wafer", new Document("$push", "$waferID"))),

  				projectFieldDimension -> new Document("$addFields",
  					   new Document("xAxis", new Document("$concatArrays",
  							 Arrays.asList(Arrays.asList("$_id." + dimensionName, "$_id." + dimensionName2X))))),

  				projectField -> new Document("$project",
  					   new Document("testvalue", new Document("$reduce",
  							 new Document("input", "$value").append("initialValue", new ArrayList<>()).append(
  								    "in", new Document("$concatArrays", Arrays.asList("$$value", "$$this")))))
  										  .append("Dimension", "$xAxis").append("WaferId", "$wafer")),

  				projectFiel -> projectFilteringMinMax,
  				matchZero -> new Document("$match", new Document("testvalue", new Document("$gt", 0))),
  				soreField -> new Document("$project",
  					   new Document("testvalue", new Document("$function",
  							 new Document("body", "function(mv){return mv.sort(function(a, b){return a - b})}")
  								    .append("args", Arrays.asList("$testvalue")).append("lang", "js")))
  										  .append("Dimension", "$Dimension").append("Wafer", "$WaferID")),
  				BoxPlotcal -> new Document("$project",
  					   new Document("Dimension", "$Dimension").append("WaferID", "$Wafer")
  							 .append("Max", new Document("$max", Arrays.asList("$testvalue")))
  							 .append("Min", new Document("$min", Arrays.asList("$testvalue")))
  							 .append("Q1", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor",
  										  new Document("$divide",
  												Arrays.asList(new Document("$size", "$testvalue"), 4))))))
  							 .append("median", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor",
  										  new Document("$divide",
  												Arrays.asList(new Document("$size", "$testvalue"), 2))))))
  							 .append("Q3", new Document("$arrayElemAt",
  								    Arrays.asList("$testvalue", new Document("$floor", new Document("$multiply",
  										  Arrays.asList(new Document("$size", "$testvalue"), 0.75))))))

  				), projectExcludeId -> new Document("$project", new Document("_id", 0))

  		  ).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
  	   }
  	   else {

  		  aggregationval = autoGraphService.getAutoGraphBoxPlotTestValue2X(Dimension,Dimension2X, criteriaList,dimensionName,dimensionName2X,
  				projectFilteringMinMax);
  	   }
  	   logger.info("Preparing Query gettingTestValueBoxPlot");
  	   List<Document> keyResultsTestValue = mongoTemplate.aggregate(aggregationval, "TestBoundValues", Document.class)
  			 .getMappedResults();
  	   List<ObjectId> listWafers = new ArrayList<>();
  	   
  	   if (waferIds.length > 0) {
  	   for (Document obj : keyResultsTestValue) {
  		  listWafers = (List<ObjectId>) obj.get("WaferID");
  		 List<String> waferIdList = new ArrayList<>();
  		  for (ObjectId id : listWafers) {
  			 waferIdList.add(id.toHexString());
  			 obj.put("waferID", waferIdList);
  			 obj.remove("WaferID");
  		  }
  	   }
  	   }
  	   return keyResultsTestValue;
      }
   
    
    /*
     * LineGraph TestValueMeasure One X
     */
    public List<Document> getTestValueMeasure(ObjectId[] waferIds, String dimensionName, String measureName,
  		  String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
  		  String aggregator, String granuality, List<FilterParam> filterParams) throws IOException {
  	   logger.info("Inside getTestValueMeasure service ");
  	   List<Document> testResults = null;
  	   if (group.equals("WaferNumber")) {
  		  if ((aggregator != null) && (aggregator.equals("Avg"))) {
  			 testResults = getTestValueDetails(waferIds, dimensionName, measureName, group, selectedBin, testName,
  				    testNumber, minVal, maxVal, aggregator, granuality, filterParams);
  		  }
  		  else {
  			 throw new IOException("Selected combination is not supported");
  		  }

  	   }
  	   else {
  		  throw new IOException("Selected combination is not supported");
  	   }
  	   return testResults;
      }

      private List<Document> getTestValueDetails(ObjectId[] waferIds, String dimensionName, String measureName,
  		  String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
  		  String aggregator, String granuality, List<FilterParam> filterParams) throws IOException {

  	   String CollectionName = "TestHistory";
  	   String DimensionQuery = null;
  	   AggregationOperation FilterMinMax = null;
  	   Aggregation aggregationMean = null;
  	   List<Criteria> criteriaList = new ArrayList<>();
  	   List<Criteria> criteriaListforwafers = new ArrayList<>();
  	   List<Criteria> criteriaListForMinMax = new ArrayList<>();
  	   if (waferIds.length == 0) {
  		  if (filterParams != null && filterParams.size() >= 1) {
  			 for (int i = 0; i < filterParams.size(); i++) {
  				if (filterParams.get(i).getField().equals("Device")) {
  				    filterParams.get(i).setField("TestHistory.ProductInfo.deviceName");
  				    System.out.println(filterParams.get(i).getField());
  				}
  			 }
  			 criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

  		  }
  	   }
  	   else {
  		  criteriaList.add(Criteria.where("wd").in(waferIds));

  	   }
  	   Criteria testCriteria = (Criteria.where("tn").is(testNumber));
  	   criteriaListForMinMax.add(testCriteria);
  	   Criteria testNameCriteria = (Criteria.where("tm").in(testName));
  	   criteriaListForMinMax.add(testNameCriteria);

  	   MatchOperation matchForMinMax = Aggregation.match(
  			 new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
  	   MatchOperation matchTestM1Operation = Aggregation
  			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
  	   LookupOperation lookupOperationWafer = LookupOperation.newLookup().from("TestHistory").localField("wd")
  			 .foreignField("mesWafer._id").as("TestHistory");
  	   UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
  	   LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
  			 .localField("TestHistory.mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
  	   UnwindOperation unwindProductInfo = Aggregation.unwind("$TestHistory.ProductInfo", false);
  	   AddFieldsOperation addFieldsOperation = null;

  	   String grouping = CriteriaUtil.generateFieldNameForCriteria(group);

  	   String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
  	   if (Dimension == null) {
  		  throw new IOException("Selected combination is not supported");
  	   }

  	   if (granuality == null) {

  		  addFieldsOperation = Aggregation.addFields().addFieldWithValue("$Dimension", "$TestHistory." + Dimension)
  				.addFieldWithValue("$Group", "$TestHistory." + grouping).build();

  	   }
  	   else {

  		  String attributePath = "$TestHistory." + Dimension;

  		  String dateFormat = "";

  		  switch (granuality) {
  			 case "Year":
  				dateFormat = "%Y";
  				break;
  			 case "Month":
  				dateFormat = "%Y/%m";
  				break;
  			 case "Day":
  				dateFormat = "%Y/%m/%d";
  				break;
  			 case "Hour":
  				dateFormat = "%Y/%m/%d %H:00:00";
  				break;
  			 case "Minute":
  				dateFormat = "%Y/%m/%d %H:%M:00";
  				break;
  			 case "Second":
  				dateFormat = "%Y/%m/%d %H:%M:%S";
  				break;
  			 default:
  				break;
  		  }

  		  addFieldsOperation = Aggregation.addFields()
  				.addFieldWithValue("$Dimension", DateOperators.dateOf(attributePath).toString(dateFormat))
  				.addFieldWithValue("$Group", "$TestHistory." + grouping).build();

  	   }
  	   GroupOperation groupOperationTest = Aggregation.group("Dimension", "Group").avg("$mv").as("value")
  			 .min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
  			 .max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

  	   ProjectionOperation projectVAl = Aggregation.project().andArrayOf("$_id.Dimension").as("xAxis").and("_id.Group")
  			 .as("group").andArrayOf("$value").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
  			 .andArrayOf("$WaferEndTime").as("WaferEndTime");
  	   ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
  	   if (addFieldsOperation == null) {
  		  if (minVal == 0.0 && maxVal == 0.0) {
  			 if (waferIds.length > 0) {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
  					   lookupOperationWafer, unwindTestHistory, lookupProductOperation, unwindProductInfo,
  					   groupOperationTest, projectVAl, excludeId);
  			 }
  			 else {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
  					   unwindTestHistory, lookupProductOperation, unwindProductInfo, matchTestM1Operation,
  					   groupOperationTest, projectVAl, excludeId);
  			 }
  		  }
  		  else {
  			 if (minVal > 0 && maxVal == 0.0) {
  				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").gte(minVal)));
  			 }
  			 else if (minVal == 0.0 && maxVal > 0.0) {
  				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").lte(maxVal)));
  			 }
  			 if (waferIds.length > 0) {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
  					   lookupOperationWafer, unwindTestHistory, lookupProductOperation, unwindProductInfo,
  					   groupOperationTest, FilterMinMax, projectVAl, excludeId);
  			 }
  			 else {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
  					   unwindTestHistory, lookupProductOperation, unwindProductInfo, matchTestM1Operation,
  					   groupOperationTest, FilterMinMax, projectVAl, excludeId);
  			 }
  		  }

  	   }
  	   else {

  		  if (minVal == 0.0 && maxVal == 0.0) {
  			 if (waferIds.length > 0) {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
  					   lookupOperationWafer, unwindTestHistory, lookupProductOperation, unwindProductInfo,
  					   addFieldsOperation, groupOperationTest, projectVAl, excludeId);
  			 }
  			 else {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
  					   unwindTestHistory, lookupProductOperation, unwindProductInfo, matchTestM1Operation,
  					   addFieldsOperation, groupOperationTest, projectVAl, excludeId);
  			 }
  		  }
  		  else {
  			 if (minVal > 0 && maxVal == 0.0) {
  				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").gte(minVal)));
  			 }
  			 else if (minVal == 0.0 && maxVal > 0.0) {
  				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").lte(maxVal)));
  			 }
  			 if (waferIds.length > 0) {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
  					   lookupOperationWafer, lookupProductOperation, unwindProductInfo, unwindTestHistory,
  					   addFieldsOperation, groupOperationTest, FilterMinMax, projectVAl, excludeId);
  			 }
  			 else {
  				aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
  					   lookupProductOperation, unwindProductInfo, unwindTestHistory, matchTestM1Operation,
  					   addFieldsOperation, groupOperationTest, FilterMinMax, projectVAl, excludeId);
  			 }
  		  }

  	   }

  	   List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
  			 .getMappedResults();

  	   return testResults;
      }

      /*
       * LineGraph TestValueMeasure Two X
       */
      public List<Document> getTestValueMeasure2X(ObjectId[] waferIds, String dimensionName, String measureName,
    		  String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
    		  String aggregator, String granuality, String granuality2x, String dimensionName2X,
    		  List<FilterParam> filterParams) throws IOException {
    	   logger.info("Inside getTestValueMeasure service ");
    	   List<Document> testResults = null;
    	   if (group.equals("WaferNumber")) {
    		  if ((aggregator != null) && (aggregator.equals("Avg"))) {
    			 testResults = getTestValueDetails2X(waferIds, dimensionName, measureName, group, selectedBin, testName,
    				    testNumber, minVal, maxVal, aggregator, granuality, dimensionName2X, granuality2x,
    				    filterParams);
    		  }
    		  else {
    			 throw new IOException("Selected combination is not supported");
    		  }

    	   }
    	   else {
    		  throw new IOException("Selected combination is not supported");
    	   }
    	   return testResults;
        }

        private List<Document> getTestValueDetails2X(ObjectId[] waferIds, String dimensionName, String measureName,
    		  String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
    		  String aggregator, String granuality, String dimensionName2X, String granuality2x,
    		  List<FilterParam> filterParams) throws IOException {
    	   if ((granuality != null && granuality != "") || (granuality2x != null && granuality2x != "")) {
    		  throw new IOException("Selected combination is not supported");
    	   }
    	   String DimensionQuery = null;
    	   String CollectionName = "TestHistory";
    	   AddFieldsOperation addFieldsOperationFor2X = null;
    	   AggregationOperation FilterMinMax = null;
    	   Aggregation aggregationMean = null;
    	   List<Criteria> criteriaList = new ArrayList<Criteria>();
    	   List<Criteria> criteriaListforwafers = new ArrayList<>();
    	   List<Criteria> criteriaListForMinMax = new ArrayList<>();
    	   if (filterParams != null && filterParams.size() >= 1) {
    		  for (int i = 0; i < filterParams.size(); i++) {
    			 if (filterParams.get(i).getField().equals("Device")) {
    				filterParams.get(i).setField("TestHistory.ProductInfo.deviceName");
    			 }
    		  }
    		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
    	   }
    	   else {
    		  criteriaList.add(Criteria.where("wd").in(waferIds));

    	   }
    	   Criteria testCriteria = (Criteria.where("tn").is(testNumber));
    	   criteriaListForMinMax.add(testCriteria);
    	   Criteria testNameCriteria = (Criteria.where("tm").in(testName));
    	   criteriaListForMinMax.add(testNameCriteria);
    	   MatchOperation matchForMinMax = Aggregation.match(
    			 new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
    	   MatchOperation matchTestM1Operation = Aggregation
    			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
    	   LookupOperation lookupOperationWafer = LookupOperation.newLookup().from("TestHistory").localField("wd")
    			 .foreignField("mesWafer._id").as("TestHistory");
    	   UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
    	   LookupOperation lookupProductInfo = LookupOperation.newLookup().from("ProductInfo")
    			 .localField("TestHistory.mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
    	   UnwindOperation unwindProductInfo = Aggregation.unwind("$TestHistory.ProductInfo", false);
    	   AddFieldsOperation addFieldsOperation = null;

    	   String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
    	   String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(dimensionName2X);
    	   if (Dimension2X == null) {
    		  throw new IOException("Selected combination is not supported");
    	   }
    	   String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
    	   if (Dimension == null) {
    		  throw new IOException("Selected combination is not supported");
    	   }

    	   if (Dimension.equals(Dimension2X)) {
    		  List<String> DimensionExp = Arrays.asList("$_id.Dimension");
    		  List<String> Dimension2Exp = Arrays.asList("$_id.Dimension2X");
    		  List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
    				.collect(Collectors.toList());

    		  addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
    				.withValue(ConcatArrays.arrayOf(newDimeList)).build();
    	   }
    	   else {
    		  List<String> DimensionExp = Arrays.asList("$_id.Dimension");
    		  List<String> Dimension2Exp = Arrays.asList("$_id.Dimension2X");
    		  List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
    				.collect(Collectors.toList());

    		  addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
    				.withValue(ConcatArrays.arrayOf(newDimeList)).build();
    	   }

    	   if (granuality == null) {

    		  addFieldsOperation = Aggregation.addFields().addFieldWithValue("$Dimension", "$TestHistory." + Dimension)
    				.addFieldWithValue("$Group", "$TestHistory." + grouping)
    				.addFieldWithValue("$Dimension2X", "$TestHistory." + Dimension2X).build();

    	   }
    	   GroupOperation groupOperationTest = Aggregation.group("Dimension", "Group", "Dimension2X").avg("$mv")
    			 .as("value").addToSet("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
    			 .addToSet("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
    	   ;

    	   ProjectionOperation projectVAl = Aggregation.project().and("$xAxis").as("xAxis").and("_id.Group").as("group")
    			 .andArrayOf("$value").as("yAxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
    			 .as("WaferEndTime");
    	   ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
    	   if (addFieldsOperation == null) {
    		  if (minVal == 0.0 && maxVal == 0.0) {
    			 if (group.equals("Device") || group.equals("deviceGroup") || dimensionName2X.equals("deviceGroup")
    				    || dimensionName2X.equals("Device") || dimensionName.equals("deviceGroup")
    				    || dimensionName.equals("Device")) {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, lookupProductInfo, unwindProductInfo,
    						  groupOperationTest, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, lookupProductInfo, unwindProductInfo, matchTestM1Operation,
    						  groupOperationTest, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    			 else {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, groupOperationTest, addFieldsOperationFor2X,
    						  projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, matchTestM1Operation, groupOperationTest, addFieldsOperationFor2X,
    						  projectVAl, excludeId);
    				}
    			 }
    		  }
    		  else {
    			 if (minVal > 0 && maxVal == 0.0) {
    				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").gte(minVal)));
    			 }
    			 else if (minVal == 0.0 && maxVal > 0.0) {
    				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").lte(maxVal)));
    			 }
    			 if (group.equals("Device") || group.equals("deviceGroup") || dimensionName2X.equals("deviceGroup")
    				    || dimensionName2X.equals("Device") || dimensionName.equals("deviceGroup")
    				    || dimensionName.equals("Device")) {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, lookupProductInfo, unwindProductInfo,
    						  groupOperationTest, FilterMinMax, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, lookupProductInfo, unwindProductInfo, matchTestM1Operation,
    						  groupOperationTest, FilterMinMax, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    			 else {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, groupOperationTest, FilterMinMax,
    						  addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, matchTestM1Operation, groupOperationTest, FilterMinMax,
    						  addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    		  }

    	   }
    	   else {

    		  if (minVal == 0.0 && maxVal == 0.0) {
    			 if (group.equals("Device") || group.equals("deviceGroup") || dimensionName2X.equals("deviceGroup")
    				    || dimensionName2X.equals("Device") || dimensionName.equals("deviceGroup")
    				    || dimensionName.equals("Device")) {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, lookupProductInfo, unwindProductInfo,
    						  addFieldsOperation, groupOperationTest, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, lookupProductInfo, unwindProductInfo, matchTestM1Operation,
    						  addFieldsOperation, groupOperationTest, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    			 else {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, addFieldsOperation, groupOperationTest,
    						  addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, matchTestM1Operation, addFieldsOperation, groupOperationTest,
    						  addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    		  }
    		  else {
    			 if (minVal > 0 && maxVal == 0.0) {
    				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").gte(minVal)));
    			 }
    			 else if (minVal == 0.0 && maxVal > 0.0) {
    				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("value").lte(maxVal)));
    			 }
    			 if (group.equals("Device") || group.equals("deviceGroup") || dimensionName2X.equals("deviceGroup")
    				    || dimensionName2X.equals("Device") || dimensionName.equals("deviceGroup")
    				    || dimensionName.equals("Device")) {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, lookupProductInfo, unwindProductInfo,
    						  addFieldsOperation, groupOperationTest, FilterMinMax, addFieldsOperationFor2X,
    						  projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, lookupProductInfo, unwindProductInfo, matchTestM1Operation,
    						  addFieldsOperation, groupOperationTest, FilterMinMax, addFieldsOperationFor2X,
    						  projectVAl, excludeId);
    				}
    			 }
    			 else {
    				if (waferIds.length > 0) {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestM1Operation,
    						  lookupOperationWafer, unwindTestHistory, addFieldsOperation, groupOperationTest,
    						  FilterMinMax, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    				else {
    				    aggregationMean = Aggregation.newAggregation(matchForMinMax, lookupOperationWafer,
    						  unwindTestHistory, matchTestM1Operation, addFieldsOperation, groupOperationTest,
    						  FilterMinMax, addFieldsOperationFor2X, projectVAl, excludeId);
    				}
    			 }
    		  }

    	   }

    	   List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
    			 .getMappedResults();

    	   return testResults;

        }
        
        public DataListModel getAvgMedian1X(ObjectId[] waferIds, int splitTestNumber, String testName, String dimensions,
      		  String yearMonth, double minVal, double maxVal, List<FilterParam> filterParams) {

      	   String DimensionQuery = null;
      	   AggregationOperation FilterMinMax = null;
      	   String CollectionName = null;
      	   String dimensionName = CriteriaUtil.generateFieldNameForCriteria(dimensions);

      	   if (yearMonth == null) {

      		  DimensionQuery = "$TestHistory." + dimensionName;
      	   }
      	   else {
      		  if (yearMonth.equals("Year")) {
      			 DimensionQuery = "{$year :  \"$TestHistory." + dimensionName + "\"}";
      		  }
      		  else if (yearMonth.equals("Month")) {
      			 DimensionQuery = "{$month :  \"$TestHistory." + dimensionName + "\"}";
      		  }
      		  else if (yearMonth.equals("Hour")) {
      			 DimensionQuery = "{$hour :  \"$TestHistory." + dimensionName + "\"}";
      		  }
      		  else if (yearMonth.equals("Minute")) {
      			 DimensionQuery = "{$minute :  \"$TestHistory." + dimensionName + "\"}";
      		  }
      		  else if (yearMonth.equals("Second")) {
      			 DimensionQuery = "{$second :  \"$TestHistory." + dimensionName + "\"}";
      		  }
      		  else {
      			 DimensionQuery = "$TestHistory." + dimensionName;
      		  }
      	   }

      	   Aggregation aggregationMean = null;
      	   List<Criteria> criteriaList = new ArrayList<Criteria>();
      	   if (filterParams != null && filterParams.size() >= 1) {
      		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
      	   }
      	   else {
      		  criteriaList.add(Criteria.where("wd").in(waferIds));

      	   }
      	   Criteria testCriteria = (Criteria.where("tn").is(splitTestNumber));
      	   criteriaList.add(testCriteria);
      	   Criteria testNameCriteria = (Criteria.where("tm").in(testName));
      	   criteriaList.add(testNameCriteria);

      	   MatchOperation matchTestOperation = Aggregation
      			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
      	   LookupOperation lookupOperationWafer = LookupOperation.newLookup().from("TestHistory").localField("wd")
      			 .foreignField("mesWafer._id").as("TestHistory");
      	   UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);

      	   LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
      			 .localField("TestHistory.mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
      	   UnwindOperation unwindProduct = Aggregation.unwind("$TestHistory.ProductInfo", true);

      	   ProjectionOperation projectionOperationForAvgGroup = Aggregation.project().andExpression(DimensionQuery)
      			 .as("dimensionss").and("$tn").as("tn").and("$tm").as("tm").and("$mv").as("mv");

      	   GroupOperation groupOperation = null;

      	   groupOperation = Aggregation.group("$tn", "$tm", "$dimensionss").first("dimensionss").as("wafer").avg("mv")
      			 .as("average").push("mv").as("mvArreay");
      	   ;

      	   String Cond = "cond(((size(mvArreay)) % 2 == 0), (avg((arrayElemAt(mvArreay,  (size(mvArreay) / 2)-1)),(arrayElemAt(mvArreay,  size(mvArreay) / 2))) ) , (arrayElemAt(mvArreay,  floor(size(mvArreay)) / 2)))";

      	   ProjectionOperation projectionOperationForAvg = Aggregation.project().andExpression(Cond).as("median")
      			 .and("$average").as("average").and("$wafer").as("dimension");
      	   //
      	   ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

      	   if (minVal == 0.0 && maxVal == 0.0) {
      		  aggregationMean = Aggregation.newAggregation(lookupOperationWafer, unwindTestHistory, lookupProduct,
      				unwindProduct, matchTestOperation, projectionOperationForAvgGroup, groupOperation,
      				Aggregation.sort(Sort.Direction.ASC, "mvArreay"), projectionOperationForAvg, excludeId);
      	   }
      	   else {
      		  if (minVal > 0 && maxVal == 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv").gte(minVal)));
      		  }
      		  else if (minVal == 0.0 && maxVal > 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv").lte(maxVal)));
      		  }
      		  else if (minVal == 0.0 && maxVal == 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv")));
      		  }
      		  else {
      			 FilterMinMax = Aggregation
      				    .match(new Criteria().andOperator(Criteria.where("mv").gte(minVal).lte(maxVal)));
      		  }
      		  aggregationMean = Aggregation.newAggregation(lookupOperationWafer, unwindTestHistory, lookupProduct,
      				unwindProduct, matchTestOperation, projectionOperationForAvgGroup, FilterMinMax, groupOperation,
      				Aggregation.sort(Sort.Direction.ASC, "mvArreay"), projectionOperationForAvg, excludeId);
      	   }

      	   List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
      			 .getMappedResults();
      	   // for (Document document : testResults) {
      	   // String objId = (String) document.get("wafer");
      	   // document.put("dimension", objId.toString());
      	   // }
      	   List<Document> newList = testResults.stream().distinct().collect(Collectors.toList());
      	   DataListModel testListModel = new DataListModel();
      	   testListModel.setDataList(newList);
      	   return testListModel;
          }
        
        public DataListModel getAvgMedian2X(ObjectId[] waferIds, int testNumber, String testName, String dimension1x,
      		  String dimension2x, String yearMonth, double minVal, double maxVal, List<FilterParam> filterParams) {

      	   String CollectionName = null;
      	   String DimensionQuery = null;
      	   String Dimension2XQuery = null;
      	   AggregationOperation FilterMinMax = null;

      	   String dimensionName = CriteriaUtil.generateFieldNameForCriteria(dimension1x);
      	   String dimensionName2X = CriteriaUtil.generateFieldNameForCriteria(dimension2x);

      	   if (yearMonth == null) {

      		  DimensionQuery = "$TestHistory." + dimensionName;
      		  Dimension2XQuery = "$TestHistory." + dimensionName2X;
      	   }
      	   else {

      	   }

      	   // TODO Auto-generated method stub
      	   Aggregation aggregationMean = null;
      	   List<Criteria> criteriaList = new ArrayList<Criteria>();
      	   if (filterParams != null && filterParams.size() >= 1) {
      		  criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
      	   }
      	   else {
      		  criteriaList.add(Criteria.where("wd").in(waferIds));

      	   }
      	   Criteria testCriteria = (Criteria.where("tn").is(testNumber));
      	   criteriaList.add(testCriteria);
      	   Criteria testNameCriteria = (Criteria.where("tm").in(testName));
      	   criteriaList.add(testNameCriteria);

      	   MatchOperation matchTestOperation = Aggregation
      			 .match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
      	   LookupOperation lookupOperationWafer = LookupOperation.newLookup().from("TestHistory").localField("wd")
      			 .foreignField("mesWafer._id").as("TestHistory");
      	   UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);

      	   LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
      			 .localField("TestHistory.mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
      	   UnwindOperation unwindProduct = Aggregation.unwind("$TestHistory.ProductInfo", true);

      	   ProjectionOperation projectionOperationForAvgGroup = null;
      	   GroupOperation groupOperation = null;
      	   ProjectionOperation projectionOperationForAvg = null;

      	   if (DimensionQuery.equals(Dimension2XQuery)) {
      		  projectionOperationForAvgGroup = Aggregation.project().andExpression(DimensionQuery).as("dimensionss")
      				.and("$tn").as("tn").and("$tm").as("tm").and("$mv").as("mv");
      		  groupOperation = Aggregation.group("$tn", "$tm", "$dimensionss").first("dimensionss").as("wafer").avg("mv")
      				.as("average").push("mv").as("mvArreay");
      		  String Cond = "cond(((size(mvArreay)) % 2 == 0), (avg((arrayElemAt(mvArreay,  (size(mvArreay) / 2)-1)),(arrayElemAt(mvArreay,  size(mvArreay) / 2))) ) , (arrayElemAt(mvArreay,  floor(size(mvArreay)) / 2)))";

      		  projectionOperationForAvg = Aggregation.project().andExpression(Cond).as("median").and("$average")
      				.as("average").and("$wafer").as("dimension").and("$wafer").as("dimension2X");

      	   }
      	   else {
      		  projectionOperationForAvgGroup = Aggregation.project().andExpression(DimensionQuery).as("dimensionss")
      				.andExpression(Dimension2XQuery).as("dimension2X").and("$tn").as("tn").and("$tm").as("tm")
      				.and("$mv").as("mv");
      		  groupOperation = Aggregation.group("$tn", "$tm", "$dimensionss", "$dimension2X").first("dimensionss")
      				.as("wafer").first("dimension2X").as("wafer2X").avg("mv").as("average").push("mv").as("mvArreay");
      		  String Cond = "cond(((size(mvArreay)) % 2 == 0), (avg((arrayElemAt(mvArreay,  (size(mvArreay) / 2)-1)),(arrayElemAt(mvArreay,  size(mvArreay) / 2))) ) , (arrayElemAt(mvArreay,  floor(size(mvArreay)) / 2)))";

      		  projectionOperationForAvg = Aggregation.project().andExpression(Cond).as("median").and("$average")
      				.as("average").and("$wafer").as("dimension").and("$wafer2X").as("dimension2X");
      	   }

      	   ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

      	   if (minVal == 0.0 && maxVal == 0.0) {
      		  aggregationMean = Aggregation.newAggregation(lookupOperationWafer, unwindTestHistory, lookupProduct,
      				unwindProduct, matchTestOperation, projectionOperationForAvgGroup, groupOperation,
      				Aggregation.sort(Sort.Direction.ASC, "mvArreay"), projectionOperationForAvg, excludeId);
      	   }
      	   else {
      		  if (minVal > 0 && maxVal == 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv").gte(minVal)));
      		  }
      		  else if (minVal == 0.0 && maxVal > 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv").lte(maxVal)));
      		  }
      		  else if (minVal == 0.0 && maxVal == 0.0) {
      			 FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("mv")));
      		  }
      		  else {
      			 FilterMinMax = Aggregation
      				    .match(new Criteria().andOperator(Criteria.where("mv").gte(minVal).lte(maxVal)));
      		  }
      		  aggregationMean = Aggregation.newAggregation(lookupOperationWafer, unwindTestHistory, lookupProduct,
      				unwindProduct, matchTestOperation, projectionOperationForAvgGroup, FilterMinMax, groupOperation,
      				Aggregation.sort(Sort.Direction.ASC, "mvArreay"), projectionOperationForAvg, excludeId);
      	   }

      	   List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
      			 .getMappedResults();
      	   // for (Document document : testResults) {
      	   // String objId = (String) document.get("wafer");
      	   // document.put("dimension", objId.toString());
      	   // }
      	   List<Document> newList = testResults.stream().distinct().collect(Collectors.toList());
      	   DataListModel testListModel = new DataListModel();
      	   testListModel.setDataList(newList);
      	   return testListModel;
          }
      
}
