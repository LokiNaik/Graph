package com.graph.service.wt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.ConcatArrays;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.service.wt.OneXGraphDetailsService;
import com.graph.model.wt.FilterParam;
import com.graph.utils.CriteriaUtil;
import com.mongodb.BasicDBObject;

@Service
public class TwoXGraphDetailsService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;
	@Autowired
	private OneXGraphDetailsService oneXGraphDetails;
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
	public List<Document> getTwoXTestRelatedMeasure(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
			String aggregator, String granuality, String granuality2X, String DimensionName2X,
			List<FilterParam> filterParams) throws IOException {
		if (testName == null) {
			throw new IOException("TestName is Empty");
		}
		if (testNumber < 0) {
			throw new IOException("TestName is Empty.");
		}
		if (granuality2X != null && granuality2X != "" || granuality != null && granuality != "") {
			throw new IOException("Selected combination is not supported");
		}
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			grouping = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(DimensionName2X);

		if (Dimension2X == null) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension) && !grouping.equals("mesWafer.WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (grouping.equals(Dimension2X) && !grouping.equals("mesWafer.WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (Dimension.startsWith("$")) {
			Dimension = Dimension.split("\\$")[1];
		}
		if (Dimension2X.startsWith("$")) {
			Dimension2X = Dimension2X.split("\\$")[1];
		}

		AddFieldsOperation addFieldsOperation2X = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + grouping).addFieldWithValue("$Dimension1X", "$" + Dimension)
				.addFieldWithValue("$Dimension2X", "$" + Dimension2X).addFieldWithValue("$Wafer", "$mesWafer._id")
				.build();

		List<Document> TestResult = null;
		switch (measureName) {
		case "TestPassChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTwoXTestFields(waferIds, testNumber, testName, true, addFieldsOperation2X,
						Dimension, Dimension2X, grouping, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailChips":
			if (aggregator.equals("Sum")) {
				TestResult = generateTwoXTestFields(waferIds, testNumber, testName, false, addFieldsOperation2X,
						Dimension, Dimension2X, grouping, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatio":
			if (aggregator.equals("Avg")) {
				TestResult = generateTwoXTestFailedRatioFields(waferIds, testNumber, testName, false, grouping,
						Dimension, Dimension2X, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		case "TestFailedRatioActualCount":
			if (aggregator.equals("Avg")) {
				TestResult = generateTwoXWithActualCountFields(waferIds, testNumber, testName, grouping, Dimension,
						Dimension2X, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
			break;
		default:
			throw new IOException("Selected combination is not supported");
		}
		return TestResult;
	}

	/*
	 * 
	 */

	private List<Document> generateTwoXTestFields(ObjectId[] waferIds, int testNumber, String testName, boolean status,
			AddFieldsOperation addFieldsOperation2X, String Dimension, String Dimension2X, String grouping, double min,
			double max, List<FilterParam> filterParams) {

		logger.info("Preparing Query getComparisionHistogramDetails");
		String CollectionName = null;
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
		Criteria testCriteriaFail = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteriaFail);
		Criteria testNameCriteriaFail = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteriaFail);
		Criteria testFailCriteria = (Criteria.where("tp").is(status));
		criteriaListForMinMax.add(testFailCriteria);
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
		Aggregation aggregationMean = null;
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
		if (addFieldsOperation2X != null) {
			List<Criteria> criteriaList = new ArrayList<>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

			}
			MatchOperation matchOperationTest = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

			GroupOperation groupOperationFail = Aggregation
					.group("GroupingField", "Dimension1X", "Dimension2X", "Wafer").min("mesWafer.WaferStartTime")
					.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id")
					.as("waferID");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.$Dimension1X").as("Dimension1X").and("$_id.$Dimension2X").as("Dimension2X")
					.and("$_id.Wafer").as("waferId").andArrayOf("$WaferStartTime").as("WaferStartTime")
					.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID").andExclude("_id");

			Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchOperationTest, addFieldsOperation2X, groupOperationFail, projectFetchData);
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}
		logger.info("Preparing aggregationMean getComparisionHistogramDetails" + aggregationMean);
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (grouping.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						}

					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				List<ObjectId> listWafers = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
							listWafers = (List<ObjectId>) results.get("waferID");

						}
					}
				}
				if (count > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					List<String> list1 = new ArrayList<>();
					if (grouping != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);

					} else {
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);

					}

					val.put("xAxis", list1);
					val.put("WaferStartTime", listStart);
					val.put("WaferEndTime", listEnd);
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

	/*
	 * 
	 */
	private List<Document> generateTwoXTestFailedRatioFields(ObjectId[] waferIds, int testNumber, String testName,
			boolean status, String group, String Dimension, String Dimension2X, double min, double max,
			List<FilterParam> filterParams) {
		String CollectionName = null;
		List<Document> keyResultsMean = null;
		List<Document> testFaileRatioDocuments = new ArrayList<>();
		logger.info("getComparisionHistogramDetails");
		List<Criteria> criteriaListTestFail = new ArrayList<Criteria>();
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
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

		Aggregation aggregationMean = null;

		AddFieldsOperation addFieldsOperation2X = Aggregation.addFields()
				.addFieldWithValue("$GroupingField", "$" + group).addFieldWithValue("$Dimension1X", "$" + Dimension)
				.addFieldWithValue("$Dimension2X", "$" + Dimension2X).addFieldWithValue("$Wafer", "$mesWafer._id")
				.addFieldWithValue("$TestChipCount", "$mesWafer.TestChipCount").build();
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));

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
		if (addFieldsOperation2X != null) {
			List<Criteria> criteriaList = new ArrayList<>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

			}
			MatchOperation matchOperationTest = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
			;
			GroupOperation groupOperationFailval = Aggregation
					.group("GroupingField", "Dimension1X", "Dimension2X", "Wafer").sum("TestChipCount")
					.as("TestChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime")
					.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.$Dimension1X").as("Dimension1X").and("$_id.$Dimension2X").as("Dimension2X")
					.and("$_id.Wafer").as("waferId").andExclude("_id").and("TestChipCount").as("TestChipCount")
					.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime")
					.and("$waferID").as("waferID");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchOperationTest, addFieldsOperation2X, groupOperationFailval, projectFetchData, excludeId);
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (group.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						}
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int count = 0;
				int TestChip = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				List<ObjectId> listWafers = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {
						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							count += (int) testval.get("TestPass");
							TestChip += (int) results.get("TestChipCount");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
							listWafers = (List<ObjectId>) results.get("waferID");
						}
					}
				}
				if (count > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					List<String> list1 = new ArrayList<>();
					if (group != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);
					} else {
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);
					}
					val.put("xAxis", list1);
					val.put("WaferStartTime", listStart);
					val.put("WaferEndTime", listEnd);
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
	private List<Document> generateTwoXWithActualCountFields(ObjectId[] waferIds, int testNumber, String testName,
			String group, String Dimension, String Dimension2X, double min, double max,
			List<FilterParam> filterParams) {

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
		List<Criteria> criteriaListTest = new ArrayList<Criteria>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {

				criteriaListTest = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaListTest.add(Criteria.where("wd").in(waferIds));

		}
		Criteria testCriteria = (Criteria.where("tn").is(testNumber));
		criteriaListForMinMax.add(testCriteria);
		Criteria testNameCriteria = (Criteria.where("tm").is(testName));
		criteriaListForMinMax.add(testNameCriteria);

		AddFieldsOperation addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", "$" + group)
				.addFieldWithValue("$Dimension1X", "$" + Dimension).addFieldWithValue("$Dimension2X", "$" + Dimension2X)
				.addFieldWithValue("$Wafer", "$mesWafer._id").build();
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));
		MatchOperation matchTestFailOperation = Aggregation
				.match(new Criteria().andOperator(criteriaListTest.toArray(new Criteria[criteriaListTest.size()])));
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
			List<Criteria> criteriaList = new ArrayList<>();
			if (waferIds.length == 0) {
				if (filterParams != null && filterParams.size() >= 1) {
					criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
				}
			} else {
				criteriaList.add(Criteria.where("mesWafer._id").in(waferIds));

			}
			MatchOperation matchOperationTest = Aggregation
					.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

			GroupOperation groupOperationFailval = Aggregation
					.group("GroupingField", "Dimension1X", "Dimension2X", "Wafer").min("mesWafer.WaferStartTime")
					.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id")
					.as("waferID");
			ProjectionOperation projectFetchData = Aggregation.project().and("_id.GroupingField").as("group")
					.and("$_id.$Dimension1X").as("Dimension1X").and("$_id.$Dimension2X").as("Dimension2X")
					.and("$_id.Wafer").as("waferId").andArrayOf("$WaferStartTime").as("WaferStartTime")
					.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID").andExclude("_id");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregationMeanTest = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchOperationTest, addFieldsOperation, groupOperationFailval, projectFetchData, excludeId);
			keyResultsMean = mongoTemplate.aggregate(aggregationMeanTest, "TestHistory", Document.class)
					.getMappedResults();
		}
		if (keyResultsMean.size() > 0 && testResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = keyResultsMean.stream()
					.collect(Collectors.groupingBy(results -> {
						if (group.equals("mesWafer._id")) {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						} else {
							return results.get("group").toString() + "$" + results.get("Dimension1X").toString() + "$"
									+ results.get("Dimension2X").toString();
						}
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				int TestPass = 0;
				int TestFail = 0;
				List<String> listStart = new ArrayList<>();
				List<String> listEnd = new ArrayList<>();
				List<ObjectId> listWafers = new ArrayList<>();
				for (Document results : valu) {
					for (Document testval : testResults) {

						if ((results.get("waferId").toString().equals(testval.get("waferId").toString()))) {
							TestPass += (int) testval.get("TestPass");
							TestFail += (int) testval.get("TestFail");
							listStart = (List<String>) results.get("WaferStartTime");
							listEnd = (List<String>) results.get("WaferEndTime");
							listWafers = (List<ObjectId>) results.get("waferID");
						}
					}
				}
				if (TestPass > 0 || TestFail > 0) {
					String KeyValue = (String) keyVal;
					String[] Keyparts = KeyValue.split("\\$");
					Document val = new Document();
					List<String> list1 = new ArrayList<>();
					if (group != "mesWafer._id") {
						val.put("group", (String) Keyparts[0]);
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);
					} else {
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);
					}
					val.put("xAxis", list1);
					val.put("WaferStartTime", listStart);
					val.put("WaferEndTime", listEnd);
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

	public List<Document> getTwoXDUTService(ObjectId[] waferIds, String groupingDim, String Group_DUT, String dimension, String measure,
			int chipsPerBin, double minVal, double maxVal, String aggregator, String granuality2x,
			String dimensionName2X, List<FilterParam> filterParams) throws IOException {
		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(true))
				.then("$ChipCount").otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(false))
				.then("$ChipCount").otherwise(0);

		GROUP_DUT = Group_DUT;
		Aggregation aggregationMean = null;
		Aggregation aggregationDUTTest = null;
		List<Criteria> criteriaList = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				String CollectionName = "TestHistory";
				for (int i = 0; i < filterParams.size(); i++) {
					if (filterParams.get(i).getField().equals("Device")) {
						filterParams.get(i).setField("TestHistory.ProductInfo.deviceName");
						System.out.println(filterParams.get(i).getField());
					}
				}
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

		LookupOperation lookupOperationTestHistory = LookupOperation.newLookup().from("TestHistory")
				.localField("_id.waferID").foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
				.localField("TestHistory.mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
		UnwindOperation unwindProduct = Aggregation.unwind("$TestHistory.ProductInfo", true);
		ProjectionOperation projectFieldSelectionOperation = null;

		String grouping = CriteriaUtil.generateFieldNameForCriteria(groupingDim);
		if (grouping == null) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimension);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(dimensionName2X);
		if (Dimension2X == null) {
			throw new IOException("Selected combination is not supported");
		}

		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measure);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}

		List<Document> dutResults = null;
		ProjectionOperation projectField = null;

		if (measure.equalsIgnoreCase("NumberOfChipsPerBin")) {
			dutResults = getNumberOfChips2X(grouping, Dimension, chipsPerBin, matchOperation, matchTestOperation,
					matchBinOperation, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					Dimension2X);
			dutResults = extractDocument2X(grouping, measure, dutResults, chipsPerBin, minVal, maxVal);
			return dutResults;
		} else if (measure.equalsIgnoreCase("BinRatio")) {
			List<Document> binRatioDocument = new ArrayList<>();
			dutResults = getBinRatio2X(grouping, Dimension, chipsPerBin, matchOperation, matchTestOperation,
					matchBinOperation, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					Dimension2X);
			dutResults = extractDocument2X(grouping, "NumberOfChipsBinRatio", dutResults, chipsPerBin, minVal, maxVal);

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

			groupOperation = Aggregation.group(Dimension, "waferID").count().as("totalCount");
			projectFieldSelectionOperation = Aggregation.project().and("_id.DUTNumber").as("dimension")
					.and("_id.waferID").as("waferID").and("totalCount").as("totalCount").andExclude("_id");
			if (waferIds.length > 0) {
				aggregationDUTTest = Aggregation.newAggregation(matchOperationBinRatio, groupOperation,
						projectFieldSelectionOperation);
			} else {
				List<Criteria> criteriaListforwafers = new ArrayList<>();
				LookupOperation lookupProductAuto = LookupOperation.newLookup().from("ProductInfo")
						.localField("mesWafer.productId").foreignField("_id").as("TestHistory.ProductInfo");
				ProjectionOperation projectFetchWafer = Aggregation.project().and("$mesWafer._id").as("waferId")
						.andExclude("_id");
				Aggregation aggregationResults = Aggregation.newAggregation(lookupProductAuto, unwindProduct,
						matchOperationBinRatio, projectFetchWafer);
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
				MatchOperation match1 = Aggregation.match(new Criteria()
						.andOperator(criteriaListforwafers.toArray(new Criteria[criteriaListforwafers.size()])));
				aggregationDUTTest = Aggregation.newAggregation(match1, groupOperation, projectFieldSelectionOperation);
			}
			List<Document> dutResultsBin = mongoTemplate.aggregate(aggregationDUTTest, "MESTest", Document.class)
					.getMappedResults();

			if (dutResults.size() > 0 && dutResultsBin.size() > 0) {

				Map<Object, List<Document>> comblistGrouped = dutResults.stream()
						.collect(Collectors.groupingBy(results -> {
							return results.get("group").toString() + "$" + results.get("xAxis").toString() + "$"
									+ results.get("xAxis2").toString();
						}));

				comblistGrouped.forEach((keyVal, valu) -> {
					int count = 0;
					int testChipCount = 0;
					List<String> listStart = new ArrayList<>();
					List<String> listEnd = new ArrayList<>();
					List<ObjectId> listWafers = new ArrayList<>();
					for (Document results : valu) {
						for (Document totalCountBin : dutResultsBin) {

							if ((results.get("waferID").toString().equals(totalCountBin.get("waferID").toString()))) {
								count += (int) results.get("yAxis");
								testChipCount += (int) totalCountBin.get("totalCount");
								listStart = (List<String>) results.get("WaferStartTime");
								listEnd = (List<String>) results.get("WaferEndTime");
								listWafers = (List<ObjectId>) results.get("WaferID");

							}

						}

					}
					if (count > 0 || testChipCount > 0) {
						String KeyValue = (String) keyVal;
						String[] Keyparts = KeyValue.split("\\$");
						Document val = new Document();
						val.put("group", (String) Keyparts[0]);
						List<String> list1 = new ArrayList<>();
						list1.add((String) Keyparts[1]);
						list1.add((String) Keyparts[2]);
						val.put("xAxis", list1);
						val.put("WaferStartTime", listStart);
						val.put("WaferEndTime", listEnd);
						val.put("waferID", listWafers);
						count = count * 100;
						double TestFailRatio = (double) count / testChipCount;
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
			if (measure.equalsIgnoreCase("Yield") && aggregator.equalsIgnoreCase("Sum")) {
				throw new IOException("Selected combination is not supported");
			}
			if (Dimension.equals("DUTNumber")) {
				groupOperation = Aggregation.group(Dimension, "mapID", "TestPass", "waferID").count().as("ChipCount");
				if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
					projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
							.and("$_id." + Dimension).as("dimension").and("$TestHistory." + Dimension2X)
							.as("dimension2X").and(passChips).as("passChips").and(failChips).as("failChips")
							.and("TestHistory").as("TestHistory").andExclude("_id");
				} else {
					projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
							.and("$_id." + Dimension).as("dimension").and("$TestHistory." + Dimension2X)
							.as("dimension2X").and(passChips).as("passChips").and(failChips).as("failChips")
							.and("TestHistory").as("TestHistory").andExclude("_id");
				}

			} else {
				groupOperation = Aggregation.group(Dimension2X, "mapID", "TestPass", "waferID").count().as("ChipCount");
				if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
					projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
							.and("$_id." + Dimension2X).as("dimension2X").and("$TestHistory." + Dimension)
							.as("dimension").and(passChips).as("passChips").and(failChips).as("failChips")
							.and("TestHistory").as("TestHistory").andExclude("_id");
				} else {
					projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
							.and("$_id." + Dimension2X).as("dimension2X").and("$TestHistory." + Dimension)
							.as("dimension").and(passChips).as("passChips").and(failChips).as("failChips")
							.and("TestHistory").as("TestHistory").andExclude("_id");
				}

			}

			groupOperationTest = Aggregation.group("grouping", "dimension", "dimension2X").sum("failChips")
					.as("failChips").sum("passChips").as("passChips").min("TestHistory.mesWafer.WaferStartTime")
					.as("WaferStartTime").max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime")
					.addToSet("TestHistory.mesWafer._id").as("waferID");

			List<String> DimensionExp = Arrays.asList(("$_id.dimension"));
			List<String> Dimension2Exp = Arrays.asList(("$_id.dimension2X"));
			List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
					.collect(Collectors.toList());

			AddFieldsOperation addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
					.withValue(ConcatArrays.arrayOf(newDimeList)).build();

			projectFieldTest = Aggregation.project().and("_id.grouping").as("grouping").and("$xAxis").as("xAxis")
					.and("passChips").as("passChips").and("failChips").as("failChips").andArrayOf("$WaferStartTime")
					.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			aggregationMean = Aggregation.newAggregation(groupOperation, lookupOperationTestHistory, unwindTestHistory,
					lookupProduct, unwindProduct, matchOperation, projectField, groupOperationTest,
					addFieldsOperationFor2X, projectFieldTest);
			dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
			dutResults = extractDocument2X(grouping, measure, dutResults, chipsPerBin, minVal, maxVal);
			return dutResults;
		}

	}

	private List<Document> getNumberOfChips2X(String grouping, String dimension, int chipsPerBin,
			MatchOperation matchOperation, MatchOperation matchTestOperation, MatchOperation matchBinOperation,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String Dimension2X) {
		Aggregation aggregationMean;
		GroupOperation groupOperation;
		GroupOperation groupOperationNext;
		ProjectionOperation projectFieldSelectionOperation;
		List<Document> dutResults;
		ProjectionOperation projectField;
		if (dimension.equals("DUTNumber")) {
			groupOperation = Aggregation.group(dimension, "mapID", "TestPass", "waferID").count().as("totalCount");
			if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension).as("dimension").and("$TestHistory." + Dimension2X).as("dimension2X")
						.and("TestHistory").as("TestHistory").and("totalCount").as("totalCount");
			} else {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension).as("dimension").and("$TestHistory." + Dimension2X).as("dimension2X")
						.and("TestHistory").as("TestHistory").and("totalCount").as("totalCount");
			}

		} else {
			groupOperation = Aggregation.group(Dimension2X, "mapID", "TestPass", "waferID").count().as("totalCount");
			if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + Dimension2X).as("dimension2X").and("$TestHistory." + dimension).as("dimension")
						.and("TestHistory").as("TestHistory").and("totalCount").as("totalCount");
			} else {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + Dimension2X).as("dimension2X").and("$TestHistory." + dimension).as("dimension")
						.and("TestHistory").as("TestHistory").and("totalCount").as("totalCount");
			}

		}

		List<String> DimensionExp = Arrays.asList(("$_id.dimension"));
		List<String> Dimension2Exp = Arrays.asList(("$_id.dimension2X"));
		List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
				.collect(Collectors.toList());

		AddFieldsOperation addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
				.withValue(ConcatArrays.arrayOf(newDimeList)).build();

		groupOperationNext = Aggregation.group("grouping", "dimension", "dimension2X").sum("totalCount")
				.as("totalCount").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
				.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime").addToSet("TestHistory.mesWafer._id")
				.as("waferID");
		;
		projectFieldSelectionOperation = Aggregation.project().and("_id.grouping").as("grouping").and("$xAxis")
				.as("dimension").and("totalCount").as("binCount").andArrayOf("$WaferStartTime").as("WaferStartTime")
				.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID").andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchTestOperation, matchBinOperation, groupOperation,
				lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct, matchOperation,
				projectField, groupOperationNext, addFieldsOperationFor2X, projectFieldSelectionOperation);
		dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
		return dutResults;
	}

	private List<Document> extractDocument2X(String grouping, String measure, List<Document> dutResults,
			int chipsPerBin, double min, double max) throws IOException {
		List<Document> dutResultDocuments = new ArrayList<>();
		for (Document obj : dutResults) {
			Document val = new Document();
			if (measure.equalsIgnoreCase("Yield")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> dutList = new ArrayList<String>();
					dutList = (List<String>) obj.get("xAxis");
					for (int i = 0; i <= dutList.size(); i++) {
						val.put("group", dutList.get(0));
						break;
					}
					dutList.set(0, (String) obj.get("grouping"));

					val.put("xAxis", dutList);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));

				} else {
					val.put("group", obj.get("grouping"));
					List<?> DimensionArray = (List<?>) obj.get("xAxis");
					val.put("xAxis", DimensionArray);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				}

				Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					double yield = ((double) passChip / (double) totalCount) * 100;
					double[] arr1 = new double[1];
					if (yield > 0) {
						if (min > 0 && max == 0.0) {
							if (yield > min) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}

						else if (min == 0.0 && max > 0) {
							if (yield < max) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						} else if (min == 0.0 && max == 0.0) {
							arr1[0] = yield;
							val.put("yAxis", arr1);

						} else {
							if (yield > min && yield < max) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}

					}
				}
			} else if (measure.equalsIgnoreCase("FailRatio")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> dutList = new ArrayList<String>();
					dutList = (List<String>) obj.get("xAxis");
					for (int i = 0; i <= dutList.size(); i++) {
						val.put("group", dutList.get(0));
						break;
					}
					dutList.set(0, (String) obj.get("grouping"));

					val.put("xAxis", dutList);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				} else {
					val.put("group", obj.get("grouping"));
					List<?> DimensionArray = (List<?>) obj.get("xAxis");
					val.put("xAxis", DimensionArray);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				}

				Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					double yield = ((double) failChip / (double) totalCount) * 100;
					double[] arr1 = new double[1];
					if (yield > 0) {
						if (min > 0 && max == 0.0) {
							if (yield > min) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}

						else if (min == 0.0 && max > 0) {
							if (yield < max) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						} else if (min == 0.0 && max == 0.0) {
							arr1[0] = yield;
							val.put("yAxis", arr1);

						} else {
							if (yield > min && yield < max) {
								arr1[0] = yield;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfPassedChips")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> dutList = new ArrayList<String>();
					dutList = (List<String>) obj.get("xAxis");
					for (int i = 0; i <= dutList.size(); i++) {
						val.put("group", dutList.get(0));
						break;
					}
					dutList.set(0, (String) obj.get("grouping"));

					val.put("xAxis", dutList);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));

				} else {
					val.put("group", obj.get("grouping"));
					List<?> DimensionArray = (List<?>) obj.get("xAxis");
					val.put("xAxis", DimensionArray);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				}
				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					double[] arr = new double[1];
					double[] arr1 = new double[1];
					if (passChip > 0) {
						if (min > 0 && max == 0.0) {
							if (passChip > min) {
								arr1[0] = passChip;
								val.put("yAxis", arr1);
							}
						}

						else if (min == 0.0 && max > 0) {
							if (passChip < max) {
								arr1[0] = passChip;
								val.put("yAxis", arr1);
							}
						} else if (min == 0.0 && max == 0.0) {
							arr1[0] = passChip;
							val.put("yAxis", arr1);

						} else {
							if (passChip > min && passChip < max) {
								arr1[0] = passChip;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfFailedChips")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> dutList = new ArrayList<String>();
					dutList = (List<String>) obj.get("xAxis");
					for (int i = 0; i <= dutList.size(); i++) {
						val.put("group", dutList.get(0));
						break;
					}
					dutList.set(0, (String) obj.get("grouping"));

					val.put("xAxis", dutList);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));

				} else {
					val.put("group", obj.get("grouping"));
					List<?> DimensionArray = (List<?>) obj.get("xAxis");
					val.put("xAxis", DimensionArray);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				}
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					double[] arr = new double[1];
					double[] arr1 = new double[1];
					if (failChip > 0) {
						if (min > 0 && max == 0.0) {
							if (failChip > min) {
								arr1[0] = failChip;
								val.put("yAxis", arr1);
							}
						}

						else if (min == 0.0 && max > 0) {
							if (failChip < max) {
								arr1[0] = failChip;
								val.put("yAxis", arr1);
							}
						} else if (min == 0.0 && max == 0.0) {
							arr1[0] = failChip;
							val.put("yAxis", arr1);

						} else {
							if (failChip > min && failChip < max) {
								arr1[0] = failChip;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("NumberOfChipsPerBin")) {
				if (GROUP_DUT.equals("DUT")) {
					List<String> dutList = new ArrayList<String>();
					dutList = (List<String>) obj.get("dimension");
					for (int i = 0; i <= dutList.size(); i++) {
						val.put("group", dutList.get(0));
						break;
					}
					dutList.set(0, (String) obj.get("grouping"));
					val.put("xAxis", dutList);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				} else {
					val.put("group", obj.get("grouping"));
					List<?> DimensionArray = (List<?>) obj.get("dimension");
					val.put("xAxis", DimensionArray);
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("waferID", (obj.get("waferID")));
				}

				if (obj.get("binCount") != null) {
					Integer binCount = (Integer) obj.get("binCount");
					double[] arr = new double[1];
					double[] arr1 = new double[1];
					if (binCount > 0) {
						if (min > 0 && max == 0.0) {
							if (binCount > min) {
								arr1[0] = binCount;
								val.put("yAxis", arr1);
							}
						}

						else if (min == 0.0 && max > 0) {
							if (binCount < max) {
								arr1[0] = binCount;
								val.put("yAxis", arr1);
							}
						} else if (min == 0.0 && max == 0.0) {
							arr1[0] = binCount;
							val.put("yAxis", arr1);

						} else {
							if (binCount > min && binCount < max) {
								arr1[0] = binCount;
								val.put("yAxis", arr1);
							}
						}
					}
				}
			} else if (measure.equalsIgnoreCase("BinRatio")) {
				val.put("group", obj.get("grouping"));
				val.put("xAxis", obj.get("dimension"));
				val.put("WaferStartTime", (obj.get("WaferStartTime")));
				val.put("WaferEndTime", (obj.get("WaferEndTime")));
				val.put("waferID", (obj.get("waferID")));

				if (obj.get("binCount") != null) {
					Integer binCount = (Integer) obj.get("binCount");
					double[] arr = new double[1];
					double[] arr1 = new double[1];
					arr1[0] = binCount;
					val.put("yAxis", arr1);
				}
			} else if (measure.equalsIgnoreCase("NumberOfChipsBinRatio")) {

				val.put("waferID", obj.get("WaferId"));
				if (GROUP_DUT.equals("DUT")) {
					val.put("group", obj.get("dimension"));
					val.put("xAxis", obj.get("grouping"));
					val.put("xAxis2", obj.get("dimension2X"));
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("WaferID", (obj.get("binWaferId")));
				} else {
					val.put("group", obj.get("grouping"));
					val.put("xAxis", obj.get("dimension"));
					val.put("xAxis2", obj.get("dimension2X"));
					val.put("WaferStartTime", (obj.get("WaferStartTime")));
					val.put("WaferEndTime", (obj.get("WaferEndTime")));
					val.put("WaferID", (obj.get("binWaferId")));
				}
				if (obj.get("binCount") != null) {
					Integer binCount = (Integer) obj.get("binCount");
					val.put("yAxis", binCount);
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

	private List<Document> getBinRatio2X(String grouping, String dimension, int chipsPerBin,
			MatchOperation matchOperation, MatchOperation matchTestOperation, MatchOperation matchBinOperation,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String dimension2x) {

		Aggregation aggregationMean;
		GroupOperation groupOperation;
		GroupOperation groupOperationNext;
		ProjectionOperation projectFieldSelectionOperation;
		List<Document> dutResults;
		ProjectionOperation projectField;

		if (dimension.equals("DUTNumber")) {
			groupOperation = Aggregation.group(dimension, "mapID", "TestPass", "waferID").count().as("totalCount");
			if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension).as("dimension").and("$TestHistory." + dimension2x).as("dimension2X")
						.and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
						.and("TestHistory").as("TestHistory");
			} else {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension).as("dimension").and("$TestHistory." + dimension2x).as("dimension2X")
						.and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
						.and("TestHistory").as("TestHistory");
			}

		} else {
			groupOperation = Aggregation.group(dimension2x, "mapID", "TestPass", "waferID").count().as("totalCount");
			if (grouping.contains("deviceName") || grouping.contains("deviceGroup")) {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension2x).as("dimension2X").and("$TestHistory." + dimension).as("dimension")
						.and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
						.and("TestHistory").as("TestHistory");
			} else {
				projectField = Aggregation.project().and("$TestHistory." + grouping).as("grouping")
						.and("$_id." + dimension2x).as("dimension2X").and("$TestHistory." + dimension).as("dimension")
						.and("$TestHistory.mesWafer._id").as("waferID").and("totalCount").as("totalCount")
						.and("TestHistory").as("TestHistory");
			}

		}

		groupOperationNext = Aggregation.group("grouping", "dimension", "dimension2X", "$waferID").sum("totalCount")
				.as("totalCount").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
				.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
		projectFieldSelectionOperation = Aggregation.project().and("_id.grouping").as("grouping").and("$_id.dimension")
				.as("dimension").and("$_id.dimension2X").as("dimension2X").and("totalCount").as("binCount")
				.and("waferID").as("WaferId").andArrayOf("$WaferStartTime").as("WaferStartTime")
				.andArrayOf("$WaferEndTime").as("WaferEndTime").andArrayOf("$_id.waferID").as("binWaferId")
				.andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchTestOperation, matchBinOperation, groupOperation,
				lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct, matchOperation,
				projectField, groupOperationNext, projectFieldSelectionOperation);
		dutResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();
		return dutResults;

	}

	/*
	 * 
	 */

	public List<Document> getGroupByBin2X(ObjectId[] waferIds, String measureName, String group, String dimensionName,
			String testName, int testNumber, String dimensionName2X, double minVal, double maxVal, String Aggregator,
			List<FilterParam> filterParams) throws IOException {

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(dimensionName2X);
		List<Document> testResults = new ArrayList<Document>();
		handleAxisExceptions2X(grouping, Dimension, MeasureVal, Dimension2X);
		boolean binGroupingCountableMeasures = Arrays.stream(binGroupingList).anyMatch(measureName::equals);

		if (Dimension.equals("DUTNumber") || Dimension2X.equals("DUTNumber")) {
			throw new IOException("Selected combination is not supported");
		}
		if (binGroupingCountableMeasures) {
			if (Aggregator.equals("Sum")) {
				testResults = getBinGroupingCountableMeasuresFor2x(waferIds, measureName, testName, testNumber,
						grouping, Dimension, Dimension2X, minVal, maxVal, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
		} else if (measureName.equals(BIN_RATIO)) {
			if (Aggregator.equals("Avg")) {
				testResults = getBinRatioBinsFor2X(waferIds, grouping, Dimension, Dimension2X, filterParams);
			} else {
				throw new IOException("Selected combination is not supported");
			}
		} else {
			throw new IOException("Selected combination is not supported");
		}

		return oneXGraphDetails.getMeasureCount(measureName, testResults, minVal, maxVal);
	}

	private void handleAxisExceptions2X(String grouping, String Dimension, String MeasureVal, String Dimension2X)
			throws IOException {
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
		if (Dimension2X == null) {
			throw new IOException("Selected combination is not supported");
		}
	}

	private List<Document> getBinGroupingCountableMeasuresFor2x(ObjectId[] waferIds, String measureName,
			String testName, int testNumber, String grouping, String Dimension, String dimension2x, double min,
			double max, List<FilterParam> filterParams) {

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
		String[] dimension2XArray = dimension2x.split("\\.");
		String dimensionSplit = dimensionArray[1];
		String dimension2XSplit = dimension2XArray[1];
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
						.and(Fields.fields().and(dimensionSplit, Dimension))
						.and(Fields.fields().and(dimension2XSplit, dimension2x)))
				.first("$mesWafer.BinSummary.BinCount.BINNumber").as("BINNumber")
				.first("$mesWafer.BinSummary.TestPassBINNumber").as("TestPassBINNumber").first(Dimension)
				.as(dimensionSplit).first(dimension2x).as(dimension2XSplit).push("$mesWafer._id").as("waferId")
				.sum("$mesWafer.BinSummary.BinCount.Count").as("average").min("mesWafer.WaferStartTime")
				.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id")
				.as("waferID");

		List<String> DimensionExp = Arrays.asList(("$" + dimensionSplit));
		List<String> Dimension2Exp = Arrays.asList(("$" + dimension2XSplit));
		List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
				.collect(Collectors.toList());

		AddFieldsOperation addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
				.withValue(ConcatArrays.arrayOf(newDimeList)).build();

		projectFetchData = Aggregation.project().and("waferId").as("waferId").and("BINNumber").as("BINNumber")
				.and("TestPassBINNumber").as("TestPassBINNumber").and("_id.BINNumber").as("group").and("$xAxis")
				.as("xAxis").andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
				.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");

		aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
				unwindBinNumber, groupOperation, addFieldsOperationFor2X, projectFetchData, excludeId);

		testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class).getMappedResults();

		if (measureName.equals("TestPassChips")) {
			testResults = oneXGraphDetails.getGroupbyBinTestPassChips(waferIds, testName, testNumber,
					testHistoryResults, testResults, min, max, filterParams);
		}
		return testResults;
	}

	/*
	 * GroupByBins for 2X comb
	 * NumberOfChipsPerBin,NumberOfFailedChips,NumberOfPassedChips,TestPassChips
	 */
	private List<Document> getBinRatioBinsFor2X(ObjectId[] waferIds, String grouping, String Dimension,
			String dimension2x, List<FilterParam> filterParams) {

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
		String[] dimension2XArray = dimension2x.split("\\.");
		String dimension2XSplit = dimension2XArray[1];
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
		;
		groupOperation = Aggregation
				.group(Fields.fields().and(groupingArray[3].toString(), grouping)
						.and(Fields.fields().and(dimensionSplit, Dimension))
						.and(Fields.fields().and(dimension2XSplit, dimension2x)))
				.first(Dimension).as(dimensionSplit).first(dimension2x).as(dimension2XSplit).first("$mesWafer._id")
				.as("waferId").first("$mesWafer.BinSummary.BinCount.BINNumber").as("BINNumber")
				.sum("$mesWafer.BinSummary.BinCount.Count").as("average").sum("$mesWafer.TestChipCount")
				.as("TestChipCount").min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
				.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");

		List<String> DimensionExp = Arrays.asList(("$" + dimensionSplit));
		List<String> Dimension2Exp = Arrays.asList(("$" + dimension2XSplit));
		List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
				.collect(Collectors.toList());

		AddFieldsOperation addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis")
				.withValue(ConcatArrays.arrayOf(newDimeList)).build();

		projectFetchData = Aggregation.project().and("waferId").as("waferId").and("BINNumber").as("BINNumber")
				.and("_id.BINNumber").as("group").and("$xAxis").as("xAxis").and("$average").as("yAxis")
				.and("TestChipCount").as("TestChipCount").andArrayOf("$WaferStartTime").as("WaferStartTime")
				.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");

		aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
				unwindBinNumber, groupOperation, addFieldsOperationFor2X, projectFetchData, excludeId);
		testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class).getMappedResults();
		return testResults;

	}

	public List<Document> getTwoXCommonDimensionsAndMeasure(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, int selectedBin, String testName, int testNumber, double minVal,
			double maxVal, String aggregator, String DimensionName2X, List<FilterParam> filterParams)
			throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsOperationFor2X = null;
		Aggregation aggregationMean = null;
		AddFieldsOperation addFieldsFilterOperation = null;
		AggregationOperation FilterMinMax = null;
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
		String Dimension2X = CriteriaUtil.generateFieldNameForCriteria(DimensionName2X);
		if (Dimension2X == null) {
			throw new IOException("Selected combination is not supported");
		}

		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
		GroupOperation groupOperation = null;
		String[] parts = Dimension.split("\\.");
		String dimension1X = parts[1];
		String[] parts2X = Dimension2X.split("\\.");
		String dimension2X = parts2X[1];

		String[] groupparts = grouping.split("\\.");
		String grouping2 = groupparts[1];

		if (grouping2.equals(dimension1X) && !grouping2.equals("WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		} else if (grouping2.equals(dimension2X) && !grouping2.equals("WaferNumber")) {
			throw new IOException("Selected combination is not supported");
		}

		List<String> DimensionExp = Arrays.asList(("$_id." + dimension1X));
		List<String> Dimension2Exp = Arrays.asList(("$_id." + dimension2X));
		List<String> newDimeList = Stream.concat(DimensionExp.stream(), Dimension2Exp.stream())
				.collect(Collectors.toList());

		addFieldsOperationFor2X = Aggregation.addFields().addField("xAxis").withValue(ConcatArrays.arrayOf(newDimeList))
				.build();

		if (measureName.equals("BinRatio") || measureName.equals("NumberOfChipsPerBin")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();
			if (measureName.equals("NumberOfChipsPerBin")) {
				addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$GroupingField")
						.build();
				if ((grouping2.equals(dimension2X) && grouping2.equals("WaferNumber"))
						|| (grouping2.equals(dimension1X) && grouping2.equals("WaferNumber"))) {
					if (dimension2X.equals("WaferNumber")) {
						groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
								.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
								.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
					} else {
						groupOperation = Aggregation.group(grouping, Dimension2X).sum("Filterval").as("average")
								.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
								.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
					}
				} else {
					if (dimension1X.equals(dimension2X)) {
						groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
								.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
								.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
					} else {
						groupOperation = Aggregation.group(grouping, Dimension, Dimension2X).sum("Filterval")
								.as("average").min("mesWafer.WaferStartTime").as("WaferStartTime")
								.max("mesWafer.WaferEndTime").as("WaferEndTime").addToSet("$mesWafer._id")
								.as("waferID");
					}
				}
				projectFetchData = Aggregation.project().and("_id." + grouping2).as("group").and("$xAxis").as("xAxis")
						.andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime").as("WaferStartTime")
						.andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
			} else {
				if ((grouping2.equals(dimension2X) && grouping2.equals("WaferNumber"))
						|| (grouping2.equals(dimension1X) && grouping2.equals("WaferNumber"))) {
					if (dimension2X.equals("WaferNumber")) {
						groupOperation = Aggregation.group(grouping, Dimension).sum("GroupingField").as("average")
								.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
								.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
								.addToSet("$mesWafer._id").as("waferID");
					} else {
						groupOperation = Aggregation.group(grouping, Dimension2X).sum("GroupingField").as("average")
								.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
								.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
								.addToSet("$mesWafer._id").as("waferID");
					}
				} else {
					if (dimension1X.equals(dimension2X)) {
						groupOperation = Aggregation.group(grouping, Dimension).sum("GroupingField").as("average")
								.sum("$mesWafer.TestChipCount").as("TestChipCount").min("mesWafer.WaferStartTime")
								.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime")
								.addToSet("$mesWafer._id").as("waferID");
					} else {
						groupOperation = Aggregation.group(grouping, Dimension, Dimension2X).sum("GroupingField")
								.as("average").sum("$mesWafer.TestChipCount").as("TestChipCount")
								.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
								.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
					}
				}
				projectFetchData = Aggregation.project().and("_id." + grouping2).as("group").and("xAxis").as("xAxis")
						.and("$xAxis").as("xAxis").and("average").as("yAxis").and("TestChipCount").as("TestChipCount")
						.andArrayOf("$WaferStartTime").as("WaferStartTime").andArrayOf("$WaferEndTime")
						.as("WaferEndTime").and("$waferID").as("waferID");
			}
		} else if (aggregator.equals("Avg")) {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2X) && grouping2.equals("WaferNumber"))
					|| (grouping2.equals(dimension1X) && grouping2.equals("WaferNumber"))) {
				if (dimension2X.equals("WaferNumber")) {
					groupOperation = Aggregation.group(grouping, Dimension).avg("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension2X).avg("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}
			} else {
				if (dimension1X.equals(dimension2X)) {
					groupOperation = Aggregation.group(grouping, Dimension).avg("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension, Dimension2X).avg("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}

			}
			projectFetchData = Aggregation.project().and("_id." + grouping2).as("group").and("xAxis").as("xAxis")
					.and("$xAxis").as("xAxis").andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime")
					.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
		} else {
			addFieldsFilterOperation = Aggregation.addFields().addFieldWithValue("$Filterval", "$" + MeasureVal)
					.build();
			if ((grouping2.equals(dimension2X) && grouping2.equals("WaferNumber"))
					|| (grouping2.equals(dimension1X) && grouping2.equals("WaferNumber"))) {
				if (dimension2X.equals("WaferNumber")) {
					groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				} else {
					groupOperation = Aggregation.group(grouping, Dimension2X).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}
			} else {
				if (dimension1X.equals(dimension2X)) {
					groupOperation = Aggregation.group(grouping, Dimension).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
					;
				} else {
					groupOperation = Aggregation.group(grouping, Dimension, Dimension2X).sum("Filterval").as("average")
							.min("mesWafer.WaferStartTime").as("WaferStartTime").max("mesWafer.WaferEndTime")
							.as("WaferEndTime").addToSet("$mesWafer._id").as("waferID");
				}
			}
			projectFetchData = Aggregation.project().and("_id." + grouping2).as("group").and("$xAxis").as("xAxis")
					.and("$xAxis").as("xAxis").andArrayOf("$average").as("yAxis").andArrayOf("$WaferStartTime")
					.as("WaferStartTime").andArrayOf("$WaferEndTime").as("WaferEndTime").and("$waferID").as("waferID");
		}
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

		if (addFieldsOperation == null) {
			if (minVal == 0.0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsFilterOperation, groupOperation, addFieldsOperationFor2X, projectFetchData, matchZero,
						excludeId);
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
						addFieldsFilterOperation, groupOperation, FilterMinMax, addFieldsOperationFor2X,
						projectFetchData, matchZero, excludeId);
			}
		} else {
			if (measureName.equals("BinRatio")) {
				aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
						addFieldsOperation, groupOperation, addFieldsOperationFor2X, projectFetchData, matchZero,
						excludeId);
			} else {
				if (minVal == 0.0 && maxVal == 0.0) {
					aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
							matchOperation, addFieldsOperation, addFieldsFilterOperation, groupOperation,
							addFieldsOperationFor2X, projectFetchData, matchZero, excludeId);

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
							addFieldsOperationFor2X, projectFetchData, matchZero, excludeId);
				}
			}
		}
		logger.info("Preparing aggregationMean getComparisonHistogramDetails" + aggregationMean);
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
				.getMappedResults();
		if (measureName.equals("BinRatio")) {
			List<Document> binRatioResultDocuments = new ArrayList<>();
			for (Document document : testResults) {
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
					double[] arr = new double[1];
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
