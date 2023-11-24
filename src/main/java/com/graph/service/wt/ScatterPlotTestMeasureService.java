package com.graph.service.wt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.constants.wt.AdvancedGraphConstants;
import com.graph.model.wt.FilterParam;
import com.graph.utils.CriteriaUtil;
import com.mongodb.BasicDBObject;

@Service
public class ScatterPlotTestMeasureService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired(required = true)
	private IAutographService autoGraphService;

	private static final int LIMIT_VALUE = 40000;

	/*
	 * List TestRelatedMeasureForScatter
	 */
	public List<Document> getTestRelatedMeasureForScatter(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, String testName, int testNumber, String dimensionTestName, int dimensionTestNumber,
			double minVal, double maxVal, String aggregator, int DimensionSelectedBin, int measureSelectedBin,
			double min, double max, List<FilterParam> filterParams) throws IOException {
		List<Document> dimensionResults;
		Boolean isTestvalueDimension = false;
		Boolean isTestvalueMeasure = false;
		List<Document> measureResults;
		List<Document> groupResults = null;

		switch (dimensionName) {

		case AdvancedGraphConstants.TEST_PASS_CHIPS:
			if (dimensionTestName == null) {
				throw new IOException("TestName is Empty");
			}
			if (dimensionTestNumber < 0) {
				throw new IOException("TestName is Empty.");
			}

			dimensionResults = generateTestFieldsForScatter(waferIds, dimensionTestNumber, dimensionTestName, true, min,
					max, filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_CHIPS:
			if (dimensionTestName == null) {
				throw new IOException("TestName is Empty");
			}
			if (dimensionTestNumber < 0) {
				throw new IOException("TestName is Empty.");
			}

			dimensionResults = generateTestFieldsForScatter(waferIds, dimensionTestNumber, dimensionTestName, false,
					min, max, filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_RATIO:
			if (dimensionTestName == null) {
				throw new IOException("TestName is Empty");
			}
			if (dimensionTestNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			dimensionResults = generateTestFailRatioForScatter(waferIds, dimensionTestNumber, dimensionTestName, false,
					filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_RATIO_ACTUAL_COUNT:
			if (dimensionTestName == null) {
				throw new IOException("TestName is Empty");
			}
			if (dimensionTestNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			dimensionResults = generateWithActualCountFieldsForScatter(waferIds, dimensionTestNumber, dimensionTestName,
					min, max, filterParams);
			break;

		case AdvancedGraphConstants.TEST_VALUE:
			if (dimensionTestName == null) {
				throw new IOException("TestName is Empty");
			}
			if (dimensionTestNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			dimensionResults = generateTestValueMeasures(waferIds, dimensionTestNumber, dimensionTestName, min, max,
					filterParams);
			isTestvalueDimension = true;

			break;

		default:
			dimensionResults = generateCommonnScatterDimension(dimensionName, group, minVal, maxVal, aggregator,
					DimensionSelectedBin, waferIds, filterParams);
			break;

		}

		switch (measureName) {

		case AdvancedGraphConstants.TEST_PASS_CHIPS:
			if (testName == null) {
				throw new IOException("TestName is Empty");
			}
			if (testNumber < 0) {
				throw new IOException("TestName is Empty.");
			}

			measureResults = generateTestFieldsForScatter(waferIds, testNumber, testName, true, minVal, maxVal,
					filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_CHIPS:
			if (testName == null) {
				throw new IOException("TestName is Empty");
			}
			if (testNumber < 0) {
				throw new IOException("TestName is Empty.");
			}

			measureResults = generateTestFieldsForScatter(waferIds, testNumber, testName, false, minVal, maxVal,
					filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_RATIO:
			if (testName == null) {
				throw new IOException("TestName is Empty");
			}
			if (testNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			measureResults = generateTestFailRatioForScatter(waferIds, testNumber, testName, false, filterParams);
			break;
		case AdvancedGraphConstants.TEST_FAIL_RATIO_ACTUAL_COUNT:
			if (testName == null) {
				throw new IOException("TestName is Empty");
			}
			if (testNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			measureResults = generateWithActualCountFieldsForScatter(waferIds, testNumber, testName, minVal, maxVal,
					filterParams);
			break;

		case AdvancedGraphConstants.TEST_VALUE:
			if (testName == null) {
				throw new IOException("TestName is Empty");
			}
			if (testNumber < 0) {
				throw new IOException("TestName is Empty.");
			}
			measureResults = generateTestValueMeasures(waferIds, testNumber, testName, minVal, maxVal, filterParams);
			isTestvalueMeasure = true;

			break;
		default:
			measureResults = generateCommonnScatterDimension(measureName, group, minVal, maxVal, aggregator,
					measureSelectedBin, waferIds, filterParams);
			break;

		}

		if ((dimensionTestName != null && !dimensionTestName.isEmpty()) && (testName != null && !testName.isEmpty())) {
			List<Document> testResultsDoc;
			groupResults = genarateGroupScatterDetails(group, waferIds, filterParams);

			if (isTestvalueDimension.equals(true) && isTestvalueMeasure.equals(true)) {
				testResultsDoc = genarateTestValueResultsForDimMeas(dimensionResults, measureResults, groupResults,
						minVal, maxVal, min, max);
			} else if (isTestvalueDimension.equals(true)) {
				testResultsDoc = genarateCommonTestValueResults(dimensionResults, measureResults, groupResults, true,
						minVal, maxVal, min, max);
			} else if (isTestvalueMeasure.equals(true)) {
				testResultsDoc = genarateCommonTestValueResults(measureResults, dimensionResults, groupResults, false,
						minVal, maxVal, min, max);

			} else {
				testResultsDoc = genarateTestRelatedResults(measureResults, dimensionResults, groupResults, minVal,
						maxVal, min, max);
			}

			return testResultsDoc;

		} else {

			if (dimensionTestName != null) {
				List<Document> dimensionResultsDoc;

				if (isTestvalueDimension) {
					dimensionResultsDoc = genarateTestValueResults(measureResults, dimensionResults, false, minVal,
							maxVal, min, max);

				} else {
					dimensionResultsDoc = genarateResults(dimensionResults, measureResults, true, minVal, maxVal, min,
							max);

				}

				return dimensionResultsDoc;

			} else {
				List<Document> mesureResultsDoc;
				if (isTestvalueMeasure) {
					mesureResultsDoc = genarateTestValueResults(dimensionResults, measureResults, true, minVal, maxVal,
							min, max);

				} else {
					mesureResultsDoc = genarateResults(measureResults, dimensionResults, false, minVal, maxVal, min,
							max);
				}

				return mesureResultsDoc;

			}

		}
	}
	private List<Document> generateTestFieldsForScatter(ObjectId[] waferIds, int testNumber, String testName,
			Boolean status, double minVal, double maxVal, List<FilterParam> filterParams) {

		String CollectionName = null;
		Aggregation aggregationMean = null;
		List<Document> keyResultsMean = null;
		List<Criteria> criteriaListTestStatus = new ArrayList<>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaListTestStatus = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTestStatus.add(Criteria.where("wd").in(waferIds));
		}
		Criteria testNumberCriteria = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testNumberCriteria);
		Criteria testNameCriteria = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteria);
		Criteria teststatusCriteria = (Criteria.where("tp").is(status));
		criteriaListForMinMax.add(teststatusCriteria);
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
		LookupOperation lookupOperationWafer = LookupOperation.newLookup().from(AdvancedGraphConstants.TEST_HISTORY_KEY).localField("wd")
				.foreignField("mesWafer._id").as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		UnwindOperation unwindTestHistory = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_TEST_HISTORY, true);

		MatchOperation matchTestFailOperation = Aggregation.match(new Criteria()
				.andOperator(criteriaListTestStatus.toArray(new Criteria[criteriaListTestStatus.size()])));

		GroupOperation groupOperation = Aggregation.group("$tn", "$tp", "$tm", "$wd").count().as("totalCount")
				.min("TestHistory.mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME)
				.max("TestHistory.mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId")
				.and("$totalCount").as("TestPass").andExclude("_id").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME)
				.and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME).as(AdvancedGraphConstants.WAFER_END_TIME);
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		}
		keyResultsMean = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class).getMappedResults();
		return keyResultsMean;
	}

	private List<Document> generateTestFailRatioForScatter(ObjectId[] waferIds, int testNumber, String testName,
			boolean status, List<FilterParam> filterParams) {

		String CollectionName = null;
		Aggregation aggregationMean = null;
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
		Criteria testFailCriteria = (Criteria.where("tp").is(status));
		criteriaListForMinMax.add(testFailCriteria);

		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
		LookupOperation lookupOperationWafer = LookupOperation.newLookup().from(AdvancedGraphConstants.TEST_HISTORY_KEY).localField("wd")
				.foreignField("mesWafer._id").as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		UnwindOperation unwindTestHistory = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_TEST_HISTORY, true);

		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));

		GroupOperation groupOperation = Aggregation.group("$tn", "$tp", "$tm", "$wd").count().as("totalCount")
				.min("TestHistory.mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME)
				.max("TestHistory.mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId")
				.and("$totalCount").as("TestPass").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)
				.as(AdvancedGraphConstants.WAFER_END_TIME).andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
		return testResults;
	}

	private List<Document> generateWithActualCountFieldsForScatter(ObjectId[] waferIds, int testNumber, String testName,
			double minVal, double maxVal, List<FilterParam> filterParams) {

		String collectionName = null;
		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(true)).then(1)
				.otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(false)).then(1)
				.otherwise(0);

		Aggregation aggregationMean = null;
		ProjectionOperation projectFieldSelectionOperation = null;
		ProjectionOperation projectFieldTest = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<Criteria>();
		List<Criteria> criteriaListForMinMax = new ArrayList<Criteria>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, collectionName));
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
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
		LookupOperation lookupOperationWafer = LookupOperation.newLookup().from(AdvancedGraphConstants.TEST_HISTORY_KEY).localField("waferId")
				.foreignField("mesWafer._id").as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		UnwindOperation unwindTestHistory = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_TEST_HISTORY, true);

		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		GroupOperation groupOperation = Aggregation.group("$tn", "$tm", "$wd").push("$tp").as("tp");

		UnwindOperation unwindtp = Aggregation.unwind("$tp", true);

		projectFieldSelectionOperation = Aggregation.project().and("_id.wd").as("waferId").and(passChips).as("TestPass")
				.and(failChips).as("TestFail").andExclude("_id");
		GroupOperation groupOperationTest = Aggregation.group("waferId").count().as("totalCount").sum("TestPass")
				.as("passChips").sum("TestFail").as("failChips").min("TestHistory.mesWafer.WaferStartTime")
				.as(AdvancedGraphConstants.WAFER_START_TIME).max("TestHistory.mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);

		projectFieldTest = Aggregation.project().and("_id").as("waferId").and("passChips").as("TestPass")
				.and("failChips").as("TestFail").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)
				.as(AdvancedGraphConstants.WAFER_END_TIME).andExclude("_id");

		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
					unwindtp, projectFieldSelectionOperation, lookupOperationWafer, unwindTestHistory,
					groupOperationTest, projectFieldTest);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));

			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation,
					unwindtp, projectFieldSelectionOperation, lookupOperationWafer, unwindTestHistory,
					groupOperationTest, projectFieldTest);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

		int TestPass = 0;
		int TestFail = 0;
		for (Document testval : testResults) {
			TestPass += (int) testval.get("TestPass");
			TestFail += (int) testval.get("TestFail");
			int TotalVAl = TestFail + TestPass;
			double TestFailRatio = (double) TestFail / TotalVAl;
			TestFailRatio = TestFailRatio * 100;
			if (TestFailRatio > 0) {
				if (minVal > 0 && maxVal == 0.0) {
					if (TestFailRatio > minVal) {
						testval.put("TestPass", TestFailRatio);
					} else {
						testval.put("TestPass", 0);
					}
				}

				else if (minVal == 0.0 && maxVal > 0) {
					if (TestFailRatio < maxVal) {
						testval.put("TestPass", TestFailRatio);
					} else {
						testval.put("TestPass", 0);
					}
				} else if (minVal == 0.0 && maxVal == 0.0) {
					testval.put("TestPass", TestFailRatio);
				} else {
					if (TestFailRatio > minVal && TestFailRatio < maxVal) {
						testval.put("TestPass", TestFailRatio);
					} else {
						testval.put("TestPass", 0);
					}
				}

			}

		}

		return testResults;
	}

	private List<Document> genarateTestValueResultsForDimMeas(List<Document> dimensionResults,
			List<Document> measureResults, List<Document> groupResults, double minVal, double maxVal, double min,
			double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0 && groupResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = groupResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						if (!(results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							continue;
						}
						for (Document mesureRes : measureResults) {
							if (!(mesureRes.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								continue;
							}
							List<?> DataArrDime = (List<?>) testval.get("testValue");
							List<?> DataArr = (List<?>) mesureRes.get("testValue");
							for (Object testDataDime : DataArrDime) {
								for (Object testData : DataArr) {

									double[] arr = new double[2];
									double TestPassDime = 0;
									double TestPassMeas = 0;
									if (testDataDime instanceof Integer) {
										int TestPass = (int) testDataDime;
										TestPassDime = Double.valueOf(TestPass);
									} else {
										double TestPass = (double) testDataDime;
										TestPassDime = Double.valueOf(TestPass);
									}
									if (testData instanceof Integer) {
										int TestPass = (int) testData;
										TestPassMeas = Double.valueOf(TestPass);
									} else {
										double TestPass = (double) testData;
										TestPassMeas = Double.valueOf(TestPass);
									}
									arr[0] = TestPassDime;
									arr[1] = TestPassMeas;
									boolean count = false;
									boolean count1 = false;
									if (arr[0] > 0 && arr[1] > 0) {

										if (minVal > 0 && maxVal == 0.0) {
											if (arr[1] > minVal) {
												count = true;
											}
										} else if (minVal == 0.0 && maxVal > 0) {
											if (arr[1] < maxVal) {
												count = true;
											}
										} else if (minVal == 0.0 && maxVal == 0.0) {
											count = true;
										} else if (minVal > 0.0 && maxVal > 0.0) {
											if (arr[1] > minVal && arr[1] < maxVal) {
												count = true;
											}
										} else {
											count = false;
										}
										if (min > 0 && max == 0.0) {
											if (arr[0] > min) {
												count1 = true;
											}
										} else if (min == 0.0 && max > 0) {
											if (arr[0] < max) {
												count1 = true;
											}
										} else if (min == 0.0 && max == 0.0) {
											count1 = true;
										} else if (min > 0.0 && max > 0.0) {
											if (arr[0] > min && arr[0] < max) {
												count1 = true;
											}
										} else {
											count1 = false;
										}
									}
									if (count) {
										if (count1) {
											listArr.add(arr);
											listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
											listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
										}
									}
								}
							}
						}
					}
				}
				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}

		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;
	}

	private List<Document> genarateCommonTestValueResults(List<Document> measureResults,
			List<Document> dimensionResults, List<Document> groupResults, boolean flag, double minVal, double maxVal,
			double min, double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0 && groupResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = groupResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();

				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						if (!(results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							continue;
						}
						for (Document mesureRes : measureResults) {
							if (!(mesureRes.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								continue;
							}
							List<?> DataArr = (List<?>) mesureRes.get("testValue");
							for (Object testData : DataArr) {
								double[] arr = new double[2];
								double TestPassDime = 0;
								double TestPassMeas = 0;
								if (testval.get("TestPass") instanceof Integer) {
									int TestPass = (int) testval.get("TestPass");
									TestPassDime = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testval.get("TestPass");
									TestPassDime = Double.valueOf(TestPass);
								}
								if (testData instanceof Integer) {
									int TestPass = (int) testData;
									TestPassMeas = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testData;
									TestPassMeas = Double.valueOf(TestPass);
								}
								if (flag) {
									arr[0] = TestPassMeas;
									arr[1] = TestPassDime;
								} else {
									arr[1] = TestPassMeas;
									arr[0] = TestPassDime;
								}

								boolean count = false;
								boolean count1 = false;
								if (arr[0] > 0 && arr[1] > 0) {

									if (minVal > 0 && maxVal == 0.0) {
										if (arr[1] > minVal) {
											count = true;
										}
									} else if (minVal == 0.0 && maxVal > 0) {
										if (arr[1] < maxVal) {
											count = true;
										}
									} else if (minVal == 0.0 && maxVal == 0.0) {
										count = true;
									} else if (minVal > 0.0 && maxVal > 0.0) {
										if (arr[1] > minVal && arr[1] < maxVal) {
											count = true;
										}
									} else {
										count = false;
									}
									if (min > 0 && max == 0.0) {
										if (arr[0] > min) {
											count1 = true;
										}
									} else if (min == 0.0 && max > 0) {
										if (arr[0] < max) {
											count1 = true;
										}
									} else if (min == 0.0 && max == 0.0) {
										count1 = true;
									} else if (min > 0.0 && max > 0.0) {
										if (arr[0] > min && arr[0] < max) {
											count1 = true;
										}
									} else {
										count1 = false;
									}
								}
								if (count) {
									if (count1) {
										listArr.add(arr);
										listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
										listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
									}
								}
							}
						}

					}
				}
				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);
					testResultsDocument.add(val);
				}

			});

		}

		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;
	}

	private List<Document> genarateTestValueResults(List<Document> dimensionResults, List<Document> measureResults,
			boolean flag, double minVal, double maxVal, double min, double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = dimensionResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {

					for (Document testval : measureResults) {
						if (!(testval.get("waferId").toString().equals(results.get("waferId").toString()))) {
							continue;
						}
						List<?> DataArr = (List<?>) testval.get("testValue");

						for (Object testData : DataArr) {
							double[] arr = new double[2];

							if (flag) {

								if (testData instanceof Integer) {
									int TestPass = (int) testData;
									arr[1] = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testData;
									arr[1] = Double.valueOf(TestPass);
								}

								List<?> list1 = (List<?>) results.get("yAxis");
								if (list1.get(0) instanceof Integer) {
									int Yaxis = (int) list1.get(0);
									arr[0] = Double.valueOf(Yaxis);
								} else {
									double Yaxis = (double) list1.get(0);
									arr[0] = Double.valueOf(Yaxis);
								}
							} else {

								if (testData instanceof Integer) {
									int TestPass = (int) testData;
									arr[0] = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testData;
									arr[0] = Double.valueOf(TestPass);
								}

								List<?> list1 = (List<?>) results.get("yAxis");
								if (list1.get(0) instanceof Integer) {
									int Yaxis = (int) list1.get(0);
									arr[1] = Double.valueOf(Yaxis);
								} else {
									double Yaxis = (double) list1.get(0);
									arr[1] = Double.valueOf(Yaxis);
								}

							}
							boolean count = false;
							boolean count1 = false;
							if (arr[0] > 0 && arr[1] > 0) {

								if (minVal > 0 && maxVal == 0.0) {
									if (arr[1] > minVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal > 0) {
									if (arr[1] < maxVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal == 0.0) {
									count = true;
								} else if (minVal > 0.0 && maxVal > 0.0) {
									if (arr[1] > minVal && arr[1] < maxVal) {
										count = true;
									}
								} else {
									count = false;
								}
								if (min > 0 && max == 0.0) {
									if (arr[0] > min) {
										count1 = true;
									}
								} else if (min == 0.0 && max > 0) {
									if (arr[0] < max) {
										count1 = true;
									}
								} else if (min == 0.0 && max == 0.0) {
									count1 = true;
								} else if (min > 0.0 && max > 0.0) {
									if (arr[0] > min && arr[0] < max) {
										count1 = true;
									}
								} else {
									count1 = false;
								}
							}
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
									listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
								}
							}
						}
					}
				}
				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}
		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;

	}

	private List<Document> generateTestValueMeasures(ObjectId[] waferIds, int testNumber, String testName,
			double minVal, double maxVal, List<FilterParam> filterParams) {

		String CollectionName = null;
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
		LookupOperation lookupOperationWafer = LookupOperation.newLookup().from(AdvancedGraphConstants.TEST_HISTORY_KEY).localField("wd")
				.foreignField("mesWafer._id").as(AdvancedGraphConstants.TEST_HISTORY_KEY);
		UnwindOperation unwindTestHistory = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_TEST_HISTORY, true);
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));

		GroupOperation groupOperation = Aggregation.group("$wd").addToSet("$mv").as("testValue")
				.min("TestHistory.mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME)
				.max("TestHistory.mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("waferId")
				.and("$testValue").as("testValue").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)
				.as(AdvancedGraphConstants.WAFER_END_TIME).andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");
		Aggregation aggregationMean = null;
		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		} else {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationWafer,
					unwindTestHistory, groupOperation, projectFieldSelectionOperation);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
		return testResults;
	}

	private List<Document> genarateTestRelatedResults(List<Document> measureResults, List<Document> dimensionResults,
			List<Document> groupResults, double minVal, double maxVal, double min, double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0 && groupResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = groupResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						if (!(results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							continue;
						}
						for (Document mesureRes : measureResults) {

							double[] arr = new double[2];
							if ((mesureRes.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								double TestPassDime = 0;
								double TestPassMeas = 0;
								if (testval.get("TestPass") instanceof Integer) {
									int TestPass = (int) testval.get("TestPass");
									TestPassDime = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testval.get("TestPass");
									TestPassDime = Double.valueOf(TestPass);
								}
								if (mesureRes.get("TestPass") instanceof Integer) {
									int TestPass = (int) mesureRes.get("TestPass");
									TestPassMeas = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) mesureRes.get("TestPass");
									TestPassMeas = Double.valueOf(TestPass);
								}

								arr[0] = TestPassDime;
								arr[1] = TestPassMeas;
								boolean count = false;
								boolean count1 = false;
								if (arr[0] > 0 && arr[1] > 0) {

									if (minVal > 0 && maxVal == 0.0) {
										if (arr[1] > minVal) {
											count = true;
										}
									} else if (minVal == 0.0 && maxVal > 0) {
										if (arr[1] < maxVal) {
											count = true;
										}
									} else if (minVal == 0.0 && maxVal == 0.0) {
										count = true;
									} else if (minVal > 0.0 && maxVal > 0.0) {
										if (arr[1] > minVal && arr[1] < maxVal) {
											count = true;
										}
									} else {
										count = false;
									}
									if (min > 0 && max == 0.0) {
										if (arr[0] > min) {
											count1 = true;
										}
									} else if (min == 0.0 && max > 0) {
										if (arr[0] < max) {
											count1 = true;
										}
									} else if (min == 0.0 && max == 0.0) {
										count1 = true;
									} else if (min > 0.0 && max > 0.0) {
										if (arr[0] > min && arr[0] < max) {
											count1 = true;
										}
									} else {
										count1 = false;
									}
								}
								if (count) {
									if (count1) {
										listArr.add(arr);
										listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
										listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
									}
								}
							}

						}

					}
				}
				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}
		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;
	}

	private List<Document> genarateResults(List<Document> dimensionResults, List<Document> measureResults, Boolean flag,
			double minVal, double maxVal, double min, double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						double[] arr = new double[2];
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							if (flag) {
								if (testval.get("TestPass") instanceof Integer) {
									int TestPass = (int) testval.get("TestPass");
									arr[0] = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testval.get("TestPass");
									arr[0] = Double.valueOf(TestPass);
								}

								List<?> list1 = (List<?>) results.get("yAxis");
								if (list1.get(0) instanceof Integer) {
									int Yaxis = (int) list1.get(0);
									arr[1] = Double.valueOf(Yaxis);
								} else {
									double Yaxis = (double) list1.get(0);
									arr[1] = Double.valueOf(Yaxis);
								}

							} else {
								if (testval.get("TestPass") instanceof Integer) {
									int TestPass = (int) testval.get("TestPass");
									arr[1] = Double.valueOf(TestPass);
								} else {
									double TestPass = (double) testval.get("TestPass");
									arr[1] = Double.valueOf(TestPass);
								}
								List<?> list1 = (List<?>) results.get("yAxis");

								if (list1.get(0) instanceof Integer) {
									int Yaxis = (int) list1.get(0);
									arr[0] = Double.valueOf(Yaxis);
								} else {
									double Yaxis = (double) list1.get(0);
									arr[0] = Double.valueOf(Yaxis);
								}

							}

							boolean count = false;
							boolean count1 = false;
							if (arr[0] > 0 && arr[1] > 0) {

								if (minVal > 0 && maxVal == 0.0) {
									if (arr[1] > minVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal > 0) {
									if (arr[1] < maxVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal == 0.0) {
									count = true;
								} else if (minVal > 0.0 && maxVal > 0.0) {
									if (arr[1] > minVal && arr[1] < maxVal) {
										count = true;
									}
								} else {
									count = false;
								}
								if (min > 0 && max == 0.0) {
									if (arr[0] > min) {
										count1 = true;
									}
								} else if (min == 0.0 && max > 0) {
									if (arr[0] < max) {
										count1 = true;
									}
								} else if (min == 0.0 && max == 0.0) {
									count1 = true;
								} else if (min > 0.0 && max > 0.0) {
									if (arr[0] > min && arr[0] < max) {
										count1 = true;
									}
								} else {
									count1 = false;
								}
							}
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
									listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
								}
							}
						}

					}

				}
				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}
		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;

	}

	private List<Document> genarateGroupScatterDetails(String group, ObjectId[] waferIds,
			List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		List<Criteria> criteriaListTestFail = new ArrayList<Criteria>();
		Aggregation aggregationMean = null;
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaListTestFail = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTestFail.add(Criteria.where("wd").in(waferIds));

		}
		MatchOperation matchTestFailOperation = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));
		MatchOperation matchOperation = Aggregation.match(Criteria.where("mesWafer._id").in(waferIds));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);

		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			throw new IOException("Selected combination is not supported");
		}

		AddFieldsOperation addFieldsDimensionOperation = Aggregation.addFields()
				.addFieldWithValue("$Grouping", "$" + grouping).addFieldWithValue("$Wafer", "$mesWafer._id").build();

		GroupOperation groupOperation = Aggregation.group("Grouping", "Wafer");
		ProjectionOperation projectFetchData = Aggregation.project().and("_id.Grouping").as("group").and("$_id.Wafer")
				.as("waferId");
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (waferIds.length > 0) {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
		} else {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchTestFailOperation, addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();

		return testResults;
	}

	private List<Document> generateCommonnScatterDimension(String dimensionName, String group, double minVal,
			double maxVal, String aggregator, int selectedBin, ObjectId[] waferIds, List<FilterParam> filterParams)
			throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsOperationWithBinRatio = null;
		AddFieldsOperation addFieldsDimensionOperation = null;
		Aggregation aggregationMean = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);

		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}

		GroupOperation groupOperation = null;
		ProjectionOperation projectFetchData = null;

		addFieldsDimensionOperation = Aggregation.addFields().addFieldWithValue("$Grouping", "$" + grouping)
				.addFieldWithValue("$Wafer", "$mesWafer._id").build();

		if (Dimension.equals("NumberOfChipsPerBin") || Dimension.equals("BinRatio")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();

			String BinRatioCalculationFormeasure = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$GroupingField'," + "            '$mesWafer.TestChipCount'" + "          ]"
					+ "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatio = BasicDBObject.parse(BinRatioCalculationFormeasure);

			addFieldsOperationWithBinRatio = Aggregation.addFields().addFieldWithValue("$Measure", BinRatio).build();
		}

		if (Dimension.equals("NumberOfChipsPerBin")) {
			groupOperation = Aggregation.group("Grouping", "Wafer").push("GroupingField").as("average")
					.min("mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME).max("mesWafer.WaferEndTime")
					.as(AdvancedGraphConstants.WAFER_END_TIME);
			projectFetchData = Aggregation.project().and("_id.Grouping").as("group").and("$_id.Wafer").as("waferId")
					.and("$average").as("yAxis");

		} else if (Dimension.equals("BinRatio")) {

			groupOperation = Aggregation.group("Grouping", "Wafer").push("Measure").as("average")
					.min("mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME).max("mesWafer.WaferEndTime")
					.as(AdvancedGraphConstants.WAFER_END_TIME);
			projectFetchData = Aggregation.project().and("_id.Grouping").as("group").and("$_id.Wafer").as("waferId")
					.and("$average").as("yAxis").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)
					.as(AdvancedGraphConstants.WAFER_END_TIME);
		} else {
			groupOperation = Aggregation.group("Grouping", "Wafer").push(Dimension).as("average")
					.min("mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME).max("mesWafer.WaferEndTime")
					.as(AdvancedGraphConstants.WAFER_END_TIME);
			projectFetchData = Aggregation.project().and("_id.Grouping").as("group").and("$_id.Wafer").as("waferId")
					.and("$average").as("yAxis").and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME)
					.as(AdvancedGraphConstants.WAFER_END_TIME);
		}
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		// addFieldsDimensionOperation is not null for any cases. Just for adding for
		// null check if it happens in future.
		if (addFieldsDimensionOperation == null) {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					groupOperation, projectFetchData, excludeId);
		} else {
			switch (Dimension) {
			case "NumberOfChipsPerBin":
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsOperation, addFieldsOperationWithBinRatio, addFieldsDimensionOperation, groupOperation,
						projectFetchData, excludeId);
				break;
			case "BinRatio":
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsOperation, addFieldsOperationWithBinRatio, addFieldsDimensionOperation, groupOperation,
						projectFetchData, excludeId);
				break;

			default:
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
				break;
			}

		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();

		return testResults;
	}

	public List<Document> getGroupByTestdMeasureForScatter(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, String testName, int testNumber, String dimensionTestName,
			int dimensionTestNumber, double minVal, double maxVal, String aggregator, int dimensionselectedBin,
			int measureselectedBin, double min, double max, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		Boolean TestRelatedDimensions = false;
		Boolean TestRelatedMeasures = false;
		List<Document> dimensionResults = null;
		List<Document> measureResults = null;
		List<Document> testResultsDocument;
		Aggregation aggregation = null;
		Aggregation aggregationMeasureOne = null;
		if (!dimensionName.isEmpty()) {
			switch (dimensionName) {
			case AdvancedGraphConstants.TEST_FAIL_CHIPS:
			case AdvancedGraphConstants.TEST_FAIL_RATIO:

				List<Document> keyResultsMean1 = null;
				AddFieldsOperation addFieldsOperation = Aggregation.addFields()
						.addFieldWithValue("$GroupingField", "$" + group)
						.addFieldWithValue("$DimensionField", "$" + dimensionName)
						.addFieldWithValue("$Wafer", "$mesWafer._id")
						.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();

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
				Criteria testFailCriteria = (Criteria.where("tp").is(false));
				criteriaListForMinMax.add(testFailCriteria);

				MatchOperation matchForMinMax = Aggregation.match(new Criteria()
						.andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
				LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				MatchOperation match = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));

				GroupOperation groupOperation = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
						.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				MatchOperation matchZero = Aggregation.match(Criteria.where("testFail").gt(0));
				ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");

				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchForMinMax, match, groupOperation, matchZero,
							excludeId);
				} else {
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
							lookupProductOperation, unwindProductInfo, match, projectFetchWafers);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							excludeId);
				}
				dimensionResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class)
						.getMappedResults();
				TestRelatedMeasures = true;
				if (addFieldsOperation != null) {
					List<Criteria> criteriaList = new ArrayList<Criteria>();
					if (waferIds.length == 0) {
						if (filterParams != null && filterParams.size() >= 1) {
							criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
						}
					} else {
						criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

					}
					MatchOperation matchOperationTest = Aggregation
							.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

					GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DimensionField", "Wafer")
							.sum(AdvancedGraphConstants.TEST_CHIP_COUNT).as(AdvancedGraphConstants.TEST_CHIP_COUNT).min("mesWafer.WaferStartTime")
							.as(AdvancedGraphConstants.WAFER_START_TIME).max("mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);
					ProjectionOperation projectFetchData = Aggregation.project().and("$_id.Wafer").as("waferId")
							.andExclude("_id").and(AdvancedGraphConstants.TEST_CHIP_COUNT).as(AdvancedGraphConstants.TEST_CHIP_COUNT).and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME)
							.as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME).as(AdvancedGraphConstants.WAFER_END_TIME);
					ProjectionOperation excludedId = Aggregation.project().andExclude("_id");
					Aggregation aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, addFieldsOperation,
							groupOperationFailval, projectFetchData, excludedId);
					keyResultsMean1 = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
							.getMappedResults();
				}

				if (keyResultsMean1.size() > 0 && dimensionResults.size() > 0) {

					int count2 = 0;
					int TestChip2 = 0;
					for (Document testval : dimensionResults) {
						for (Document results : keyResultsMean1) {

							if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								count2 = (int) testval.get("testFail");
								TestChip2 = (int) results.get(AdvancedGraphConstants.TEST_CHIP_COUNT);
								int TotalVAl = TestChip2;
								double TestFailRatio = (double) count2 / TotalVAl;
								TestFailRatio = TestFailRatio * 100;
								System.out.println(TestFailRatio);
								testval.put("testFail", TestFailRatio);
								testval.put(AdvancedGraphConstants.WAFER_START_TIME, results.get(AdvancedGraphConstants.WAFER_START_TIME));
								testval.put(AdvancedGraphConstants.WAFER_END_TIME, results.get(AdvancedGraphConstants.WAFER_END_TIME));
							}
						}

					}

				}

				TestRelatedDimensions = true;

				break;
			case AdvancedGraphConstants.TEST_PASS_CHIPS:
				List<Criteria> criteriaListKey = new ArrayList<Criteria>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaListKey = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaListKey.add(Criteria.where("mesWafer._id").in(waferIds));

				}
				MatchOperation matchOperationTest = Aggregation
						.match(new Criteria().andOperator(criteriaListKey.toArray(new Criteria[criteriaListKey.size()])));

				ProjectionOperation projectFetchData = Aggregation.project().and("mesWafer._id").as("waferId")
						.and("$mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME).and("$mesWafer.WaferEndTime")
						.as(AdvancedGraphConstants.WAFER_END_TIME);
				ProjectionOperation excludedId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, projectFetchData,
						excludedId);
				keyResultsMean1 = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();
				List<Criteria> criteriaListTestFail1 = new ArrayList<Criteria>();
				List<Criteria> criteriaListforwafers1 = new ArrayList<>();
				List<Criteria> criteriaListForMinMax1 = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaListTestFail1 = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaListTestFail1.add(Criteria.where("wd").in(waferIds));

				}
				Criteria testFailCriteria1 = (Criteria.where("tp").is(true));
				criteriaListForMinMax1.add(testFailCriteria1);

				MatchOperation matchForMinMax1 = Aggregation.match(new Criteria()
						.andOperator(criteriaListForMinMax1.toArray(new Criteria[criteriaListForMinMax1.size()])));
				LookupOperation lookupProductOperation1 = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfo1 = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				MatchOperation matchTest = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail1.toArray(new Criteria[criteriaListTestFail1.size()])));

				GroupOperation groupOperationTest = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
						.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId");

				ProjectionOperation excludeIdTest = Aggregation.project().andExclude("_id");
				MatchOperation matchZeroTest = Aggregation.match(Criteria.where("testFail").gt(0));
				ProjectionOperation projectFetchWafers1 = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");

				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchForMinMax1, matchTest, groupOperationTest,
							matchZeroTest, excludeIdTest);
				} else {

					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers1,
							lookupProductOperation1, unwindProductInfo1, matchTest, projectFetchWafers1);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(matchForMinMax1, match1, groupOperationTest, matchZeroTest,
							excludeIdTest);
				}
				dimensionResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class)
						.getMappedResults();
				if (keyResultsMean1.size() > 0 && dimensionResults.size() > 0) {

					for (Document testval : dimensionResults) {
						for (Document results : keyResultsMean1) {

							if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								testval.put(AdvancedGraphConstants.WAFER_START_TIME, results.get(AdvancedGraphConstants.WAFER_START_TIME));
								testval.put(AdvancedGraphConstants.WAFER_END_TIME, results.get(AdvancedGraphConstants.WAFER_END_TIME));
							}
						}

					}

				}
				TestRelatedDimensions = true;

				break;
			case AdvancedGraphConstants.TEST_FAIL_RATIO_ACTUAL_COUNT:
				List<Criteria> criteriaListTestFailCount = new ArrayList<Criteria>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaListTestFailCount = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaListTestFailCount.add(Criteria.where("mesWafer._id").in(waferIds));

				}
				MatchOperation matchOperation = Aggregation
						.match(new Criteria().andOperator(criteriaListTestFailCount.toArray(new Criteria[criteriaListTestFailCount.size()])));
				ProjectionOperation projectFetchDataTest = Aggregation.project().and("mesWafer._id").as("waferId")
						.and("$mesWafer.WaferStartTime").as(AdvancedGraphConstants.WAFER_START_TIME).and("$mesWafer.WaferEndTime")
						.as(AdvancedGraphConstants.WAFER_END_TIME);
				ProjectionOperation excludedTestId = Aggregation.project().andExclude("_id");
				Aggregation aggregationMeanTestPass = Aggregation.newAggregation(matchOperation, projectFetchDataTest,
						excludedTestId);
				keyResultsMean1 = mongoTemplate.aggregate(aggregationMeanTestPass, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
						.getMappedResults();

				GroupOperation groupOperationPassFail = Aggregation.group("$tn", "$tm", "$wd").push("$tp").as("tp");
				UnwindOperation unwindtp = Aggregation.unwind("$tp", true);
				Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(true))
						.then(1).otherwise(0);
				Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(false))
						.then(1).otherwise(0);
				ProjectionOperation projectFieldSelectionOperation = null;
				ProjectionOperation projectFieldTest = null;
				List<Criteria> criteriaList = new ArrayList<Criteria>();
				List<Criteria> criteriaListforwafersFailedRatio = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaList.add(Criteria.where("wd").in(waferIds));

				}

				MatchOperation matchTestFailOperation = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				projectFieldSelectionOperation = Aggregation.project().and(passChips).as("TestPass").and(failChips)
						.as("TestFail").and("$_id").as("_id");

				LookupOperation lookupProductOperationFailRatio = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfoFailRatio = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				groupOperationTest = Aggregation.group("_id").sum("TestPass").as("passChips").sum("TestFail")
						.as("failChips");
				projectFieldTest = Aggregation.project().and("$_id.tn").as("testNumber").and("$_id.tm").as("testName")
						.and("$_id.wd").as("waferId").and("passChips").as("TestPass").and("failChips").as("TestFail")
						.andExclude("_id");
				ProjectionOperation projectFetchWafersFailRatio = Aggregation.project().and("$mesWafer._id")
						.as("waferId").andExclude("_id");

				if (waferIds.length > 0) {
					aggregationMeasureOne = Aggregation.newAggregation(matchTestFailOperation, groupOperationPassFail,
							unwindtp, projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
				} else {
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(
							criteriaListforwafersFailedRatio, lookupProductOperationFailRatio,
							unwindProductInfoFailRatio, matchTestFailOperation, projectFetchWafersFailRatio);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregationMeasureOne = Aggregation.newAggregation(match1, groupOperationPassFail, unwindtp,
							projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
				}
				AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();
				dimensionResults = mongoTemplate
						.aggregate(aggregationMeasureOne.withOptions(options), "TestResults", Document.class)
						.getMappedResults();
				int TestPass = 0;
				int TestFail = 0;
				for (Document testval : dimensionResults) {
					for (Document results : keyResultsMean1) {

						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							TestPass += (int) testval.get("TestPass");
							TestFail += (int) testval.get("TestFail");
							int TotalVAl = TestFail + TestPass;
							double TestFailRatio = (double) TestFail / TotalVAl;
							TestFailRatio = TestFailRatio * 100;
							testval.put("testFail", TestFailRatio);
							testval.put(AdvancedGraphConstants.WAFER_START_TIME, results.get(AdvancedGraphConstants.WAFER_START_TIME));
							testval.put(AdvancedGraphConstants.WAFER_END_TIME, results.get(AdvancedGraphConstants.WAFER_END_TIME));
						}
					}

				}
				TestRelatedDimensions = true;
				break;
			default:
				dimensionResults = generateCommonnScatterDimensionForTest(dimensionName, group, minVal, maxVal,
						aggregator, waferIds, filterParams);
				TestRelatedDimensions = false;
				break;
			}
		}

		if (!measureName.isEmpty()) {
			switch (measureName) {
			case AdvancedGraphConstants.TEST_FAIL_CHIPS:
			case AdvancedGraphConstants.TEST_FAIL_RATIO:

				List<Document> keyResultsMean1 = null;
				AddFieldsOperation addFieldsOperation = Aggregation.addFields()
						.addFieldWithValue("$GroupingField", "$" + group)
						.addFieldWithValue("$DimensionField", "$" + dimensionName)
						.addFieldWithValue("$Wafer", "$mesWafer._id")
						.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();

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
				Criteria testFailCriteria = (Criteria.where("tp").is(false));
				criteriaListForMinMax.add(testFailCriteria);

				MatchOperation matchForMinMax = Aggregation.match(new Criteria()
						.andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
				LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				MatchOperation match = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));

				GroupOperation groupOperation = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
						.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId");
				ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
				MatchOperation matchZero = Aggregation.match(Criteria.where("testFail").gt(0));
				ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");

				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchForMinMax, match, groupOperation, matchZero,
							excludeId);
				} else {
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
							lookupProductOperation, unwindProductInfo, match, projectFetchWafers);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							excludeId);
				}
				measureResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class).getMappedResults();
				TestRelatedMeasures = true;
				if (addFieldsOperation != null) {
					List<Criteria> criteriaList = new ArrayList<Criteria>();
					if (waferIds.length == 0) {
						if (filterParams != null && filterParams.size() >= 1) {
							criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
						}
					} else {
						criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

					}
					MatchOperation matchOperationTest = Aggregation
							.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

					GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DimensionField", "Wafer")
							.sum(AdvancedGraphConstants.TEST_CHIP_COUNT).as(AdvancedGraphConstants.TEST_CHIP_COUNT);
					ProjectionOperation projectFetchData = Aggregation.project().and("$_id.Wafer").as("waferId")
							.andExclude("_id").and(AdvancedGraphConstants.TEST_CHIP_COUNT).as(AdvancedGraphConstants.TEST_CHIP_COUNT);
					ProjectionOperation excludedId = Aggregation.project().andExclude("_id");
					Aggregation aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, addFieldsOperation,
							groupOperationFailval, projectFetchData, excludedId);
					keyResultsMean1 = mongoTemplate.aggregate(aggregationMeanTest, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
							.getMappedResults();
				}

				if (keyResultsMean1.size() > 0 && measureResults.size() > 0) {

					int count2 = 0;
					int TestChip2 = 0;
					for (Document testval : measureResults) {
						for (Document results : keyResultsMean1) {

							if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
								count2 = (int) testval.get("testFail");
								TestChip2 = (int) results.get(AdvancedGraphConstants.TEST_CHIP_COUNT);
								int TotalVAl = TestChip2;
								double TestFailRatio = (double) count2 / TotalVAl;
								TestFailRatio = TestFailRatio * 100;
								System.out.println(TestFailRatio);
								testval.put("testFail", TestFailRatio);
							}
						}

					}

				}

				TestRelatedMeasures = true;
				break;
			case AdvancedGraphConstants.TEST_PASS_CHIPS:
				List<Criteria> criteriaListTestFail1 = new ArrayList<Criteria>();
				List<Criteria> criteriaListforwafers1 = new ArrayList<>();
				List<Criteria> criteriaListForMinMax1 = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaListTestFail1 = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaListTestFail1.add(Criteria.where("wd").in(waferIds));

				}
				Criteria testFailCriteria1 = (Criteria.where("tp").is(true));
				criteriaListForMinMax1.add(testFailCriteria1);

				MatchOperation matchForMinMax1 = Aggregation.match(new Criteria()
						.andOperator(criteriaListForMinMax1.toArray(new Criteria[criteriaListForMinMax1.size()])));
				LookupOperation lookupProductOperation1 = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfo1 = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				MatchOperation matchTest = Aggregation.match(new Criteria()
						.andOperator(criteriaListTestFail1.toArray(new Criteria[criteriaListTestFail1.size()])));
				GroupOperation groupOperationTest = Aggregation.group("$tn", "$tm", "$wd").first("$tn").as("testNumber")
						.first("$tm").as("testName").count().as("testFail").first("$wd").as("waferId");

				ProjectionOperation excludeIdTest = Aggregation.project().andExclude("_id");
				MatchOperation matchZeroTest = Aggregation.match(Criteria.where("testFail").gt(0));
				ProjectionOperation projectFetchWafers1 = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");

				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchForMinMax1, matchTest, groupOperationTest,
							matchZeroTest, excludeIdTest);
				} else {
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers1,
							lookupProductOperation1, unwindProductInfo1, matchTest, projectFetchWafers1);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(matchForMinMax1, match1, groupOperationTest, matchZeroTest,
							excludeIdTest);
				}
				measureResults = mongoTemplate.aggregate(aggregation, "TestResults", Document.class).getMappedResults();
				TestRelatedMeasures = true;
				break;
			case AdvancedGraphConstants.TEST_FAIL_RATIO_ACTUAL_COUNT:
				GroupOperation groupOperationPassFail = Aggregation.group("$tn", "$tm", "$wd").push("$tp").as("tp");
				UnwindOperation unwindtp = Aggregation.unwind("$tp", true);
				Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(true))
						.then(1).otherwise(0);
				Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$tp").equalToValue(false))
						.then(1).otherwise(0);
				ProjectionOperation projectFieldSelectionOperation = null;
				ProjectionOperation projectFieldTest = null;
				List<Criteria> criteriaList = new ArrayList<Criteria>();
				List<Criteria> criteriaListforwafersFailedActual = new ArrayList<>();
				if (waferIds.length == 0) {
					if (filterParams != null && filterParams.size() >= 1) {
						criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
					}
				} else {
					criteriaList.add(Criteria.where("wd").in(waferIds));

				}

				MatchOperation matchTestFailOperation = Aggregation
						.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
				projectFieldSelectionOperation = Aggregation.project().and(passChips).as("TestPass").and(failChips)
						.as("TestFail").and("$_id").as("_id");
				LookupOperation lookupProductOperationFailedActual = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
						.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);
				UnwindOperation unwindProductInfoFailedActual = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
				groupOperationTest = Aggregation.group("_id").sum("TestPass").as("passChips").sum("TestFail")
						.as("failChips");
				projectFieldTest = Aggregation.project().and("$_id.tn").as("testNumber").and("$_id.tm").as("testName")
						.and("passChips").as("TestPass").and("failChips").as("TestFail").andExclude("_id")
						.and("$_id.wd").as("waferId");

				ProjectionOperation projectFetchWafersFailedActual = Aggregation.project().and("$mesWafer._id")
						.as("waferId").andExclude("_id");

				if (waferIds.length > 0) {
					aggregation = Aggregation.newAggregation(matchTestFailOperation, groupOperationPassFail, unwindtp,
							projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
				} else {
					List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(
							criteriaListforwafersFailedActual, lookupProductOperationFailedActual,
							unwindProductInfoFailedActual, matchTestFailOperation, projectFetchWafersFailedActual);

					MatchOperation match1 = Aggregation
							.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
					aggregation = Aggregation.newAggregation(match1, groupOperationPassFail, unwindtp,
							projectFieldSelectionOperation, groupOperationTest, projectFieldTest);
				}
				AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();
				measureResults = mongoTemplate
						.aggregate(aggregation.withOptions(options), "TestResults", Document.class).getMappedResults();
				int TestPass = 0;
				int TestFail = 0;
				for (Document testval : measureResults) {
					TestPass += (int) testval.get("TestPass");
					TestFail += (int) testval.get("TestFail");
					int TotalVAl = TestFail + TestPass;
					double TestFailRatio = (double) TestFail / TotalVAl;
					TestFailRatio = TestFailRatio * 100;
					testval.put("testFail", TestFailRatio);

				}
				TestRelatedMeasures = true;
				break;
			default:
				measureResults = generateCommonnScatterDimensionForTest(measureName, group, minVal, maxVal, aggregator,
						waferIds, filterParams);
				TestRelatedMeasures = false;
				break;
			}
		}

		if (TestRelatedMeasures.equals(true) && TestRelatedDimensions.equals(true)) {
			testResultsDocument = generateResultsForTestComb(measureResults, dimensionResults, minVal, maxVal, min,
					max);
			return testResultsDocument;
		} else if (TestRelatedMeasures) {
			testResultsDocument = generateResultsForTest(measureResults, dimensionResults, true, minVal, maxVal, min,
					max);
			return testResultsDocument;
		} else if (TestRelatedDimensions) {
			testResultsDocument = generateResultsForTest(dimensionResults, measureResults, false, minVal, maxVal, min,
					max);
			return testResultsDocument;
		} else {
			throw new IOException("Selected combination is not supported");
		}

	}

	private List<Document> generateResultsForTestComb(List<Document> measureResults, List<Document> dimensionResults,
			double minVal, double maxVal, double min, double max) throws IOException {
		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("testNumber").toString() + "_" + results.get("testName").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						double[] arr = new double[2];
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {

							if (results.get("testFail") instanceof Integer) {
								int TestPass = (int) results.get("testFail");
								arr[1] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) results.get("testFail");
								arr[1] = Double.valueOf(TestPass);
							}

							if (testval.get("testFail") instanceof Integer) {
								int TestPass = (int) testval.get("testFail");
								arr[0] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) testval.get("testFail");
								arr[0] = Double.valueOf(TestPass);
							}
							boolean count = false;
							boolean count1 = false;
							if (arr[0] > 0 && arr[1] > 0) {

								if (minVal > 0 && maxVal == 0.0) {
									if (arr[1] > minVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal > 0) {
									if (arr[1] < maxVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal == 0.0) {
									count = true;
								} else if (minVal > 0.0 && maxVal > 0.0) {
									if (arr[1] > minVal && arr[1] < maxVal) {
										count = true;
									}
								} else {
									count = false;
								}
								if (min > 0 && max == 0.0) {
									if (arr[0] > min) {
										count1 = true;
									}
								} else if (min == 0.0 && max > 0) {
									if (arr[0] < max) {
										count1 = true;
									}
								} else if (min == 0.0 && max == 0.0) {
									count1 = true;
								} else if (min > 0.0 && max > 0.0) {
									if (arr[0] > min && arr[0] < max) {
										count1 = true;
									}
								} else {
									count1 = false;
								}
							}
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
									listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
								}
							}
						}

					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}
		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;
	}

	private List<Document> generateResultsForTest(List<Document> measureResults, List<Document> dimensionResults,
			boolean flag, double minVal, double maxVal, double min, double max) throws IOException {

		List<Document> testResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("testNumber").toString() + "_" + results.get("testName").toString();
					}));

			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					for (Document testval : dimensionResults) {
						double[] arr = new double[2];
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {

							if (results.get("testFail") instanceof Integer) {
								int TestPass = (int) results.get("testFail");
								if (flag) {
									arr[1] = Double.valueOf(TestPass);
								} else {
									arr[0] = Double.valueOf(TestPass);
								}
							} else {
								double TestPass = (double) results.get("testFail");
								if (flag) {
									arr[1] = Double.valueOf(TestPass);
								} else {
									arr[0] = Double.valueOf(TestPass);
								}
							}

							List<?> list1 = (List<?>) testval.get("yAxis");

							if (list1.get(0) instanceof Integer) {
								int Yaxis = (int) list1.get(0);
								if (flag) {
									arr[0] = Double.valueOf(Yaxis);
								} else {
									arr[1] = Double.valueOf(Yaxis);
								}
							} else {
								double Yaxis = (double) list1.get(0);
								if (flag) {
									arr[0] = Double.valueOf(Yaxis);
								} else {
									arr[1] = Double.valueOf(Yaxis);
								}

							}
							boolean count = false;
							boolean count1 = false;
							if (arr[0] > 0 && arr[1] > 0) {

								if (minVal > 0 && maxVal == 0.0) {
									if (arr[1] > minVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal > 0) {
									if (arr[1] < maxVal) {
										count = true;
									}
								} else if (minVal == 0.0 && maxVal == 0.0) {
									count = true;
								} else if (minVal > 0.0 && maxVal > 0.0) {
									if (arr[1] > minVal && arr[1] < maxVal) {
										count = true;
									}
								} else {
									count = false;
								}
								if (min > 0 && max == 0.0) {
									if (arr[0] > min) {
										count1 = true;
									}
								} else if (min == 0.0 && max > 0) {
									if (arr[0] < max) {
										count1 = true;
									}
								} else if (min == 0.0 && max == 0.0) {
									count1 = true;
								} else if (min > 0.0 && max > 0.0) {
									if (arr[0] > min && arr[0] < max) {
										count1 = true;
									}
								} else {
									count1 = false;
								}
							}
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_START_TIME));
									listEndTime.add((Date) testval.get(AdvancedGraphConstants.WAFER_END_TIME));
								}
							}
						}

					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put(AdvancedGraphConstants.WAFER_START_TIME, listStartTime);
					val.put(AdvancedGraphConstants.WAFER_END_TIME, listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					testResultsDocument.add(val);
				}

			});

		}
		for (Document dataArrDoc : testResultsDocument) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return testResultsDocument;

	}

	private List<Document> generateCommonnScatterDimensionForTest(String dimensionName, String group, double minVal,
			double maxVal, String aggregator, ObjectId[] waferIds, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsDimensionOperation = null;
		Aggregation aggregationMean = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from(AdvancedGraphConstants.PRODUCT_INFO)
				.localField("mesWafer.productId").foreignField("_id").as(AdvancedGraphConstants.PRODUCT_INFO);

		UnwindOperation unwindProductInfo = Aggregation.unwind(AdvancedGraphConstants.DOLLAR_PRODUCT_INFO, false);
		if (dimensionName.equals("NumberOfChipsPerBin") || dimensionName.equals("BinRatio")) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}

		GroupOperation groupOperation = null;
		ProjectionOperation projectFetchData = null;

		addFieldsDimensionOperation = Aggregation.addFields().addFieldWithValue("$Wafer", "$mesWafer._id").build();

		groupOperation = Aggregation.group("Wafer").push(Dimension).as("average").min("mesWafer.WaferStartTime")
				.as(AdvancedGraphConstants.WAFER_START_TIME).max("mesWafer.WaferEndTime").as(AdvancedGraphConstants.WAFER_END_TIME);
		projectFetchData = Aggregation.project().and("$_id").as("waferId").and("$average").as("yAxis")
				.and(AdvancedGraphConstants.DOLLAR_WAFER_START_TIME).as(AdvancedGraphConstants.WAFER_START_TIME).and(AdvancedGraphConstants.DOLLAR_WAFER_END_TIME).as(AdvancedGraphConstants.WAFER_END_TIME);
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsDimensionOperation == null) {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					groupOperation, projectFetchData, excludeId);
		} else {

			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsDimensionOperation, groupOperation, projectFetchData, excludeId);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, AdvancedGraphConstants.TEST_HISTORY_KEY, Document.class)
				.getMappedResults();

		return testResults;
	}
}
