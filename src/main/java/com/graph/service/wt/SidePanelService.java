package com.graph.service.wt;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.ReplaceRootOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.FilterPreference;
import com.graph.model.wt.RegularGraphList;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.model.wt.TestListResponse;
import com.graph.model.wt.UserPreferenceDetails;
import com.graph.model.wt.UserPreferenceModel;
import com.graph.repository.wt.UserPreferenceRepository;
import com.mongodb.BasicDBObject;

@Service
public class SidePanelService implements ISidePanelService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	@Autowired
	private UserPreferenceRepository userPreferenceRepository;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/*
	 * Method for fetching graph axis settings.
	 */
	@Override
	public List<JSONObject> getAxisSettings(String graphType) {
		List<JSONObject> axes = null;
		JSONParser jsonParser = new JSONParser();
		InputStream graphTypeSettingJson = null;
		graphTypeSettingJson = getClass().getResourceAsStream("/wt/graphTypeSetting.json");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(graphTypeSettingJson))) {
			axes = (List<JSONObject>) jsonParser.parse(reader);

		} catch (Exception e) {
			 logger.error("Fetch Axis Settings Error : " + e);

		}
		return axes;
	}

	/*
	 * Method for fetching axis parameters.
	 */
	@Override
	public JSONObject getAxisParameters(String applicationName) {
		JSONObject axisParameter = null;
		JSONParser jsonParser = new JSONParser();
		InputStream axisParameterJson = null;
		axisParameterJson = getClass().getResourceAsStream("/wt/axisParameters.json");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(axisParameterJson))) {
			List<JSONObject> axes = (List<JSONObject>) jsonParser.parse(reader);
			for (JSONObject axis : axes) {
				if (axis.get("applicationName").equals(applicationName)) {
					axisParameter = axis;
				}
			}
		} catch (Exception e) {
			 logger.error("Fetch Axis Parameters Error : " + e);
		}
		return axisParameter;
	}

	// /*
	// * Method for sorting axis parameters.
	// */
	// private List<Document> CriteriaUtil.sortAxisParameters(List<Document>
	// results, List<SortParam> sortParams) {
	//
	// String fieldName = null;
	// for (SortParam sortParam : sortParams) {
	// fieldName = sortParam.getField();
	// switch (fieldName) {
	// case "xAxis.axisParameter":
	// results = ValidationUtil.sortAxisParameter(results, "xAxis",
	// sortParam.isAsc());
	// break;
	// case "yAxis.axisParameter":
	// results = ValidationUtil.sortAxisParameter(results, "yAxis",
	// sortParam.isAsc());
	// break;
	// case "zAxis.axisParameter":
	// results = ValidationUtil.sortAxisParameter(results, "zAxis",
	// sortParam.isAsc());
	// break;
	// case "grouping.axisParameter":
	// results = ValidationUtil.sortAxisParameter(results, "grouping",
	// sortParam.isAsc());
	// break;
	// default:
	// break;
	// }
	// }
	// return results;
	// }
	//

	/*
	 * Update UserPreference
	 */
	@Override
	public boolean updateUserPreference(UserPreferenceDetails userPreferenceDetails) {
		String userId = "User1";
		UserPreferenceModel userPreferenceModel;
		List<FilterPreference> filterPreferenceList = null;
		if (userPreferenceDetails.getRegularGraphList() != null) {
			filterPreferenceList = userPreferenceDetails.getRegularGraphList().getFilterPreference();
			userPreferenceModel = userPreferenceRepository.findByUserId(userId);
			if (userPreferenceModel != null) {
				RegularGraphList regularGraphList = userPreferenceModel.getRegularGraphList();
				if (regularGraphList == null) {
					regularGraphList = new RegularGraphList();
				}
				if (filterPreferenceList != null) {
					regularGraphList.setFilterPreference(filterPreferenceList);
				}
				userPreferenceModel.setRegularGraphList(regularGraphList);
				userPreferenceRepository.save(userPreferenceModel);
			} else {
				userPreferenceModel = new UserPreferenceModel();
				userPreferenceModel.setUserId(userId);
				RegularGraphList regularGraphList = new RegularGraphList();
				regularGraphList.setFilterPreference(filterPreferenceList);
				userPreferenceModel.setRegularGraphList(regularGraphList);
				userPreferenceRepository.insert(userPreferenceModel);
			}
			return true;
		}
		return false;
	}

	/*
	 * Listing WaferIds
	 */
	public DataListModel getListOfWaferIds(ObjectId[] testIds) {
		Aggregation aggregation = null;
		MatchOperation matchTestOperation = match(Criteria.where("_id").in(testIds));
		ProjectionOperation projectOp = Aggregation.project().and("mesWafer._id").as("waferid").andExclude("_id");
		aggregation = Aggregation.newAggregation(matchTestOperation, projectOp);
		List<Document> results = mongoTemplate.aggregate(aggregation, "TestHistory", Document.class).getMappedResults();
		List<String> resultList = new ArrayList<String>();
		for (Document document : results) {
			ObjectId objId = (ObjectId) document.get("waferid");
			resultList.add(objId.toString());
		}
		DataListModel testListModel = new DataListModel();
		testListModel.setDataList(resultList);
		return testListModel;
	}

	/*
	 * Listing SidePanelData
	 */
	@Override
	public SidePanelResponseParam getSidePanelData(String[] waferID) {
		logger.info("Inside getSidePanelData ");
		SidePanelResponseParam response = new SidePanelResponseParam();
		try {
			ObjectId[] waferIds = new ObjectId[waferID.length];
			for (int i = 0; i < waferID.length; i++) {
				waferIds[i] = new ObjectId(waferID[i]);
			}
			logger.info("Preparing Query getSidePanelData ");
			MatchOperation matchOperation = Aggregation.match(Criteria.where("mesWafer._id").in(waferIds));
			LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
					.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
			UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
			ProjectionOperation projectFetchData = Aggregation.project().and("mesMap.ProductName").as("ArticleName")
					.and("mesMap.LotId").as("LotId").and("mesWafer.SlotNumber").as("SlotNumber")
					.and("mesWafer.ProcessCode").as("ProcessCode").and("mesWafer.WaferNumber").as("WaferNumber")
					.and("mesMap.ProgramName").as("ProgramName").and("waferStatus.status").as("active")
					.and("mesWafer._id").as("Wafer_Id").and("mesWafer._id").as("id");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregationMean = Aggregation.newAggregation(lookupProductOperation, unwindProductInfo,
					matchOperation, projectFetchData, excludeId);
			logger.info("Preparing aggregationMean getSidePanelData" + aggregationMean);
			List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "TestHistory", Document.class)
					.getMappedResults();
			logger.info("Fetching Data from Db done getSidePanelData ");
			if (testResults != null && testResults.size() > 0) {
				for (Document doc : testResults) {
					if (doc.containsKey("Wafer_Id") || doc.containsKey("id")) {
						doc.put("Wafer_Id", new ObjectId(doc.get("Wafer_Id").toString()).toHexString());
						doc.put("id", new ObjectId(doc.get("id").toString()).toHexString());
					}
				}
				logger.info("Success");
				response.setData(testResults);
			} else {
				response.setData(new ArrayList<>());
			}
			return response;
		} catch (Exception e) {
			logger.error("Exception" + e.getMessage() + "in getSidePanelData");
			response.setData(new ArrayList<>());
			return response;
		}
	}

	/*
	 * Listing TestList
	 */
	@Override
	public TestListResponse getTestList(ObjectId[] waferIds) {
		TestListResponse response = new TestListResponse();
		logger.info("Inside getTestList serviced ");
		try {
			logger.info("Preparing Query getTestList ");
			MatchOperation matchTestOperation = Aggregation.match(Criteria.where("waferID").in(waferIds));
			UnwindOperation unwindTestBoundSummary = Aggregation.unwind("$TestBoundSummary");
			GroupOperation groupOperation = Aggregation.group("TestBoundSummary.tn", "TestBoundSummary.tm")
					.first("TestBoundSummary.tn").as("testNumber").first("TestBoundSummary.tm").as("testName");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregation = Aggregation.newAggregation(matchTestOperation, unwindTestBoundSummary,
					groupOperation, excludeId);
			logger.info("Preparing aggregation getTestList" + aggregation);
			List<Document> combinedResults = new ArrayList<>();
			if (waferIds != null && waferIds.length > 0) {
				List<Document> keyResultsHeader = mongoTemplate.aggregate(aggregation, "MESBoundValues", Document.class)
						.getMappedResults();
				combinedResults.addAll(keyResultsHeader);
			}
			ArrayList<JSONObject> TestList = new ArrayList();
			List<Document> newList = new ArrayList<>();
			logger.info("Fetching Data from Db done getTestList ");
			newList = combinedResults.stream().distinct().collect(Collectors.toList());
			if (newList != null && newList.size() > 0) {
				logger.info("Success");
				for (Document document : newList) {
					JSONObject obj = new JSONObject();
					int testNumber = Integer.parseInt(document.get("testNumber").toString());
					String testName = document.get("testName").toString();
					obj.put(testNumber, testName);
					TestList.add(obj);
				}
				response.setTestList(TestList);
			} else {
				response.setTestList(new ArrayList<>());
				return response;
			}
		} catch (Exception e) {
			logger.error("Exception" + e.getMessage() + "in getTestList");
			response.setTestList(new ArrayList<>());
			return response;
		}
		return response;
	}

	/*
	 * Listing Bin
	 */
	@Override
	public Document getBinList(ObjectId[] waferIds) {
		Document binDetailsList = null;
		try {
			logger.info("Inside binListAPI serviced ");
			Aggregation aggregation = null;
			MatchOperation match = null;
			ProjectionOperation project = null;
			AddFieldsOperation addFieldsOperation = null;
			logger.info("Preparing Query binListAPI ");
			match = Aggregation.match(Criteria.where("mesWafer._id").in(waferIds));
			project = Aggregation.project()
					.and(ArrayOperators.Filter.filter("mesWafer.BinSummary.BinCount").as("binSummarryDetails")
							.by(BooleanOperators.And.and(
									ComparisonOperators.Ne.valueOf("binSummarryDetails.BINNumber")
											.notEqualTo("BinSummary.TestPassBINNumber"),
									ComparisonOperators.valueOf("binSummarryDetails.Count").greaterThanValue(0))))
					.as("BinList").andExclude("_id");
			UnwindOperation unwind = Aggregation.unwind("BinList");
			ReplaceRootOperation replaceRoot = Aggregation.replaceRoot("BinList");
			String qu1 = "{$concat:[\"Bin\", \" \",{$toString: \"$BINNumber\" }]}";
			BasicDBObject bdo = BasicDBObject.parse(qu1);
			addFieldsOperation = Aggregation.addFields().addFieldWithValue("$GroupingField", bdo).build();

			GroupOperation groupOperation = Aggregation.group().addToSet("GroupingField").as("binList");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			if (addFieldsOperation == null) {
				aggregation = Aggregation.newAggregation(match, project, unwind, replaceRoot, groupOperation,
						excludeId);
			} else {
				aggregation = Aggregation.newAggregation(match, project, unwind, replaceRoot, addFieldsOperation,
						groupOperation, excludeId);
			}

			logger.info("Preparing aggregation binListAPI" + aggregation);
			List<Document> binList = mongoTemplate.aggregate(aggregation, "TestHistory", Document.class)
					.getMappedResults();
			if (binList.size() == 1) {
				logger.info("Success");
				binDetailsList = binList.get(0);
			}

		} catch (Exception e) {
			logger.error("Error in binListAPI()" + e.getMessage());
		}
		return binDetailsList;
	}
}
