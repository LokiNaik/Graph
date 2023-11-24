package com.graph.service.wt;

import java.io.IOException;
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
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
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
public class ScatterPlotService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired(required = true)
	private IAutographService autoGraphService;

	private static final String[] binGroupingScatter = { "TestPassChips", "TestFailChips", "TestValue",
			"TestFailedRatioActualCount", "TestFailedRatio", "TestChipCount" };
	private static final int LIMIT_VALUE = 40000;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public List<Document> getGroupByBinForScatter(ObjectId[] waferIds, String dimensionName, String measureName,
			double minVal, double maxVal, String aggregator, double min, double max, List<FilterParam> filterParams)
			throws IOException {
		List<Document> testResults = null;
		String CollectionName = null;
		AddFieldsOperation addFieldsOperationWithBinRatio = null;
		AddFieldsOperation addFieldsOperationWithChipsPerBIn = null;
		AddFieldsOperation addFieldsOperationDimension = null;
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
		boolean isBothSelectedBin = false;
		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");

		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		GroupOperation groupOperation = null;
		AddFieldsOperation addFieldsConcatOperation = null;
		ProjectionOperation projectFetchData = null;
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		UnwindOperation unwindBinNumber = null;
		unwindBinNumber = Aggregation.unwind("$mesWafer.BinSummary.BinCount", false);
		MatchOperation matchZero = Aggregation.match(Criteria.where("GroupingField").gt(0));
		MatchOperation match20 = Aggregation.match(Criteria.where("_id").ne(20));
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
		boolean binGroupingCountableMeasures = Arrays.stream(binGroupingScatter).anyMatch(dimensionName::equals);

		if (binGroupingCountableMeasures) {
			throw new IOException("Selected Combination is not supported.");
		}

		switch (measureName) {
		case "NumberOfChipsPerBin":
			addFieldsOperationWithBinRatio = Aggregation.addFields()
					.addFieldWithValue("GroupingField", "$mesWafer.BinSummary.BinCount.Count").build();
			break;

		case "BinRatio":
			String BinRatioCalculationForDimension = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$mesWafer.BinSummary.BinCount.Count'," + "            '$mesWafer.TestChipCount'"
					+ "          ]" + "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatioDimension = BasicDBObject.parse(BinRatioCalculationForDimension);

			addFieldsOperationWithBinRatio = Aggregation.addFields()
					.addFieldWithValue("GroupingField", BinRatioDimension).build();
			break;
		default:
			throw new IOException("Selected Combination is not supported.");

		}
		switch (dimensionName) {
		case "NumberOfChipsPerBin":
			addFieldsOperationDimension = Aggregation.addFields()
					.addFieldWithValue("GroupingFieldDimension", "$mesWafer.BinSummary.BinCount.Count").build();
			String concatNOBin = "{ $concat: [{$toString:\"$GroupingFieldDimension\"}, \",\", {$toString: \"$GroupingField\"}] }";

			BasicDBObject concatValNOBin = BasicDBObject.parse(concatNOBin);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatValNOBin)
					.build();
			isBothSelectedBin = true;
			break;

		case "BinRatio":
			String BinRatioCalculationForDimension = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$mesWafer.BinSummary.BinCount.Count'," + "            '$mesWafer.TestChipCount'"
					+ "          ]" + "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatioDimension = BasicDBObject.parse(BinRatioCalculationForDimension);

			addFieldsOperationDimension = Aggregation.addFields()
					.addFieldWithValue("GroupingFieldDimension", BinRatioDimension).build();
			String concatBin = "{ $concat: [{$toString:\"$GroupingFieldDimension\"}, \",\", {$toString: \"$GroupingField\"}] }";

			BasicDBObject concatValBin = BasicDBObject.parse(concatBin);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatValBin).build();
			isBothSelectedBin = true;
			break;
		default:

			String concat = "{ $concat: [{$toString:\"$" + Dimension + "\"}, \",\", {$toString: \"$GroupingField\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();

			isBothSelectedBin = false;
			break;

		}
		Aggregation aggregationMean = null;

		groupOperation = Aggregation.group("$mesWafer.BinSummary.BinCount.BINNumber").addToSet("$ConcatValue")
				.as("average").addToSet("mesWafer.WaferStartTime").as("WaferStartTime")
				.addToSet("mesWafer.WaferEndTime").as("WaferEndTime");

		projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$average").as("data")
				.and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime");
		if (isBothSelectedBin) {
			MatchOperation matchDimensionZero = Aggregation.match(Criteria.where("GroupingFieldDimension").gt(0));
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					unwindBinNumber, addFieldsOperationWithBinRatio, addFieldsOperationDimension, matchZero,
					matchDimensionZero, addFieldsConcatOperation, groupOperation, match20, projectFetchData);

		} else {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					unwindBinNumber, addFieldsOperationWithBinRatio, matchZero, addFieldsConcatOperation,
					groupOperation, match20, projectFetchData);
		}
		testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class).getMappedResults();
		List<Document> binRatioResultDocuments = new ArrayList<>();

		for (Document document : testResults) {
			ArrayList<List<double[]>> listval = new ArrayList<>();
			Document val = new Document();
			val.put("group", document.get("group"));
			val.put("WaferStartTime", document.get("WaferStartTime"));
			val.put("WaferEndTime", document.get("WaferEndTime"));
			List<String> listStart = new ArrayList<>();
			List<String> listEnd = new ArrayList<>();
			if (document.containsKey("data")) {
				List<Document> valueArr = (List<Document>) document.get("data");
				List<String> DataArr = (List<String>) valueArr.get(0);
				List<double[]> listArr = new ArrayList<>();
				for (int i = 0; i < DataArr.size(); i++) {
					double[] arr = new double[2];
					String DataVal = DataArr.get(i);
					String[] parts = DataVal.split(",");
					String part1 = parts[0];
					String part2 = parts[1];
					arr[0] = Double.parseDouble(part1);
					arr[1] = Double.parseDouble(part2);
					if (arr[0] > 0 && arr[1] > 0) {
						boolean count = false;
						boolean count1 = false;
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
						if (count) {
							if (count1) {
								listArr.add(arr);
							}
						}
					}
				}

				if (listArr.size() > 0) {
					val.put("data", listArr);
					binRatioResultDocuments.add(val);
				}

			}
		}

		for (Document dataArrDoc : binRatioResultDocuments) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}
		return binRatioResultDocuments;
	}


	public List<Document> getGroupByDUTForScatter(ObjectId[] waferIds, String dimensionName, String measureName,
			String group, String testName, int testNumber, String dimensionTestName, int dimensionTestNumber,
			double minVal, double maxVal, String aggregator, int dimensionselectedBin, int measureselectedBin,
			double min, double max, List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		boolean isBothSelectedBin = false;
		boolean isDimensionSelectedBin = false;
		boolean isMeasureSelectedBin = false;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsConcatOperation = null;
		AddFieldsOperation addFieldsOperationWithBinRatio = null;
		Aggregation aggregationMean = null;
		Aggregation aggregationDimension = null;
		Aggregation aggregationMeasure = null;
		List<Document> testResults = null;
		ProjectionOperation excludeId = null;
		MatchOperation matchBinOperationDimension = null;
		MatchOperation matchBinOperationMeasure = null;
		MatchOperation match1 = null;
		MatchOperation match2 = null;
		UnwindOperation unwindBinSumm = null;
		Cond passChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(true))
				.then("$ChipCount").otherwise(0);
		Cond failChips = ConditionalOperators.when(ComparisonOperators.Eq.valueOf("_id.TestPass").equalToValue(false))
				.then("$ChipCount").otherwise(0);
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		List<Criteria> criteriaListNew = new ArrayList<Criteria>();

		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("waferID").in(waferIds));

		}
		criteriaListNew.add(Criteria.where("TestHistory.mesWafer._id").in(waferIds));
		MatchOperation matchOperation = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		MatchOperation matchTestOperation = Aggregation.match(Criteria.where("TestPass").is(false));

		LookupOperation lookupOperationTestHistory = LookupOperation.newLookup().from("TestHistory")
				.localField("_id.waferID").foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
				.localField("TestHistory.mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProduct = Aggregation.unwind("$ProductInfo", true);
		if (waferIds.length == 0) {
			List<Criteria> criteriaListforwafers = new ArrayList<>();
			List<Criteria> criteriaListforwafers1 = new ArrayList<>();
			LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
					.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");

			ProjectionOperation projectFetchWafers = Aggregation.project().and("$mesWafer._id").as("waferId")
					.andExclude("_id");
			Aggregation aggregationResults = Aggregation.newAggregation(lookupProductOperation, unwindProduct,
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
			criteriaListforwafers1.add(Criteria.where("waferID").in(waferIdsnew));
			criteriaListforwafers.add(Criteria.where("TestHistory.mesWafer._id").in(waferIdsnew));
			match1 = Aggregation.match(new Criteria()
					.andOperator(criteriaListforwafers.toArray(new Criteria[criteriaListforwafers.size()])));
			match2 = Aggregation.match(new Criteria()
					.andOperator(criteriaListforwafers1.toArray(new Criteria[criteriaListforwafers1.size()])));
		}
		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		GroupOperation groupOperation = null;
		ProjectionOperation projectFetchData = null;
		GroupOperation groupOperationDUT = Aggregation.group("$" + grouping, "mapID", "TestPass", "waferID").count()
				.as("ChipCount");
		GroupOperation groupOperationCommon = Aggregation.group("$group", "$waferId").first("group").as("group")
				.first("waferId").as("waferId").sum("TotalCount").as("TotalCount").sum("passChips").as("passChips")
				.sum("failChips").as("failChips");
		GroupOperation groupOperationCommonAuto = Aggregation.group("$group", "$waferId").first("group").as("group")
				.first("waferId").as("waferId").sum("TotalCount").as("TotalCount").sum("passChips").as("passChips")
				.sum("failChips").as("failChips").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
				.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");

		if (measureName.equals("NumberOfChipsPerBin") || Dimension.equals("NumberOfChipsPerBin")
				|| measureName.equals("BinRatio") || Dimension.equals("BinRatio")) {

			// unwindBinSumm =
			// Aggregation.unwind("$TestHistory.mesWafer.BinSummary.BinCount", true);

			matchBinOperationDimension = Aggregation.match(Criteria.where("BINNumber").is(dimensionselectedBin));
			matchBinOperationMeasure = Aggregation.match(Criteria.where("BINNumber").is(measureselectedBin));
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("GroupingField", "$ChipCount").build();

			String BinRatioCalculationForDimension = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$GroupingField'," + "            '$TestHistory.mesWafer.TestChipCount'"
					+ "          ]" + "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatioDimension = BasicDBObject.parse(BinRatioCalculationForDimension);

			addFieldsOperationWithBinRatio = Aggregation.addFields()
					.addFieldWithValue("GroupingFieldBinRatio", BinRatioDimension).build();

		}

		if (Dimension.equals("BinRatio") && measureName.equals("BinRatio")) {
			matchOperation = Aggregation.match(Criteria.where("_id.waferID").in(waferIds));
			groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingFieldBinRatio")
					.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
			projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
					.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
					.as("WaferEndTime");
			excludeId = Aggregation.project().andExclude("_id");
			if (waferIds.length > 0) {
			aggregationDimension = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
					groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
					projectFetchData, excludeId);

			aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
					groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
					matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
					projectFetchData, excludeId);
			}else {
				aggregationDimension = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
						projectFetchData, excludeId);

				aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
						projectFetchData, excludeId);
				}
			isBothSelectedBin = true;
		} else if (Dimension.equals("NumberOfChipsPerBin") && measureName.equals("NumberOfChipsPerBin")) {

			groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField").as("average")
					.min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
			projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
					.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
					.as("WaferEndTime");
			excludeId = Aggregation.project().andExclude("_id");
			if (waferIds.length > 0) {
				aggregationDimension = Aggregation.newAggregation(matchOperation, matchBinOperationDimension,
						matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
						unwindTestHistory, groupOperation, projectFetchData, excludeId);
			} else {
				aggregationDimension = Aggregation.newAggregation(match2, matchBinOperationDimension,
						matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
						unwindTestHistory, groupOperation, projectFetchData, excludeId);
			}
			groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField").as("average");
			projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
					.and("$average").as("xaxis");
			if (waferIds.length > 0) {
				aggregationMeasure = Aggregation.newAggregation(matchOperation, matchBinOperationMeasure,
						matchTestOperation, groupOperationDUT, addFieldsOperation, groupOperation, projectFetchData,
						excludeId);
			} else {
				aggregationMeasure = Aggregation.newAggregation(match2, matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, addFieldsOperation, groupOperation, projectFetchData, excludeId);
			}
			isBothSelectedBin = true;
		} else if (Dimension.equals("NumberOfChipsPerBin")) {

			if (measureName.equals("BinRatio")) {
				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField")
						.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
						.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
						.as("WaferEndTime");
				excludeId = Aggregation.project().andExclude("_id");
				if (waferIds.length > 0) {
					aggregationDimension = Aggregation.newAggregation(matchOperation, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
							unwindTestHistory, groupOperation, projectFetchData, excludeId);
				} else {
					aggregationDimension = Aggregation.newAggregation(match2, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
							unwindTestHistory, groupOperation, projectFetchData, excludeId);
				}

				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingFieldBinRatio")
						.as("average");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis");
				matchOperation = Aggregation.match(Criteria.where("_id.waferID").in(waferIds));
				if (waferIds.length > 0) {
					aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio,
							groupOperation, projectFetchData, excludeId);
				} else {
					aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
							projectFetchData, excludeId);
				}
				isBothSelectedBin = true;

			} else if (measureName.equals("Yield") || measureName.equals("FailRatio")
					|| measureName.equals("NumberOfPassedChips") || measureName.equals("NumberOfFailedChips")) {

				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField")
						.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
						.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
						.as("WaferEndTime");
				;
				excludeId = Aggregation.project().andExclude("_id");
				if (waferIds.length > 0) {
					aggregationDimension = Aggregation.newAggregation(matchOperation, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
							unwindTestHistory, groupOperation, projectFetchData, excludeId);
				} else {
					aggregationDimension = Aggregation.newAggregation(match2, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, lookupOperationTestHistory,
							unwindTestHistory, groupOperation, projectFetchData, excludeId);
				}

				ProjectionOperation projectField = Aggregation.project().and(grouping).as("group").and("waferID")
						.as("waferId").and("$ChipCount").as("TotalCount").and(passChips).as("passChips").and(failChips)
						.as("failChips");
				if (waferIds.length > 0) {
					aggregationMeasure = Aggregation.newAggregation(matchOperation, groupOperationDUT, projectField,
							groupOperationCommon, excludeId);
				} else {
					aggregationMeasure = Aggregation.newAggregation(match2, groupOperationDUT, projectField,
							 lookupOperationTestHistory,unwindTestHistory,groupOperationCommonAuto, excludeId);
				}
				isDimensionSelectedBin = true;
			} else {
				throw new IOException("Selected Combination is not supported.");
			}

		} else if (Dimension.equals("BinRatio")) {

			if (measureName.equals("NumberOfChipsPerBin")) {

				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField")
						.as("average");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis");
				excludeId = Aggregation.project().andExclude("_id");
				if (waferIds.length > 0) {
					aggregationMeasure = Aggregation.newAggregation(matchOperation, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, groupOperation, projectFetchData,
							excludeId);
				} else {
					aggregationMeasure = Aggregation.newAggregation(match2, matchBinOperationDimension,
							matchTestOperation, groupOperationDUT, addFieldsOperation, groupOperation, projectFetchData,
							excludeId);
				}
				matchOperation = Aggregation.match(Criteria.where("_id.waferID").in(waferIds));
				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingFieldBinRatio")
						.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
						.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
						.as("WaferEndTime");
				if (waferIds.length > 0) {
					aggregationDimension = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio,
							groupOperation, projectFetchData, excludeId);
				} else {
					aggregationDimension = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
							projectFetchData, excludeId);
				}
				isBothSelectedBin = true;

			} else if (measureName.equals("Yield") || measureName.equals("FailRatio")
					|| measureName.equals("NumberOfPassedChips") || measureName.equals("NumberOfFailedChips")) {

				ProjectionOperation projectField = Aggregation.project().and(grouping).as("group").and("waferID")
						.as("waferId").and("$ChipCount").as("TotalCount").and(passChips).as("passChips").and(failChips)
						.as("failChips");
				excludeId = Aggregation.project().andExclude("_id");
				if (waferIds.length > 0) {
					aggregationMeasure = Aggregation.newAggregation(matchOperation, groupOperationDUT, projectField,
							groupOperationCommon, excludeId);
				} else {
					aggregationMeasure = Aggregation.newAggregation(match2, groupOperationDUT, projectField,
							lookupOperationTestHistory, unwindTestHistory,groupOperationCommonAuto, excludeId);
				}
				matchOperation = Aggregation.match(Criteria.where("_id.waferID").in(waferIds));
				groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingFieldBinRatio")
						.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
						.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
				projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
						.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
						.as("WaferEndTime");
				;
				if (waferIds.length > 0) {
					aggregationDimension = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio,
							groupOperation, projectFetchData, excludeId);
				} else {
					aggregationDimension = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
							groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
							unwindProduct, match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
							projectFetchData, excludeId);
				}
				isDimensionSelectedBin = true;
			} else {
				throw new IOException("Selected Combination is not supported.");
			}

		} else if (measureName.equals("BinRatio")) {
			if (!(dimensionName.equals("Yield") || dimensionName.equals("FailRatio")
					|| dimensionName.equals("NumberOfPassedChips") || dimensionName.equals("NumberOfFailedChips"))) {
				throw new IOException("Selected Combination is not supported.");

			}

			groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingFieldBinRatio")
					.as("average").min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
			projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
					.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
					.as("WaferEndTime");
			MatchOperation matchOperationTestHistory = Aggregation
					.match(new Criteria().andOperator(criteriaListNew.toArray(new Criteria[criteriaListNew.size()])));
			excludeId = Aggregation.project().andExclude("_id");
			if (waferIds.length > 0) {
				aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						matchOperationTestHistory, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation,
						projectFetchData, excludeId);
			} else {
				aggregationMeasure = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						match1, addFieldsOperation, addFieldsOperationWithBinRatio, groupOperation, projectFetchData,
						excludeId);
			}
			ProjectionOperation projectField = Aggregation.project().and(grouping).as("group").and("waferID")
					.as("waferId").and("$ChipCount").as("TotalCount").and(passChips).as("passChips").and(failChips)
					.as("failChips");
			if (waferIds.length > 0) {
				aggregationDimension = Aggregation.newAggregation(matchOperation, groupOperationDUT, projectField,
						groupOperationCommon, excludeId);
			} else {
				aggregationDimension = Aggregation.newAggregation(match2, groupOperationDUT, projectField,
						lookupOperationTestHistory, unwindTestHistory,groupOperationCommonAuto, excludeId);
			}

		} else if (measureName.equals("NumberOfChipsPerBin")) {
			if (!(dimensionName.equals("Yield") || dimensionName.equals("FailRatio")
					|| dimensionName.equals("NumberOfPassedChips") || dimensionName.equals("NumberOfFailedChips"))) {
				throw new IOException("Selected Combination is not supported.");

			}
			groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").first("GroupingField").as("average")
					.min("TestHistory.mesWafer.WaferStartTime").as("WaferStartTime")
					.max("TestHistory.mesWafer.WaferEndTime").as("WaferEndTime");
			projectFetchData = Aggregation.project().and(grouping).as("group").and("waferID").as("waferId")
					.and("$average").as("xaxis").and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime")
					.as("WaferEndTime");
			excludeId = Aggregation.project().andExclude("_id");
			if (waferIds.length > 0) {
				aggregationMeasure = Aggregation.newAggregation(matchOperation, matchBinOperationMeasure,
						matchTestOperation, groupOperationDUT, addFieldsOperation,lookupOperationTestHistory, unwindTestHistory,
						groupOperation, projectFetchData,
						excludeId);
			} else {
				aggregationMeasure = Aggregation.newAggregation(match2, matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, addFieldsOperation,lookupOperationTestHistory, unwindTestHistory,
						groupOperation, projectFetchData, excludeId);
			}
			ProjectionOperation projectField = Aggregation.project().and(grouping).as("group").and("waferID")
					.as("waferId").and("$ChipCount").as("TotalCount").and(passChips).as("passChips").and(failChips)
					.as("failChips");
			if (waferIds.length > 0) {
				aggregationDimension = Aggregation.newAggregation(matchOperation, groupOperationDUT, projectField,
						groupOperationCommon, excludeId);
			} else {
				aggregationDimension = Aggregation.newAggregation(match2, groupOperationDUT, projectField,
						lookupOperationTestHistory, unwindTestHistory,groupOperationCommonAuto, excludeId);
			}

		} else {

			ProjectionOperation projectField = Aggregation.project().and(grouping).as("group").and("waferID")
					.as("waferId").and("$ChipCount").as("TotalCount").and(passChips).as("passChips").and(failChips)
					.as("failChips");
			
			excludeId = Aggregation.project().andExclude("_id");

			if (waferIds.length > 0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, groupOperationDUT, projectField,
						groupOperationCommon, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(match2, groupOperationDUT, projectField,
						lookupOperationTestHistory, unwindTestHistory,groupOperationCommonAuto, excludeId);
			}
			// aggregationMean = Aggregation.newAggregation(matchOperation,
			// groupOperationDUT, projectField,
			// groupOperationCommon, excludeId);
		}

		if (aggregationMean == null) {
			List<Document> testResultsDimension = mongoTemplate
					.aggregate(aggregationDimension, "MESTest", Document.class).getMappedResults();

			List<Document> testResultsMeasure = mongoTemplate.aggregate(aggregationMeasure, "MESTest", Document.class)
					.getMappedResults();

			if (isBothSelectedBin) {
				testResults = extractBinDocument(testResultsDimension, testResultsMeasure, minVal, maxVal, min, max);
				return testResults;

			} else if (isDimensionSelectedBin) {

				testResults = extractCommonDocument(testResultsDimension, testResultsMeasure, measureName, true, minVal,
						maxVal, min, max);
				return testResults;
			} else {
				testResults = extractCommonDocument(testResultsMeasure, testResultsDimension, dimensionName, false,
						minVal, maxVal, min, max);
				return testResults;
			}

		} else {
			testResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class).getMappedResults();

			List<Document> binRatioResultDocuments = extractDocumentDUTScatter(measureName, dimensionName, testResults);
			List<Document> resultDocument = preparingResults(binRatioResultDocuments, minVal, maxVal, min, max);
			return resultDocument;
		}

	}

	private List<Document> preparingResults(List<Document> binRatioResultDocuments, double minVal, double maxVal,
			double min, double max) throws IOException {
		List<Document> ResultDocuments = new ArrayList<>();

		Map<Object, List<Document>> measureListGrouped = binRatioResultDocuments.stream()
				.collect(Collectors.groupingBy(results -> {
					return results.get("group").toString();
				}));
		List<Date> listStartTime = new ArrayList<Date>();
		List<Date> listEndTime = new ArrayList<Date>();
		measureListGrouped.forEach((keyVal, valu) -> {
			List<double[]> listArr = new ArrayList<>();
			for (Document document : valu) {
				Document val = new Document();

				double[] arr = new double[2];
				arr[0] = (double) document.get("Xaxis");
				arr[1] = (double) document.get("Yaxis");
				if (arr[0] > 0 || arr[1] > 0) {
					boolean count = false;
					boolean count1 = false;
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
					if (count) {
						if (count1) {
							listArr.add(arr);
							listStartTime.add((Date) document.get("WaferStartTime"));
							listEndTime.add((Date) document.get("WaferEndTime"));
						}
					}
				}
			}
			if (listArr.size() > 0) {
				String KeyValue = (String) keyVal;
				Document val = new Document();
				val.put("group", KeyValue);
				val.put("WaferStartTime", listStartTime);
				val.put("WaferEndTime", listEndTime);
				List<double[]> listArrVal = new ArrayList<>();
				listArrVal.addAll(listArr);
				val.put("data", listArr);

				ResultDocuments.add(val);
			}
		});

		for (Document dataArrDoc : ResultDocuments) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}

		return ResultDocuments;
	}

	private List<Document> extractDocumentDUTScatter(String measure, String dimension, List<Document> dutResults)
			throws IOException {
		List<Document> dutResultDocuments = new ArrayList<>();

		for (Document obj : dutResults) {
			Document val = new Document();

			switch (measure) {
			case "Yield":
				Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					double yield = ((double) passChip / (double) totalCount) * 100;
					val.put("Yaxis", yield);
				}
				break;

			case "FailRatio":
				Integer totalCount1 = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					double yield = ((double) failChip / (double) totalCount1) * 100;
					val.put("Yaxis", yield);
				}
				break;
			case "NumberOfPassedChips":

				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					val.put("Yaxis", passChip.doubleValue());
				}
				break;
			case "NumberOfFailedChips":
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					val.put("Yaxis", failChip.doubleValue());
				}
				break;
			default:
				throw new IOException("Selected Combination is not supported.");
			}

			switch (dimension) {
			case "Yield":
				Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					double yield = ((double) passChip / (double) totalCount) * 100;
					val.put("Xaxis", yield);
				}
				break;

			case "FailRatio":
				Integer totalCount1 = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					double yield = ((double) failChip / (double) totalCount1) * 100;
					val.put("Xaxis", yield);
				}
				break;
			case "NumberOfPassedChips":
				if (obj.get("passChips") != null) {
					Integer passChip = (Integer) obj.get("passChips");
					val.put("Xaxis", passChip.doubleValue());
				}
				break;
			case "NumberOfFailedChips":
				if (obj.get("failChips") != null) {
					Integer failChip = (Integer) obj.get("failChips");
					val.put("Xaxis", failChip.doubleValue());
				}
				break;
			default:
				throw new IOException("Selected Combination is not supported.");
			}

			val.put("group", obj.get("group"));
			val.put("WaferStartTime", obj.get("WaferStartTime"));
			val.put("WaferEndTime", obj.get("WaferEndTime"));
			dutResultDocuments.add(val);
		}

		return dutResultDocuments;

	}

	private List<Document> extractCommonDocument(List<Document> testResultsDimension, List<Document> testResultsMeasure,
			String measureName, boolean flag, double minVal, double maxVal, double min, double max) throws IOException {
		List<Document> binRatioResultDocuments = new ArrayList<>();
		if (testResultsDimension.size() > 0 && testResultsMeasure.size() > 0) {

			Map<Object, List<Document>> measureListGrouped = testResultsMeasure.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));
			measureListGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					Document obj = null;
					for (Document dimensionRes : testResultsDimension) {
						double[] arr = new double[2];
						if ((keyVal.equals(dimensionRes.get("group").toString()))
								&& (results.get("waferId").toString().equals(dimensionRes.get("waferId").toString()))) {
							obj = dimensionRes;
							Document value = extractDocumentDUT(measureName, results);

							if (dimensionRes.get("xaxis") instanceof Integer) {
								int TestPass = (int) dimensionRes.get("xaxis");
								if (flag) {
									arr[0] = Double.valueOf(TestPass);
								} else {
									arr[1] = Double.valueOf(TestPass);
								}
							} else {
								double TestPass = (double) dimensionRes.get("xaxis");
								if (flag) {
									arr[0] = Double.valueOf(TestPass);
								} else {
									arr[1] = Double.valueOf(TestPass);
								}
							}

							if (value.get("Yaxis") instanceof Integer) {
								int TestPass = (int) value.get("Yaxis");
								if (flag) {
									arr[1] = Double.valueOf(TestPass);
								} else {
									arr[0] = Double.valueOf(TestPass);
								}
							} else {
								double TestPass = (double) value.get("Yaxis");
								if (flag) {
									arr[1] = Double.valueOf(TestPass);
								} else {
									arr[0] = Double.valueOf(TestPass);
								}
							}
							if (arr[0] > 0 || arr[1] > 0) {
								boolean count = false;
								boolean count1 = false;

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
								if (count) {
									if (count1) {
										listArr.add(arr);
										listStartTime.add((Date) dimensionRes.get("WaferStartTime"));
										listEndTime.add((Date) dimensionRes.get("WaferEndTime"));
									}
								}
							}
							break;
						}

					}

					if (obj == null) {
						double[] arr = new double[2];
						Document value = extractDocumentDUT(measureName, results);

						if (value.get("Yaxis") instanceof Integer) {
							int TestPass = (int) value.get("Yaxis");
							if (flag) {
								arr[1] = Double.valueOf(TestPass);
							} else {
								arr[0] = Double.valueOf(TestPass);
							}
						} else {
							double TestPass = (double) value.get("Yaxis");
							if (flag) {
								arr[1] = Double.valueOf(TestPass);
							} else {
								arr[0] = Double.valueOf(TestPass);
							}
						}
						if (flag) {
							arr[0] = 0;
						} else {
							arr[1] = 0;
						}
						if (arr[0] > 0 || arr[1] > 0) {
							boolean count = false;
							boolean count1 = false;

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
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) value.get("WaferStartTime"));
									listEndTime.add((Date) value.get("WaferEndTime"));
								}
							}
						}
					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binRatioResultDocuments.add(val);
				}
			});

			return binRatioResultDocuments;
		}

		return null;
	}

	private Document extractDocumentDUT(String measure, Document obj) {

		if (measure.equalsIgnoreCase("yield")) {
			Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
			if (obj.get("passChips") != null) {
				Integer passChip = (Integer) obj.get("passChips");
				double yield = ((double) passChip / (double) totalCount) * 100;
				obj.put("Yaxis", yield);
				obj.put("WaferStartTime", obj.get("WaferStartTime"));
				obj.put("WaferEndTime", obj.get("WaferEndTime"));

			}
		} else if (measure.equalsIgnoreCase("failRatio")) {
			Integer totalCount = (Integer) obj.get("passChips") + (Integer) obj.get("failChips");
			if (obj.get("failChips") != null) {
				Integer failChip = (Integer) obj.get("failChips");
				double yield = ((double) failChip / (double) totalCount) * 100;
				obj.put("Yaxis", yield);
				obj.put("WaferStartTime", obj.get("WaferStartTime"));
				obj.put("WaferEndTime", obj.get("WaferEndTime"));
			}
		} else if (measure.equalsIgnoreCase("NumberOfPassedChips")) {
			if (obj.get("passChips") != null) {
				Integer passChip = (Integer) obj.get("passChips");
				obj.put("Yaxis", passChip);
				obj.put("WaferStartTime", obj.get("WaferStartTime"));
				obj.put("WaferEndTime", obj.get("WaferEndTime"));
			}
		} else if (measure.equalsIgnoreCase("NumberOfFailedChips")) {
			if (obj.get("failChips") != null) {
				Integer failChip = (Integer) obj.get("failChips");
				obj.put("Yaxis", failChip);
				obj.put("WaferStartTime", obj.get("WaferStartTime"));
				obj.put("WaferEndTime", obj.get("WaferEndTime"));
			}
		}
		return obj;

	}

	private List<Document> extractBinDocument(List<Document> dimensionResults, List<Document> measureResults,
			double minVal, double maxVal, double min, double max) {
		List<Document> binResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> dimensionistGrouped = dimensionResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			dimensionistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();

				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {

					Document obj = null;

					for (Document mesureRes : measureResults) {
						double[] arr = new double[2];

						if ((keyVal.equals(mesureRes.get("group").toString()))
								&& (results.get("waferId").toString().equals(mesureRes.get("waferId").toString()))) {
							obj = mesureRes;
							if (results.get("xaxis") instanceof Integer) {
								int TestPass = (int) results.get("xaxis");
								arr[0] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) results.get("xaxis");
								arr[0] = Double.valueOf(TestPass);
							}

							if (mesureRes.get("xaxis") instanceof Integer) {
								int TestPass = (int) mesureRes.get("xaxis");
								arr[1] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) mesureRes.get("xaxis");
								arr[1] = Double.valueOf(TestPass);
							}
							if (arr[0] > 0 || arr[1] > 0) {
								boolean count = false;
								boolean count1 = false;

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
								if (count) {
									if (count1) {
										listArr.add(arr);
										listStartTime.add((Date) results.get("WaferStartTime"));
										listEndTime.add((Date) results.get("WaferEndTime"));
									}
								}
							}
							break;
						}
					}
					if (obj == null) {
						double[] arr = new double[2];

						if (results.get("xaxis") instanceof Integer) {
							int TestPass = (int) results.get("xaxis");
							arr[0] = Double.valueOf(TestPass);
						} else {
							double TestPass = (double) results.get("xaxis");
							arr[0] = Double.valueOf(TestPass);
						}
						arr[1] = 0;
						if (arr[0] > 0 || arr[1] > 0) {
							boolean count = false;
							boolean count1 = false;

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
							if (count) {
								if (count1) {
									listArr.add(arr);
									listStartTime.add((Date) results.get("WaferStartTime"));
									listEndTime.add((Date) results.get("WaferEndTime"));
								}
							}
						}
					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});
		} else if (dimensionResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = dimensionResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString() + "_" + results.get("waferId").toString();
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					int dimesnionAverage = (int) results.get("xaxis");

					double[] arr = new double[2];

					if (results.get("xaxis") instanceof Integer) {
						int TestPass = (int) results.get("xaxis");
						arr[0] = Double.valueOf(TestPass);
					} else {
						double TestPass = (double) results.get("xaxis");
						arr[0] = Double.valueOf(TestPass);
					}
					arr[1] = 0;

					boolean count = false;
					boolean count1 = false;
					if (arr[0] > 0 && arr[1] == 0) {
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

							if (count1) {
								listArr.add(arr);
								listStartTime.add((Date) results.get("WaferStartTime"));
								listEndTime.add((Date) results.get("WaferEndTime"));
							}
						}
					}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});
		} else {
			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString() + "_" + results.get("waferId").toString();
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();
				for (Document results : valu) {
					double MeasureAverage = (double) results.get("xaxis");

					double[] arr = new double[2];

					if (results.get("xaxis") instanceof Integer) {
						int TestPass = (int) results.get("xaxis");
						arr[1] = Double.valueOf(TestPass);
					} else {
						double TestPass = (double) results.get("xaxis");
						arr[1] = Double.valueOf(TestPass);
					}
					arr[0] = 0;

					boolean count = false;
					boolean count1 = false;
					if (arr[0] == 0 && arr[1] > 0) {

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
						
						if (count) {
								listArr.add(arr);
								listStartTime.add((Date) results.get("WaferStartTime"));
								listEndTime.add((Date) results.get("WaferEndTime"));
							}
						}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});

		}

		return binResultsDocument;
	}

	private List<Document> getGroupByDUTdMeasureForScatter(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, String testName, int testNumber, String dimensionTestName,
			int dimensionTestNumber, double minVal, double maxVal, String aggregator, int dimensionselectedBin,
			int measureselectedBin, double min, double max) throws IOException {

		boolean isBothSelectedBin = false;
		boolean isDimensionSelectedBin = false;
		boolean isMeasureSelectedBin = false;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsConcatOperation = null;
		AddFieldsOperation addFieldsOperationWithBinRatio = null;
		Aggregation aggregationMean = null;
		MatchOperation matchBinOperationDimension = null;
		MatchOperation matchBinOperationMeasure = null;
		UnwindOperation unwindBinSumm = null;
		MatchOperation matchOperation = Aggregation.match(Criteria.where("waferID").in(waferIds));
		MatchOperation matchTestOperation = Aggregation.match(Criteria.where("TestPass").is(false));

		LookupOperation lookupOperationTestHistory = LookupOperation.newLookup().from("TestHistory")
				.localField("_id.waferID").foreignField("mesWafer._id").as("TestHistory");
		UnwindOperation unwindTestHistory = Aggregation.unwind("$TestHistory", true);
		LookupOperation lookupProduct = LookupOperation.newLookup().from("ProductInfo")
				.localField("TestHistory.mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProduct = Aggregation.unwind("$ProductInfo", true);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected Combination is not supported.");
		}
		GroupOperation groupOperation = null;
		ProjectionOperation projectFetchData = null;
		GroupOperation groupOperationDUT = Aggregation.group("$" + grouping, "mapID", "TestPass", "waferID").count()
				.as("ChipCount");

		if (measureName.equals("NumberOfChipsPerBin") || Dimension.equals("NumberOfChipsPerBin")
				|| measureName.equals("BinRatio") || Dimension.equals("BinRatio")) {

			// unwindBinSumm =
			// Aggregation.unwind("$TestHistory.mesWafer.BinSummary.BinCount", true);

			matchBinOperationDimension = Aggregation.match(Criteria.where("BINNumber").is(dimensionselectedBin));
			matchBinOperationMeasure = Aggregation.match(Criteria.where("BINNumber").is(measureselectedBin));
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("GroupingField", "$ChipCount").build();

			String BinRatioCalculationForDimension = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$GroupingField'," + "            '$TestHistory.mesWafer.TestChipCount'"
					+ "          ]" + "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatioDimension = BasicDBObject.parse(BinRatioCalculationForDimension);

			addFieldsOperationWithBinRatio = Aggregation.addFields()
					.addFieldWithValue("GroupingFieldBinRatio", BinRatioDimension).build();

		}

		if (Dimension.equals("BinRatio") && measureName.equals("BinRatio")) {
			String concat = "{ $concat: [{$toString:\"$GroupingFieldBinRatio\"}, \",\", {$toString:\"$GroupingFieldBinRatio\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
			isBothSelectedBin = true;
		} else if (Dimension.equals("NumberOfChipsPerBin") && measureName.equals("NumberOfChipsPerBin")) {

			String concat = "{ $concat: [{$toString:\"$GroupingField\"}, \",\", {$toString:\"$GroupingField\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
			isBothSelectedBin = true;
		} else if (Dimension.equals("NumberOfChipsPerBin")) {

			if (measureName.equals("BinRatio")) {

				String concat = "{ $concat: [{$toString:\"$GroupingField\"}, \",\", {$toString:\"$GroupingFieldBinRatio\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
				isBothSelectedBin = true;

			} else {

				String concat = "{ $concat: [{$toString:\"$GroupingField\"}, \",\", {$toString: \"$TestHistory."
						+ MeasureVal + "\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
				isDimensionSelectedBin = true;
			}

		} else if (Dimension.equals("BinRatio")) {

			if (measureName.equals("NumberOfChipsPerBin")) {
				String concat = "{ $concat: [{$toString:\"$GroupingFieldBinRatio\"}, \",\",{$toString:\"$GroupingField\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
				isBothSelectedBin = true;

			} else {

				String concat = "{ $concat: [{$toString:\"$GroupingField\"}, \",\", {$toString: \"$TestHistory."
						+ MeasureVal + "\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
				isDimensionSelectedBin = true;
			}

		} else if (measureName.equals("BinRatio")) {

			String concat = "{ $concat: [{$toString: \"$TestHistory." + Dimension
					+ "\"}, \",\",{$toString:\"$GroupingFieldBinRatio\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
			isMeasureSelectedBin = true;
		} else if (measureName.equals("NumberOfChipsPerBin")) {

			String concat = "{ $concat: [{$toString: \"$TestHistory." + Dimension
					+ "\"}, \",\",{$toString:\"$GroupingField\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
			isMeasureSelectedBin = true;
		} else {
			String concat = "{ $concat: [{$toString: \"$TestHistory." + Dimension
					+ "\"}, \",\", {$toString: \"$TestHistory." + MeasureVal + "\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("ConcatValue", concatVal).build();
		}

		groupOperation = Aggregation.group("$_id." + grouping).addToSet("ConcatValue").as("average");
		projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$average").as("data");

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsOperation == null) {
			aggregationMean = Aggregation.newAggregation(groupOperationDUT, lookupOperationTestHistory,
					unwindTestHistory, lookupProduct, unwindProduct, matchOperation, addFieldsConcatOperation,
					groupOperation, projectFetchData, excludeId);
		} else {
			if (isBothSelectedBin) {
				if (dimensionselectedBin == measureselectedBin) {
					aggregationMean = Aggregation.newAggregation(matchBinOperationDimension, matchBinOperationMeasure,
							matchTestOperation, groupOperationDUT, lookupOperationTestHistory, unwindTestHistory,
							lookupProduct, unwindProduct, matchOperation, addFieldsOperation,
							addFieldsOperationWithBinRatio, addFieldsConcatOperation, groupOperation, projectFetchData,
							excludeId);
				} else {
					List<Document> binRelatedResults = preparingResultestForBin(matchOperation,
							matchBinOperationDimension, matchBinOperationMeasure, matchTestOperation, groupOperationDUT,
							lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct, grouping,
							minVal, maxVal, min, max);
					return binRelatedResults;
				}
			} else if (isDimensionSelectedBin) {
				aggregationMean = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio, addFieldsConcatOperation,
						groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchBinOperationMeasure, matchTestOperation,
						groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
						matchOperation, addFieldsOperation, addFieldsOperationWithBinRatio, addFieldsConcatOperation,
						groupOperation, projectFetchData, excludeId);
			}

		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class)
				.getMappedResults();
		List<Document> binRatioResultDocuments = new ArrayList<>();

		for (Document document : testResults) {
			ArrayList<List<double[]>> listval = new ArrayList<>();
			Document val = new Document();

			if (document.containsKey("data")) {
				List<Document> valueArr = (List<Document>) document.get("data");

				List<String> DataArr = (List<String>) valueArr.get(0);
				List<double[]> listArr = new ArrayList<>();

				for (int i = 0; i < DataArr.size(); i++) {
					double[] arr = new double[2];
					String DataVal = DataArr.get(i);
					String[] parts = DataVal.split(",");
					String part1 = parts[0];
					String part2 = parts[1];
					arr[0] = Double.parseDouble(part1);
					arr[1] = Double.parseDouble(part2);
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
						} else {
							if (arr[1] > minVal && arr[1] < maxVal) {
								count = true;
							}
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
						if (count) {
							if (count1) {
								listArr.add(arr);
							}
						}
					}
				}
				if (listArr.size() > 0) {
					val.put("group", document.get("group"));
					listval.add(listArr);
					val.put("data", listval.toArray());
					binRatioResultDocuments.add(val);
				}
			}
		}
		return binRatioResultDocuments;

	}

	private List<Document> preparingResultestForBin(MatchOperation matchOperation,
			MatchOperation matchBinOperationDimension, MatchOperation matchBinOperationMeasure,
			MatchOperation matchTestOperation, GroupOperation groupOperationDUT,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String grouping, double minVal, double maxVal,
			double min, double max) {
		List<Document> testResults = null;
		List<Document> dimensionResults = getDimensionResults(matchOperation, matchBinOperationDimension,
				matchTestOperation, groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
				unwindProduct, grouping);
		List<Document> measureResults = getDimensionResults(matchOperation, matchBinOperationMeasure,
				matchTestOperation, groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct,
				unwindProduct, grouping);

		testResults = generateResultsForBin(dimensionResults, measureResults, minVal, maxVal, min, max);

		return testResults;

	}

	private List<Document> generateResultsForBin(List<Document> dimensionResults, List<Document> measureResults,
			double minVal, double maxVal, double min, double max) {
		List<Document> binResultsDocument = new ArrayList<>();
		if (dimensionResults.size() > 0 && measureResults.size() > 0) {

			Map<Object, List<Document>> dimensionistGrouped = dimensionResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString();
					}));

			dimensionistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				for (Document results : valu) {

					Document obj = null;

					for (Document mesureRes : measureResults) {
						double[] arr = new double[2];

						if ((keyVal.equals(mesureRes.get("group").toString()))
								&& (results.get("waferId").toString().equals(mesureRes.get("waferId").toString()))) {
							obj = mesureRes;
							List<?> dimesnionAverage = (List<?>) results.get("average");
							List<?> measureAverage = (List<?>) mesureRes.get("average");
							if (dimesnionAverage.get(0) instanceof Integer) {
								int TestPass = (int) dimesnionAverage.get(0);
								arr[0] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) dimesnionAverage.get(0);
								arr[0] = Double.valueOf(TestPass);
							}

							if (measureAverage.get(0) instanceof Integer) {
								int TestPass = (int) measureAverage.get(0);
								arr[1] = Double.valueOf(TestPass);
							} else {
								double TestPass = (double) measureAverage.get(0);
								arr[1] = Double.valueOf(TestPass);
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
								if (count) {
									if (count1) {
										listArr.add(arr);
									}
								}
							}
							break;
						}
					}
					if (obj == null) {
						double[] arr = new double[2];
						List<?> dimesnionAverage = (List<?>) results.get("average");

						if (dimesnionAverage.get(0) instanceof Integer) {
							int TestPass = (int) dimesnionAverage.get(0);
							arr[0] = Double.valueOf(TestPass);
						} else {
							double TestPass = (double) dimesnionAverage.get(0);
							arr[0] = Double.valueOf(TestPass);
						}
						arr[1] = 0;

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
							if (count) {
								if (count1) {
									listArr.add(arr);
								}
							}
						}
					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});
		} else if (dimensionResults.size() > 0) {
			Map<Object, List<Document>> comblistGrouped = dimensionResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString() + "_" + results.get("waferId").toString();
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				for (Document results : valu) {
					List<?> dimesnionAverage = (List<?>) results.get("average");

					double[] arr = new double[2];

					if (dimesnionAverage.get(0) instanceof Integer) {
						int TestPass = (int) dimesnionAverage.get(0);
						arr[0] = Double.valueOf(TestPass);
					} else {
						double TestPass = (double) dimesnionAverage.get(0);
						arr[0] = Double.valueOf(TestPass);
					}
					arr[1] = 0;

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
						if (count) {
							if (count1) {
								listArr.add(arr);
							}
						}
					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});
		} else {
			Map<Object, List<Document>> comblistGrouped = measureResults.stream()
					.collect(Collectors.groupingBy(results -> {
						return results.get("group").toString() + "_" + results.get("waferId").toString();
					}));
			comblistGrouped.forEach((keyVal, valu) -> {
				List<double[]> listArr = new ArrayList<>();
				for (Document results : valu) {
					List<?> MeasureAverage = (List<?>) results.get("average");

					double[] arr = new double[2];

					if (MeasureAverage.get(0) instanceof Integer) {
						int TestPass = (int) MeasureAverage.get(0);
						arr[1] = Double.valueOf(TestPass);
					} else {
						double TestPass = (double) MeasureAverage.get(0);
						arr[1] = Double.valueOf(TestPass);
					}
					arr[0] = 0;

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
						if (count) {
							if (count1) {
								listArr.add(arr);
							}
						}
					}

				}

				if (listArr.size() > 0) {
					String KeyValue = (String) keyVal;
					Document val = new Document();
					val.put("group", KeyValue);
					List<double[]> listArrVal = new ArrayList<>();
					listArrVal.addAll(listArr);
					val.put("data", listArr);

					binResultsDocument.add(val);
				}

			});

		}

		return binResultsDocument;
	}

	private List<Document> getDimensionResults(MatchOperation matchOperation, MatchOperation matchBinOperationDimension,
			MatchOperation matchTestOperation, GroupOperation groupOperationDUT,
			LookupOperation lookupOperationTestHistory, UnwindOperation unwindTestHistory,
			LookupOperation lookupProduct, UnwindOperation unwindProduct, String grouping) {

		GroupOperation groupOperation = Aggregation.group("$_id." + grouping, "$_id.waferID").addToSet("ChipCount")
				.as("average");
		ProjectionOperation projectFetchData = Aggregation.project().and(grouping).as("group").and("$average")
				.as("average").and("$waferID").as("waferId");

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

		Aggregation aggregationMean = Aggregation.newAggregation(matchBinOperationDimension, matchTestOperation,
				groupOperationDUT, lookupOperationTestHistory, unwindTestHistory, lookupProduct, unwindProduct,
				matchOperation, groupOperation, projectFetchData, excludeId);
		List<Document> dimensionResults = mongoTemplate.aggregate(aggregationMean, "MESTest", Document.class)
				.getMappedResults();

		return dimensionResults;
	}

	public List<Document> getCommonDimensionsAndMeasureForScatter(ObjectId[] waferIds, String dimensionName,
			String measureName, String group, int selectedBin, int dimensionselectedBin, String testName,
			int testNumber, double minVal, double maxVal, String aggregator, double min, double max,
			List<FilterParam> filterParams) throws IOException {

		String CollectionName = null;
		AddFieldsOperation addFieldsOperation = null;
		AddFieldsOperation addFieldsDimensionOperation = null;
		AddFieldsOperation addFieldsConcatOperation = null;
		AddFieldsOperation addFieldsOperationWithBinRatio = null;
		AddFieldsOperation addFieldsOperationWithBinRatioDimension = null;
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

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");

		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);

		String grouping = CriteriaUtil.generateFieldNameForCriteria(group);
		if (grouping == null) {
			throw new IOException("Selected combination is not supported");
		}
		String Dimension = CriteriaUtil.generateFieldNameForCriteria(dimensionName);
		if (Dimension == null) {
			throw new IOException("Selected combination is not supported");
		}
		String MeasureVal = CriteriaUtil.generateFieldNameForCriteria(measureName);
		if (MeasureVal == null) {
			throw new IOException("Selected combination is not supported");
		}
		GroupOperation groupOperation = null;
		ProjectionOperation projectFetchData = null;

		if (measureName.equals("NumberOfChipsPerBin") || Dimension.equals("NumberOfChipsPerBin")
				|| measureName.equals("BinRatio") || Dimension.equals("BinRatio")) {

			String qu1 = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ selectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();

			String qu1Dimension = "{'$let': {'vars': {'a':{ '$arrayElemAt':[{'$filter': { 'input': '$mesWafer.BinSummary.BinCount','as': 'list','cond': { '$eq': ['$$list.BINNumber',"
					+ dimensionselectedBin + "]}}},0]}},'in':'$$a.Count'}}";

			BasicDBObject bdoDimenison = BasicDBObject.parse(qu1Dimension);
			addFieldsDimensionOperation = Aggregation.addFields()
					.addFieldWithValue("$GroupingFieldDimension", bdoDimenison).build();

			String BinRatioCalculationFormeasure = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$GroupingField'," + "            '$mesWafer.TestChipCount'" + "          ]"
					+ "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatio = BasicDBObject.parse(BinRatioCalculationFormeasure);

			addFieldsOperationWithBinRatio = Aggregation.addFields().addFieldWithValue("$Measure", BinRatio).build();
			String BinRatioCalculationForDimension = " {" + "      $multiply: [" + "        {" + "          $divide: ["
					+ "            '$GroupingFieldDimension'," + "            '$mesWafer.TestChipCount'" + "          ]"
					+ "        }," + "        100" + "      ]" + "    }";

			BasicDBObject BinRatioDimension = BasicDBObject.parse(BinRatioCalculationForDimension);

			addFieldsOperationWithBinRatioDimension = Aggregation.addFields()
					.addFieldWithValue("$Dimension", BinRatioDimension).build();

		}

		if (Dimension.equals("BinRatio") && measureName.equals("BinRatio")) {
			String concat = "{ $concat: [{$toString:\"$Dimension\"}, \",\", {$toString:\"$Measure\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();

		} else if (Dimension.equals("NumberOfChipsPerBin") && measureName.equals("NumberOfChipsPerBin")) {

			String concat = "{ $concat: [{$toString:\"$GroupingFieldDimension\"}, \",\", {$toString:\"$GroupingField\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();

		} else if (Dimension.equals("NumberOfChipsPerBin")) {

			if (measureName.equals("BinRatio")) {

				String concat = "{ $concat: [{$toString:\"$GroupingFieldDimension\"}, \",\", {$toString:\"$Measure\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();

			} else {

				String concat = "{ $concat: [{$toString:\"$GroupingFieldDimension\"}, \",\", {$toString: \"$"
						+ MeasureVal + "\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();
			}

		} else if (Dimension.equals("BinRatio")) {

			if (measureName.equals("NumberOfChipsPerBin")) {
				String concat = "{ $concat: [{$toString:\"$Dimension\"}, \",\",{$toString:\"$GroupingField\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();

			} else {

				String concat = "{ $concat: [{$toString:\"$Dimension\"}, \",\", {$toString: \"$" + MeasureVal
						+ "\"}] }";
				BasicDBObject concatVal = BasicDBObject.parse(concat);
				addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();
			}

		} else if (measureName.equals("BinRatio")) {

			String concat = "{ $concat: [{$toString: \"$" + Dimension + "\"}, \",\",{$toString:\"$Measure\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();
		} else if (measureName.equals("NumberOfChipsPerBin")) {

			String concat = "{ $concat: [{$toString: \"$" + Dimension + "\"}, \",\",{$toString:\"$GroupingField\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();
		} else {
			String concat = "{ $concat: [{$toString:\"$" + Dimension + "\"}, \",\", {$toString: \"$" + MeasureVal
					+ "\"}] }";
			BasicDBObject concatVal = BasicDBObject.parse(concat);
			addFieldsConcatOperation = Aggregation.addFields().addFieldWithValue("$ConcatValue", concatVal).build();
		}

		groupOperation = Aggregation.group(grouping).push("$ConcatValue").as("average").min("mesWafer.WaferStartTime")
				.as("WaferStartTime").max("mesWafer.WaferEndTime").as("WaferEndTime");
		projectFetchData = Aggregation.project().and("_id").as("group").andArrayOf("$average").as("data")
				.and("$WaferStartTime").as("WaferStartTime").and("$WaferEndTime").as("WaferEndTime");

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		if (addFieldsOperation == null) {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsConcatOperation, groupOperation, projectFetchData, excludeId);
		} else {
			aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo, matchOperation,
					addFieldsOperation, addFieldsDimensionOperation, addFieldsOperationWithBinRatio,
					addFieldsOperationWithBinRatioDimension, addFieldsConcatOperation, groupOperation, projectFetchData,
					excludeId);
		}
		List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
				.getMappedResults();
		List<Document> binRatioResultDocuments = new ArrayList<>();

		for (Document document : testResults) {
			ArrayList<List<double[]>> listval = new ArrayList<>();
			Document val = new Document();
			val.put("group", document.get("group"));
			if (document.containsKey("data")) {
				List<Document> valueArr = (List<Document>) document.get("data");
				List<String> DataArr = (List<String>) valueArr.get(0);
				List<double[]> listArr = new ArrayList<>();
				List<Date> listStartTime = new ArrayList<Date>();
				List<Date> listEndTime = new ArrayList<Date>();

				for (int i = 0; i < DataArr.size(); i++) {
					double[] arr = new double[2];
					String DataVal = DataArr.get(i);
					String[] parts = DataVal.split(",");
					String part1 = parts[0];
					String part2 = parts[1];
					arr[0] = Double.parseDouble(part1);
					arr[1] = Double.parseDouble(part2);
					if (arr[0] > 0 && arr[1] > 0) {
						boolean count = false;
						boolean count1 = false;
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
						if (count) {
							if (count1) {
								listArr.add(arr);
								listStartTime.add((Date) document.get("WaferStartTime"));
								listEndTime.add((Date) document.get("WaferEndTime"));
							}
						}
					}
				}

				if (listArr.size() > 0) {
					val.put("data", listArr);
					val.put("WaferStartTime", listStartTime);
					val.put("WaferEndTime", listEndTime);
					binRatioResultDocuments.add(val);
				}

			}
		}

		for (Document dataArrDoc : binRatioResultDocuments) {
			List<?> dataArrva = (List<?>) dataArrDoc.get("data");
			if (dataArrva.size() > LIMIT_VALUE) {
				throw new IOException("limit the data and search again");
			}
		}
		return binRatioResultDocuments;
	}

}
