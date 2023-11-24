package com.graph.service.wt;

import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.model.wt.FilterParam;
import com.graph.utils.CriteriaUtil;
import com.mongodb.BasicDBObject;

@Service
public class RegularHistogramService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired(required = true)
	private IAutographService autoGraphService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public List<Document> getTestRelatedMeasureForRegularHisto(ObjectId[] waferIds, String dimensionName, String group,
			int dimensionselectedBin, String dimensionTestName, int dimensionTestNumber, double min, double max,
			double scale, double classVal, String aggregator, List<FilterParam> filterParams) throws IOException {

		if (dimensionTestName == null) {
			throw new IOException("TestName is Empty");
		}
		if (dimensionTestNumber < 0) {
			throw new IOException("TestName is Empty.");
		}
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		AddFieldsOperation addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + grouping).addFieldWithValue("$Wafer", "$mesWafer._id")
				.build();
		List<Document> TestResult = null;
		switch (dimensionName) {
		case "TestPassChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestFieldsRegularHisto(waferIds, dimensionTestNumber, dimensionTestName, true,
						addFieldsOperation, grouping, min, max, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestFieldsRegularHisto(waferIds, dimensionTestNumber, dimensionTestName, false,
						addFieldsOperation, grouping, min, max, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatio":
			if (aggregator.equals("Avg")) {
				TestResult = generateTestFailedRatioRegularHisto(waferIds, dimensionTestNumber, dimensionTestName,
						false, grouping, min, max, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatioActualCount":
			if (aggregator.equals("Avg")) {
				TestResult = generateWithActualCountRegularHisto(waferIds, dimensionTestNumber, dimensionTestName, true,
						grouping, min, max, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		default:
			throw new IOException("Selected combination is not supported");
		}
		return TestResult;

	}

	private List<Document> generateTestFieldsRegularHisto(ObjectId[] waferIds, int dimensionTestNumber,
			String dimensionTestName, boolean flag, AddFieldsOperation addFieldsOperation, String grouping, double min,
			double max, List<FilterParam> filterParams) {

		String CollectionName = null;
		List<Double> list = new ArrayList<Double>();
		logger.info("Preparing Query generateTestFieldsRegularHistogram");
		List<Document> keyResultsMean = null;
		List<Document> newDocu = new ArrayList<>();
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
		Criteria testCriteriaFail = (Criteria.where("tn").is(dimensionTestNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(dimensionTestName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(flag));
		criteriaListForMinMax.add(testFailCriteria);

		Aggregation aggregationMean = null;
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupTestHistory = LookupOperation.newLookup().from("TestHistory").localField("wd")
				.foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", false);
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
		if (addFieldsOperation != null) {
			List<Criteria> criteriaList = new ArrayList<Criteria>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

			}
			Aggregation aggregationMeanTest = null;
			MatchOperation matchOperationTest = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
			GroupOperation groupOperationFail = Aggregation.group("GroupingField", "Wafer")
					.addToSet("mesWafer.WaferStartTime").as("WaferStartTime").addToSet("mesWafer.WaferEndTime")
					.as("WaferEndTime");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.Wafer").as("waferId").andExclude("_id").and("$WaferStartTime").as("WaferStartTime")
					.and("$WaferEndTime").as("WaferEndTime");
			if (waferIds.length > 0) {
				aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, lookupProductOperation,
						unwindProductInfo, addFieldsOperation, groupOperationFail, projectFetchData);
			} else {
				aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFail, projectFetchData);
			}
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}

		logger.info("Preparing aggregationMean generateTestFieldsRegularHisto" + aggregationMean);
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));
			Document val = new Document();
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
						}
					}
				}
				if (count > 0) {

					if (min > 0 && max == 0.0) {
						if (count > min) {
							list.add((double) count);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (count < max) {
							list.add((double) count);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					} else if (min == 0.0 && max == 0.0) {
						list.add((double) count);
						val.put("WaferStartTime", listStart);
						val.put("WaferEndTime", listEnd);

					} else {
						if (count > min && count < max) {
							list.add((double) count);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

				}

			});

			if (list.size() > 0) {
				val.put("xAxis", list);
				newDocu.add(val);
			}
		}
		return newDocu;

	}

	private List<Document> generateTestFailedRatioRegularHisto(ObjectId[] waferIds, int dimensionTestNumber,
			String dimensionTestName, boolean flag, String grouping, double min, double max,
			List<FilterParam> filterParams) {

		String CollectionName = null;
		Aggregation aggregationMean = null;
		List<Document> keyResultsMean = null;
		List<Double> list = new ArrayList<Double>();
		List<Document> testFaileRatioDocuments = new ArrayList<>();
		logger.info("Preparing Query generateTestFailedRatioRegularHisto");
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
		Criteria testCriteriaFail = (Criteria.where("tn").is(dimensionTestNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(dimensionTestName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(flag));
		criteriaListForMinMax.add(testFailCriteria);
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		AddFieldsOperation addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + grouping).addFieldWithValue("$Wafer", "$mesWafer._id")
				.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();
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
					.sum("TestChipCount").as("TestChipCount").addToSet("mesWafer.WaferStartTime").as("WaferStartTime")
					.addToSet("mesWafer.WaferEndTime").as("WaferEndTime");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.DimensionField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
					.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").andExclude("_id").and("TestChipCount")
					.as("TestChipCount");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregationMeanTest = null;
			if (waferIds.length > 0) {
				aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, lookupProductOperation,
						unwindProductInfo, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
			} else {
				aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
			}
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}

		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {

						return results.get("group").toString();
					}));
			Document val = new Document();
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				int TestChip = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							TestChip += (int) results.get("TestChipCount");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
						}
					}
				}
				if (count > 0) {
					double TestFailRatio = (double) count / TestChip;
					TestFailRatio = TestFailRatio * 100;
					if (min > 0 && max == 0.0) {
						if (TestFailRatio > min) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (TestFailRatio < max) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					} else if (min == 0.0 && max == 0.0) {
						list.add((double) TestFailRatio);
						val.put("WaferStartTime", listStart);
						val.put("WaferEndTime", listEnd);

					} else {
						if (TestFailRatio > min && TestFailRatio < max) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

				}

			});

			if (list.size() > 0) {
				val.put("xAxis", list);
				testFaileRatioDocuments.add(val);
			}
		}
		return testFaileRatioDocuments;
	}

	private List<Document> generateWithActualCountRegularHisto(ObjectId[] waferIds, int dimensionTestNumber,
			String dimensionTestName, boolean flag, String grouping, double min, double max,
			List<FilterParam> filterParams) {
	
		String CollectionName = null;
		List<Document> keyResultsMean = null;
		List<Double> list = new ArrayList<Double>();
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
		Criteria testCriteria = (Criteria.where("tn").is(dimensionTestNumber));
		criteriaListForMinMax.add(testCriteria);
		Criteria testNameCriteria = (Criteria.where("tm").is(dimensionTestName));
		criteriaListForMinMax.add(testNameCriteria);

		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		AddFieldsOperation addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + grouping).addFieldWithValue("$Wafer", "$mesWafer._id")
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

		if (addFieldsOperation != null) {
			List<Criteria> criteriaListTest = new ArrayList<Criteria>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaListTest = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaListTest.add(Criteria.where("mesWafer._id").in(waferIds));

			}
			Aggregation aggregationMeanTest = null;
			MatchOperation matchOperationTest = Aggregation
					.match(new Criteria().andOperator(criteriaListTest.toArray(new Criteria[criteriaListTest.size()])));
			GroupOperation groupOperationFailval = Aggregation.group("GroupingField", "DimensionField", "Wafer")
					.addToSet("mesWafer.WaferStartTime").as("WaferStartTime").addToSet("mesWafer.WaferEndTime")
					.as("WaferEndTime");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.DimensionField").as("xAxis").and("$_id.Wafer").as("waferId").and("$WaferStartTime")
					.as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime").andExclude("_id");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			if (waferIds.length > 0) {
				aggregationMeanTest = Aggregation.newAggregation(matchOperationTest, lookupProductOperation,
						unwindProductInfo, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
			} else {
				aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
						matchOperationTest, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
			}
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}

		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();

					}));
			Document val = new Document();
			comblistGrouped.forEach((keyVal, valu) -> {
				int TestPass = 0;
				int TestFail = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {

						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							TestPass += (int) testval.get("TestPass");
							TestFail += (int) testval.get("TestFail");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
						}
					}
				}
				if (TestPass > 0 || TestFail > 0) {
					int TotalVAl = TestFail + TestPass;
					double TestFailRatio = (double) TestFail / TotalVAl;
					TestFailRatio = TestFailRatio * 100;
					double[] arr = new double[1];
					if (min > 0 && max == 0.0) {
						if (TestFailRatio > min) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

					else if (min == 0.0 && max > 0) {
						if (TestFailRatio < max) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					} else if (min == 0.0 && max == 0.0) {
						list.add((double) TestFailRatio);
						val.put("WaferStartTime", listStart);
						val.put("WaferEndTime", listEnd);

					} else {
						if (TestFailRatio > min && TestFailRatio < max) {
							list.add((double) TestFailRatio);
							val.put("WaferStartTime", listStart);
							val.put("WaferEndTime", listEnd);
						}
					}

				}

			});

			if (list.size() > 0) {
				val.put("xAxis", list);
				testActualResultDocuments.add(val);

			}
		}
		return testActualResultDocuments;
	}

	public List<Document> getDutServiceForRegularHisto(ObjectId[] waferIds, String dimensionName, String group,
			int dimensionselectedBin, double min, double max, double scale, double classVal, String aggregator,
			List<?> Include, boolean search, List<FilterParam> filterParams) throws IOException {

		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(true))
				.then("$ChipCount").otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(false))
				.then("$ChipCount").otherwise(0);
		String Yield = null;
		String passFail = null;
		Aggregation aggregationMean = null;
		Aggregation aggregationDUTTest = null;
		GroupOperation groupOperation = null;
		GroupOperation groupOperation1 = null;
		GroupOperation groupOperation2 = null;
		AggregationOperation FilterMinMax = null;
		ProjectionOperation projectFieldTest = null;
		ProjectionOperation projectRoundData = null;
		ProjectionOperation projectFetchData = null;
		GroupOperation groupOperationTest = null;
		GroupOperation groupOperationBin = null;
		AddFieldsOperation addFieldsOperation = null;
		MatchOperation matchInclude = null;
		MatchOperation match1 = null;
		ProjectionOperation projectFieldSelectionOperation = null;
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		List<Criteria> criteriaList = new ArrayList<>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = null;
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("waferID").in(waferIds));

		}

		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchTestOperation = Aggregation.match(Criteria.where("TestPass").is(false));
		MatchOperation matchBinOperation = Aggregation.match(Criteria.where("BINNumber").is(dimensionselectedBin));
		LookupOperation lookupTestHistory = LookupOperation.newLookup().from("TestHistory").localField("_id.waferID")
				.foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		if (waferIds.length == 0) {
			LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
					.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
			UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
			ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
					.andExclude("_id");
			Aggregation aggregationResults = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchOperation, projectFetchWafers);
			List<Document> wafers = mongoTemplate.aggregate(aggregationResults, "TestHistory", Document.class)
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
			match1 = Aggregation.match(new Criteria()
					.andOperator(criteriaListforwafers.toArray(new Criteria[criteriaListforwafers.size()])));
		}
		if (Include != null) {
			List<Integer> newList = new ArrayList<Integer>(Include.size());
			for (Object myInt : Include) {
				newList.add(Integer.valueOf((String) myInt));
			}
			if (search == true) {
				matchInclude = Aggregation.match(Criteria.where("_id").nin(newList));
			} else {
				matchInclude = Aggregation.match(Criteria.where("_id").in(newList));
			}
		}

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}

		groupOperation = Aggregation.group(grouping, "TestPass", "waferID").count().as("ChipCount");
		projectFieldTest = Aggregation.project().and("_id." + grouping).as("grouping").and(passChips).as("passChips")
				.and(failChips).as("failChips").and("ChipCount").as("ChipCount").andExclude();
		groupOperationTest = Aggregation.group("grouping").sum("failChips").as("failChips").sum("passChips")
				.as("passChips").sum("ChipCount").as("totalCount").min("TestHistory.mesWafer.WaferStartTime")
				.as("WaferStartTime").max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

		if (dimensionName.equals("Yield") || dimensionName.equals("FailRatio")) {

			if (dimensionName.equals("Yield")) {
				passFail = "$passChips";
			} else {
				passFail = "$failChips";
			}
			Yield = "  {" + "      $round: [" + "  {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "'" + passFail + "'" + "           '$totalCount' " + "          ]" + "        }," + "        100"
					+ "      ]" + "    }," + "        4" + "      ]" + "    }";

			BasicDBObject bdo = BasicDBObject.parse(Yield);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("GroupingField", bdo).build();
			groupOperation1 = Aggregation.group("_id").avg("GroupingField").as("Value").min("WaferStartTime")
					.as("WaferStartTime").max("WaferEndTime").as("WaferEndTime");

			groupOperation2 = Aggregation.group().push("Value").as("xAxis").addToSet("WaferStartTime")
					.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
			MatchOperation filterZero = Aggregation.match(Criteria.where("Value").gt(0));

			if (min > 0 && max == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("GroupingField").gte(min)));
			} else if (min == 0.0 && max > 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("GroupingField").lte(max)));
			} else if (min == 0.0 && max == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("GroupingField")));
			} else {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("GroupingField").gte(min).lte(max)));
			}
			if (min == 0.0 && max == 0.0) {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								groupOperation1, filterZero, matchInclude, groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								groupOperation1, filterZero, matchInclude, groupOperation2, excludeId);
					}
				}

				else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								groupOperation1, filterZero, groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								groupOperation1, filterZero, groupOperation2, excludeId);
					}
				}
			} else {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								FilterMinMax, groupOperation1, filterZero, matchInclude, groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								FilterMinMax, groupOperation1, filterZero, matchInclude, groupOperation2, excludeId);
					}
				}

				else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								FilterMinMax, groupOperation1, filterZero, groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, addFieldsOperation,
								FilterMinMax, groupOperation1, filterZero, groupOperation2, excludeId);
					}
				}
			}

		}

		else if (dimensionName.equals("NumberOfFailedChips") || dimensionName.equals("NumberOfPassedChips")) {

			if (dimensionName.equals("NumberOfPassedChips")) {
				passFail = "passChips";

				groupOperation2 = Aggregation.group().push("passChips").as("xAxis").addToSet("WaferStartTime")
						.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
			} else {
				passFail = "failChips";
				groupOperation2 = Aggregation.group().push("failChips").as("xAxis").addToSet("WaferStartTime")
						.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");

			}
			if (min > 0 && max == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where(passFail).gte(min)));
			} else if (min == 0.0 && max > 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where(passFail).lte(max)));
			} else if (min == 0.0 && max == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where(passFail)));
			} else {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where(passFail).gte(min).lte(max)));
			}
			if (min == 0.0 && max == 0.0) {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, matchInclude, groupOperation2,
								excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, matchInclude, groupOperation2,
								excludeId);
					}
				} else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, groupOperation2, excludeId);
					}
				}
			} else {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, FilterMinMax, matchInclude,
								groupOperation2, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, FilterMinMax, matchInclude,
								groupOperation2, excludeId);
					}
				} else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, FilterMinMax, groupOperation2,
								excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(match1, groupOperation, projectFieldTest,
								lookupTestHistory, unwindTestHistory, groupOperationTest, FilterMinMax, groupOperation2,
								excludeId);
					}
				}
			}
		} else if (dimensionName.equals("NumberOfChipsPerBin") || dimensionName.equals("BinRatio")) {
			if (dimensionName.equals("NumberOfChipsPerBin")) {

				if (min > 0 && max == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("totalCount").gte(min)));
				} else if (min == 0.0 && max > 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("totalCount").lte(max)));
				} else if (min == 0.0 && max == 0.0) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("totalCount")));
				} else {
					FilterMinMax = Aggregation
							.match(new Criteria().andOperator(Criteria.where("totalCount").gte(min).lte(max)));
				}

				groupOperation2 = Aggregation.group().push("totalCount").as("xAxis").addToSet("WaferStartTime")
						.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");

				if (min == 0.0 && max == 0.0) {
					if (Include != null) {
						if (waferIds.length > 0) {
							aggregationMean = Aggregation.newAggregation(matchOperation, matchTestOperation,
									matchBinOperation, groupOperation, projectFieldTest, lookupTestHistory,
									unwindTestHistory, groupOperationTest, matchInclude, groupOperation2, excludeId);
						} else {
							aggregationMean = Aggregation.newAggregation(match1, matchTestOperation, matchBinOperation,
									groupOperation, projectFieldTest, lookupTestHistory, unwindTestHistory,
									groupOperationTest, matchInclude, groupOperation2, excludeId);
						}
					} else {
						if (waferIds.length > 0) {
							aggregationMean = Aggregation.newAggregation(matchOperation, matchTestOperation,
									matchBinOperation, groupOperation, projectFieldTest, lookupTestHistory,
									unwindTestHistory, groupOperationTest, groupOperation2, excludeId);
						} else {
							aggregationMean = Aggregation.newAggregation(match1, matchTestOperation, matchBinOperation,
									groupOperation, projectFieldTest, lookupTestHistory, unwindTestHistory,
									groupOperationTest, groupOperation2, excludeId);
						}
					}
				} else {
					if (Include != null) {
						if (waferIds.length > 0) {
							aggregationMean = Aggregation.newAggregation(matchOperation, matchTestOperation,
									matchBinOperation, groupOperation, projectFieldTest, lookupTestHistory,
									unwindTestHistory, groupOperationTest, FilterMinMax, matchInclude, groupOperation2,
									excludeId);
						} else {
							aggregationMean = Aggregation.newAggregation(match1, matchTestOperation, matchBinOperation,
									groupOperation, projectFieldTest, lookupTestHistory, unwindTestHistory,
									groupOperationTest, FilterMinMax, matchInclude, groupOperation2, excludeId);
						}
					} else {
						if (waferIds.length > 0) {
							aggregationMean = Aggregation.newAggregation(matchOperation, matchTestOperation,
									matchBinOperation, groupOperation, projectFieldTest, lookupTestHistory,
									unwindTestHistory, groupOperationTest, FilterMinMax, groupOperation2, excludeId);
						} else {

							aggregationMean = Aggregation.newAggregation(match1, matchTestOperation, matchBinOperation,
									groupOperation, projectFieldTest, lookupTestHistory, unwindTestHistory,
									groupOperationTest, FilterMinMax, groupOperation2, excludeId);
						}
					}

				}
			} else {

				if (aggregator.equals("Sum")) {
					throw new IOException("Selected combination is not supported");
				}
				LookupOperation lookupTestHistoryBin = LookupOperation.newLookup().from("TestHistory")
						.localField("waferID").foreignField("mesWafer._id").as("TestHistory");
				UnwindOperation unwindTestHistoryBin = Aggregation.unwind("$TestHistory", true);
				groupOperationBin = Aggregation.group(grouping).count().as("ChipCount")
						.min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
						.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
				ProjectionOperation projectField = Aggregation.project().and("_id").as("DUT").and("ChipCount")
						.as("xAxis").and("WaferStartTime").as("WaferStartTime").and("WaferEndTime").as("WaferEndTime")
						.andExclude();
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, matchTestOperation,
							lookupTestHistoryBin, unwindTestHistoryBin, matchBinOperation, groupOperationBin,
							projectField, excludeId);
				} else {

					aggregationMean = Aggregation.newAggregation(match1, matchTestOperation, lookupTestHistoryBin,
							unwindTestHistoryBin, matchBinOperation, groupOperationBin, projectField, excludeId);
				}
				List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class)
						.getMappedResults();

				groupOperation = Aggregation.group(grouping).count().as("totalCount");
				projectFieldSelectionOperation = Aggregation.project().and("_id.DUTNumber").as("dimension").and("_id")
						.as("dut").and("totalCount").as("totalCount").andExclude("_id");
				if (waferIds.length > 0) {
					aggregationDUTTest = Aggregation.newAggregation(matchOperation, groupOperation,
							projectFieldSelectionOperation);
				} else {
					aggregationDUTTest = Aggregation.newAggregation(match1, groupOperation,
							projectFieldSelectionOperation);
				}

				List<Document> dutResultsBin = mongoTemplate.aggregate(aggregationDUTTest, "MESTest", Document.class)
						.getMappedResults();

				Document val = new Document();
				List<Document> testResultsDocument = new ArrayList<>();
				List<Double> arr = new ArrayList<Double>();
				List<Date> listStartTime = new ArrayList<>();
				List<Date> listEndTime = new ArrayList<>();
				if (testResults.size() > 0 && dutResultsBin.size() > 0) {

					double count = 0;
					int count1 = 0;
					int testChipCount = 0;
					double TestFailRatio = 0;

					for (Document results : testResults) {
						for (Document totalCountBin : dutResultsBin) {

							if ((results.get("DUT").toString().equals(totalCountBin.get("dut").toString()))) {
								if (results.get("xAxis") instanceof Integer) {
									count1 = (int) results.get("xAxis");
									testChipCount = (int) totalCountBin.get("totalCount");
									TestFailRatio = ((double) count1 / testChipCount) * 100;
									listStartTime.add((Date) results.get("WaferStartTime"));
									listEndTime.add((Date) results.get("WaferEndTime"));

								} else {
									count = (double) results.get("xAxis");
									testChipCount = (int) totalCountBin.get("totalCount");
									TestFailRatio = ((double) count / testChipCount) * 100;
									listStartTime.add((Date) results.get("WaferStartTime"));
									listEndTime.add((Date) results.get("WaferEndTime"));

								}
								if (TestFailRatio > 0) {
									if (min > 0 && max == 0.0) {
										if (TestFailRatio > min) {
											arr.add(TestFailRatio);
										}
									}

									else if (min == 0.0 && max > 0) {
										if (TestFailRatio < max) {
											arr.add(TestFailRatio);
										}
									} else if (min == 0.0 && max == 0.0) {
										arr.add(TestFailRatio);
									} else {
										if (TestFailRatio > min && TestFailRatio < max) {
											arr.add(TestFailRatio);
										}
									}
								}
							}

						}
					}
				}
				if (arr.size() > 0) {
					val.put("xAxis", arr);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					testResultsDocument.add(val);
					return testResultsDocument;
				}
			}
		} else {
			throw new IOException("Selected combination is not supported");

		}
		List<Document> testResult = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class)
				.getMappedResults();

		return testResult;
	}

	/*
	 * GroupByBinRegularHistogram
	 */
	public List<Document> getBinForRegularHisto(ObjectId[] waferIds, String dimensionName, String group,
			int dimensionselectedBin, double min, double max, double scale, double classVal, String aggregator,
			List<?> Include, boolean search, String testName, int testNumber, List<FilterParam> filterParams)
			throws IOException {

		String CollectionName = null;
		List<Document> BinResults = null;
		MatchOperation matchTestPassBin = null;
		MatchOperation matchInclude = null;
		Aggregation aggregationMean = null;
		AddFieldsOperation addFieldsOperationBin = null;
		AggregationOperation FilterMinMax = null;
		List<Integer> newList = null;
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);

		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (Include != null) {
			newList = new ArrayList<Integer>(Include.size());
			for (Object myInt : Include) {
				newList.add(Integer.valueOf((String) myInt));
			}
			if (search == true) {
				matchInclude = Aggregation.match(Criteria.where("_id").nin(newList));
			} else {
				matchInclude = Aggregation.match(Criteria.where("_id").in(newList));
			}
		}
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
		UnwindOperation unwindBin = Aggregation.unwind("$mesWafer.BinSummary.BinCount");
		AddFieldsOperation addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("grouping", "$mesWafer.BinSummary.BinCount.BINNumber")
				.addFieldWithValue("TestPassBinNumber", "$mesWafer.BinSummary.TestPassBINNumber").build();

		GroupOperation groupValue = Aggregation.group(grouping).sum("mesWafer.BinSummary.BinCount.Count").as("xAxis")
				.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
		MatchOperation matchZero = Aggregation.match(Criteria.where("xAxis").gt(0));
		GroupOperation groupOperation = Aggregation.group(grouping).push("xAxis").as("xAxis").addToSet("WaferStartTime")
				.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		if (min > 0 && max == 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min)));
		} else if (min == 0.0 && max > 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(max)));
		} else {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min).lte(max)));
		}

		if (dimensionName.equals("NumberOfFailedChips") || dimensionName.equals("NumberOfChipsPerBin")
				|| dimensionName.equals("NumberOfPassedChips")) {

			if (dimensionName.equals("NumberOfFailedChips") || dimensionName.equals("NumberOfChipsPerBin")) {
				matchTestPassBin = Aggregation.match(Criteria.where("grouping").ne(20));
			} else {
				matchTestPassBin = Aggregation.match(Criteria.where("grouping").is(20));
			}
			if (min == 0.0 && max == 0.0) {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
								matchTestPassBin, groupValue, matchZero, matchInclude, groupOperation, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue, matchZero,
								matchInclude, groupOperation, excludeId);
					}
				} else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
								matchTestPassBin, groupValue, matchZero, groupOperation, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue, matchZero,
								groupOperation, excludeId);
					}
				}
			} else {
				if (Include != null) {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
								matchTestPassBin, groupValue, matchZero, FilterMinMax, matchInclude, groupOperation,
								excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue, matchZero,
								FilterMinMax, matchInclude, groupOperation, excludeId);
					}
				} else {
					if (waferIds.length > 0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
								matchTestPassBin, groupValue, matchZero, FilterMinMax, groupOperation, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
								matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue, matchZero,
								FilterMinMax, groupOperation, excludeId);
					}
				}
			}

			BinResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class).getMappedResults();

		} else if (dimensionName.equals("TestPassChips")) {

			BinResults = getBinTestPassForRegularHisto(waferIds, dimensionName, group, dimensionselectedBin, min, max,
					scale, classVal, aggregator, newList, search, testName, testNumber, filterParams);
		} else if (dimensionName.equals("BinRatio")) {

			BinResults = getBinRatioForRegularHisto(waferIds, dimensionName, grouping, dimensionselectedBin, min, max,
					scale, classVal, aggregator, newList, search, testName, testNumber, filterParams);
		} else {
			throw new IOException("Selected combination is not supported");
		}
		return BinResults;
	}

	/*
	 * GroupByBinRegularHistogram for TestPassChips
	 */
	private List<Document> getBinTestPassForRegularHisto(ObjectId[] waferIds, String dimensionName, String group,
			int dimensionselectedBin, double min, double max, double scale, double classVal, String aggregator,
			List<Integer> Include, boolean search, String testName, int testNumber, List<FilterParam> filterParams)
			throws IOException {

		String CollectionName = null;
		MatchOperation matchTest = null;
		MatchOperation match1 = null;
		GroupOperation groupTest = null;
		GroupOperation groupTestValue = null;
		MatchOperation matchInclude = null;
		Aggregation aggregationMean = null;
		AggregationOperation FilterMinMax = null;

		LookupOperation lookupOperationTestHistory = LookupOperation.newLookup().from("TestHistory").localField("wd")
				.foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		if (search == true) {
			matchInclude = Aggregation.match(Criteria.where("_id").nin(Include));
		} else {
			matchInclude = Aggregation.match(Criteria.where("_id").in(Include));
		}

		if (min > 0 && max == 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min)));
		} else if (min == 0.0 && max > 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(max)));
		} else {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min).lte(max)));
		}

		MatchOperation matchZero = Aggregation.match(Criteria.where("xAxis").gt(0));
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
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
		matchTest = Aggregation.match(
				new Criteria().andOperator(criteriaListTestFail.toArray(new Criteria[criteriaListTestFail.size()])));

		groupTest = Aggregation.group("$bn").count().as("xAxis").min("TestHistory.mesWafer.WaferStartTime")
				.as("WaferStartTime").max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

		groupTestValue = Aggregation.group().push("xAxis").as("xAxis").addToSet("WaferStartTime").as("WaferStartTime")
				.addToSet("WaferEndTime").as("WaferEndTime");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length == 0) {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTest, projectFetchWafers);

			match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
		}
		if (min == 0 && max == 0.0) {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTest, lookupOperationTestHistory,
							unwindTestHistory, groupTest, matchInclude, groupTestValue, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationTestHistory,
							unwindTestHistory, groupTest, matchInclude, groupTestValue, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTest, lookupOperationTestHistory,
							unwindTestHistory, groupTest, groupTestValue, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationTestHistory,
							unwindTestHistory, groupTest, groupTestValue, excludeId);
				}
			}
		} else {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTest, lookupOperationTestHistory,
							unwindTestHistory, groupTest, FilterMinMax, matchInclude, groupTestValue, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationTestHistory,
							unwindTestHistory, groupTest, FilterMinMax, matchInclude, groupTestValue, excludeId);
				}

			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTest, lookupOperationTestHistory,
							unwindTestHistory, groupTest, FilterMinMax, groupTestValue, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupOperationTestHistory,
							unwindTestHistory, groupTest, FilterMinMax, groupTestValue, excludeId);
				}
			}
		}
		List<Document> testPassResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

		return testPassResults;
	}

	/*
	 * GroupByBinRegularHistogram for BinRatio
	 */
	private List<Document> getBinRatioForRegularHisto(ObjectId[] waferIds, String dimensionName, String grouping,
			int dimensionselectedBin, double min, double max, double scale, double classVal, String aggregator,
			List<Integer> Include, boolean search, String testName, int testNumber, List<FilterParam> filterParams)
			throws IOException {

		String CollectionName = null;
		MatchOperation matchTestPassBin = null;
		MatchOperation matchInclude = null;
		MatchOperation match1 = null;
		Aggregation aggregationMean = null;
		AddFieldsOperation addFieldsOperationBin = null;
		AggregationOperation FilterMinMax = null;
		AddFieldsOperation addFieldsOperation = null;
		GroupOperation groupValue = null;
		GroupOperation groupOperation = null;

		if (search == true) {
			matchInclude = Aggregation.match(Criteria.where("_id").nin(Include));
		} else {
			matchInclude = Aggregation.match(Criteria.where("_id").in(Include));
		}

		if (min > 0 && max == 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min)));
		} else if (min == 0.0 && max > 0.0) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(max)));
		} else {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(min).lte(max)));
		}
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

		}
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		UnwindOperation unwindBin = Aggregation.unwind("$mesWafer.BinSummary.BinCount");
		MatchOperation matchZero = Aggregation.match(Criteria.where("xAxis").gt(0));
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		matchTestPassBin = Aggregation.match(Criteria.where("grouping").ne(20));
		addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("grouping", "$mesWafer.BinSummary.BinCount.BINNumber")
				.addFieldWithValue("TestPassBinNumber", "$mesWafer.BinSummary.TestPassBINNumber")
				.addFieldWithValue("TestChipCount", "$mesWafer.TestChipCount").build();

		groupValue = Aggregation.group(grouping).sum("mesWafer.BinSummary.BinCount.Count").as("xAxis")
				.sum("TestChipCount").as("ChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime")
				.max("mesWafer.WaferEndTime").as("WaferEndTime");

		String Bin = "  {" + "      $round: [" + "  {" + "      $multiply: [" + "        {" + "          $divide: ["
				+ "'$xAxis'" + "           '$ChipCount' " + "          ]" + "        }," + "        100" + "      ]"
				+ "    }," + "        4" + "      ]" + "    }";

		BasicDBObject bdo = BasicDBObject.parse(Bin);
		addFieldsOperationBin = Aggregation.addFields().addFieldWithValue("xAxis", bdo).build();
		groupOperation = Aggregation.group(grouping).push("xAxis").as("xAxis").addToSet("WaferStartTime")
				.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		if (min == 0.0 && max == 0.0) {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
							matchTestPassBin, groupValue, addFieldsOperationBin, matchZero, matchInclude,
							groupOperation, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue,
							addFieldsOperationBin, matchZero, matchInclude, groupOperation, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
							matchTestPassBin, groupValue, addFieldsOperationBin, matchZero, groupOperation, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue,
							addFieldsOperationBin, matchZero, groupOperation, excludeId);
				}
			}
		} else {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
							matchTestPassBin, groupValue, addFieldsOperationBin, matchZero, FilterMinMax, matchInclude,
							groupOperation, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue,
							addFieldsOperationBin, matchZero, FilterMinMax, matchInclude, groupOperation, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, unwindBin, addFieldsOperation,
							matchTestPassBin, groupValue, addFieldsOperationBin, matchZero, FilterMinMax,
							groupOperation, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, unwindBin, addFieldsOperation, matchTestPassBin, groupValue,
							addFieldsOperationBin, matchZero, FilterMinMax, groupOperation, excludeId);
				}
			}
		}
		List<Document> BinRatio = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
				.getMappedResults();

		return BinRatio;

	}

	public List<Document> getTestServiceForRegularHisto(ObjectId[] waferIds, String dimensionName, String group,
			int dimensionselectedBin, double min, double max, double scale, double classVal, String aggregator,
			List<?> Include, boolean search, List<FilterParam> filterParams) throws IOException {
		List<Document> TestResult = null;
		MatchOperation matchInclude = null;
		List<Integer> newList = null;
		if (Include != null) {
			newList = new ArrayList<Integer>(Include.size());
			for (Object myInt : Include) {
				newList.add(Integer.valueOf((String) myInt));
			}
			if (search == true) {
				matchInclude = Aggregation.match(Criteria.where("_id").nin(newList));
			} else {
				matchInclude = Aggregation.match(Criteria.where("_id").in(newList));
			}
		}
		switch (dimensionName) {
		case "NumberOfFailedChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestChipsRegularHisto(waferIds, newList, search, min, max, filterParams, false,
						matchInclude);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestPassChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTestChipsRegularHisto(waferIds, newList, search, min, max, filterParams, true,
						matchInclude);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "BinRatio":
			if (aggregator.equals("Avg")) {
				TestResult = generateBinRatioChipsRegularHisto(waferIds, newList, search, min, max, filterParams,
						dimensionselectedBin, matchInclude);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "NumberOfChipsPerBin":
			if (aggregator.equals("Sum")) {
				TestResult = generateNumberOfChipsRegularHisto(waferIds, newList, search, min, max, filterParams,
						dimensionselectedBin, matchInclude);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		default:
			throw new IOException("Selected combination is not supported");

		}
		return TestResult;
	}

	private List<Document> generateTestChipsRegularHisto(ObjectId[] waferIds, List<Integer> Include, boolean search,
			double minVal, double maxVal, List<FilterParam> filterParams, boolean flag, MatchOperation matchInclude) {
		AggregationOperation FilterMinMax = null;
		Aggregation aggregationMean = null;
		MatchOperation match1 = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (search == true) {
			matchInclude = Aggregation.match(Criteria.where("testNumber").nin(Include));
		} else {
			matchInclude = Aggregation.match(Criteria.where("testNumber").in(Include));
		}

		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = null;
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("wd").in(waferIds));

		}
		LookupOperation lookupTestHistory = LookupOperation.newLookup().from("TestHistory").localField("wd")
				.foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);

		Criteria testNumberCriteria = (Criteria.where("tp").is(flag));
		criteriaListForMinMax.add(testNumberCriteria);
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		GroupOperation groupOperation = Aggregation.group("$tn", "$tm").first("$tn").as("testNumber").first("$tm")
				.as("testName").count().as("testFail").first("$wd").as("wd");
		MatchOperation matchZero = Aggregation.match(Criteria.where("testFail").gt(0));

		GroupOperation gropupOperationFilter = Aggregation.group().push("testFail").as("xAxis")
				.addToSet("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
				.addToSet("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length == 0) {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
		}

		if (minVal == 0.0 && maxVal == 0.0) {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, lookupTestHistory, unwindTestHistory, matchInclude, gropupOperationFilter,
							excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							lookupTestHistory, unwindTestHistory, matchInclude, gropupOperationFilter, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, lookupTestHistory, unwindTestHistory, gropupOperationFilter, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							lookupTestHistory, unwindTestHistory, gropupOperationFilter, excludeId);
				}
			}
		} else {
			if (minVal > 0 && maxVal == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail").gte(minVal)));
			} else if (minVal == 0.0 && maxVal > 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail").lte(maxVal)));
			} else if (minVal == 0.0 && maxVal == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail")));
			} else {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("testFail").gte(minVal).lte(maxVal)));
			}
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, lookupTestHistory, unwindTestHistory, FilterMinMax, matchInclude,
							gropupOperationFilter, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							lookupTestHistory, unwindTestHistory, FilterMinMax, matchInclude, gropupOperationFilter,
							excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, lookupTestHistory, unwindTestHistory, FilterMinMax, gropupOperationFilter,
							excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
							lookupTestHistory, unwindTestHistory, FilterMinMax, gropupOperationFilter, excludeId);
				}
			}

		}

		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

		return testResults;
	}

	private List<Document> generateBinRatioChipsRegularHisto(ObjectId[] waferIds, List<Integer> Include, Boolean search,
			double min, double max, List<FilterParam> filterParams, int dimensionselectedBin,
			MatchOperation matchInclude) {
		AggregationOperation FilterMinMax = null;
		List<Document> testResults = new ArrayList<Document>();
		List<Double> list = new ArrayList<Double>();
		Aggregation aggregationMean = null;
		Aggregation aggregationMeanChipCount = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaChipCount = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (search == true) {
			matchInclude = Aggregation.match(Criteria.where("testNumber").nin(Include));
		} else {
			matchInclude = Aggregation.match(Criteria.where("testNumber").in(Include));
		}
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = null;
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("wd").in(waferIds));
		}
		Criteria testNumberCriteria = (Criteria.where("bn").is(dimensionselectedBin));
		criteriaListForMinMax.add(testNumberCriteria);
		criteriaListForMinMax.add(Criteria.where("tp").is(false));
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		GroupOperation groupOperation = Aggregation.group("$tn", "$tm").first("$tn").as("testNumber").first("$tm")
				.as("testName").count().as("testFail");
		MatchOperation matchZero = Aggregation.match(Criteria.where("testFail").gt(0));

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");
		if (Include != null) {
			if (waferIds.length > 0) {
				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
						matchZero, matchInclude, excludeId);
			} else {

				List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
						lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

				MatchOperation match1 = Aggregation
						.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
				aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
						matchInclude, excludeId);
			}
		} else {
			if (waferIds.length > 0) {
				aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
						matchZero, excludeId);
			} else {

				List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
						lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

				MatchOperation match1 = Aggregation
						.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
				aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation, matchZero,
						excludeId);
			}
		}
		List<Document> BinRatio = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = null;
				criteriaChipCount = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaChipCount.add(Criteria.where("mesWafer._id").in(waferIds));
		}

		MatchOperation matchTest = Aggregation
				.match(new Criteria().andOperator(criteriaChipCount.toArray(new Criteria[criteriaChipCount.size()])));

		GroupOperation gropupOperationTest = Aggregation.group("$_id").sum("$mesWafer.TestChipCount").as("ChipCount")
				.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
		if (waferIds.length > 0) {
			aggregationMeanChipCount = Aggregation.newAggregation(matchTest, gropupOperationTest, excludeId);
		} else {
			aggregationMeanChipCount = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchTest,
					gropupOperationTest, excludeId);
		}
		List<Document> chipCount = mongoTemplate.aggregate(aggregationMeanChipCount, "TestHistory", Document.class)
				.getMappedResults();
		Document val = new Document();
		List<Date> listStartTime = new ArrayList<>();
		List<Date> listEndTime = new ArrayList<>();
		if (chipCount.size() > 0 && BinRatio.size() > 0) {
			Document testChipCount = chipCount.get(0);
			int TestChipCount = (int) testChipCount.get("ChipCount");

			Date startTime = (Date) testChipCount.get("WaferStartTime");
			Date endTime = (Date) testChipCount.get("WaferEndTime");
			for (Document results : BinRatio) {
				int BinCount = 0;
				BinCount = (int) results.get("testFail");
				double TestFailRatio = (double) BinCount / TestChipCount;
				TestFailRatio = TestFailRatio * 100;
				if (min > 0 && max == 0.0) {
					if (TestFailRatio > min) {
						list.add((double) TestFailRatio);
						listStartTime.add(startTime);
						listEndTime.add(endTime);
					}
				}

				else if (min == 0.0 && max > 0) {
					if (TestFailRatio < max) {
						list.add((double) TestFailRatio);
						listStartTime.add(startTime);
						listEndTime.add(endTime);
					}
				} else if (min == 0.0 && max == 0.0) {
					list.add((double) TestFailRatio);
					listStartTime.add(startTime);
					listEndTime.add(endTime);

				} else {
					if (TestFailRatio > min && TestFailRatio < max) {
						list.add((double) TestFailRatio);
						listStartTime.add(startTime);
						listEndTime.add(endTime);
					}
				}

			}
		}

		if (list.size() > 0) {
			val.put("xAxis", list);
			val.put("WaferStartTime", listStartTime);
			val.put("WaferEndTime", listEndTime);
			testResults.add(val);

		}

		return testResults;
	}

	private List<Document> generateNumberOfChipsRegularHisto(ObjectId[] waferIds, List<Integer> Include, Boolean search,
			double minVal, double maxVal, List<FilterParam> filterParams, int dimensionselectedBin,
			MatchOperation matchInclude) {
		AggregationOperation FilterMinMax = null;
		Aggregation aggregationMean = null;
		MatchOperation match1 = null;
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();

		if (search == true) {
			matchInclude = Aggregation.match(Criteria.where("testNumber").nin(Include));
		} else {
			matchInclude = Aggregation.match(Criteria.where("testNumber").in(Include));
		}

		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = null;
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("wd").in(waferIds));

		}
		Criteria testNumberCriteria = (Criteria.where("bn").is(dimensionselectedBin));
		criteriaListForMinMax.add(testNumberCriteria);
		criteriaListForMinMax.add(Criteria.where("tp").is(false));

		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		LookupOperation lookupTestHistory = LookupOperation.newLookup().from("TestHistory").localField("wd")
				.foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		GroupOperation groupOperation = Aggregation.group("$tn", "$tm").first("$tn").as("testNumber").first("$tm")
				.as("testName").count().as("testFail").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
				.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
		MatchOperation matchZero = Aggregation.match(Criteria.where("testFail").gt(0));

		GroupOperation gropupOperationFilter = Aggregation.group().push("testFail").as("xAxis")
				.addToSet("WaferStartTime").as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (waferIds.length == 0) {
			ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
					.andExclude("_id");
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, matchTestFailOperation, projectFetchWafers);

			match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
		}
		if (minVal == 0.0 && maxVal == 0.0) {
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, matchInclude, gropupOperationFilter, excludeId);
				} else {

					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1,lookupTestHistory,
							unwindTestHistory, groupOperation, matchZero,
							matchInclude, gropupOperationFilter, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation,
							lookupTestHistory, unwindTestHistory, groupOperation, matchZero, gropupOperationFilter,
							excludeId);
				} else {

					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupTestHistory,
							unwindTestHistory, groupOperation, matchZero, gropupOperationFilter, excludeId);
				}
			}
		} else {
			if (minVal > 0 && maxVal == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail").gte(minVal)));
			} else if (minVal == 0.0 && maxVal > 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail").lte(maxVal)));
			} else if (minVal == 0.0 && maxVal == 0.0) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("testFail")));
			} else {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("testFail").gte(minVal).lte(maxVal)));
			}
			if (Include != null) {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, FilterMinMax, matchInclude, gropupOperationFilter, excludeId);
				} else {

					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1,lookupTestHistory,
							unwindTestHistory, groupOperation, matchZero,
							FilterMinMax, matchInclude, gropupOperationFilter, excludeId);
				}
			} else {
				if (waferIds.length > 0) {
					aggregationMean = Aggregation.newAggregation(matchForMinMax, matchTestFailOperation, groupOperation,
							matchZero, FilterMinMax, gropupOperationFilter, excludeId);
				} else {

					aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, lookupTestHistory,
							unwindTestHistory,groupOperation, matchZero,
							FilterMinMax, gropupOperationFilter, excludeId);
				}
			}

		}

		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();

		return testResults;
	}

	public List<Document> getCommonDimensionsAndMeasureForRegularHisto(ObjectId[] waferIds, String dimensionName,
			String group, int dimensionselectedBin, double minVal, double maxVal, double scale, double classVal,
			String aggregator, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		AddFieldsOperation addFieldsFilterOperation = null;
		AggregationOperation FilterMinMax = null;
		logger.info("Preparing Query getRegularHistogramDetails");
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
		MatchOperation matchZero = Aggregation.match(Criteria.where("Filterval").gt(0));
		MatchOperation matchZeros = Aggregation.match(Criteria.where("average").gt(0));
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		UnwindOperation unwindWaferStartTime = Aggregation.unwind("$WaferStartTime", true);
		UnwindOperation unwindWaferEndTime = Aggregation.unwind("$WaferEndTime", true);

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

		GroupOperation groupOperation = null;
		GroupOperation gropupOperationFilter = null;

		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];
		if (dimensionName.equals("BinRatio") || dimensionName.equals("NumberOfChipsPerBin")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ dimensionselectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();
			if (dimensionName.equals("NumberOfChipsPerBin")) {
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$GroupingField")
						.build();

				groupOperation = Aggregation.group(grouping).sum("Filterval").as("average")
						.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
						.as("WaferEndTime");
				gropupOperationFilter = Aggregation.group().push("average").as("xAxis").addToSet("WaferStartTime")
						.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
			} else {
				String BinRatioCalculationFormeasure = " {" + "      $multiply: [" + "        {"
						+ "          $divide: [" + "            '$averageField'," + "            '$TestChipCount'"
						+ "          ]" + "        }," + "        100" + "      ]" + "    }";

				BasicDBObject BinRatio = BasicDBObject.parse(BinRatioCalculationFormeasure);
				groupOperation = Aggregation.group(grouping).sum("GroupingField").as("averageField")
						.sum("mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
						.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("average", BinRatio).build();
				gropupOperationFilter = Aggregation.group().push("average").as("xAxis").addToSet("WaferStartTime")
						.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
			}

		} else if (aggregator.equals("Avg")) {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + Dimension).build();
			groupOperation = Aggregation.group(grouping).avg("$Filterval").as("average").min("mesWafer.WaferStartTime")
					.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
			gropupOperationFilter = Aggregation.group().push("average").as("xAxis").addToSet("WaferStartTime")
					.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
		} else {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + Dimension).build();
			groupOperation = Aggregation.group(grouping).sum("$Filterval").as("average").min("mesWafer.WaferStartTime")
					.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
			gropupOperationFilter = Aggregation.group().push("average").as("xAxis").addToSet("WaferStartTime")
					.as("WaferStartTime").addToSet("WaferEndTime").as("WaferEndTime");
		}
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

		if (addFieldsOperation == null) {
			if (minVal == 0.0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, matchZero, groupOperation, gropupOperationFilter, excludeId);
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
				if (dimensionName.equals("BinRatio")) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, groupOperation, addFieldsFilterOperation, matchZeros,
							FilterMinMax, gropupOperationFilter, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsFilterOperation, matchZero, groupOperation, FilterMinMax,
							gropupOperationFilter, excludeId);
				}
			}
		} else {
			if (minVal == 0.0 && maxVal == 0.0) {

				if (dimensionName.equals("NumberOfChipsPerBin")) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, matchZero, groupOperation,
							gropupOperationFilter, excludeId);
				} else if (dimensionName.equals("BinRatio")) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, groupOperation, addFieldsFilterOperation, matchZeros,
							gropupOperationFilter, excludeId);
				}
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
				if (dimensionName.equals("BinRatio")) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, groupOperation, addFieldsFilterOperation, matchZeros,
							FilterMinMax, gropupOperationFilter, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, matchZero, groupOperation,
							FilterMinMax, gropupOperationFilter, excludeId);
				}
			}
		}
		logger.info("Preparing aggregationMean getComparisionHistogramDetails" + aggregationMean);
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
				.getMappedResults();

		return testResults;
	}
}
