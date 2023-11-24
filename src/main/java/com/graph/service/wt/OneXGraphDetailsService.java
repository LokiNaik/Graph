package com.graph.service.wt;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.constants.wt.AdvancedGraphConstants;
import com.graph.model.wt.FilterParam;
import com.graph.utils.CriteriaUtil;
import com.mongodb.BasicDBObject;

@Service
public class OneXGraphDetailsService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired(required = true)
	private IAutographService autoGraphService;

	private static String GROUP_DUT = "";
	private static final String[] binGroupingList = { "TestPassChips", "NumberOfPassedChips", "NumberOfFailedChips",
			"NumberOfChipsPerBin" };
	private static final String BIN_RATIO = "BinRatio";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	/*
	* 
	*/

	public List<Document> getGroupByTestForComparison(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, String testName, int testNumber, double minVal, double maxVal, String aggregator,
			int measureselectedBin, boolean status, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		Boolean TestRelatedDimensions = false;
		Boolean TestRelatedMeasures = false;
		List<Document> dimensionResults = null;
		List<Document> measureResults = null;
		List<Document> testResultsDocument;
		Aggregation aggregation = null;
		Aggregation aggregationMeasureOne = null;
		List<Document> keyResultsMean = null;
		if (!dimensionName.equals("WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (!dimensionName.isEmpty()) {
			dimensionResults = generateCommonnComparisonDimensionForTest(waferIds, dimensionName, measureName, group,
					measureselectedBin, testName, testNumber, minVal, maxVal, aggregator, filterParams);
			TestRelatedMeasures = false;
		}
		if (!measureName.isEmpty()) {
			switch (measureName) {
			case "TestFailChips":

				throw new IOException("Selected combination is not supported");
			case "TestFailedRatio":

				throw new IOException("Selected combination is not supported");

			case "TestPassChips":
				if (aggregator.equals("Avg")) {
					throw new IOException("Selected combination is not supported");
				}
				List<Criteria> criteriaListTestFail1 = new ArrayList<>();
				List<Criteria> criteriaListTestFail = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && !filterParams.isEmpty()) {
						criteriaListTestFail1 = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

					}
				} else {
					criteriaListTestFail1.add(Criteria.where("wd").in(waferIds));

				}
				criteriaListTestFail.add(Criteria.where("tp").is(true));
				// Criteria waferCriteriaTestFail1 = (Criteria.where("wd").in(waferIds));
				// criteriaListTestFail1.add(waferCriteriaTestFail1);
				// Criteria testFailCriteria1 = (Criteria.where("tp").is(true));
				// criteriaListTestFail1.add(testFailCriteria1)
				MatchOperation matchForMinMax = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
				MatchOperation matchTest = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail1.toArray(new Criteria[criteriaListTestFail1.size()])));
				LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
						.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
				UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
				GroupOperation groupOperationTest = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
						.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId");
				ProjectionOperation excludeIdTest = Aggregation.project().andExclude("_id");
				ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");
				MatchOperation matchZeroTest = Aggregation.match(Criteria.where("testFail").gt(0));
				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchForMinMax, matchTest, groupOperationTest,
							matchZeroTest, excludeIdTest);
				} else {
					List<Criteria> criteriaListforwafers = new ArrayList<>();
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
							lookupProductOperation, unwindProductInfo, matchTest, projectFetchWafers);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(matchForMinMax, match1, groupOperationTest, matchZeroTest,
							excludeIdTest);
				}
				measureResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class).getMappedResults();
				System.out.println(measureResults);
				TestRelatedMeasures = true;
				break;
			case "TestFailedRatioActualCount":
				throw new IOException("Selected combination is not supported");

			default:
				measureResults = generateCommonnComparisonMeassureForTest(waferIds, dimensionName, measureName, group,
						measureselectedBin, testName, testNumber, minVal, maxVal, aggregator, status, filterParams);
				TestRelatedMeasures = true;
				break;
			}
		}

		if (TestRelatedMeasures) {
			testResultsDocument = generateResultsForTestComparison(measureResults, dimensionResults, true, minVal,
					maxVal, filterParams);
			return testResultsDocument;
		} else {
			// throw new IOException("Selected combination is not supported");
			return measureResults;
		}

	}

	/*
	 * 
	 */

	private List<Document> generateCommonnComparisonDimensionForTest(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, int selectedBin, String testName, int testNumber, double minVal,
			double maxVal, String Aggregator, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsDimensionOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		logger.info("Preparing Query getComparisionHistogramDetails");
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && !filterParams.isEmpty()) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}

		GroupOperation groupOperation = null;

		addFieldsDimensionOperation = Aggregation.addFields().addFieldWithValue("$Dimension", "$" + Dimension)
				.addFieldWithValue("$Wafer", "$mesWafer._id").build();

		groupOperation = Aggregation.group("Wafer", "Dimension").min("mesWafer.WaferStartTime").as("WaferStartTime")
				.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
		projectFetchData = Aggregation.project().and("$_id.Wafer").as("waferId").and("$_id.Dimension").as("xAxis")
				.and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").and("$waferID")
				.as("waferID");
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsDimensionOperation == null) {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
		} else {

			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
		}
		logger.info("Preparing aggregationMean getComparisionHistogramDetails" + aggregationMean);
		return mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();
	}

	/*
	 * 
	 */

	private List<Document> generateCommonnComparisonMeassureForTest(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, int selectedBin, String testName, int testNumber, double minVal,
			double maxVal, String Aggregator, boolean status, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		logger.info("Preparing Query getComparisonHistogramDetails");
		List<Criteria> criteriaList = new ArrayList<Criteria>();

		if (waferIds.length == 0) {
			if (filterParams != null && !filterParams.isEmpty()) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (measureName.equals("NumberOfFailedChips") || measureName.equals("NumberOfChipsPerBin")) {
			if (Aggregator.equals("Avg")) {
				throw new IOException("Selected combination is not supported");
			}
		}
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension)) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
		GroupOperation groupOperation = null;
		String[] parts = Dimension.split("\\.");
		String dimension2 = parts[1];
		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];
		List<Document> keyResultsMean1 = null;
		List<Document> testFaileRatioDocuments = new ArrayList<>();
		logger.info("Preparing Query getComparisionHistogramDetails");
		List<Criteria> criteriaListTestFail = new ArrayList<>();
		List<Criteria> criteriaListTestFail1 = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && !filterParams.isEmpty()) {
				criteriaListTestFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

			}
		} else {
			criteriaListTestFail.add(Criteria.where("wd").in(waferIds));

		}
		criteriaListTestFail1.add(Criteria.where("tp").is(false));
		MatchOperation matchTest = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail1.toArray(new Criteria[criteriaListTestFail1.size()])));
		GroupOperation groupOperationTest = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
				.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId").sum("TestPass")
				.as("passChips");
		MatchOperation matchTestAuto = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
		ProjectionOperation excludeIdTest = Aggregation.project().andExclude("_id");

		MatchOperation matchZeroTest = Aggregation.match(Criteria.where("testFail").gt(0));
		Aggregation aggregation = null;
		MatchOperation matchBinNumber = Aggregation.match(Criteria.where("bn").is(selectedBin));
		List<Document> keyResultsMean11 = null;
		if (measureName.equals("NumberOfFailedChips")) {
			aggregation = Aggregation.newAggregation(matchTest, groupOperationTest, matchZeroTest, excludeIdTest);
		} else if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {
			if (measureName.equals("BinRatio")) {

				if (Aggregator.equals("Sum")) {
					throw new IOException("Selected combination is not supported");
				}
				AddFieldsOperation addFieldsOperation1 = Aggregation.addFields()
						.addFieldWithValue("$GroupingField", "$" + group)
						.addFieldWithValue("$DimensionField", "$" + dimensionName)
						.addFieldWithValue("$Wafer", "$mesWafer._id")
						.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();
				List<Criteria> criteriaList1 = new ArrayList<Criteria>();
				if (waferIds.length == 0) {
					if (filterParams != null && !filterParams.isEmpty()) {
						criteriaList1 = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaList1.add(Criteria.where("mesWafer._id").in(waferIds));

				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaList1.toArray(new Criteria[criteriaList1.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("Wafer").sum("TestChipCount")
						.as("TestChipCount");
				ProjectionOperation projectFetchData1 = Aggregation.project().and("$_id.Wafer").as("waferId")
						.and("TestChipCount").as("TestChipCount");
				ProjectionOperation excludedId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, addFieldsOperation1,
						groupOperationFailval, projectFetchData1);
				keyResultsMean11 = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
			if (waferIds.length > 0) {
				aggregation = Aggregation.newAggregation(matchTest, matchTestAuto, matchBinNumber, groupOperationTest,
						matchZeroTest, excludeIdTest);
				// lookupOperationTestHistory,unwindTestHistory,
			} else {
				List<Criteria> criteriaListforwafers = new ArrayList<>();
				ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");
				List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
						lookupProductOperation, unwindProductInfo, matchTestAuto, projectFetchWafers);

				MatchOperation match1 = Aggregation
						.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
				aggregation = Aggregation.newAggregation(matchTest, match1, matchBinNumber, groupOperationTest,
						matchZeroTest, excludeIdTest);
			}
		} else {
			throw new IOException("Selected combination is not supported");
		}
		List<Document> measureResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class)
				.getMappedResults();

		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			if (!keyResultsMean11.isEmpty() && !measureResults.isEmpty()) {
				Document val = new Document();

				int count2 = 0;
				int TestChip2 = 0;
				for (Document testval : measureResults) {
					for (Document results : keyResultsMean11) {

						if ((results.get("_id").toString().equals(testval.get("waferId").toString()))) {
							if (testval.containsKey("testFail") && results.containsKey("TestChipCount")) {
								int Average = Integer.parseInt(testval.get("testFail").toString());
								int TestChipCount = Integer.parseInt(results.get("TestChipCount").toString());
								double BinRatio = (double) Average / TestChipCount;
								BinRatio = BinRatio * 100;
								System.out.println(BinRatio + "d");
								testval.put("testFail", BinRatio);
								testval.put("WaferStartTime", results.get("WaferStartTime"));
								testval.put("WaferEndTime", results.get("WaferEndTime"));
								testval.put("waferID", results.get("waferId"));

							}
						}
					}
				}
			}
		}

		return measureResults;

	}

	/*
	 * 
	 */
	private List<Document> generateResultsForTestComparison(List<Document> measureResults,
			List<Document> dimensionResults, boolean flag, double minVal, double maxVal,
			List<FilterParam> filterParams) {

		List<Document> testResultsDocument = new ArrayList<>();
		if (!dimensionResults.isEmpty() && !measureResults.isEmpty()) {

			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("testNumber").toString() + "_" + results.get("testName").toString();

					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				double countDouble = 0;
				int count = 0;
				List<Date> listStartTime = new ArrayList<>();
				List<Date> listEndTime = new ArrayList<>();
				List<ObjectId> listwafers = new ArrayList<>();
				String xAxis = null;
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							if (results.get("testFail") instanceof Integer) {
								xAxis = null;
								count = (int) results.get("testFail");
								xAxis = (String) testval.get("xAxis");
								listStartTime.add((Date) testval.get("WaferStartTime"));
								listEndTime.add((Date) testval.get("WaferEndTime"));
								listwafers.add((ObjectId) testval.get("waferId"));

							} else {
								countDouble = (double) results.get("testFail");
								xAxis = (String) testval.get("xAxis");
								listStartTime.add((Date) testval.get("WaferStartTime"));
								listEndTime.add((Date) testval.get("WaferEndTime"));
								listwafers.add((ObjectId) testval.get("waferId"));
							}
						}
						if (count > 0 || countDouble > 0) {
							Document val = new Document();
							boolean flags = false;
							if (!testResultsDocument.isEmpty()) {
								for (Document testResult : testResultsDocument) {

									if (keyVal.equals(testResult.get("group"))
											&& (testResult.get("xAxis").toString().equals("[" + xAxis + "]"))) {
										flags = true;
										double yAxis = 0;
										if (count > 0) {
											yAxis = Array.getDouble(testResult.get("yAxis"), 0) + count;
										} else {
											yAxis = Array.getDouble(testResult.get("yAxis"), 0) + countDouble;
										}
										double[] yaxis = new double[1];
										yaxis[0] = yAxis;
										testResult.put("yAxis", yaxis);
									}
								}
							}

							String KeyValue = (String) keyVal;
							String[] Keyparts = KeyValue.split("\\$");
							val.put("group", Keyparts[0]);
							List<String> listDouble = new ArrayList<>();
							List<Integer> listInt = new ArrayList<>();
							if (testval.get("xAxis") instanceof Integer) {
								listInt.add((Integer) (testval.get("xAxis")));
								val.put("xAxis", listInt);
								val.put("WaferStartTime", listStartTime);
								val.put("WaferEndTime", listEndTime);
								val.put("waferID", listwafers);
							} else {
								listDouble.add((String) (testval.get("xAxis")));
								val.put("xAxis", listDouble);
								val.put("WaferStartTime", listStartTime);
								val.put("WaferEndTime", listEndTime);
								val.put("waferID", listwafers);
							}

							double[] ar = new double[1];
							if (count > 0) {
								ar[0] = count;
							} else {
								ar[0] = countDouble;
							}
							if (flags == false) {
								val.put("yAxis", ar);
								testResultsDocument.add(val);
							}
							count = 0;
							countDouble = 0;
						}
					}
				}
			});
		}

		List<Document> testResults = new ArrayList<>();
		for (Document testResult : testResultsDocument) {
			Document val = new Document();
			double yaxis = 0;
			yaxis = Array.getDouble(testResult.get("yAxis"), 0);
			if (minVal > 0 && maxVal == 0.0) {
				if (yaxis > minVal) {
					val.put("group", (testResult.get("group")));
					val.put("xAxis", (testResult.get("xAxis")));
					val.put("yAxis", (testResult.get("yAxis")));
					val.put("WaferStartTime", (testResult.get("WaferStartTime")));
					val.put("WaferEndTime", (testResult.get("WaferEndTime")));
					val.put("waferID", (testResult.get("waferID")));
					testResults.add(val);
				}
			} else if (minVal == 0.0 && maxVal > 0) {
				if (yaxis < maxVal) {
					val.put("group", (testResult.get("group")));
					val.put("xAxis", (testResult.get("xAxis")));
					val.put("yAxis", (testResult.get("yAxis")));
					val.put("WaferStartTime", (testResult.get("WaferStartTime")));
					val.put("WaferEndTime", (testResult.get("WaferEndTime")));
					val.put("waferID", (testResult.get("waferID")));
					testResults.add(val);

				}
			} else if (minVal == 0.0 && maxVal == 0.0) {
				val.put("group", (testResult.get("group")));
				val.put("xAxis", (testResult.get("xAxis")));
				val.put("yAxis", (testResult.get("yAxis")));
				val.put("WaferStartTime", (testResult.get("WaferStartTime")));
				val.put("WaferEndTime", (testResult.get("WaferEndTime")));
				val.put("waferID", (testResult.get("waferID")));
				testResults.add(val);

			} else {
				if (yaxis > minVal && yaxis < maxVal) {
					val.put("group", (testResult.get("group")));
					val.put("xAxis", (testResult.get("xAxis")));
					val.put("yAxis", (testResult.get("yAxis")));
					val.put("WaferStartTime", (testResult.get("WaferStartTime")));
					val.put("WaferEndTime", (testResult.get("WaferEndTime")));
					val.put("waferID", (testResult.get("waferID")));

					testResults.add(val);
				}
			}

		}
		return testResults;
	}

	/*
	 * List TestRelatedMeasures
	 */
	public List<Document> getTestRelatedMeasure(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
			String aggregator, String granuality, List<FilterParam> filterParams) throws IOException {
		if (testName == null) {
			throw new IOException("TestName is Empty");
		}
		if (testNumber < 0) {
			throw new IOException("TestName is Empty.");
		}
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension) && !grouping.equals("mesWafer.WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (Dimension.startsWith("$")) {
			Dimension = Dimension.split("\\$")[1];
		}
		AddFieldsOperation addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + grouping)
				.addFieldWithValue("$DimensionField", "$" + Dimension).addFieldWithValue("$Wafer", "$mesWafer._id")
				.build();
		List<Document> TestResult = null;
		switch (measureName) {
		case "TestPassChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestFields(waferIds, testNumber, testName, true, addFieldsOperation, granuality,
						Dimension, grouping, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestFields(waferIds, testNumber, testName, false, addFieldsOperation, granuality,
						Dimension, grouping, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatio":
			if (aggregator.equals("Avg")) {
				TestResult = generateTestFailedRatioFields(waferIds, testNumber, testName, false, grouping, granuality,
						Dimension, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatioActualCount":
			if (aggregator.equals("Avg")) {
				TestResult = generateWithActualCountFields(waferIds, testNumber, testName, grouping, granuality,
						Dimension, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		default:
			throw new IOException("Selected combination is not supported");
		}
		return TestResult;
	}

	private List<Document> generateTestFields(ObjectId[] waferIds, int testNumber, String testName, boolean status,
			AddFieldsOperation addFieldsOperation, String granuality, String Dimension, String grouping, double min,
			double max, List<FilterParam> filterParams) {
		logger.info("Preparing Query getComparisionHistogramDetails");

		String CollectionName = null;
		List<Document> keyResultsMean = null;
		List<Document> newDocu = new ArrayList<>();
		List<Criteria> criteriaListTestFail = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && !filterParams.isEmpty()) {
				criteriaListTestFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTestFail.add(Criteria.where("wd").in(waferIds));
		}
		Criteria testCriteriaFail = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(status));
		criteriaListForMinMax.add(testFailCriteria);

		Aggregation aggregationMean = null;
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
		GroupOperation groupOperation = Aggregation.group("$tn", "$tp", "$tm", "$wd").count().as("totalCount");
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId")
				.and("$totalCount").as("TestPass").andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");
		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
					projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation,
					projectFieldSelectionOperation);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

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

			AddFieldsOperation addFieldsOperationDateTimeMeasures = Aggregation.addFields()
					.addFieldWithValue("$DateField", DateOperators.dateOf("DimensionField").toString(dateFormat))
					.build();
			if (addFieldsOperationDateTimeMeasures != null) {
				List<Criteria> criteriaList = new ArrayList<>();
				if (filterParams != null && !filterParams.isEmpty()) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				} else {
					criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));
				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DateField", "Wafer")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DateField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
						.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID")
						.andExclude("_id");

				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, addFieldsOperationDateTimeMeasures,
						groupOperationFailval, projectFetchData);

				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		}

		else {
			if (addFieldsOperation != null) {
				List<Criteria> criteriaList = new ArrayList<>();
				if (filterParams != null && !filterParams.isEmpty()) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				} else {
					criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));
				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				GroupOperation groupOperationFail = Aggregation.group("GroupingField", "DimensionField", "Wafer")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				;
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DimensionField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
						.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID")
						.andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFail, projectFetchData);
				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		}
		logger.info("Preparing aggregationMean for getComparisionHistogramDetails" + aggregationMean);

		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (grouping.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						}
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				List<Date> listStartTime = new ArrayList<>();
				List<Date> listEndTime = new ArrayList<>();
				List<ObjectId> listWafers = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							listStartTime.add((Date) results.get("WaferStartTime"));
							listEndTime.add((Date) results.get("WaferEndTime"));
							listWafers.add((ObjectId) results.get("waferId"));
						}
					}
				}
				if (count > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					if (grouping != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
					}
					List<String> list1 = new ArrayList<>();
					list1.add((String) Keyparts[1]);
					val.put("xAxis", list1);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					val.put("waferID", listWafers);
					double[] arr = new double[1];

					if (min > 0 && max == 0.0) {
						if (count > min) {
							arr[0] = count;
							val.put("yAxis", arr);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (count < max) {
							arr[0] = count;
							val.put("yAxis", arr);
						}
					} else if (min == 0.0 && max == 0.0) {
						arr[0] = count;
						val.put("yAxis", arr);

					} else {
						if (count > min && count < max) {
							arr[0] = count;
							val.put("yAxis", arr);
						}
					}

					if (val.containsKey("yAxis")) {
						newDocu.add(val);
					}

				}
			});
		}
		return newDocu;
	}

	private List<Document> generateTestFailedRatioFields(ObjectId[] waferIds, int testNumber, String testName,
			boolean status, String group, String granuality, String Dimension, double min, double max,
			List<FilterParam> filterParams) {

		String CollectionName = null;
		List<Document> keyResultsMean = null;
		List<Document> testFaileRatioDocuments = new ArrayList<>();
		logger.info("Preparing Query getComparisionHistogramDetails ");
		List<Criteria> criteriaListTestFail = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && !filterParams.isEmpty()) {
				criteriaListTestFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTestFail.add(Criteria.where("wd").in(waferIds));
		}
		Criteria testCriteriaFail = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(status));
		criteriaListForMinMax.add(testFailCriteria);

		Aggregation aggregationMean = null;
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		AddFieldsOperation addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", "$" + group)
				.addFieldWithValue("$DimensionField", "$" + Dimension).addFieldWithValue("$Wafer", "$mesWafer._id")
				.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();
		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
		GroupOperation groupOperation = Aggregation.group("$tn", "$tp", "$tm", "$wd").count().as("totalCount");
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId")
				.and("$totalCount").as("TestPass").andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");
		logger.info("Prepary Query generateTestFailedRatioFields ");
		if (waferIds.length > 0) {

			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
					projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation,
					projectFieldSelectionOperation);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
		if (granuality != null && !"".equals(granuality)) {

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
			AddFieldsOperation addFieldsOperationDateTimeMeasures = Aggregation.addFields()
					.addFieldWithValue("$DateField", DateOperators.dateOf("DimensionField").toString(dateFormat))
					.build();
			if (addFieldsOperationDateTimeMeasures != null) {
				List<Criteria> criteriaList = new ArrayList<>();
				if (filterParams != null && !filterParams.isEmpty()) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				} else {
					criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));
				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DateField", "Wafer")
						.sum("TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime")
						.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DateField").as("xAxis").and("$_id.Wafer").as("waferId").andExclude("_id")
						.and("TestChipCount").as("TestChipCount").and("$WaferStartTime").as("WaferStartTime")
						.and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, addFieldsOperationDateTimeMeasures,
						groupOperationFailval, projectFetchData, excludeId);

				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		} else {
			if (addFieldsOperation != null) {

				List<Criteria> criteriaList = new ArrayList<>();
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				} else {
					criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));
				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DimensionField", "Wafer")
						.sum("TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime")
						.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DimensionField").as("xAxis").and("$_id.Wafer").as("waferId").andExclude("_id")
						.and("TestChipCount").as("TestChipCount").and("$WaferStartTime").as("WaferStartTime")
						.and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		}
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (group.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						}
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				int TestChip = 0;
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				List<ObjectId> listWafers = new ArrayList<ObjectId>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							TestChip += (int) results.get("TestChipCount");
							listStartTime.add((Date) results.get("WaferStartTime"));
							listEndTime.add((Date) results.get("WaferEndTime"));
							listWafers.add((ObjectId) results.get("waferId"));
						}
					}
				}
				if (count > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					if (group != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
					}
					List<String> list1 = new ArrayList<>();
					list1.add((String) Keyparts[1]);
					val.put("xAxis", list1);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					val.put("waferID", listWafers);
					double TestFailRatio = (double) count / TestChip;
					TestFailRatio = TestFailRatio * 100;
					double[] arr = new double[1];
					if (min > 0 && max == 0.0) {
						if (TestFailRatio > min) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (TestFailRatio < max) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					} else if (min == 0.0 && max == 0.0) {
						arr[0] = TestFailRatio;
						val.put("yAxis", arr);

					} else {
						if (TestFailRatio > min && TestFailRatio < max) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					}

					if (val.containsKey("yAxis")) {
						testFaileRatioDocuments.add(val);
					}
				}
			});
		}
		return testFaileRatioDocuments;
	}

	/*
	 * 
	 */
	private List<Document> generateWithActualCountFields(ObjectId[] waferIds, int testNumber, String testName,
			String group, String granuality, String dimension, double min, double max, List<FilterParam> filterParams) {
	

		String CollectionName = null;
		List<Document> keyResultsMean = null;
		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(true)).then(1)
				.otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(false)).then(1)
				.otherwise(0);

		List<Document> testActualResultDocuments = new ArrayList<Document>();
		Aggregation aggregationMean = null;
		ProjectionOperation projectFieldSelectionOperation = null;
		ProjectionOperation projectFieldTest = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("wd").in(waferIds));

		}
		Criteria testCriteria = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteria);
		Criteria testNameCriteria = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteria);

		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		AddFieldsOperation addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", "$" + group)
				.addFieldWithValue("$DimensionField", "$" + dimension).addFieldWithValue("$Wafer", "$mesWafer._id")
				.build();
		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		GroupOperation groupOperation = Aggregation.group("$tn", "$tm", "$wd").push("$tp").as("tp");
		UnwindOperation unwindtp = Aggregation.unwind("$tp", true);
		projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId").and(passChips).as("TestPass")
				.and(failChips).as("TestFail").andExclude("_id");
		GroupOperation groupOperationTest = Aggregation.group("waferId").count().as("totalCount").sum("TestPass")
				.as("passChips").sum("TestFail").as("failChips");
		projectFieldTest = Aggregation.project().and("_id").as("waferId").and("passChips").as("TestPass")
				.and("failChips").as("TestFail").andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");
		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
					unwindtp, projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, unwindtp,
					projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
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

			AddFieldsOperation addFieldsOperationDateTimeMeasures = Aggregation.addFields()
					.addFieldWithValue("$DateField", DateOperators.dateOf("DimensionField").toString(dateFormat))
					.build();
			if (addFieldsOperationDateTimeMeasures != null) {
				List<Criteria> criteriaListFail = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaListFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaListFail.add(Criteria.where("mesWafer._id").in(waferIds));

				}
				MatchOperation matchOperationTest = Aggregation.match(
						new Criteria().andOperator(criteriaListFail.toArray(new Criteria[criteriaListFail.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DateField", "Wafer")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DateField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
						.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID")
						.andExclude("_id");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, addFieldsOperationDateTimeMeasures,
						groupOperationFailval, projectFetchData, excludeId);
				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		} else {
			if (addFieldsOperation != null) {

				List<Criteria> criteriaListFail = new ArrayList<>();
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaListFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				} else {
					criteriaListFail.add(Criteria.where("mesWafer._id").in(waferIds));

				}
				MatchOperation matchOperationTest = Aggregation.match(
						new Criteria().andOperator(criteriaListFail.toArray(new Criteria[criteriaListFail.size()])));
				GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DimensionField", "Wafer")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
						.and("$_id.DimensionField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
						.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID")
						.andExclude("_id");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
				keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
			}
		}
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (group.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("xAxis").toString();
						}
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int TestPass = 0;
				int TestFail = 0;
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				List<ObjectId> listWafers = new ArrayList<ObjectId>();
				for (Document results : valu) {
					for (Document testval : testResults) {

						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							TestPass += (int) testval.get("TestPass");
							TestFail += (int) testval.get("TestFail");
							listStartTime.add((Date) results.get("WaferStartTime"));
							listEndTime.add((Date) results.get("WaferEndTime"));
							listWafers.add((ObjectId) results.get("waferId"));

						}
					}
				}
				if (TestPass > 0 || TestFail > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					if (group != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
					}
					List<String> list1 = new ArrayList<>();
					list1.add((String) Keyparts[1]);
					val.put("xAxis", list1);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					val.put("waferID", listWafers);
					int TotalVAl = TestFail + TestPass;
					double TestFailRatio = (double) TestFail / TotalVAl;
					TestFailRatio = TestFailRatio * 100;
					double[] arr = new double[1];
					if (min > 0 && max == 0.0) {
						if (TestFailRatio > min) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (TestFailRatio < max) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					} else if (min == 0.0 && max == 0.0) {
						arr[0] = TestFailRatio;
						val.put("yAxis", arr);

					} else {
						if (TestFailRatio > min && TestFailRatio < max) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);
						}
					}

					if (val.containsKey("yAxis")) {
						testActualResultDocuments.add(val);
					}

				}
			});
		}
		return testActualResultDocuments;
	}

	/*
	 * List DateTimeRelatedMeasure
	 */
	public List<Document> getDateTimeRelatedMeasure(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
			String Aggregator, String granuality, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsOperationForDimension = null;
		Aggregation aggregationMean = null;
		AddFieldsOperation addFieldsFilterOperation = null;
		MatchOperation FilterMinMax = null;
		ProjectionOperation projectFetchData = null;
		logger.info("Preparing Query getComparisionHistogramDetails");
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchZero = Aggregation.match(Criteria.where("yAxis").gt(0));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension)) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
		GroupOperation groupOperation = null;
		String[] parts = Dimension.split("\\.");
		String dimension2 = parts[1];
		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];
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
				.addFieldWithValue("$DateField", DateOperators.dateOf(Dimension).toString(dateFormat)).build();

		if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {
			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperationForDimension = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();

			if (measureName.equals("NumberOfChipsPerBin")) {
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$GroupingField")
						.build();

				groupOperation = Aggregation.group(grouping, "DateField").sum("Filterval").as("average")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + "DateField").as("xAxis")
							.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
					;
				} else {

					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + "DateField").as("xAxis").andArrayOf("$average").as("yAxis")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
					;
				}

			} else {
				groupOperation = Aggregation.group(grouping, "DateField").sum("GroupingField").as("average")
						.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
						.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id")
						.as("waferID");
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + "DateField").as("xAxis")
							.and("average").as("yAxis").and("TestChipCount").as("TestChipCount")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + "DateField").as("xAxis").and("average").as("yAxis")
							.and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				}
			}

		} else if (Aggregator.equals("Avg")) {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();

			groupOperation = Aggregation.group(grouping, "DateField").avg("Filterval").as("average")
					.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
					.addToSet("$mesWafer._id").as("waferID");
			if (grouping2.equals("_id")) {
				projectFetchData = Aggregation.project().andArrayOf("$_id." + "DateField").as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
						.andArrayOf("$_id." + "DateField").as("xAxis").andArrayOf("$average").as("yAxis")
						.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
						.as("WaferEndTime").and("$waferID").as("waferID");
			}
		} else {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();

			groupOperation = Aggregation.group(grouping, "DateField").sum("Filterval").as("average")
					.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
					.addToSet("$mesWafer._id").as("waferID");
			if (grouping2.equals("_id")) {
				projectFetchData = Aggregation.project().andArrayOf("$_id." + "DateField").as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
						.andArrayOf("$_id." + "DateField").as("xAxis").andArrayOf("$average").as("yAxis")
						.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
						.as("WaferEndTime").and("$waferID").as("waferID");
			}
		}

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsOperation == null) {

			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					groupOperation, projectFetchData, excludeId);

		} else {
			if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {
				if (measureName.equals("BinRatio")) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsOperationForDimension, groupOperation,
							projectFetchData, matchZero, excludeId);
				} else {
					if (minVal == 0.0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, addFieldsOperation, addFieldsOperationForDimension,
								addFieldsFilterOperation, groupOperation, projectFetchData, matchZero, excludeId);
					} else {
						if (minVal > 0 && maxVal == 0.0) {
							FilterMinMax = Aggregation
									.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
						} else if (minVal == 0.0 && maxVal > 0.0) {
							FilterMinMax = Aggregation
									.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
						} else if (minVal == 0.0 && maxVal == 0.0) {
							FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
						} else {
							FilterMinMax = Aggregation.match(
									new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
						}
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, addFieldsOperation, addFieldsOperationForDimension,
								addFieldsFilterOperation, groupOperation, FilterMinMax, projectFetchData, matchZero,
								excludeId);
					}
				}

			} else {
				if (minVal == 0.0 && maxVal == 0.0) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation,
							projectFetchData, matchZero, excludeId);
				} else {
					if (minVal > 0 && maxVal == 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
					} else if (minVal == 0.0 && maxVal > 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
					} else if (minVal == 0.0 && maxVal == 0.0) {
						FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
					} else {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
					}
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation, FilterMinMax,
							projectFetchData, excludeId);
				}
			}

		}

		logger.info("Preparing Aggregation getComparisionHistogramDetails" + aggregationMean);
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();
		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			for (Document document : testResults) {
				Document val = new Document();
				if (group != "mesWafer._id") {
					val.put("group", document.get("group"));
				}
				val.put("xAxis", document.get("xAxis"));
				val.put("WaferStartTime", document.get("WaferStartTime"));
				val.put("WaferEndTime", document.get("WaferEndTime"));
				val.put("waferID", document.get("waferID"));
				if (document.containsKey("yAxis") && document.containsKey("TestChipCount")) {
					int Average = Integer.parseInt(document.get("yAxis").toString());
					int TestChipCount = Integer.parseInt(document.get("TestChipCount").toString());
					double BinRatio = (double) Average / TestChipCount;
					BinRatio = BinRatio * 100;
					double[] arr = new double[1];
					if (BinRatio > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (BinRatio > minVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);

						} else {
							if (BinRatio > minVal && BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}
					}

				}
				if (val.containsKey("yAxis")) {
					binRatioResultDocuments.add(val);
				}
			}
			return binRatioResultDocuments;
		} else {
			return testResults;
		}
	}

	/*
	 * List DUT Service
	 */
	public List<Document> getDUTService(ObjectId[] waferIds, String groupingDim, String dimension, String measure,
			Integer chipsPerBin, double minVal, double maxVal, String Aggregator, List<FilterParam> filterParams)
			throws IOException {
		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(true))
				.then("$ChipCount").otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(false))
				.then("$ChipCount").otherwise(0);

		Aggregation aggregationMean = null;
		Aggregation aggregationDUTTest = null;
		GROUP_DUT = groupingDim;
		if (GROUP_DUT.equals("DUT")) {
			String dimension1 = dimension;
			String groupingVal = groupingDim;
			dimension = groupingVal;
			groupingDim = dimension1;
		}
		final String tempName = groupingDim;
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {

				String CollectionName = AdvancedGraphConstants.TEST_HISTORY_KEY;
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));

			}
		} else {
			criteriaList.add(Criteria.where("_id.waferID").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchTestOperation = Aggregation.match(Criteria.where("TestPass").is(false));
		MatchOperation matchBinOperation = Aggregation.match(Criteria.where("BINNumber").is(chipsPerBin));

		GroupOperation groupOperation = null;
		ProjectionOperation projectFieldTest = null;
		GroupOperation groupOperationTest = null;

		LookupOperation lookupOperationTestHistory = LookupOperation.newLookup().from(AdvancedGraphConstants.TEST_HISTORY_KEY)
				.localField("_id.waferID").foreignField("mesWafer._id").as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
				.localField("TestHistory.mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProduct = Aggregation.unwind("$ProductInfo", true);
		ProjectionOperation projectFieldSelectionOperation = null;

		String grouping = CriteriaUtil.generateFieldNameForCriteria(tempName);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimension);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension)) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measure);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}

		List<Document> dutResults = null;
		ProjectionOperation projectField = null;

		if (measure.equalsIgnoreCase("NumberOfChipsPerBin")) {
			dutResults = getNumberOfChips(grouping, Dimension, chipsPerBin, matchOperation, matchTestOperation,
					matchBinOperation, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					Aggregator);
			dutResults = extractDocument(grouping, measure, dutResults, chipsPerBin, minVal, maxVal);
			return dutResults;
		} else if (measure.equalsIgnoreCase("BinRatio")) {
			List<Document> binRatioDocument = new ArrayList<>();
			dutResults = getBinRatio(grouping, Dimension, chipsPerBin, matchOperation, matchTestOperation,
					matchBinOperation, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					Aggregator);
			dutResults = extractDocument(grouping, "NumberOfChipsBinRatio", dutResults, chipsPerBin, minVal, maxVal);

			List<Criteria> criteriaListWafer = new ArrayList<>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					String CollectionName = null;
					criteriaListWafer = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaListWafer.add(Criteria.where("waferID").in(waferIds));

			}

			MatchOperation matchOperationBinRatio = Aggregation.match(
					new Criteria().andOperator(criteriaListWafer.toArray(new Criteria[criteriaListWafer.size()])));

			groupOperation = Aggregation.group(dimension, "waferID").count().as("totalCount");
			projectFieldSelectionOperation = Aggregation.project().and("_id.DUTNumber").as("dimension")
					.and("_id.waferID").as("waferID").and("totalCount").as("totalCount").andExclude("_id");
			if (waferIds.length > 0) {
				aggregationDUTTest = Aggregation.newAggregation(matchOperationBinRatio, groupOperation,
						projectFieldSelectionOperation);
			} else {
				List<Criteria> criteriaListforwafers = new ArrayList<>();
				LookupOperation lookupProductAuto = LookupOperation.newLookup().from("ProductInfo")
						.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
				ProjectionOperation projectFetchWafer = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");
				Aggregation aggregationResults = Aggregation.newAggregation(lookupProductAuto, unwindProduct,
						matchOperationBinRatio, projectFetchWafer);
				List<Document> wafers = mongoTemplate.aggregate(aggregationResults, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
				ArrayList<String> wafersnew = new ArrayList<String>();

				for (Document doc : wafers) {
					wafersnew.add(doc.get("waferId").toString());
				}
				ObjectId[] waferIdsnew = new ObjectId[wafersnew.size()];
				for (int i = 0; i < wafersnew.size(); i++) {
					waferIdsnew[i] = new ObjectId(wafersnew.get(i));
				}
				criteriaListforwafers.add(Criteria.where("waferID").in(waferIdsnew));
				MatchOperation match1 = Aggregation.match(new Criteria()
						.andOperator(criteriaListforwafers.toArray(new Criteria[criteriaListforwafers.size()])));
				aggregationDUTTest = Aggregation.newAggregation(match1, groupOperation, projectFieldSelectionOperation);
			}
			List<Document> dutResultsBin = mongoTemplate.aggregate(aggregationDUTTest, "MESTest", Document.class)
					.getMappedResults();

			if (dutResults.size() > 0 && dutResultsBin.size() > 0) {

				Map<Object, List<Document>> comblistGrouped = dutResults.stream()
						.collect(Collectors.groupingBy(results -> {
							if (tempName.equals("mesWafer._id")) {
								return "$" + results.get("xAxis").toString();
							} else {
								return results.get("group").toString() + "$" + results.get("xAxis").toString();
							}
						}));

				comblistGrouped.forEach((keyVal, valu) -> {
					double count = 0;
					int count1 = 0;
					int testChipCount = 0;
					List<String> listStart = new ArrayList<>();
					List<String> listEnd = new ArrayList<>();
					List<ObjectId> listWaferIds = new ArrayList<>();
					for (Document results : valu) {
						for (Document totalCountBin : dutResultsBin) {

							if ((results.get("waferID").toString().equals(totalCountBin.get("waferID").toString()))) {
								if (results.get("yAxis") instanceof Integer) {

									count1 += (int) results.get("yAxis");
								} else {
									count += (double) results.get("yAxis");
								}
								testChipCount += (int) totalCountBin.get("totalCount");
								listStart = (List<String>) results.get("WaferStartTime");
								listEnd = (List<String>) results.get("WaferEndTime");
								listWaferIds = (List<ObjectId>) results.get("WaferID");
							}

						}

					}

					if (count > 0 || count1 > 0 || testChipCount > 0) {
						String KeyValue = (String) keyVal;
						String[] Keyparts = KeyValue.split("\\$");
						Document val = new Document();
						if (!tempName.equals("mesWafer._id")) {
							val.put("group", (String) Keyparts[0]);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
							val.put("waferID", listWaferIds);
						}
						List<String> list1 = new ArrayList<>();
						if (Keyparts.length > 1) {
							list1.add((String) Keyparts[1]);
						} else {
							list1.add("");
						}
						val.put("xAxis", list1);
						double Bin = 0;
						if (count > 0) {
							Bin = count;
						} else if (count1 > 0) {
							Bin = count1;
						}
						Bin = Bin * 100;
						double TestFailRatio = (double) Bin / testChipCount;
						double[] arr = new double[1];
						if (minVal > 0 && maxVal == 0.0) {
							if (TestFailRatio > minVal) {
								arr[0] = TestFailRatio;
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (TestFailRatio < maxVal) {
								arr[0] = TestFailRatio;
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr[0] = TestFailRatio;
							val.put("yAxis", arr);

						} else {
							if (TestFailRatio > minVal && TestFailRatio < maxVal) {
								arr[0] = TestFailRatio;
								val.put("yAxis", arr);
							}
						}

						if (val.containsKey("yAxis")) {
							binRatioDocument.add(val);
						}
					}

				});

			}

			return binRatioDocument;
		} else {
			groupOperation = Aggregation.group(Dimension, "mapID", "TestPass", "waferID").count().as("ChipCount");
			if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
				projectField = Aggregation.project().and("$" + grouping).as("grouping").and("$_id." + Dimension)
						.as("dimension").and(passChips).as("passChips").and(failChips).as("failChips")
						.and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY).andExclude("_id");
			} else {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + Dimension).as("dimension").and(passChips).as("passChips").and(failChips)
						.as("failChips").and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY).andExclude("_id");
			}
			if (Aggregator.equals("Avg")) {
				if (measure.equalsIgnoreCase("NumberOfFailedChips")
						|| measure.equalsIgnoreCase("NumberOfPassedChips")) {
					throw new IOException("Selected combination is not supported");
				}
				groupOperationTest = Aggregation.group("grouping", "dimension").avg("failChips").as("failChips")
						.avg("passChips").as("passChips").min("TestHistory.mesWafer.WaferStartTime")
						.as("WaferStartTime").max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime")
						.addToSet("TestHistory.mesWafer._id").as("waferID");
			} else {
				if (measure.equals("Yield")) {
					throw new IOException("Selected combination is not supported");
				}
				groupOperationTest = Aggregation.group("grouping", "dimension").sum("failChips").as("failChips")
						.sum("passChips").as("passChips").min("TestHistory.mesWafer.WaferStartTime")
						.as("WaferStartTime").max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime")
						.addToSet("TestHistory.mesWafer._id").as("waferID");
			}
			projectFieldTest = Aggregation.project().and("_id.grouping").as("grouping").andArrayOf("$_id.dimension")
					.as("dimension").and("passChips").as("passChips").and("failChips").as("failChips")
					.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime")
					.and("$waferID").as("waferID");
			aggregationMean = Aggregation.newAggregation(groupOperation, lookupOperationTestHistory, unwindTestHistory,
					lookupProduct, unwindProduct, matchOperation, projectField, groupOperationTest, projectFieldTest);
			dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
			dutResults = extractDocument(grouping, measure, dutResults, chipsPerBin, minVal, maxVal);
			return dutResults;
		}
	}

	/*
	 * List Number of Chips
	 */
	private List<Document> getNumberOfChips(String grouping, String dimension, Integer chipsPerBin,
			MatchOperation matchOperation, MatchOperation matchTestOperation, MatchOperation matchBinOperation,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String Aggregator) {
		Aggregation aggregationMean;
		GroupOperation groupOperation;
		GroupOperation groupOperationNext;
		ProjectionOperation projectFieldSelectionOperation;
		List<Document> dutResults;
		ProjectionOperation projectField;
		groupOperation = Aggregation.group(dimension, "mapID", "TestPass", "waferID").count().as("totalCount");
		if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
			projectField = Aggregation.project().and("$" + grouping).as("grouping").and("$_id." + dimension)
					.as("dimension").and("totalCount").as("totalCount").and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		} else {
			projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping").and("$_id." + dimension)
					.as("dimension").and("totalCount").as("totalCount").and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		}
		if (Aggregator.equals("Avg")) {
			groupOperationNext = Aggregation.group("grouping", "dimension").avg("totalCount").as("totalCount")
					.min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime").addToSet("TestHistory.mesWafer._id")
					.as("waferID");

		} else {
			groupOperationNext = Aggregation.group("grouping", "dimension").sum("totalCount").as("totalCount")
					.min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime").addToSet("TestHistory.mesWafer._id")
					.as("waferID");

		}
		projectFieldSelectionOperation = Aggregation.project().and("_id.grouping").as("grouping")
				.andArrayOf("$_id.dimension").as("dimension").and("totalCount").as("binCount")
				.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime")
				.and("$waferID").as("waferID").andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchTestOperation, matchBinOperation, groupOperation,
				lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct, matchOperation,
				projectField, groupOperationNext, projectFieldSelectionOperation);
		dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
		return dutResults;
	}

	private List<Document> extractDocument(String grouping, String measure, List<Document> dutResults,
			Integer chipsPerBin, double minVal, double maxVal) throws IOException {
		List<Document> dutResultDocuments = new ArrayList<>();
		for (Document obj : dutResults) {
			Document val = new Document();
			if (measure.equalsIgnoreCase("Yield")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				}
				double yield = 0.0;
				if (obj.get("passChips") != null) {
					if (obj.get("passChips") instanceof Integer) {
						Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");

						Integer passChip = (Integer) obj.get("passChips");
						yield = ((double) passChip / (double) totalCount) * 100;
					} else {
						double totalCount = (double) obj.get("passChips") + (double) obj.get("failChips");

						double passChip = (double) obj.get("passChips");
						yield = (passChip / totalCount) * 100;
					}

					double[] arr1 = new double[1];
					if (yield > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (yield > minVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (yield < maxVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr1[0] = yield;
							val.put("yAxis", arr1);

						} else {
							if (yield > minVal && yield < maxVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("FailRatio")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}

				} else {

					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				}

				double yield = 0.0;
				if (obj.get("failChips") != null) {

					double[] arr1 = new double[1];
					if (obj.get("failChips") instanceof Integer) {
						Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");

						Integer failChip = (Integer) obj.get("failChips");
						yield = ((double) failChip / (double) totalCount) * 100;
					} else {
						double totalCount = (double) obj.get("passChips") + (double) obj.get("failChips");

						double failChip = (double) obj.get("failChips");
						yield = (failChip / totalCount) * 100;
					}

					if (yield > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (yield > minVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (yield < maxVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr1[0] = yield;
							val.put("yAxis", arr1);

						} else {
							if (yield > minVal && yield < maxVal) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfPassedChips")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}

				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				}
				double[] arr1 = new double[1];
				if (obj.get("passChips") != null) {
					if (obj.get("passChips") instanceof Integer) {
						Integer passChip = (Integer) obj.get("passChips");
						arr1[0] = passChip;
					} else {
						double passChip = (double) obj.get("passChips");
						arr1[0] = Double.valueOf(passChip);
					}
					if (arr1[0] > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (arr1[0] > minVal) {
								val.put("yAxis", arr1);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (arr1[0] < maxVal) {
								val.put("yAxis", arr1);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							val.put("yAxis", arr1);

						} else {
							if (arr1[0] > minVal && arr1[0] < maxVal) {
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfFailedChips")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();

					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				}
				double[] arr1 = new double[1];
				if (obj.get("failChips") != null) {
					if (obj.get("failChips") instanceof Integer) {
						Integer failChip = (Integer) obj.get("failChips");
						arr1[0] = failChip;
					} else {
						double failChip = (double) obj.get("failChips");
						arr1[0] = Double.valueOf(failChip);
					}

					if (arr1[0] > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (arr1[0] > minVal) {
								val.put("yAxis", arr1);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (arr1[0] < maxVal) {
								val.put("yAxis", arr1);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							val.put("yAxis", arr1);

						} else {
							if (arr1[0] > minVal && arr1[0] < maxVal) {
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfChipsPerBin")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferID")));
					}
				}

				if (obj.get("binCount") != null) {
					double[] arr = new double[1];
					if (obj.get("binCount") instanceof Integer) {
						Integer binCount = (Integer) obj.get("binCount");
						arr[0] = binCount;
					} else {
						double binCount = (double) obj.get("binCount");
						arr[0] = Double.valueOf(binCount);
					}

					if (arr[0] > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (arr[0] > minVal) {
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (arr[0] < maxVal) {
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							val.put("yAxis", arr);

						} else {
							if (arr[0] > minVal && arr[0] < maxVal) {
								val.put("yAxis", arr);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("BinRatio")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> groupDUT = new ArrayList<String>();
					groupDUT.add(obj.get("grouping").toString());
					StringBuffer sb = new StringBuffer();
					List<Integer> dimensionList = (List<Integer>) obj.get("dimension");
					for (Integer dimension : dimensionList) {
						sb.append(dimension);
					}
					String dimensionDUT = sb.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferId")));
					} else {
						val.put("xAxis", groupDUT);
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferId")));
					}

				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferId")));
					} else {
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("waferID", (obj.get("waferId")));
					}
				}

				if (obj.get("binCount") != null) {
					double[] arr = new double[1];
					if (obj.get("binCount") instanceof Integer) {
						Integer binCount = (Integer) obj.get("binCount");
						arr[0] = binCount;
					} else {
						double binCount = (double) obj.get("binCount");
						arr[0] = Double.valueOf(binCount);
					}
					if (arr[0] > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (arr[0] > minVal) {
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (arr[0] < maxVal) {
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							val.put("yAxis", arr);

						} else {
							if (arr[0] > minVal && arr[0] < maxVal) {
								val.put("yAxis", arr);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfChipsBinRatio")) {
				if (GROUP_DUT.equals("DUT")) {
					Integer dimensionList = (Integer) obj.get("dimension");
					String dimensionDUT = dimensionList.toString();
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", dimensionDUT);
						val.put("xAxis", obj.get("grouping").toString());
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("WaferID", (obj.get("binWaferId")));
					} else {
						val.put("xAxis", obj.get("grouping").toString());
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("WaferID", (obj.get("binWaferId")));
					}

				} else {
					if (!grouping.equals("mesWafer._id")) {
						val.put("group", obj.get("grouping"));
						val.put("xAxis", obj.get("dimension"));
						val.put("WaferStartTime", (obj.get("WaferStartTime")));
						val.put("WaferEndTime", (obj.get("WaferEndTime")));
						val.put("WaferID", (obj.get("binWaferId")));
					} else {
						if (!grouping.equals("mesWafer._id")) {
							val.put("group", obj.get("grouping"));
							val.put("xAxis", obj.get("dimension"));
							val.put("WaferStartTime", (obj.get("WaferStartTime")));
							val.put("WaferEndTime", (obj.get("WaferEndTime")));
							val.put("WaferID", (obj.get("binWaferId")));
						} else {
							val.put("xAxis", obj.get("dimension"));
							val.put("WaferStartTime", (obj.get("WaferStartTime")));
							val.put("WaferEndTime", (obj.get("WaferEndTime")));
							val.put("WaferID", (obj.get("binWaferId")));
						}
					}
				}
				val.put("waferID", obj.get("WaferId"));

				if (obj.get("binCount") != null) {
					if (obj.get("binCount") instanceof Integer) {
						int binCount = (int) obj.get("binCount");
						val.put("yAxis", binCount);
					} else {
						double binCount = (double) obj.get("binCount");
						val.put("yAxis", binCount);
					}
				}
			} else {
				throw new IOException("Selected combination is not supported");
			}
			if (val.containsKey("yAxis")) {
				dutResultDocuments.add(val);
			}
		}
		return dutResultDocuments;
	}

	private List<Document> getBinRatio(String grouping, String dimension, Integer chipsPerBin,
			MatchOperation matchOperation, MatchOperation matchTestOperation, MatchOperation matchBinOperation,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String Aggregator) {
		Aggregation aggregationMean;
		GroupOperation groupOperation;
		GroupOperation groupOperationNext;
		ProjectionOperation projectFieldSelectionOperation;
		List<Document> dutResults;
		ProjectionOperation projectField;
		groupOperation = Aggregation.group(dimension, "mapID", "TestPass", "waferID").count().as("totalCount");
		if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
			projectField = Aggregation.project().and("$" + grouping).as("grouping").and("$_id." + dimension)
					.as("dimension").and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
					.and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		} else {
			projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping").and("$_id." + dimension)
					.as("dimension").and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
					.and(AdvancedGraphConstants.TEST_HISTORY_KEY).as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		}
		if (Aggregator.equals("Avg")) {
			groupOperationNext = Aggregation.group("grouping", "dimension", "$waferID").avg("totalCount")
					.as("totalCount").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

		} else {
			groupOperationNext = Aggregation.group("grouping", "dimension", "$waferID").sum("totalCount")
					.as("totalCount").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
		}
		projectFieldSelectionOperation = Aggregation.project().and("_id.grouping").as("grouping").and("$_id.dimension")
				.as("dimension").and("totalCount").as("binCount").and("waferID").as("WaferId")
				.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime")
				.andArrayOf("$_id.waferID").as("binWaferId").andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchTestOperation, matchBinOperation, groupOperation,
				lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct, matchOperation,
				projectField, groupOperationNext, projectFieldSelectionOperation);
		dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
		return dutResults;
	}

	/*
	 * GroupByBins for all
	 * NumberOfChipsPerBin,NumberOfFailedChips,NumberOfPassedChips,TestPassChips,
	 * BinRatio
	 */
	public List<Document> getGroupByBin(ObjectId[] waferIds, String measureName, String group, String dimensionName,
			String testName, int testNumber, double min, double max, String Aggregator, List<FilterParam> filterParams)
			throws IOException {

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		List<Document> testResults = new ArrayList<Document>();
		handleAxisExceptions(grouping, Dimension, MeasureVal);
		boolean binGroupingCountableMeasures = Arrays.stream(binGroupingList).anyMatch(measureName::equals);

		if (Dimension.equals("DUTNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (binGroupingCountableMeasures) {
			if (Aggregator.equals("Sum")) {
				testResults = getBinGroupingCountableMeasures(waferIds, measureName, testName, testNumber, grouping,
						Dimension, min, max, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
		} else if (measureName.equals(BIN_RATIO)) {
			if (Aggregator.equals("Avg")) {
				testResults = getBinRatioBins(waferIds, grouping, Dimension, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
		} else {
			throw new IOException("Selected combination is not supported");
		}

		return getMeasureCount(measureName, testResults, min, max);
	}

	/*
	 * To calculate measure count values
	 */
	public List<Document> getMeasureCount(String measureName, List<Document> testResults, double min, double max) {
		List<Document> test = new ArrayList<Document>();
		for (Document testIterate : testResults) {
			Integer testPassBINNumber = (Integer) testIterate.get("TestPassBINNumber");
			Integer binNumber = (Integer) testIterate.get("BINNumber");

			if (!measureName.equalsIgnoreCase("TestPassChips") && measureName.equalsIgnoreCase("NumberOfPassedChips")
					&& binNumber == 20) {
				Document document = new Document();
				getBinCounts(testIterate, document, measureName, min, max);
				if (document.containsKey("yAxis")) {
					test.add(document);
				}
			} else if (!measureName.equalsIgnoreCase("TestPassChips")
					&& measureName.equalsIgnoreCase("NumberOfFailedChips") && binNumber != testPassBINNumber) {
				Document document = new Document();
				getBinCounts(testIterate, document, measureName, min, max);
				if (document.containsKey("yAxis")) {
					test.add(document);
				}
			} else if (!measureName.equalsIgnoreCase("TestPassChips")
					&& !measureName.equalsIgnoreCase("NumberOfFailedChips")
					&& !measureName.equalsIgnoreCase("NumberOfPassedChips") && binNumber != 20) {
				Document document = new Document();
				getBinCounts(testIterate, document, measureName, min, max);
				if (document.containsKey("yAxis")) {
					if (measureName.equalsIgnoreCase("BinRatio")) {
						if (document.get("yAxis") instanceof Integer) {
							int yAxis = (Integer) document.get("yAxis");
							if (yAxis > 0) {
								test.add(document);
							}
						}
					} else {
						test.add(document);
					}
				}
			}
		}
		if (measureName.equalsIgnoreCase("TestPassChips")) {
			test.addAll(testResults);
		}
		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			for (Document document : test) {
				Document val = new Document();
				val.put("group", document.get("group"));
				val.put("xAxis", document.get("xAxis"));
				val.put("WaferStartTime", document.get("WaferStartTime"));
				val.put("WaferEndTime", document.get("WaferEndTime"));
				val.put("waferID", document.get("waferID"));
				if (document.containsKey("yAxis") && document.containsKey("TestChipCount")) {
					int Average = Integer.parseInt(document.get("yAxis").toString());
					int TestChipCount = Integer.parseInt(document.get("TestChipCount").toString());
					double BinRatio = (double) Average / TestChipCount;
					BinRatio = BinRatio * 100;
					System.out.println(BinRatio + "d");
					double[] arr = new double[1];
					if (min > 0 && max == 0.0) {
						if (BinRatio > min) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (BinRatio < max) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);
						}
					} else if (min == 0.0 && max == 0.0) {
						arr[0] = BinRatio;
						val.put("yAxis", arr);

					} else {
						if (BinRatio > min && BinRatio < max) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);
						}
					}
				}
				if (val.containsKey("yAxis")) {
					binRatioResultDocuments.add(val);
				}
			}
			return binRatioResultDocuments;
		} else {
			return test;
		}
	}

	/*
	 * GroupBy Bin Exception Handling
	 */
	private void handleAxisExceptions(String grouping, String Dimension, String MeasureVal) throws IOException {
		if (grouping == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension)) {
			throw new IOException("Selected combination is not supported");
		}
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
	}

	/*
	 * GroupByBins for all
	 * NumberOfChipsPerBin,NumberOfFailedChips,NumberOfPassedChips,TestPassChips
	 */
	private List<Document> getBinGroupingCountableMeasures(ObjectId[] waferIds, String measureName, String testName,
			int testNumber, String grouping, String Dimension, double min, double max, List<FilterParam> filterParams) {

		String CollectionName = null;
		List<Document> testHistoryResults = new ArrayList<Document>();
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		UnwindOperation unwindBinNumber = null;
		unwindBinNumber = Aggregation.unwind("$mesWafer.BinSummary.BinCount", false);
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		String[] groupingArray = grouping.split("\\.");
		String[] dimensionArray = Dimension.split("\\.");
		String dimensionSplit = dimensionArray[1];
		Aggregation aggregationMean;
		ProjectionOperation projectFetchData;
		GroupOperation groupOperation;
		List<Document> testResults;
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		groupOperation = Aggregation
				.group(Fields.fields().and(groupingArray[3].toString(), grouping)
						.and(Fields.fields().and(dimensionSplit, Dimension)))
				.first("$mesWafer.BinSummary.BinCount.BINNumber").as("BINNumber")
				.first("$mesWafer.BinSummary.TestPassBINNumber").as("TestPassBINNumber").first(Dimension)
				.as(dimensionSplit).push("$mesWafer._id").as("waferId").sum("$mesWafer.BinSummary.BinCount.Count")
				.as("average").min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
				.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
		projectFetchData = Aggregation.project().and("waferId").as("waferId").and("BINNumber").as("BINNumber")
				.and("TestPassBINNumber").as("TestPassBINNumber").and("_id.BINNumber").as("group")
				.andArrayOf("$" + dimensionSplit).as("xAxis").andArrayOf("$average").as("yAxis")
				.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime")
				.and("$waferID").as("waferID");

		aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
				unwindBinNumber, groupOperation, projectFetchData, excludeId);

		testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class).getMappedResults();

		if (measureName.equals("TestPassChips")) {
			testResults = getGroupbyBinTestPassChips(waferIds, testName, testNumber, testHistoryResults, testResults,
					min, max, filterParams);
		}
		return testResults;
	}

	/*
	 * get BinRatio by group by bin
	 */
	private List<Document> getBinRatioBins(ObjectId[] waferIds, String grouping, String Dimension,
			List<FilterParam> filterParams) {

		String CollectionName = null;
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		UnwindOperation unwindBinNumber = null;
		unwindBinNumber = Aggregation.unwind("$mesWafer.BinSummary.BinCount", false);
		String[] groupingArray = grouping.split("\\.");
		String[] dimensionArray = Dimension.split("\\.");
		String dimensionSplit = dimensionArray[1];
		Aggregation aggregationMean;
		ProjectionOperation projectFetchData;
		GroupOperation groupOperation;
		List<Document> testResults;
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		groupOperation = Aggregation
				.group(Fields.fields().and(groupingArray[3].toString(), grouping)
						.and(Fields.fields().and(dimensionSplit, Dimension)))
				.first("$mesWafer.BinSummary.BinCount.BINNumber").as("BINNumber").first(Dimension).as(dimensionSplit)
				.first("$mesWafer._id").as("waferId").sum("$mesWafer.BinSummary.BinCount.Count").as("average")
				.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime")
				.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
		projectFetchData = Aggregation.project().and("waferId").as("waferId").and("BINNumber").as("BINNumber")
				.and("_id.BINNumber").as("group").andArrayOf("$" + dimensionSplit).as("xAxis").and("$average")
				.as("yAxis").and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime").as("WaferStartTime")
				.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");

		aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
				unwindBinNumber, groupOperation, projectFetchData, excludeId);
		testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class).getMappedResults();
		return testResults;
	}

	/*
	 * Calculate bin count group by bin
	 */
	private void getBinCounts(Document testIterate, Document document, String measureName, double min, double max) {
		// document.put("BINCount", testIterate.get("BINCount"));

		// document.put("waferId", testIterate.get("waferId"));
		if (measureName.equalsIgnoreCase("BinRatio")) {
			document.put("yAxis", testIterate.get("yAxis"));
			document.put("group", testIterate.get("group"));
			document.put("xAxis", testIterate.get("xAxis"));
			document.put("WaferStartTime", testIterate.get("WaferStartTime"));
			document.put("WaferEndTime", testIterate.get("WaferEndTime"));
			document.put("TestChipCount", testIterate.get("TestChipCount"));
			document.put("waferID", testIterate.get("waferID"));
		} else {
			document.put("group", testIterate.get("group"));
			document.put("xAxis", testIterate.get("xAxis"));
			document.put("WaferStartTime", testIterate.get("WaferStartTime"));
			document.put("WaferEndTime", testIterate.get("WaferEndTime"));
			document.put("waferID", testIterate.get("waferID"));
			List<?> list1 = (List<?>) testIterate.get("yAxis");

			double[] arr = new double[1];
			if (list1.get(0) instanceof Integer) {
				int Yaxis = (int) list1.get(0);
				if (Yaxis > 0) {
					if (min > 0 && max == 0.0) {
						if (Yaxis > min) {
							arr[0] = Double.valueOf(Yaxis);
							document.put("yAxis", testIterate.get("yAxis"));
						}
					}

					else if (min == 0.0 && max > 0) {
						if (Yaxis < max) {
							arr[0] = Double.valueOf(Yaxis);
							document.put("yAxis", testIterate.get("yAxis"));
						}
					} else if (min == 0.0 && max == 0.0) {
						arr[0] = Double.valueOf(Yaxis);
						document.put("yAxis", testIterate.get("yAxis"));

					} else {
						if (Yaxis > min && Yaxis < max) {
							arr[0] = Double.valueOf(Yaxis);
							document.put("yAxis", testIterate.get("yAxis"));
						}
					}

				}
			} else {
				double Yaxis = (double) list1.get(0);
				if (min > 0 && max == 0.0) {
					if (Yaxis > min) {
						arr[0] = Double.valueOf(Yaxis);
						document.put("yAxis", testIterate.get("yAxis"));
					}
				}

				else if (min == 0.0 && max > 0) {
					if (Yaxis < max) {
						arr[0] = Double.valueOf(Yaxis);
						document.put("yAxis", testIterate.get("yAxis"));
					}
				} else if (min == 0.0 && max == 0.0) {
					arr[0] = Double.valueOf(Yaxis);
					document.put("yAxis", testIterate.get("yAxis"));

				} else {
					if (Yaxis > min && Yaxis < max) {
						arr[0] = Double.valueOf(Yaxis);
						document.put("yAxis", testIterate.get("yAxis"));
					}
				}

			}

		}
	}

	/*
	 * To getGroupbyBinTestPassChips
	 */
	public List<Document> getGroupbyBinTestPassChips(ObjectId[] waferIds, String testName, int testNumber,
			List<Document> testHistoryResults, List<Document> testResults, double min, double max,
			List<FilterParam> filterParams) {

		String CollectionName = null;
		Aggregation aggregationMean;
		GroupOperation groupOperation;
		List<Criteria> criteriaListTestFail = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaListTestFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTestFail.add(Criteria.where("wd").in(waferIds));

		}
		Criteria testCriteriaFail = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(true));
		criteriaListForMinMax.add(testFailCriteria);
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
		groupOperation = Aggregation.group("$wd", "$bn").first("$wd").as("waferId").count().as("testPassCount");
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId")
				.and("_id.bn").as("bn").and("$testPassCount").as("TestPass").andExclude("_id");
		ProjectionOperation projectFetchData = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
					projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchData);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation,
					projectFieldSelectionOperation);
		}
		List<Document> testPassResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
		boolean isTestPassExists = false;
		for (Document testResult : testResults) {
			Integer count = 0;
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (Document testPassResult : testPassResults) {
				List<ObjectId> testHistory = (List<ObjectId>) testResult.get("waferId");
				ObjectId testResultObj = (ObjectId) testPassResult.get("waferId");
				for (ObjectId testItem : testHistory) {
					if (testItem.equals(testResultObj)
							&& testResult.get("BINNumber").equals(testPassResult.get("bn"))) {
						count = count + (Integer) testPassResult.get("TestPass");
						isTestPassExists = true;
					}
				}
			}
			testResult.remove("yAxis");
			if (count > 0) {
				if (min > 0 && max == 0.0) {
					if (count > min) {
						list.add(count);

						testResult.put("yAxis", list);
					}
				}

				else if (min == 0.0 && max > 0) {
					if (count < max) {
						list.add(count);

						testResult.put("yAxis", list);
					}
				} else if (min == 0.0 && max == 0.0) {
					list.add(count);

					testResult.put("yAxis", list);
				} else {
					if (count > min && count < max) {
						list.add(count);

						testResult.put("yAxis", list);
					}
				}
			}
			if (testResult.containsKey("yAxis")) {
				testHistoryResults.add(testResult);
			}
		}
		testResults = new ArrayList<Document>();
		testResults.addAll(testHistoryResults);
		if (!isTestPassExists) {
			testResults = new ArrayList<Document>();
		}
		return testResults;
	}

	/*
	 * List CommonDimensions And Measures
	 */
	public List<Document> getCommonDimensionsAndMeasure(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
			String Aggregator, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		AddFieldsOperation addFieldsFilterOperation = null;
		AggregationOperation FilterMinMax = null;
		logger.info("Preparing Query getComparisonHistogramDetails");
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchZero = Aggregation.match(Criteria.where("yAxis").gt(0));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension) && !grouping.equals("mesWafer.WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}

		GroupOperation groupOperation = null;
		String[] parts = Dimension.split("\\.");
		String dimension2 = parts[1];
		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];
		if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();
			if (measureName.equals("NumberOfChipsPerBin")) {
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$GroupingField")
						.build();
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					groupOperation = Aggregation.group(grouping).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
							.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
						projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
								.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
								.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
					} else {
						projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
								.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
								.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
								.as("WaferEndTime").and("$waferID").as("waferID");
					}
				}
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					groupOperation = Aggregation.group(grouping).sum("GroupingField").as("average")
							.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
							.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
							.addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension).sum("GroupingField").as("average")
							.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
							.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
							.addToSet("$mesWafer._id").as("waferID");
				}
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis").and("average")
							.as("yAxis").and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime")
							.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID")
							.as("waferID");
				} else {

					if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
						projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
								.and("average").as("yAxis").and("TestChipCount").as("TestChipCount")
								.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
								.as("WaferEndTime").and("$waferID").as("waferID");
					} else {
						projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
								.andArrayOf("$_id." + dimension2).as("xAxis").and("average").as("yAxis")
								.and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime")
								.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID")
								.as("waferID");
					}
				}
			}
		} else if (Aggregator.equals("Avg")) {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
				groupOperation = Aggregation.group(grouping).avg("Filterval").as("average")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			} else {
				groupOperation = Aggregation.group(grouping, Dimension).avg("$Filterval").as("average")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			}
			if (grouping2.equals("_id")) {
				projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				;
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
							.andArrayOf("$average").as("yAxis").and("$WaferStartTime").as("WaferStartTime")
							.and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
				}
			}
		} else {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
				groupOperation = Aggregation.group(grouping).sum("Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			} else {
				groupOperation = Aggregation.group(grouping, Dimension).sum("$Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			}
			if (grouping2.equals("_id")) {

				projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
							.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
				}
			}
		}
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsOperation == null) {
			if (minVal == 0.0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, groupOperation, projectFetchData, matchZero, excludeId);
			} else {
				if (minVal > 0 && maxVal == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
				} else if (minVal == 0.0 && maxVal > 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
				} else if (minVal == 0.0 && maxVal == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
				} else {
					FilterMinMax = Aggregation
							.match(new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
				}
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, groupOperation, FilterMinMax, projectFetchData, matchZero, excludeId);
			}
		} else {
			if (measureName.equals("BinRatio")) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsOperation, groupOperation, projectFetchData, matchZero, excludeId);
			} else {
				if (minVal == 0.0 && maxVal == 0.0) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation,
							projectFetchData, matchZero, excludeId);
				} else {
					if (minVal > 0 && maxVal == 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
					} else if (minVal == 0.0 && maxVal > 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
					} else if (minVal == 0.0 && maxVal == 0.0) {
						FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
					} else {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
					}
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation, FilterMinMax,
							projectFetchData, matchZero, excludeId);
				}
			}
		}
		logger.info("Preparing aggregationMean getComparisionHistogramDetails" + aggregationMean);
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();
		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			for (Document document : testResults) {
				Document val = new Document();
				if (group != "mesWafer._id") {
					val.put("group", document.get("group"));
				}
				val.put("xAxis", document.get("xAxis"));
				val.put("WaferStartTime", document.get("WaferStartTime"));
				val.put("WaferEndTime", document.get("WaferEndTime"));
				val.put("waferID", document.get("waferID"));
				if (document.containsKey("yAxis") && document.containsKey("TestChipCount")) {
					int Average = Integer.parseInt(document.get("yAxis").toString());
					int TestChipCount = Integer.parseInt(document.get("TestChipCount").toString());
					double BinRatio = (double) Average / TestChipCount;
					BinRatio = BinRatio * 100;
					System.out.println(BinRatio + "d");
					double[] arr = new double[1];
					if (BinRatio > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (BinRatio > minVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);

						} else {
							if (BinRatio > minVal && BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}

					}
				}
				if (val.containsKey("yAxis")) {
					binRatioResultDocuments.add(val);
				}
			}
			return binRatioResultDocuments;
		} else {
			return testResults;
		}
	}

	public List<Document> getCommonDimensionsAndMeasureHisto(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, int selectedBin, String testName, int testNumber, double minVal,
			double maxVal, String Aggregator, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		AddFieldsOperation addFieldsFilterOperation = null;
		AggregationOperation FilterMinMax = null;
		List<Criteria> criteriaList = new ArrayList<>();
		logger.info("Preparing Query getComparisonHistogramDetails");
		if (filterParams != null && filterParams.size() >= 1) {
			criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchZero = Aggregation.match(Criteria.where("yAxis").gt(0));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension) && !grouping.equals("mesWafer.WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}

		GroupOperation groupOperation = null;
		GroupOperation gropupOperationFilter = null;
		String[] parts = Dimension.split("\\.");
		String dimension2 = parts[1];
		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];
		if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();
			if (measureName.equals("NumberOfChipsPerBin")) {
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$GroupingField")
						.build();
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					groupOperation = Aggregation.group(grouping).sum("Filterval").as("average")
							.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");

				} else {
					groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
							.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
							.andArrayOf("$average").as("yAxis").and("$WaferStartTime").as("WaferStartTime")
							.and("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
						projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
								.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
								.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
					} else {
						projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
								.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
								.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
								.as("WaferEndTime").and("$waferID").as("waferID");
					}
				}
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					groupOperation = Aggregation.group(grouping).sum("GroupingField").as("average")
							.sum("$mesWafer.TestChipCount").as("TestChipCount").min("$mesWafer.WaferStartTime")
							.as("WaferStartTime").max("$mesWafer.WaferEndTime").as("WaferEndTime")
							.addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension).sum("GroupingField").as("average")
							.sum("$mesWafer.TestChipCount").as("TestChipCount").min("$mesWafer.WaferStartTime")
							.as("WaferStartTime").max("$mesWafer.WaferEndTime").as("WaferEndTime")
							.addToSet("$mesWafer._id").as("waferID");
				}
				if (grouping2.equals("_id")) {
					projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis").and("average")
							.as("yAxis").and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime")
							.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID")
							.as("waferID");
				} else {
					if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
						projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
								.and("average").as("yAxis").and("TestChipCount").as("TestChipCount")
								.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
								.as("WaferEndTime").and("$waferID").as("waferID");
					} else {
						projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
								.andArrayOf("$_id." + dimension2).as("xAxis").and("average").as("yAxis")
								.and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime")
								.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID")
								.as("waferID");
					}
				}
			}
			gropupOperationFilter = Aggregation.group("$_id." + dimension2, "$average").sum("average").as("average");
		} else if (Aggregator.equals("Avg")) {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
				groupOperation = Aggregation.group(grouping).avg("Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			} else {
				groupOperation = Aggregation.group(grouping, Dimension).avg("$Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			}
			if (grouping2.equals("_id")) {
				projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
							.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
				}
			}
			gropupOperationFilter = Aggregation.group("$_id." + dimension2, "$average").avg("average").as("average");
		} else {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
				groupOperation = Aggregation.group(grouping).sum("Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			} else {
				groupOperation = Aggregation.group(grouping, Dimension).sum("$Filterval").as("average")
						.min("$mesWafer.WaferStartTime").as("WaferStartTime").max("$mesWafer.WaferEndTime")
						.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			}
			if (grouping2.equals("_id")) {

				projectFetchData = Aggregation.project().andArrayOf("$_id." + dimension2).as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				if ((grouping2.equals(dimension2) && grouping2.equals("WaferNumber"))) {
					projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$_id").as("xAxis")
							.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
							.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
				} else {
					projectFetchData = Aggregation.project().and("_id." + grouping2).as("group")
							.andArrayOf("$_id." + dimension2).as("xAxis").andArrayOf("$average").as("yAxis")
							.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
							.as("WaferEndTime").and("$waferID").as("waferID");
				}
			}
			gropupOperationFilter = Aggregation.group("$_id." + dimension2, "$average").sum("average").as("average");
		}
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsOperation == null) {
			if (minVal == 0.0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, groupOperation, projectFetchData, matchZero, excludeId);
			} else {
				if (minVal > 0 && maxVal == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
				} else if (minVal == 0.0 && maxVal > 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
				} else if (minVal == 0.0 && maxVal == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
				} else {
					FilterMinMax = Aggregation
							.match(new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
				}
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, groupOperation, FilterMinMax, projectFetchData, matchZero, excludeId);
			}
		} else {
			if (measureName.equals("BinRatio")) {

				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsOperation, groupOperation, projectFetchData, matchZero, excludeId);
			} else {
				if (minVal == 0.0 && maxVal == 0.0) {

					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation,
							projectFetchData, matchZero, excludeId);
				} else {
					if (minVal > 0 && maxVal == 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal)));
					} else if (minVal == 0.0 && maxVal > 0.0) {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").lte(maxVal)));
					} else if (minVal == 0.0 && maxVal == 0.0) {
						FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("average")));
					} else {
						FilterMinMax = Aggregation
								.match(new Criteria().andOperator(Criteria.where("average").gte(minVal).lte(maxVal)));
					}

					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation, FilterMinMax,
							projectFetchData, matchZero, excludeId);
				}
			}
		}
		logger.info("Preparing aggregationMean getComparisionHistogramDetails" + aggregationMean);
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();
		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			for (Document document : testResults) {
				Document val = new Document();
				if (group != "mesWafer._id") {
					val.put("group", document.get("group"));
				}
				val.put("xAxis", document.get("xAxis"));
				val.put("WaferStartTime", document.get("WaferStartTime"));
				val.put("WaferEndTime", document.get("WaferEndTime"));
				val.put("waferID", document.get("waferID"));
				if (document.containsKey("yAxis") && document.containsKey("TestChipCount")) {
					int Average = Integer.parseInt(document.get("yAxis").toString());
					int TestChipCount = Integer.parseInt(document.get("TestChipCount").toString());
					double BinRatio = (double) Average / TestChipCount;
					BinRatio = BinRatio * 100;
					System.out.println(BinRatio + "d");
					double[] arr = new double[1];
					if (BinRatio > 0) {
						if (minVal > 0 && maxVal == 0.0) {
							if (BinRatio > minVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}

						else if (minVal == 0.0 && maxVal > 0) {
							if (BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						} else if (minVal == 0.0 && maxVal == 0.0) {
							arr[0] = BinRatio;
							val.put("yAxis", arr);

						} else {
							if (BinRatio > minVal && BinRatio < maxVal) {
								arr[0] = BinRatio;
								val.put("yAxis", arr);
							}
						}

					}
				}
				if (val.containsKey("yAxis")) {
					binRatioResultDocuments.add(val);
				}
			}
			return binRatioResultDocuments;
		} else {
			return testResults;
		}
	}

}
