package com.graph.service.vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.enums.SelectionType;
import com.graph.model.vdi.Area;
import com.graph.model.vdi.FilterParamVdi;
import com.graph.model.vdi.GraphDefectFilterCriteria;
import com.graph.model.vdi.GridDataResponse;
import com.graph.model.vdi.LineGraphRequest;
import com.graph.model.vdi.LineGraphResponse;
import com.graph.model.vdi.LockParamVdi;
import com.graph.model.vdi.SelectedTestDetails;
import com.graph.model.vdi.SortParamVdi;
import com.graph.model.vdi.WaferDefectFilterCriteria;
import com.graph.model.vdi.XaxisDetails;
import com.graph.model.vdi.XaxisResponseDetails;
import com.graph.model.vdi.XaxisSetting;
import com.graph.model.vdi.YaxisDetails;
import com.graph.model.vdi.YaxisSetting;
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
import com.graph.model.wt.SavedAnnotation;
import com.graph.model.wt.SavedAnnotationModel;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.model.wt.SortParam;
import com.graph.model.wt.WaferIdModel;
import com.graph.model.wt.XAxisModel;
import com.graph.model.wt.YAxisModel;
import com.graph.model.wt.YaxisResponseDetails;
import com.graph.model.wt.ZAxisModel;
import com.graph.model.wt.groupResponseDetails;
import com.graph.repository.vdi.RecentRepository;
import com.graph.repository.vdi.RegularRepository;
import com.graph.repository.vdi.SavedRepository;
import com.graph.repository.vdi.WaferRepository;
import com.graph.utils.DateUtil;
import com.graph.utils.FilterUtil;
import com.graph.utils.ValidationUtil;
import com.graph.utils.vdi.GraphUtil;

@Service
public class GraphService implements IGraphService {
	@Autowired
	@Qualifier(value = "secondaryMongoTemplate")
	MongoTemplate mongoTemplate;
	@Value("${currentlocale}")
	private String currentLocale;
	@Autowired
	private RecentRepository recentGraphRepository;
	@Autowired
	private RegularRepository regularGraphRepository;
	@Autowired
	private SavedRepository savedAnnotationRepository;
	@Autowired
	private WaferRepository waferIdRepository;
	@Value("${columnMappingDefectList.path}")
	private String columnMappingDefectList;

	public static final Logger logger = LoggerFactory.getLogger(GraphService.class);

	@Override
	public LineGraphResponse getGraphData(LineGraphRequest requestParam) throws IOException {
		String collectionName = "InspectedWaferData";
		List<Document> graphData = new ArrayList<Document>();
		List<Document> documentDimension = new ArrayList<Document>();
		List<Document> DimensionArray = new ArrayList<Document>();
		LineGraphResponse res = new LineGraphResponse();
		String aggregationType = requestParam.yAxis.settings.get(0).aggregator;
		/* String DimensionName = requestParam.xAxis.settings.get(0).getName(); */
		String Granuality = requestParam.xAxis.settings.get(0).granularity;
		String MeasureName = requestParam.yAxis.settings.get(0).name;
		String group = requestParam.group;

		Map<String, String> jsonObjectForXaxis = GraphUtil
				.getLineGraphSchema("src/main/resources/vdi/Graph.properties");
		Map<String, String> jsonObjForYAxis_Ws = GraphUtil
				.getLineGraphSchema("src/main/resources/vdi/yAxis_ws.properties");
		Map<String, String> jsonObjForYxis_Wd = GraphUtil
				.getLineGraphSchema("src/main/resources/vdi/yAxis_wd.properties");

		MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(requestParam.waferID));
		LookupOperation lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");

		UnwindOperation unwWindOperationonWaferDefects = Aggregation.unwind("$waferDefects", true);

		LookupOperation lookupOnWaferInspectionSummary = null;
		UnwindOperation unwWindOperation = null;

		if (jsonObjForYAxis_Ws.containsKey(requestParam.yAxis.settings.get(0).name)) {
			lookupOnWaferInspectionSummary = LookupOperation.newLookup().from("WaferInspectionSummary")
					.localField("_id").foreignField("waferId").as("waferInspectionSummary");
			unwWindOperation = Aggregation.unwind("$waferInspectionSummary", true);
		}

		List<String> xAxes = new ArrayList<String>();
		List<XaxisSetting> settings = requestParam.xAxis.settings;
		int i = 0;
		for (XaxisSetting x : settings) {
			xAxes.add(i, requestParam.xAxis.settings.get(i).name);
			i++;
		}
		Map<String, String> jsonForxAxes = new HashMap<String, String>();
		if (xAxes != null && xAxes.size() > 0) {
			for (Map.Entry<String, String> entry : jsonObjectForXaxis.entrySet()) {
				for (String string : xAxes) {
					if (string.equals(entry.getKey())) {
						jsonForxAxes.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}

		String yAxis = requestParam.yAxis.settings.get(0).name;
		Map<String, String> jsonForYAxis = new HashMap<String, String>();
		String yAxisKey = null;
		for (Map.Entry<String, String> entry : jsonObjForYAxis_Ws.entrySet()) {
			if (yAxis.equals(entry.getKey())) {
				yAxisKey = entry.getValue();
				jsonForYAxis.put(entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<String, String> entry : jsonObjForYxis_Wd.entrySet()) {
			if (yAxis.equals(entry.getKey())) {
				yAxisKey = entry.getValue();
				jsonForYAxis.put(entry.getKey(), entry.getValue());
			}
		}

		GroupOperation groupOperation = createGroupOperation(jsonForxAxes, yAxisKey, aggregationType);
		ProjectionOperation projectionOperation = createProjection(jsonForxAxes, yAxisKey, aggregationType, group);
		Aggregation aggregation = null;
		if ((jsonForxAxes.containsKey("CAT") || jsonForxAxes.containsKey("RB") || jsonForxAxes.containsKey("FB")
				|| jsonForxAxes.containsKey("Test"))
				&& jsonObjForYAxis_Ws.containsKey(requestParam.yAxis.settings.get(0).name)) {
			aggregation = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
					unwWindOperationonWaferDefects, lookupOnWaferInspectionSummary, unwWindOperation, groupOperation,
					projectionOperation);

		} else if (jsonObjForYxis_Wd.containsKey(requestParam.yAxis.settings.get(0).name)) {
			aggregation = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
					unwWindOperationonWaferDefects, groupOperation, projectionOperation);
		} else {
			aggregation = Aggregation.newAggregation(matchOperation, lookupOnWaferInspectionSummary, unwWindOperation,
					groupOperation, projectionOperation);
		}

		graphData = mongoTemplate.aggregate(aggregation, collectionName, Document.class).getMappedResults();
		logger.info("Aggregation Query" + aggregation);
		if (graphData.size() > 0) {
			res.setxAxisCount(requestParam.xAxis.getAxisCount());
			res.setyAxisCount(requestParam.yAxis.getAxisCount());
			res.setData(graphData);
			documentDimension = graphData;
			DimensionArray.add((Document) documentDimension.get(0).get("xAxis"));
			String Dimension = String.valueOf(DimensionArray.get(0));
			List<XaxisResponseDetails> l = new ArrayList<XaxisResponseDetails>();
			for (int i1 = 0; i1 < requestParam.xAxis.getAxisCount(); i1++) {
				XaxisResponseDetails XResDetails = new XaxisResponseDetails();
				// DimensionName
				XResDetails.setAxisName(requestParam.xAxis.settings.get(i1).getName());
				if (Granuality != null && Granuality != "") {
					XResDetails.setType("DateTime");
				} else {
					if (isNumeric((Dimension))) {
						XResDetails.setType("String");
					} else {
						XResDetails.setType("Number");
					}
				}
				l.add(i1, XResDetails);
			}
			res.setxAxisDetails(l);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			res.setyAxisDetails(yResDetails);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			res.setGroupDetails(groupResDetails);
		} else {
			res.setData(new ArrayList<>());
		}
		return res;
	}

	private GroupOperation createGroupOperation(Map<String, String> jsonForxAxes, String jsonForYAxis,
			String aggregationtype) {
		GroupOperation groupOperation = null;
		Fields fields = Fields.fields();
		Iterator<?> keys = jsonForxAxes.keySet().iterator();
		if (aggregationtype.equalsIgnoreCase("Avg")) {
			while (keys.hasNext()) {
				String key = (String) keys.next();
				fields = fields.and(key, jsonForxAxes.get(key));
			}
			groupOperation = Aggregation.group(fields).avg(jsonForYAxis).as("average");
		} else if (aggregationtype.equalsIgnoreCase("count")) {
			while (keys.hasNext()) {
				String key = (String) keys.next();
				fields = fields.and(key, jsonForxAxes.get(key));
			}
			groupOperation = Aggregation.group(fields).sum(jsonForYAxis).as("sum");
		}
		Iterator<?> key1 = jsonForxAxes.keySet().iterator();
		while (key1.hasNext()) {
			String key = (String) key1.next();
			groupOperation = groupOperation.first(jsonForxAxes.get(key)).as(key);
		}
		return groupOperation;
	}

	private ProjectionOperation createProjection(Map<String, String> jsonForxAxes, String jsonForYAxis,
			String aggregationtype, String group) {
		ProjectionOperation projection = Aggregation.project().and("_id." + group).as("group");
		if (aggregationtype.equalsIgnoreCase("Avg")) {
			Iterator<?> keys = jsonForxAxes.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				projection = projection.and(key).as("xAxis." + key);
			}
			projection = projection.andArrayOf("$average").as("yAxis").andExclude("_id");
		} else if (aggregationtype.equalsIgnoreCase("count")) {

			Iterator<?> keys = jsonForxAxes.keySet().iterator();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				projection = projection.and(key).as("xAxis." + key);
			}
			projection = projection.andArrayOf("$sum").as("yAxis").andExclude("_id");
		}
		return projection;
	}

	@Override
	public LineGraphResponse getLineGraphDetails(LineGraphRequest request) throws IOException {
		logger.info("Inside getLineGraphDetails....");
		LineGraphResponse res = new LineGraphResponse();
		List<Document> testResults = null;
		List<ObjectId> waferIds = request.getWaferID();
		String group = request.getGroup();
		XaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();
		int XaxisCount = xaxis.getAxisCount();
		int YaxisCount = yaxis.getAxisCount();
		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String Granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();
		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		List<Area> areas = request.getAreas();
		List<Area> discardedAreas = request.getDiscardedAreas();

		Set<String> invalidGroupSet = new HashSet<String>(Arrays.asList("CAT", "RB", "FB"));
		Set<String> invalidDimensionNameSet = new HashSet<String>(Arrays.asList("CAT", "RB", "FB"));
		Set<String> invalidMeasureNameSet = new HashSet<String>(Arrays.asList("Density"));
		Set<String> invalidArea = new HashSet<String>(Arrays.asList("Area", "N-Die", "D-Die"));

		List<Integer> crfFilter = null;
		String crfValue = null;
		if (request.filter.cat.size() > 0) {
			crfFilter = request.filter.cat;
			crfValue = "classNumber";
		}
		if (request.filter.rbn.size() > 0) {
			crfFilter = request.filter.rbn;
			crfValue = "roughBinNumber";
		}
		if (request.filter.fbn.size() > 0) {
			crfFilter = request.filter.fbn;
			crfValue = "fineBinNumber";
		}
		List<String> elements = null;
		if (request.getElements().size() > 0) {
			elements = request.getElements();
		} else {
			if (!(crfFilter == null)) {
				res.setData(new ArrayList<>());
				return res;
			}
		}

		if (crfFilter == null) {
			if (MeasureName.equals("Area") || MeasureName.equals("N-Die") || MeasureName.equals("D-Die")) {
				if ((invalidDimensionNameSet.contains(DimensionName) && invalidArea.contains(MeasureName))
						|| (invalidGroupSet.contains(group))) {
					throw new IOException("Combination is currently not supported");
				}
				testResults = nofilterMeasure(waferIds, DimensionName, MeasureName, group, selectedBin, testName,
						testNumber, minVal, maxVal, Aggregator, Granuality, discardedAreas);
			} else {
				return null;
			}
		} else if (DimensionName.equals("Test") && group.equals("Device")
				&& (MeasureName.equals("tDefect") || MeasureName.equals("aDefect"))) {
			testResults = getTestRelatedMeasure(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName,
					group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, areas);
		} else if (invalidGroupSet.contains(group) && invalidMeasureNameSet.contains(MeasureName)) {
			throw new IOException("Combination is currently not supported");
		} else if (invalidDimensionNameSet.contains(DimensionName) && invalidMeasureNameSet.contains(MeasureName)) {
			throw new IOException("Combination is currently not supported");
		} else if (invalidGroupSet.contains(group) && invalidDimensionNameSet.contains(DimensionName))
			throw new IOException("Combination is currently not supported");
		else if (group.equals("CAT") || group.equals("FB") || group.equals("RB")) {
			testResults = getGroupingRelatedMeasure(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName,
					group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, Granuality, areas);
		} else if (DimensionName.equals("ResultTimeStamp")) {
			testResults = getTimeRelatedMeasure(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName,
					group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, Granuality, areas,
					discardedAreas);
		} else if (DimensionName.equals("CAT") || DimensionName.equals("FB") || DimensionName.equals("RB")) {
			testResults = getUniqueDimensionMeasure(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName,
					group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, areas);
		} else if (MeasureName.equals("Sx") || MeasureName.equals("Sy") || MeasureName.equals("A-Size")
				|| MeasureName.equals("D-Size") || MeasureName.equals("Process")) {
			testResults = getCommonDimensionsAndMeasure1(waferIds, crfFilter, crfValue, elements, DimensionName,
					MeasureName, group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, areas);
		} else {
			testResults = getCommonDimensionsAndMeasure(waferIds, crfFilter, crfValue, elements, DimensionName,
					MeasureName, group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, areas,
					discardedAreas);
		}

		if (testResults.size() > 0) {
			res.setxAxisCount(XaxisCount);
			res.setyAxisCount(YaxisCount);
			res.setData(testResults);
			Document documentDimension = testResults.get(0);

			List DimensionArray = (List) documentDimension.get("xAxis");
			String Dimension = String.valueOf(DimensionArray.get(0));
			List<XaxisResponseDetails> l = new ArrayList<XaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (Granuality != null && Granuality != "") {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("String");
				} else {
					XResDetails.setType("Number");
				}
			}
			l.add(0, XResDetails);
			res.setxAxisDetails(l);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			res.setyAxisDetails(yResDetails);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			res.setGroupDetails(groupResDetails);
		} else {
			res.setData(new ArrayList<>());
		}
		return res;
	}

	private List<Document> nofilterMeasure(List<ObjectId> waferIds, String dimensionName, String measureName,
			String group, int selectedBin, String testName, int testNumber, double minVal, double maxVal,
			String aggregator, String granuality, List<Area> discardedAreas) throws IOException {
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null, projectionForZeroArea, projectionForNullArea, projectdiscardedarea,
				excludeId = null;
		GroupOperation groupOperation = null, finalgroup;
		GroupOperation groupOperationTest = null;
		UnwindOperation unwWindOperationTest = null;
		List<Document> testResults = null;
		MatchOperation matchOperation = null;
		String collectionName = null;
		List<Document> getkeyCount = null;
		String DimensionX = null;
		String MeasureValY = null;
		String splitDimension = null;
		String dateFormat = "";
		AddFieldsOperation addFieldsOperation = null, addFieldsdiscardedarea;
		float discardedArea = 0;
		AggregationOperation FilterMinMax = null;

		Set<String> measureValues = new HashSet<String>(Arrays.asList("Area", "N-Die", "D-Die"));

		if (minVal > 0 && maxVal == 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}
		if (discardedAreas != null) {
			for (Area area : discardedAreas) {
				float to = area.getRadius().getTo();
				float from = area.getRadius().getFrom();
				float angleto = area.getAngle().getTo();
				float anglefrom = area.getAngle().getFrom();
				float angle = (angleto) - (anglefrom);
				float sectorArea = (float) ((angle / 360) * (22 / 7) * ((to * to) - (from * from)));
				discardedArea = discardedArea + sectorArea;
			}
			discardedArea = discardedArea / 100;
		}

		if (dimensionName.equals("Test") && measureValues.contains(measureName)) {
			MeasureValY = generateFieldNameForTest(measureName);
			DimensionX = "Test.testNo";
			splitDimension = "lotRecord.waferRecord.summaryRecords";
			collectionName = "InspectedWaferData";
			logger.info("Preparing Query getLineGraphDetails");
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));

			groupOperationTest = Aggregation.group("_id").first(splitDimension).as("Test");
			unwWindOperationTest = Aggregation.unwind("$Test", false);

			groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").sum(MeasureValY).as("yObj").addToSet("_id").as("waferIds");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferIds").as("waferIds");

			excludeId = Aggregation.project().andExclude("_id");
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, groupOperationTest, unwWindOperationTest,
						groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, groupOperationTest, unwWindOperationTest,
						groupOperation, projectFetchData, FilterMinMax, excludeId);
			}

			logger.info("Aggregation Query for Summary in Inspected WaferData: " + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();

			if (aggregator.equals("Avg")) {
				getkeyCount = getTestKeycount(waferIds, splitDimension, DimensionX);
				int count = 0;
				double i = 0;
				for (Document document : testResults) {
					for (Document document1 : getkeyCount) {
						List DimensionArray = (List) document.get("xAxis");
						String Dimension = String.valueOf(DimensionArray.get(0));
						Object DimensionArray1 = document1.get("key");
						String Dimension1 = String.valueOf(DimensionArray1);
						if (Dimension.equals(Dimension1)) {
							count = ((int) document1.get("count"));
							break;
						}
					}
					List DimensionArray = (List) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i2 = Double.parseDouble(Dimension);
					double total = i2 / count;
					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal && total > minVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}

				}
			}

			List<Document> tempDoc = new ArrayList<>();
			for (Document document : testResults) {
				if (!document.isEmpty()) {
					List<ObjectId> wafers = (List) document.get("waferIds");
					List waferList = new ArrayList<>();
					for (int i = 0; i < wafers.size(); i++) {
						String hexString = wafers.get(i).toHexString();
						waferList.add(hexString);
					}
					document.put("waferIds", waferList);
					tempDoc.add(document);
					testResults = tempDoc;
				}
			}
			return testResults;
		} else {
			DimensionX = generateFieldNameForCriteria(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}

			MeasureValY = generateFieldNameForCriteria(measureName);
			if (MeasureValY == null) {
				throw new IOException("Selected Measure is not supported.");
			}

			collectionName = "InspectedWaferData";

			if (granuality != null && granuality != "") {
				dateFormat = getDateFormat(granuality);
			}
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));

			addFieldsOperation = Aggregation.addFields()
					.addFieldWithValue("$DateField", DateOperators.dateOf(DimensionX).toString(dateFormat)).build();
			addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea).build();
			groupOperation = Aggregation.group("_id").first(DimensionX).as("xObj").first(MeasureValY).as("yObj")
					.first("$lotRecord.sampleSize.x").as("diameter").first("$discardedArea").as("discardedArea")
					.first("$_id").as("waferId");
			projectionForZeroArea = Aggregation.project().andInclude("_id", "xObj", "yObj", "diameter", "discardedArea",
						"waferId")
					.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$yObj").equalToValue(0))
							.then(new Document("$multiply",
									Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
							.otherwise("$yObj"))
					.as("area");
			projectionForNullArea = Aggregation
					.project().andInclude("_id", "xObj", "discardedArea","waferId").and(
							ConditionalOperators.ifNull("$area")
									.then(new Document("$multiply",
											Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
					.as("yObj");
			projectdiscardedarea = Aggregation.project().andInclude("xObj","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
							.then(new Document("$subtract", Arrays.asList("$yObj", "$discardedArea")))
							.otherwise("$yObj"))
					.as("yObj");
			finalgroup = Aggregation.group("xObj").first("xObj").as("xObj").sum("yObj").as("yObj")
							.addToSet("waferId").as("waferIds");
			projectFetchData = Aggregation.project().and("$xObj").as("_id").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferIds").as("waferIds");
			excludeId = Aggregation.project().andExclude("_id");

			if (dimensionName.equals("ResultTimeStamp")) {
				groupOperation = Aggregation.group("$_id", "DateField").first("DateField").as("xObj").first(MeasureValY)
						.as("yObj").first("$lotRecord.sampleSize.x").as("diameter").first("$discardedArea")
						.as("discardedArea").first("$_id").as("waferId");
				if (measureName.equalsIgnoreCase("Area")) {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsOperation,
								addFieldsdiscardedarea, groupOperation, projectionForZeroArea, projectionForNullArea,
								projectdiscardedarea, finalgroup, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsOperation,
								addFieldsdiscardedarea, groupOperation, projectionForZeroArea, projectionForNullArea,
								projectdiscardedarea, finalgroup, projectFetchData, FilterMinMax, excludeId);
					}

				} else {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsOperation,
								addFieldsdiscardedarea, groupOperation, projectdiscardedarea, finalgroup,
								projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsOperation,
								addFieldsdiscardedarea, groupOperation, projectdiscardedarea, finalgroup,
								projectFetchData, FilterMinMax, excludeId);
					}

				}
			} else {
				if (measureName.equals("Area")) {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsdiscardedarea,
								groupOperation, projectionForZeroArea, projectionForNullArea, projectdiscardedarea,
								finalgroup, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsdiscardedarea,
								groupOperation, projectionForZeroArea, projectionForNullArea, projectdiscardedarea,
								finalgroup, projectFetchData, FilterMinMax, excludeId);
					}

				} else {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsdiscardedarea,
								groupOperation, projectdiscardedarea, finalgroup, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsdiscardedarea,
								groupOperation, projectdiscardedarea, finalgroup, projectFetchData, FilterMinMax,
								excludeId);
					}

				}
			}

			logger.info("Aggregation Query for Summary in Inspected WaferData: " + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		}

		if (aggregator.equals("Avg")) {

			if (dimensionName.equals("ResultTimeStamp")) {
				getkeyCount = getkeyCountTimeStamp(waferIds, dimensionName, granuality);
			} else {
				getkeyCount = getkeyCount(waferIds, dimensionName);
			}
			int count = 0;
			double i = 0;
			for (Document document : testResults) {
				for (Document document1 : getkeyCount) {
					List DimensionArray = (List) document.get("xAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					Object DimensionArray1 = document1.get("key");
					String Dimension1 = String.valueOf(DimensionArray1);
					if (Dimension.equals(Dimension1)) {
						count = ((int) document1.get("count"));
						break;
					}
				}

				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i2 = Double.parseDouble(Dimension);
				double total = i2 / count;
				if (minVal == 0 && maxVal == 0.0) {
					document.put("yAxis", Arrays.asList(total));
				} else {
					if (total < maxVal && total > minVal) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();

					}
				}
			}
		}

		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				List<ObjectId> wafers = (List) document.get("waferIds");
				List waferList = new ArrayList<>();
				for (int i = 0; i < wafers.size(); i++) {
					String hexString = wafers.get(i).toHexString();
					waferList.add(hexString);
				}
				document.put("waferIds", waferList);
				tempDoc.add(document);
				testResults = tempDoc;
			}
		}
		return testResults;
	}

	private List<Document> getGroupingRelatedMeasure(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin,
			String testName, int testNumber, double minVal, double maxVal, String aggregator, String granuality,
			List<Area> areas) throws IOException {
		AddFieldsOperation addFieldsOperation = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		LookupOperation lookupOnEdx, lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonEdx, unwWindOperationonWaferDefects = null;
		GroupOperation groupOperation = null;
		String dateFormat = "";
		String DimensionX = "";
		String MeasureValY = "";
		String[] parts = null;
		String key = null;
		List<Document> testResults = null;
		MatchOperation edxMatch = null, matchOperation = null;
		String grouping = null;
		String collectionName = "";
		MatchOperation crfMatch = null;
		List<Criteria> criteriaListForSelection = null;
		boolean edxflag = true;
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		collectionName = "WaferDefects";
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;
		AggregationOperation FilterMinMax = null;

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		if (minVal > 0 && maxVal == 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		if (dimensionName.equals("Test")) {
			matchOperation = Aggregation.match(Criteria.where("waferId").in(waferIds));
			grouping = generateFields(group);

			criteriaForEdx = generateQueryForEdx(elements, edxflag);
			if (areas == null || areas.isEmpty()) {
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
			} else {
				criteriaListForSelection = generateQueryForSelection(areas);
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
						.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
			}

			if (edxflag == true) {
				criteriaForEdxMatch = generateQueryforEdxMatch(elements);
				edxMatch = Aggregation.match(new Criteria()
						.orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
			}
			lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("edx").foreignField("_id").as("edxObj");
			unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

			DimensionX = generateFields(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}
			MeasureValY = generateTestFieldNameForCriteria(measureName);
			if (MeasureValY == null) {
				throw new IOException("Selected Measure is not supported");
			}

			if (measureName.equalsIgnoreCase("aDefect")) {
				groupOperation = Aggregation.group(grouping, DimensionX).first(grouping).as("group").first(DimensionX)
						.as("xObj").push("$adder").as("yObj").addToSet("_id").as("waferId");

				projectFetchData = Aggregation.project().and("$group").as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId").as("waferIds");
			}

			if (measureName.equalsIgnoreCase("tDefect")) {
				groupOperation = Aggregation.group(grouping, DimensionX).first(grouping).as("group").first(DimensionX)
						.as("xObj").count().as("yAxis").addToSet("_id").as("waferId");

				projectFetchData = Aggregation.project().and("$group").as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf("$yAxis").as("yAxis").and("$waferId").as("waferIds");

			}
			if (edxflag == false) {
				if (minVal == 0 && maxVal == 0.0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData, FilterMinMax, excludeId);
				}
			} else {
				if (minVal == 0 && maxVal == 0.0) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, FilterMinMax, excludeId);
				}
			}

		} else {
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
			lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
					.foreignField("waferId").as("waferDefects");
			unwWindOperationonWaferDefects = Aggregation.unwind("$waferDefects", false);

			if (crfValue.equals("classNumber")) {
				crfValue = "waferDefects.classNumber";
			}
			if (crfValue.equals("roughBinNumber")) {
				crfValue = "waferDefects.roughBinNumber";
			}
			if (crfValue.equals("fineBinNumber")) {
				crfValue = "waferDefects.fineBinNumber";
			}

			criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
			if (areas == null || areas.isEmpty()) {
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
			} else {
				criteriaListForSelection = generateQueryForSelectionAreas(areas);
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
						.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
			}

			if (edxflag == true) {
				criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
				edxMatch = Aggregation.match(new Criteria()
						.orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
			}
			lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
					.as("edxObj");
			unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

			grouping = generateFieldNameForCriteria(group);
			DimensionX = generateFieldNameForCriteria(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}

			MeasureValY = generateFieldNameForCriteria(measureName);
			if (MeasureValY == null) {
				throw new IOException("Selected Measure is not supported.");
			}
			parts = DimensionX.split("\\.");
			key = null;
			for (String end : parts) {
				key = end;
			}

			if (dimensionName.equals("ResultTimeStamp")) {

				if (granuality != null && granuality != "") {
					dateFormat = getDateFormat(granuality);
				}
				addFieldsOperation = Aggregation.addFields()
						.addFieldWithValue("$DateField", DateOperators.dateOf(DimensionX).toString(dateFormat)).build();

				if (measureName.equals("tDefect")) {
					groupOperation = Aggregation.group(grouping, "DateField").first(grouping).as("grouping")
							.first("DateField").as("xObj").count().as("yObj").addToSet("_id").as("waferId");
					projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");

				} else if (measureName.equals("aDefect")) {
					groupOperation = Aggregation.group(grouping, "DateField").first(grouping).as("grouping")
							.first("DateField").as("xObj").push("$waferDefects.adder").as("yObj").addToSet("_id")
							.as("waferId");
					projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId")
							.as("waferIds");
				}
				if (edxflag == false) {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, addFieldsOperation, groupOperation,
								projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, addFieldsOperation, groupOperation,
								projectFetchData, FilterMinMax, excludeId);
					}
				} else {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsOperation, groupOperation, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsOperation, groupOperation, projectFetchData, FilterMinMax, excludeId);
					}
				}
				logger.info("Preparing aggregationMean getGraphDetails" + aggregationMean);
				testResults = mongoTemplate.aggregate(aggregationMean, "InspectedWaferData", Document.class)
						.getMappedResults();
				if (testResults == null || testResults.isEmpty()) {
					return null;
				}
				List<Document> getkeyCount = getkeyCountTimeStamp(waferIds, dimensionName, granuality);
				if (aggregator.equalsIgnoreCase("Avg")) {
					int count = 0;
					double i = 0;
					for (Document document : testResults) {
						for (Document document1 : getkeyCount) {
							List DimensionArray = (List) document.get("xAxis");
							String Dimension = String.valueOf(DimensionArray.get(0));
							Object DimensionArray1 = document1.get("key");
							String Dimension1 = String.valueOf(DimensionArray1);
							if (Dimension.equals(Dimension1)) {
								count = ((int) document1.get("count"));
								break;
							}
						}

						List DimensionArray = (List) document.get("yAxis");
						String Dimension = String.valueOf(DimensionArray.get(0));
						double i2 = Double.parseDouble(Dimension);
						double total = i2 / count;
						if (minVal == 0 && maxVal == 0.0) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							if (total < maxVal && total > minVal) {
								document.put("yAxis", Arrays.asList(total));
							} else {
								document.clear();

							}
						}
					}
				}

				List<Document> tempDoc = new ArrayList<>();
				for (Document document : testResults) {
					if (!document.isEmpty()) {
						List<ObjectId> wafers = (List) document.get("waferIds");
						List waferList = new ArrayList<>();
						for (int i = 0; i < wafers.size(); i++) {
							String hexString = wafers.get(i).toHexString();
							waferList.add(hexString);
						}
						document.put("waferIds", waferList);
						tempDoc.add(document);
						testResults = tempDoc;
					}
				}
				return testResults;
			}

			else {

				if (measureName.equals("tDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("grouping", grouping).and(key, DimensionX))
							.first(grouping).as("grouping").first(DimensionX).as("xObj").count().as("yObj")
							.addToSet("_id").as("waferId");
					projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");
				} else if (measureName.equals("aDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("grouping", grouping).and(key, DimensionX))
							.first(grouping).as("grouping").first(DimensionX).as("xObj").push("$waferDefects.adder")
							.as("yObj").addToSet("_id").as("waferId");
					projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId")
							.as("waferIds");
				}
				if (edxflag == false) {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, groupOperation, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, groupOperation, projectFetchData,
								FilterMinMax, excludeId);
					}
				} else {
					if (minVal == 0 && maxVal == 0.0) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectFetchData, excludeId);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectFetchData, FilterMinMax, excludeId);
					}
				}
			}

		}
		logger.info("Preparing aggregationMean getGraphDetails" + aggregationMean);
		if (dimensionName.equals("Test")) {
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}
			if (aggregator.equalsIgnoreCase("Avg")) {
				int wSize = waferIds.size();
				for (Document document : testResults) {
					List DimensionArray = (List) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i = Double.parseDouble(Dimension);
					double total = i / wSize;
					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal && total > minVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}
				}
			}
			List<Document> tempDoc = new ArrayList<>();
			for (Document document : testResults) {
				if (!document.isEmpty()) {
					List<ObjectId> wafers = (List) document.get("waferIds");
					List waferList = new ArrayList<>();
					for (int i = 0; i < wafers.size(); i++) {
						String hexString = wafers.get(i).toHexString();
						waferList.add(hexString);
					}
					document.put("waferIds", waferList);
					tempDoc.add(document);
					testResults = tempDoc;
				}
			}

			return testResults;
		} else {
			testResults = mongoTemplate.aggregate(aggregationMean, "InspectedWaferData", Document.class)
					.getMappedResults();
		}
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}
		List<Document> getkeyCount = getkeyCount(waferIds, dimensionName);
		if (aggregator.equalsIgnoreCase("Avg")) {
			int count = 0;
			double i = 0;
			for (Document document : testResults) {
				for (Document document1 : getkeyCount) {
					List DimensionArray = (List) document.get("xAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					Object DimensionArray1 = document1.get("key");
					String Dimension1 = String.valueOf(DimensionArray1);
					if (Dimension.equals(Dimension1)) {
						count = ((int) document1.get("count"));
						break;
					}
				}
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i2 = Double.parseDouble(Dimension);
				double total = i2 / count;

				if (minVal == 0 && maxVal == 0.0) {
					document.put("yAxis", Arrays.asList(total));
				} else {
					if (total < maxVal && total > minVal) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();

					}
				}

			}
		}

		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				List<ObjectId> wafers = (List) document.get("waferIds");
				List waferList = new ArrayList<>();
				for (int i = 0; i < wafers.size(); i++) {
					String hexString = wafers.get(i).toHexString();
					waferList.add(hexString);
				}
				document.put("waferIds", waferList);
				tempDoc.add(document);
				testResults = tempDoc;
			}
		}
		return testResults;
	}

	private List<Document> getTimeRelatedMeasure(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin,
			String testName, int testNumber, double minVal, double maxVal, String aggregator, String granuality,
			List<Area> areas, List<Area> discardedAreas) throws IOException {

		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		ProjectionOperation projectionForZeroDensity = null, projectionForNullDensity = null,
				projectdiscardedarea = null;
		GroupOperation groupOperation, groupOperationForDensity = null;
		AddFieldsOperation addFieldsOperation, addFieldsOperationDensity = null, addFieldsdiscardedarea = null;
		LookupOperation lookupOnEdx, lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonEdx, unwWindwaferDefects = null;
		MatchOperation edxMatch = null, crfMatch = null;
		boolean edxflag = true;
		List<Document> testResults = null;
		List<Criteria> criteriaListForSelection = null;
		float discardedArea = 0;
		AggregationOperation FilterMinMax = null;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;

		if (minVal > 0 && maxVal == 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		String MeasureValY = generateFieldNameForCriteria(measureName);
		if (MeasureValY == null) {
			throw new IOException("Selected Combination is not supported.");
		}

		MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));

		String[] parts = DimensionX.split("\\.");
		String key = null;
		for (String end : parts) {
			key = end;
		}
		String dateFormat = "";
		if (granuality != null && granuality != "") {
			dateFormat = getDateFormat(granuality);
		}
		if (discardedAreas != null) {
			for (Area area : discardedAreas) {
				float to = area.getRadius().getTo();
				float from = area.getRadius().getFrom();
				float angleto = area.getAngle().getTo();
				float anglefrom = area.getAngle().getFrom();
				float angle = (angleto) - (anglefrom);
				float sectorArea = (float) ((angle / 360) * (22 / 7) * ((to * to) - (from * from)));
				discardedArea = discardedArea + sectorArea;
			}
			discardedArea = discardedArea / 100;
		}

		addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$DateField", DateOperators.dateOf(DimensionX).toString(dateFormat)).build();
		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");

		lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		unwWindwaferDefects = Aggregation.unwind("$waferDefects", false);

		if (crfValue.equals("classNumber")) {
			crfValue = "waferDefects.classNumber";
		}
		if (crfValue.equals("roughBinNumber")) {
			crfValue = "waferDefects.roughBinNumber";
		}
		if (crfValue.equals("fineBinNumber")) {
			crfValue = "waferDefects.fineBinNumber";
		}
		criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));

		} else {
			criteriaListForSelection = generateQueryForSelectionAreas(areas);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));

		}

		if (edxflag == true) {
			criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
			edxMatch = Aggregation.match(
					new Criteria().orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
		}
		lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
				.as("edxObj");
		unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

		groupOperation = Aggregation.group(key, "DateField").first(key).as("grouping").first("DateField").as("xObj")
				.sum(MeasureValY).as("yObj").addToSet("_id").as("waferId");
		projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj").as("xAxis")
				.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");

		if (measureName.equals("tDefect")) {
			groupOperation = Aggregation.group(key, "DateField").first(key).as("grouping").first("DateField").as("xObj")
					.count().as("yObj").addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");

		} else if (measureName.equals("aDefect")) {
			groupOperation = Aggregation.group(key, "DateField").first(key).as("grouping").first("DateField").as("xObj")
					.push("$waferDefects.adder").as("yObj").addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("$grouping").as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId").as("waferIds");
		} else if (measureName.equals("Density")) {

			addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea).build();
			groupOperation = Aggregation.group(Fields.fields().and("id", "$_id").and(key, "DateField"))
					.first("DateField").as("xObj").first("$waferInspectionSummary.testArea").as("area")
					.first("$lotRecord.sampleSize.x").as("diameter").count().as("yObj").first("$discardedArea")
					.as("discardedArea").first("$_id").as("waferId");

			projectionForZeroDensity = Aggregation.project()
					.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea", "waferId")
					.and(ConditionalOperators.when((ComparisonOperators.Eq.valueOf("$area").equalToValue(0)))
							.then(new Document("$multiply",
									Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
							.otherwise("$area"))
					.as("area");

			projectionForNullDensity = Aggregation
					.project().andInclude("_id", "xObj", "yObj", "discardedArea", "waferId").and(
							ConditionalOperators.ifNull("$area")
									.then(new Document("$multiply",
											Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
					.as("area");
			projectdiscardedarea = Aggregation.project()
					.and(ConditionalOperators.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
							.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
							.otherwise("$area"))
					.as("area").andInclude("yObj", "waferId").andInclude("xObj");

			addFieldsOperationDensity = Aggregation.addFields()
					.addFieldWithValue("defDensity",
							new Document("$round",
									Arrays.asList(new Document("$divide", Arrays.asList("$yObj", "$area")), 16L)))
					.build();

			groupOperationForDensity = Aggregation.group("xObj").first("xObj").as("xObj").sum("defDensity").as("yObj")
					.addToSet("waferId").as("waferId");

			projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj").as("yAxis")
					.and("$waferId").as("waferIds");

		}
		if (edxflag == false) {
			if (minVal == 0 && maxVal == 0.0) {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, addFieldsOperation, addFieldsdiscardedarea, groupOperation,
							projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
							addFieldsOperationDensity, groupOperationForDensity, projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, addFieldsOperation, groupOperation, projectFetchData,
							excludeId);
				}
			} else {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, addFieldsOperation, addFieldsdiscardedarea, groupOperation,
							projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
							addFieldsOperationDensity, groupOperationForDensity, projectFetchData, FilterMinMax,
							excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, addFieldsOperation, groupOperation, projectFetchData,
							FilterMinMax, excludeId);
				}
			}

		} else {
			if (minVal == 0 && maxVal == 0.0) {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							addFieldsOperation, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
							projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
							groupOperationForDensity, projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							addFieldsOperation, groupOperation, projectFetchData, excludeId);
				}
			} else {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							addFieldsOperation, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
							projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
							groupOperationForDensity, projectFetchData, FilterMinMax, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindwaferDefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							addFieldsOperation, groupOperation, projectFetchData, FilterMinMax, excludeId);
				}
			}

		}
		logger.info("Preparing aggregationMean getGraphDetails" + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, "InspectedWaferData", Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}
		List<Document> getkeyCount = getkeyCountTimeStamp(waferIds, dimensionName, granuality);
		if (aggregator.equalsIgnoreCase("Avg")) {
			int count = 0;
			double i = 0;
			for (Document document : testResults) {
				for (Document document1 : getkeyCount) {
					List DimensionArray = (List) document.get("xAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					Object DimensionArray1 = document1.get("key");
					String Dimension1 = String.valueOf(DimensionArray1);
					if (Dimension.equals(Dimension1)) {
						count = ((int) document1.get("count"));
						break;
					}
				}
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i2 = Double.parseDouble(Dimension);
				double total = i2 / count;
				if (minVal == 0 && maxVal == 0.0) {
					document.put("yAxis", Arrays.asList(total));
				} else {
					if (total < maxVal && total > minVal) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();

					}
				}
			}
		}

		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				if (!document.isEmpty()) {
					List<ObjectId> wafers = (List) document.get("waferIds");
					List waferList = new ArrayList<>();
					for (int i = 0; i < wafers.size(); i++) {
						String hexString = wafers.get(i).toHexString();
						waferList.add(hexString);
					}
					document.put("waferIds", waferList);
					tempDoc.add(document);
					testResults = tempDoc;
				}
			}
		}
		return testResults;
	}

	private List<Document> getTestRelatedMeasure(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin,
			String testName, int testNumber, double minVal, double maxVal, String aggregator, List<Area> areas)
			throws IOException {

		List<Document> testResults = null;
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null;
		AggregationOperation FilterMinMax = null;
		List<Document> getkeyCount = null;
		MatchOperation crfMatch, edxMatch = null;
		List<Criteria> criteriaListForSelection = null;
		boolean edxflag = true;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;
		LookupOperation lookupOnEdx = null;
		UnwindOperation unwWindOperationonEdx = null;

		if (minVal > 0 && maxVal == 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		String DimensionX = generateTestFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		if (measureName.equals("Density")) {
			throw new IOException("Combination is currently not supported");
		}

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		GroupOperation groupOperation = null;
		String groupParameter = null;
		MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));

		String[] parts = DimensionX.split("\\.");
		String key = null;
		for (String end : parts) {
			key = end;
		}

		ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
		LookupOperation lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		UnwindOperation unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", true);

		if (crfValue.equals("classNumber")) {
			crfValue = "waferDefects.classNumber";
		}
		if (crfValue.equals("roughBinNumber")) {
			crfValue = "waferDefects.roughBinNumber";
		}
		if (crfValue.equals("fineBinNumber")) {
			crfValue = "waferDefects.fineBinNumber";
		}
		criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
		} else {
			criteriaListForSelection = generateQueryForSelectionAreas(areas);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
		}

		if (edxflag == true) {
			criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
			edxMatch = Aggregation.match(
					new Criteria().orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
		}
		lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
				.as("edxObj");
		unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

		groupParameter = "waferDefects.test";
		if (measureName.equals("tDefect")) {
			groupOperation = Aggregation.group(Fields.fields().and(key, groupParameter)).first(groupParameter)
					.as("xObj").count().as("yObj").addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");
		} else if (measureName.equals("aDefect")) {
			groupOperation = Aggregation.group(Fields.fields().and(key, groupParameter)).first(groupParameter)
					.as("xObj").push("$waferDefects.adder").as("yObj").addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId").as("waferIds");
		}
		if (edxflag == false) {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, FilterMinMax,
						excludeId);
			}
		} else {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
						groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
						groupOperation, projectFetchData, FilterMinMax, excludeId);
			}
		}
		logger.info("Preparing aggregationMean getGraphDetails" + aggregationMean);

		testResults = mongoTemplate.aggregate(aggregationMean, "InspectedWaferData", Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}

		if (aggregator.equals("Avg")) {
			DimensionX = "Test.testNo";
			String splitDimension = "lotRecord.waferRecord.summaryRecords";
			getkeyCount = getTestKeycount(waferIds, splitDimension, DimensionX);
			int count = 0;
			double i = 0;
			for (Document document : testResults) {
				for (Document document1 : getkeyCount) {
					List DimensionArray = (List) document.get("xAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					Object DimensionArray1 = document1.get("key");
					String Dimension1 = String.valueOf(DimensionArray1);
					if (Dimension.equals(Dimension1)) {
						count = ((int) document1.get("count"));
						break;
					}
				}
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i2 = Double.parseDouble(Dimension);
				double total = i2 / count;
				if (minVal == 0 && maxVal == 0.0) {
					document.put("yAxis", Arrays.asList(total));
				} else {
					if (total < maxVal && total > minVal) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();

					}
				}

			}
		}
		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				if (!document.isEmpty()) {
					List<ObjectId> wafers = (List) document.get("waferIds");
					List waferList = new ArrayList<>();
					for (int i = 0; i < wafers.size(); i++) {
						String hexString = wafers.get(i).toHexString();
						waferList.add(hexString);
					}
					document.put("waferIds", waferList);
					tempDoc.add(document);
					testResults = tempDoc;
				}
			}
		}
		return testResults;
	}

	private List<Criteria> generateQueryforEdxMatchlookup(List<String> elements) {
		Criteria criteria = new Criteria();
		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(Criteria.where("edxObj.elementsPresent.elements").in(elements));
		criteria.and("waferDefects.edx").exists(false);
		criteriaList.add(criteria);
		return criteriaList;
	}

	private Criteria generateQueryForEdxlookup(List<String> elements, boolean edxflag) {
		Criteria criteria = new Criteria();
		if (!elements.contains("emptyElements")) {
			criteria.and("waferDefects.edx").exists(true);
		}
		if (edxflag == false) {
			criteria.and("waferDefects.edx").exists(false);
		}
		elements.remove("null");
		return criteria;
	}

	private List<Criteria> generateQueryforEdxMatch(List<String> elements) {
		Criteria criteria = new Criteria();
		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(Criteria.where("edxObj.elementsPresent.elements").in(elements));
		criteria.and("edx").exists(false);
		criteriaList.add(criteria);
		return criteriaList;
	}

	private Criteria generateQueryForEdx(List<String> elements, boolean edxflag) {
		Criteria criteria = new Criteria();
		if (!elements.contains("emptyElements")) {
			criteria.and("edx").exists(true);
		}
		if (edxflag == false) {
			criteria.and("edx").exists(false);
		}
		elements.remove("null");
		return criteria;
	}

	private static boolean isNumeric(String string) {
		int intValue;
		System.out.println(String.format("Parsing string: \"%s\"", string));
		if (string == null || string.equals("")) {
			System.out.println("String cannot be parsed, it is null or empty.");
			return false;
		}

		try {
			intValue = Integer.parseInt(string);
			return true;
		} catch (NumberFormatException e) {
			System.out.println("Input String cannot be parsed to Integer.");
		}
		return false;
	}

	private List<Criteria> generateQueryForSelectionAreas(List<Area> areas) {

		List<Criteria> criteriaList = new ArrayList<Criteria>();

		for (Area area : areas) {
			Criteria criteria = Criteria.where("waferDefects.radius").lt(area.getRadius().getTo())
					.gt(area.getRadius().getFrom());
			if (area.getAngle() != null) {
				criteria = criteria.and("waferDefects.angle").lt(area.getAngle().getTo()).gt(area.getAngle().getFrom());
			}
			criteriaList.add(criteria);
		}
		return criteriaList;
	}

	private List<Document> getUniqueDimensionMeasure(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin,
			String testName, int testNumber, double minVal, double maxVal, String Aggregator, List<Area> areas)
			throws IOException {

		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation = null;
		List<Document> testResults = null;
		MatchOperation matchOperation = null;
		String collectionName = null;
		Criteria matchCriteria = null;
		String dimensionNameX, measureNameY = null;
		MatchOperation edxMatch = null, crfMatch = null;
		boolean edxflag = true;
		AggregationOperation FilterMinMax = null;
		List<Criteria> criteriaListForSelection = null;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;
		LookupOperation lookupOnEdx = null;
		UnwindOperation unwWindOperationonEdx = null;
		List<Document> getkeyCount = null;

		excludeId = Aggregation.project().andExclude("_id");
		if (measureName.equals("Area")) {
			throw new IOException("Selected Measure is not supported.");
		}
		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		if (minVal > 0 && maxVal == 0.0 && Aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && Aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (Aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		collectionName = "WaferDefects";
		matchCriteria = Criteria.where("waferId").in(waferIds);
		dimensionNameX = generateFields(dimensionName);
		measureNameY = generateFields(measureName);
		matchOperation = Aggregation.match(matchCriteria);

		criteriaForEdx = generateQueryForEdx(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
		} else {
			criteriaListForSelection = generateQueryForSelection(areas);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
		}

		if (edxflag == true) {
			criteriaForEdxMatch = generateQueryforEdxMatch(elements);
			edxMatch = Aggregation.match(
					new Criteria().orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
		}
		lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("edx").foreignField("_id").as("edxObj");
		unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

		groupOperation = Aggregation.group(dimensionNameX).first(dimensionNameX).as("xObj").sum(measureNameY).as("yObj")
				.addToSet("waferId").as("waferId");
		projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
				.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");

		if (measureName.equals("tDefect")) {
			groupOperation = Aggregation.group(dimensionNameX).first(dimensionNameX).as("xObj").count().as("yObj")
					.addToSet("waferId").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");
		} else if (measureName.equals("aDefect")) {
			groupOperation = Aggregation.group(dimensionNameX).first(dimensionNameX).as("xObj").push("$adder")
					.as("yObj").addToSet("waferId").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId").as("waferIds");
		}

		if (edxflag == false) {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation, projectFetchData,
						excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation, projectFetchData,
						FilterMinMax, excludeId);
			}
		} else {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
						unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
						unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, FilterMinMax, excludeId);
			}
		}

		logger.info("Aggregation Query for Summary in CATInXaxis: " + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}

		if (Aggregator.equalsIgnoreCase("Avg")) {
			getkeyCount = FilterkeyCount(waferIds, dimensionName, crfFilter, crfValue);
			int count = 0;
			double i = 0;
			for (Document document : testResults) {
				for (Document document1 : getkeyCount) {
					List DimensionArray = (List) document.get("xAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					Object DimensionArray1 = document1.get("key");
					String Dimension1 = String.valueOf(DimensionArray1);
					if (Dimension.equals(Dimension1)) {
						count = ((int) document1.get("count"));
						break;
					}
				}

				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i2 = Double.parseDouble(Dimension);
				double total = i2 / count;

				if (minVal == 0 && maxVal == 0.0) {
					document.put("yAxis", Arrays.asList(total));
				} else {
					if (total < maxVal && total > minVal) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();

					}
				}

			}
		}

		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				List<ObjectId> wafers = (List) document.get("waferIds");
				List waferList = new ArrayList<>();
				for (int i = 0; i < wafers.size(); i++) {
					String hexString = wafers.get(i).toHexString();
					waferList.add(hexString);
				}
				document.put("waferIds", waferList);
				tempDoc.add(document);
				testResults = tempDoc;
			}
		}
		return testResults;
	}

	private List<Document> getCommonDimensionsAndMeasure(List<ObjectId> waferIds, List<Integer> crfFilter,
			String crfValue, List<String> elements, String dimensionName, String measureName, String group,
			int selectedBin, String testName, int testNumber, double minVal, double maxVal, String Aggregator,
			List<Area> areas, List<Area> discardedAreas) throws IOException {

		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData, excludeId = null;
		ProjectionOperation projectionForZeroDensity = null, projectionForNullDensity = null,
				projectdiscardedarea = null;
		GroupOperation groupOperation, groupOperationDensity = null;
		List<Document> testResults = null;
		MatchOperation edxMatch = null, matchOperation = null;
		String collectionName = null;
		LookupOperation lookupOnEdx, lookupOnWaferDefects;
		UnwindOperation unwWindOperationonEdx, unwWindOperationonWaferdefects;
		MatchOperation crfMatch = null;
		AddFieldsOperation addFieldsOperation = null, addFieldsdiscardedarea = null;
		AggregationOperation FilterMinMax = null;
		List<Criteria> criteriaListForSelection = null;
		float discardedArea = 0;
		boolean edxflag = true;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;

		collectionName = "InspectedWaferData";
		matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
		excludeId = Aggregation.project().andExclude("_id");

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		String MeasureValY = generateFieldNameForCriteria(measureName);
		if (MeasureValY == null) {
			throw new IOException("Selected Measure is not supported.");
		}

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		if (minVal > 0 && maxVal == 0.0 && Aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && Aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (Aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);

		if (crfValue.equals("classNumber")) {
			crfValue = "waferDefects.classNumber";
		}
		if (crfValue.equals("roughBinNumber")) {
			crfValue = "waferDefects.roughBinNumber";
		}
		if (crfValue.equals("fineBinNumber")) {
			crfValue = "waferDefects.fineBinNumber";
		}
		criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
		} else {
			criteriaListForSelection = generateQueryForSelectionAreas(areas);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
		}

		if (edxflag == true) {
			criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
			edxMatch = Aggregation.match(
					new Criteria().orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
		}
		lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
				.as("edxObj");
		unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

		String[] parts = DimensionX.split("\\.");
		String key = null;
		for (String end : parts) {
			key = end;
		}

		if (discardedAreas != null) {
			for (Area area : discardedAreas) {
				float to = area.getRadius().getTo();
				float from = area.getRadius().getFrom();
				float angleto = area.getAngle().getTo();
				float anglefrom = area.getAngle().getFrom();
				float angle = (angleto) - (anglefrom);
				float sectorArea = (float) ((angle / 360) * (22 / 7) * ((to * to) - (from * from)));
				discardedArea = discardedArea + sectorArea;
			}
			discardedArea = discardedArea / 100;
		}

		if (measureName.equals("tDefect")) {
			groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").count().as("yObj")
					.addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");

		} else if (measureName.equals("aDefect")) {
			groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").push("$waferDefects.adder")
					.as("yObj").addToSet("_id").as("waferId");
			projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
					.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferId").as("waferIds");
		} else {
			addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea).build();
			groupOperation = Aggregation.group(Fields.fields().and("id", "$_id").and(key, DimensionX)).first(DimensionX)
					.as("xObj").first("$waferInspectionSummary.testArea").as("area").first("$lotRecord.sampleSize.x")
					.as("diameter").count().as("yObj").first("$discardedArea").as("discardedArea").first("_id")
					.as("waferId");

			projectionForZeroDensity = Aggregation.project()
					.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea", "waferId")
					.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$area").equalToValue(0))
							.then(new Document("$multiply",
									Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
							.otherwise("$area"))
					.as("area");

			projectionForNullDensity = Aggregation
					.project().andInclude("_id", "xObj", "yObj", "discardedArea", "waferId").and(
							ConditionalOperators.ifNull("$area")
									.then(new Document("$multiply",
											Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
					.as("area");
			projectdiscardedarea = Aggregation.project()
					.and(ConditionalOperators.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
							.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
							.otherwise("$area"))
					.as("area").andInclude("yObj", "waferId").andInclude("xObj");

			addFieldsOperation = Aggregation.addFields()
					.addFieldWithValue("defDensity",
							new Document("$round",
									Arrays.asList(new Document("$divide", Arrays.asList("$yObj", "$area")), 16L)))
					.build();

			groupOperationDensity = Aggregation.group("xObj").first("xObj").as("xObj").sum("defDensity")
					.as("defDensity").addToSet("waferId").as("waferId");

			projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$defDensity")
					.as("yAxis").and("$waferId").as("waferIds");

		}

		if (edxflag == false) {
			if (minVal == 0 && maxVal == 0.0) {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
							projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
							addFieldsOperation, groupOperationDensity, projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, excludeId);
				}
			} else {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroDensity,
							projectionForNullDensity, addFieldsOperation, groupOperationDensity, projectFetchData,
							excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, FilterMinMax,
							excludeId);
				}
			}

		} else {
			if (minVal == 0 && maxVal == 0.0) {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							addFieldsdiscardedarea, groupOperation, projectionForZeroDensity, projectionForNullDensity,
							projectdiscardedarea, addFieldsOperation, groupOperationDensity, projectFetchData,
							excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							groupOperation, projectFetchData, excludeId);
				}
			} else {
				if (measureName.equals("Density")) {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							groupOperation, projectionForZeroDensity, projectionForNullDensity, addFieldsOperation,
							groupOperationDensity, projectFetchData, excludeId);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
							unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
							groupOperation, projectFetchData, FilterMinMax, excludeId);
				}
			}

		}

		logger.info("Aggregation Query for Summary in WaferDefects: " + aggregationMean);

		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}

		List<Document> getkeyCount = getkeyCount(waferIds, dimensionName);

		if (Aggregator.equalsIgnoreCase("Avg")) {

			if (measureName.equals("Density")) {
				int wSize = waferIds.size();
				for (Document document : testResults) {
					List DimensionArray = (List) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i = Double.parseDouble(Dimension);
					double total = i / wSize;
					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal && total > minVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}
				}

			} else {
				int count = 0;
				for (Document document : testResults) {
					for (Document document1 : getkeyCount) {
						List<?> DimensionArray = (List<?>) document.get("xAxis");
						String Dimension = String.valueOf(DimensionArray.get(0));
						Object DimensionArray1 = document1.get("key");
						String Dimension1 = String.valueOf(DimensionArray1);
						if (Dimension.equals(Dimension1)) {
							count = ((int) document1.get("count"));
							break;
						}
					}
					List<?> DimensionArray = (List<?>) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i2 = Double.parseDouble(Dimension);
					double total = i2 / count;

					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal && total > minVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}
				}
			}

		}
		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				List<ObjectId> wafers = (List) document.get("waferIds");
				List waferList = new ArrayList<>();
				for (int i = 0; i < wafers.size(); i++) {
					String hexString = wafers.get(i).toHexString();
					waferList.add(hexString);
				}
				document.put("waferIds", waferList);
				tempDoc.add(document);
				testResults = tempDoc;
			}
		}

		return testResults;

	}

	private List<Document> getCommonDimensionsAndMeasure1(List<ObjectId> waferIds, List<Integer> crfFilter,
			String crfValue, List<String> elements, String dimensionName, String measureName, String group,
			int selectedBin, String testName, int testNumber, double minVal, double maxVal, String aggregator,
			List<Area> areas) throws IOException {
		
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation = null;
		List<Document> testResults = null;
		MatchOperation matchOperation = null;
		String collectionName = null;
		MatchOperation edxMatch = null, crfMatch = null;
		boolean edxflag = true;
		LookupOperation lookupOnEdx, lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonEdx, unwWindOperationonWaferdefects = null;
		List<Document> getkeyCount = null;
		AggregationOperation FilterMinMax = null;
		List<Criteria> criteriaListForSelection = null;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;

		collectionName = "InspectedWaferData";
		matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
		lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);
		
		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		if (crfValue.equals("classNumber")) {
			crfValue = "waferDefects.classNumber";
		}
		if (crfValue.equals("roughBinNumber")) {
			crfValue = "waferDefects.roughBinNumber";
		}
		if (crfValue.equals("fineBinNumber")) {
			crfValue = "waferDefects.fineBinNumber";
		}
		criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
		} else {
			criteriaListForSelection = generateQueryForSelectionAreas(areas);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
		}

		if (edxflag == true) {
			criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
			edxMatch = Aggregation.match(
					new Criteria().orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
		}
		lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
				.as("edxObj");
		unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}
		String MeasureValY = generateFieldNameForCriteria(measureName);
		if (MeasureValY == null) {
			throw new IOException("Selected Measure is not supported.");
		}

		groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").sum(MeasureValY).as("yObj")
							.addToSet("_id").as("waferId");

		projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
				.andArrayOf("$yObj").as("yAxis").and("$waferId").as("waferIds");
		excludeId = Aggregation.project().andExclude("_id");

		if (minVal > 0 && maxVal == 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
		} else if (minVal == 0.0 && maxVal > 0.0 && aggregator.equalsIgnoreCase("count")) {
			FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
		} else {
			if (aggregator.equalsIgnoreCase("count")) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else {
				FilterMinMax = Aggregation.match(new Criteria());
			}
		}

		if (edxflag == false) {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, groupOperation, projectFetchData, FilterMinMax,
						excludeId);
			}

		} else {
			if (minVal == 0 && maxVal == 0.0) {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
						groupOperation, projectFetchData, excludeId);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
						unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
						groupOperation, projectFetchData, FilterMinMax, excludeId);
			}
		}
		
		logger.info("Aggregation Query for Summary in Inspected WaferData: " + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();

		if (aggregator.equals("Avg")) {
			if (dimensionName.equals("Test")) {
				/*----------------*/
				DimensionX = "Test.testNo";
				String splitDimension = "lotRecord.waferRecord.summaryRecords";
				getkeyCount = getTestKeycount(waferIds, splitDimension, DimensionX);
				int count = 0;
				double i = 0;
				for (Document document : testResults) {
					for (Document document1 : getkeyCount) {
						List DimensionArray = (List) document.get("xAxis");
						String Dimension = String.valueOf(DimensionArray.get(0));
						Object DimensionArray1 = document1.get("key");
						String Dimension1 = String.valueOf(DimensionArray1);
						if (Dimension.equals(Dimension1)) {
							count = ((int) document1.get("count"));
							break;
						}
					}
					List DimensionArray = (List) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i2 = Double.parseDouble(Dimension);
					double total = i2 / count;
					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal && total > minVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}

				}

			} else {
				getkeyCount = getkeyCount(waferIds, dimensionName);
				int count = 0;
				double i = 0;
				for (Document document : testResults) {
					for (Document document1 : getkeyCount) {
						List DimensionArray = (List) document.get("xAxis");
						String Dimension = String.valueOf(DimensionArray.get(0));
						Object DimensionArray1 = document1.get("key");
						String Dimension1 = String.valueOf(DimensionArray1);
						if (Dimension.equals(Dimension1)) {
							count = ((int) document1.get("count"));
							break;
						}
					}

					List DimensionArray = (List) document.get("yAxis");
					String Dimension = String.valueOf(DimensionArray.get(0));
					double i2 = Double.parseDouble(Dimension);
					double total = i2 / count;
					if (minVal == 0 && maxVal == 0.0) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						if (total < maxVal) {
							document.put("yAxis", Arrays.asList(total));
						} else {
							document.clear();

						}
					}
				}
			}
		}

		List<Document> tempDoc = new ArrayList<>();
		for (Document document : testResults) {
			if (!document.isEmpty()) {
				List<ObjectId> wafers = (List) document.get("waferIds");
				List waferList = new ArrayList<>();
				for (int i = 0; i < wafers.size(); i++) {
					String hexString = wafers.get(i).toHexString();
					waferList.add(hexString);
				}
				document.put("waferIds", waferList);
				tempDoc.add(document);
				testResults = tempDoc;
			}
		}

		return testResults;
	}

	/*--keyCount--*/

	private List<Document> getTestKeycount(List<ObjectId> waferIds, String splitDimension, String DimensionX) {
		Aggregation aggregationMean = null;
		String collectionName = null;
		MatchOperation matchOperation = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation, groupOperationTest = null;
		UnwindOperation unwWindOperationTest = null;

		collectionName = "InspectedWaferData";
		matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
		groupOperationTest = Aggregation.group("_id").first(splitDimension).as("Test");
		unwWindOperationTest = Aggregation.unwind("$Test", false);
		groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("key").count().as("frequency");
		projectFetchData = Aggregation.project().and("$key").as("key").and("$frequency").as("count");
		excludeId = Aggregation.project().andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchOperation, groupOperationTest, unwWindOperationTest,
				groupOperation, projectFetchData, excludeId);
		logger.info("KeyCount" + aggregationMean);
		List<Document> results = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
				.getMappedResults();
		return results;
	}

	private List<Document> getkeyCount(List<ObjectId> waferIds, String dimensionName) throws IOException {
		Aggregation aggregationMean = null;
		String collectionName = null;
		MatchOperation matchOperation = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation = null;

		LookupOperation lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonWaferDefects = null;

		collectionName = "InspectedWaferData";
		matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
		excludeId = Aggregation.project().andExclude("_id");

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		unwWindOperationonWaferDefects = Aggregation.unwind("$waferDefects", true);
		groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("key").count().as("frequency");
		projectFetchData = Aggregation.project().and("$key").as("key").and("$frequency").as("count");

		if (dimensionName.equals("Test")) {
			aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
					unwWindOperationonWaferDefects, groupOperation, projectFetchData, excludeId);
		} else {
			aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFetchData, excludeId);
		}

		logger.info("KeyCount" + aggregationMean);
		List<Document> results = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
				.getMappedResults();
		return results;

	}

	private List<Document> getkeyCountTimeStamp(List<ObjectId> waferIds, String dimensionName, String granuality)
			throws IOException {
		Aggregation aggregationMean = null;
		String collectionName = null;
		MatchOperation matchOperation = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation = null;
		String dateFormat = "";
		AddFieldsOperation addFieldsOperation = null;

		collectionName = "InspectedWaferData";
		matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}
		if (granuality != null && granuality != "") {
			dateFormat = getDateFormat(granuality);
		}
		addFieldsOperation = Aggregation.addFields()
				.addFieldWithValue("$DateField", DateOperators.dateOf(DimensionX).toString(dateFormat)).build();

		groupOperation = Aggregation.group("DateField").first("DateField").as("key").count().as("frequency");
		projectFetchData = Aggregation.project().and("$key").as("key").and("$frequency").as("count");
		excludeId = Aggregation.project().andExclude("_id");
		aggregationMean = Aggregation.newAggregation(matchOperation, addFieldsOperation, groupOperation,
				projectFetchData, excludeId);
		logger.info("KeyCount" + aggregationMean);
		List<Document> results = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
				.getMappedResults();
		return results;
	}

	private List<Document> FilterkeyCount(List<ObjectId> waferIds, String dimensionName, List<Integer> crfFilter,
			String crfValue) throws IOException {
		Aggregation aggregationMean = null;
		String collectionName = null;
		MatchOperation matchOperation = null;
		ProjectionOperation projectFetchData = null, excludeId = null;
		GroupOperation groupOperation = null;
		GroupOperation groupOperation1 = null;
		MatchOperation crfMatch = null;
		LookupOperation lookupOninspectedWaferData = null;
		UnwindOperation unwWindOperationoninspectedWaferData = null;

		collectionName = "WaferDefects";
		matchOperation = Aggregation.match(Criteria.where("waferId").in(waferIds));

		String DimensionX = generateFields(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		groupOperation = Aggregation.group(DimensionX, "waferId").first(DimensionX).as("key").first("waferId")
				.as("waferId");
		lookupOninspectedWaferData = LookupOperation.newLookup().from("InspectedWaferData").localField("waferId")
				.foreignField("_id").as("inspectedWaferData");
		unwWindOperationoninspectedWaferData = Aggregation.unwind("$inspectedWaferData", false);
		groupOperation1 = Aggregation.group("key").first("key").as("key").count().as("frequency");
		projectFetchData = Aggregation.project().and("$key").as("key").and("$frequency").as("count");
		excludeId = Aggregation.project().andExclude("_id");
		crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter));
		aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
				lookupOninspectedWaferData, unwWindOperationoninspectedWaferData, groupOperation1, projectFetchData,
				excludeId);
		logger.info("FilterKeyCount" + aggregationMean);
		List<Document> results = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
				.getMappedResults();
		return results;
	}

	private String generateFieldNameForTest(String measureName) {
		switch (measureName) {
		case "Area":
			return "Test.areaPerTest";
		case "N-Die":
			return "Test.nDie";
		case "D-Die":
			return "Test.nDefDie";
		default:
			return null;
		}
	}

	private String generateFields(String dimensionName) {
		switch (dimensionName) {
		case "CAT":
			return "classNumber";
		case "RB":
			return "roughBinNumber";
		case "FB":
			return "fineBinNumber";
		case "Test":
			return "test";
		case "Sx":
			return "xSize";
		case "Sy":
			return "ySize";
		case "A-Size":
			return "defectArea";
		case "D-Size":
			return "dSize";
		default:
			return null;
		}
	}

	private String generateFieldNameForCriteria(String fieldName) {
		switch (fieldName) {
		case "LotId":
			return "lotRecord.lotId";
		case "aDefect":
			return "waferInspectionSummary.adderCount";
		case "Density":
			return "waferInspectionSummary.defectDensity";
		case "WaferId":
			return "lotRecord.waferRecord.waferNumber";
		case "Device":
			return "lotRecord.deviceId";
		case "tDefect":
			return "waferInspectionSummary.nDefect";
		case "SlotNumber":
			return "lotRecord.waferRecord.slotNumber";
		case "DeviceId":
			return "lotRecord.deviceId";
		case "Process":
			return "lotRecord.stepId";
		case "Equipment":
			return "lotRecord.inspectionStationId.equipmentID";
		case "Recipe":
			return "lotRecord.waferRecord.recipeId.recipeName";
		case "ResultTimeStamp":
			return "lotRecord.waferRecord.resultTimeStamp";
		case "CAT":
			return "waferDefects.classNumber";
		case "RB":
			return "waferDefects.roughBinNumber";
		case "FB":
			return "waferDefects.fineBinNumber";
		case "Area":
			return "waferInspectionSummary.testArea";
		case "N-Die":
			return "waferInspectionSummary.nDie";
		case "D-Die":
			return "waferInspectionSummary.nDefDie";
		case "Sx":
			return "waferDefects.xSize";
		case "Sy":
			return "waferDefects.ySize";
		case "A-Size":
			return "waferDefects.defectArea";
		case "D-Size":
			return "waferDefects.dSize";
		case "Test":
			return "waferDefects.test";
		case "lastUpdatedDate":
			return "lastUpdatedDate";
		case "accessTime":
			return "accessTime";
		default:
			return null;
		}
	}

	private String generateTestFieldNameForCriteria(String fieldName) {
		switch (fieldName) {
		case "tDefect":
			return "nDefect";
		case "Test":
			return "lotRecord.waferRecord.summaryRecords.testNo";
		case "Density":
			return "defDensity";
		case "aDefect":
			return "adderCount";
		default:
			return null;
		}
	}

	private String getDateFormat(String granuality) {
		switch (granuality) {
		case "Year":
			return "%Y";
		case "Month":
			return "%Y/%m";
		case "Day":
			return "%Y/%m/%d";
		case "Hour":
			return "%Y/%m/%d %H:00:00";
		case "Minute":
			return "%Y/%m/%d %H:%M:00";
		case "Second":
			return "%Y/%m/%d %H:%M:%S";
		default:
			return "%Y/%m/%d %H:%M:%S";
		}

	}

	/*
	 * List SavedGraph
	 */
	@Override
	public DataModel getSavedGraphs(GraphModelJSON search) throws IOException {

		List<SortParam> sortParams = search.getSort();
		List<FilterParam> filterParams = search.getFilter();
		String userId = search.getUserId();
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
		} else {
			pageable = PageRequest.of(0, 25);
		}
		if (filterParams != null && filterParams.size() >= 1) {
			criteriaList = (generateCriteriaForFileds(filterParams));
		}
		Criteria commonCriteria = null;

		commonCriteria = Criteria.where("userId").is(userId).and("deleteFlag").ne(true).and("category").is("Saved");
		AggregationOptions options = AggregationOptions.builder()
				.collation(Collation.of(currentLocale).numericOrdering(true)).build();
		for (Criteria criteria : criteriaList) {
			if (criteria.getKey().equals("graphName")) {
				options = AggregationOptions.builder().collation(Collation.of(currentLocale).numericOrdering(false))
						.build();
				break;
			}
		}

		MatchOperation matchApplication = Aggregation.match(commonCriteria);
		MatchOperation matchFilter = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		List<FacetOperation> facets = new ArrayList<>();
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			facets.add(Aggregation.facet(matchApplication, matchFilter).as("output"));
		} else {
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
				} else {
					sort = sort.descending();
				}
				FacetOperation unwindSortFacet = Aggregation
						.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
				facets.add(unwindSortFacet);
			}
		} else {
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
						.as("lastUpdatedDate").and("$" + output + "graphType").as("graphType")
						.and("$" + output + "graphName").as("graphName").and("$" + output + "accessType")
						.as("accessType").and("$" + output + "xAxis.axisParameter").as("xAxis.axisParameter")
						.and("$" + output + "yAxis.axisParameter").as("yAxis.axisParameter")
						.and("$" + output + "zAxis.axisParameter").as("zAxis.axisParameter")
						.and("$" + output + "grouping").as("grouping")
						.and("$"+ output + "userId").as("userId"))
				.as("output");

		facets.add(unWindAndProjectFieldsOperation);

		Aggregation aggregation = Aggregation.newAggregation(facets).withOptions(options);
		Aggregation countAggregation = null;
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			countAggregation = Aggregation.newAggregation(matchApplication, matchFilter,
					Aggregation.count().as("count"));
		} else {
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
			result = sortAxisParameters(result, sortParams);
		}
		DataModel response = new DataModel();
		response.setData(result);
		if (!count.isEmpty()) {
			response.setCount((int) count.get(0).get("count"));
		}
		return response;
	}

	private List<Criteria> generateCriteriaForFileds(List<FilterParam> filterParams) {

		List<Criteria> criteriaList = new ArrayList<Criteria>();
		String fieldName;
		String operator;
		String fromDate;
		String toDate;
		String includeDateValue;

		for (FilterParam searchFilter : filterParams) {
			List<?> excludeValues = searchFilter.getExcludeValues();
			Criteria criteria = null;
			fieldName = searchFilter.getField().trim();
			String fieldNameCheck = null;
			if (fieldName.endsWith("EndTime") || fieldName.endsWith("StartTime")) {
				fieldNameCheck = "WaferTime";
			} else {
				fieldNameCheck = fieldName;
			}
			switch (fieldNameCheck) {
			case "lastUpdatedDate":
				fromDate = searchFilter.getValueFirst();
				toDate = searchFilter.getValueSecond();
				operator = searchFilter.getOperator();
				includeDateValue = searchFilter.getIncludeValue();
				criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						toDate, excludeValues, includeDateValue);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "accessTime":
				fromDate = searchFilter.getValueFirst();
				toDate = searchFilter.getValueSecond();
				operator = searchFilter.getOperator();
				includeDateValue = searchFilter.getIncludeValue();
				criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						toDate, excludeValues, includeDateValue);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;

			default:
				criteria = FilterUtil.generateCriteriaForString(null, searchFilter);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			}
		}
		return criteriaList;
	}

	private Criteria generateCriteriaForDateGrid(String fieldName, String operator, String fromDate, String toDate,
			List<?> excludeValues, String includeDateValue) {
		Date from = null;
		Date to = null;
		Criteria criteria = null;

		try {
			logger.info("Generating Criteria for Date Fields");
			if (operator.equals("between")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate);
				// criteria = new
				// Criteria().andOperator(Criteria.where(fieldName).gt(from).lt(to));
				criteria = Criteria.where(fieldName).gt(from).lt(to);
			}

			else if (operator.equals(">")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				criteria = Criteria.where(fieldName).gt(from);
			} else if (operator.equals(">=")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				criteria = Criteria.where(fieldName).gte(from);
			} else if (operator.equals("<")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				criteria = Criteria.where(fieldName).lt(from);
			} else if (operator.equals("<=")) {
				// from = DateUtil.convertStringToDateTimeStamp(fromDate.substring(0, 19));
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				criteria = Criteria.where(fieldName).lte(from);
			} else if (operator.equals("=")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(fromDate);
				criteria = Criteria.where(fieldName).gte(from).lte(to);
			} else if (operator.equals("A<X<B")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate);
				criteria = Criteria.where(fieldName).gt(from).lt(to);
			} else if (operator.equals("A<=X<B")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate);
				criteria = Criteria.where(fieldName).gte(from).lt(to);
			} else if (operator.equals("A<X<=B")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate.substring(0, 19) + ".999Z");
				criteria = Criteria.where(fieldName).gt(from).lte(to);
			} else if (operator.equals("A<=X<=B")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate.substring(0, 19) + ".999Z");
				criteria = Criteria.where(fieldName).gte(from).lte(to);
			} else if (operator.equals("A>X>B")) {
				from = DateUtil.convertStringToDateTimeStamp(fromDate);
				to = DateUtil.convertStringToDateTimeStamp(toDate);
				criteria = Criteria.where(fieldName).lt(from).gt(to);
			} else {
				from = DateUtil.getCurrentDateInUTC();
				criteria = Criteria.where(fieldName).is(from);
			}
			return criteria;

		} catch (Exception e) {
			logger.error("Exception occured:" + e.getMessage());
		}
		return criteria;
	}

	/*
	 * Method for sorting axis parameters.
	 */
	private List<Document> sortAxisParameters(List<Document> results, List<SortParam> sortParams) {

		String fieldName = null;
		for (SortParam sortParam : sortParams) {
			fieldName = sortParam.getField();
			switch (fieldName) {
			case "xAxis.axisParameter":
				results = ValidationUtil.sortAxisParameter(results, "xAxis", sortParam.isAsc());
				break;
			case "yAxis.axisParameter":
				results = ValidationUtil.sortAxisParameter(results, "yAxis", sortParam.isAsc());
				break;
			case "zAxis.axisParameter":
				results = ValidationUtil.sortAxisParameter(results, "zAxis", sortParam.isAsc());
				break;
			case "grouping.axisParameter":
				results = ValidationUtil.sortAxisParameter(results, "grouping", sortParam.isAsc());
				break;
			default:
				break;
			}
		}
		return results;
	}

	/*
	 * List RecentGraph
	 */

	@Override
	public DataModel getRecentGraphs(GraphModelJSON search) throws IOException {
		List<SortParam> sortParams = search.getSort();
		List<FilterParam> filterParams = search.getFilter();
		LockParam lock = search.getLock();
		List<Criteria> criteriaList = new ArrayList<>();
		Pageable pageable = null;
		int pageSize = 0;
		int pageNo = 0;
		String userId = search.getUserId();
		if ((search.getPageSize() != null || search.getPageNo() != null) && search.getPageSize() != ""
				&& search.getPageNo() != "") {
			pageSize = Integer.parseInt(search.getPageSize().trim());
			pageNo = Integer.parseInt(search.getPageNo().trim());
		}
		if (pageSize >= 1) {
			pageable = PageRequest.of(pageNo, pageSize);
		} else {
			pageable = PageRequest.of(0, 25);
		}
		if (filterParams != null && filterParams.size() >= 1) {
			criteriaList = (generateCriteriaForFileds(filterParams));
		}
		Criteria commonCriteria = null;
		commonCriteria = Criteria.where("userId").is(userId).and("deleteFlag").ne(true);
		AggregationOptions options = AggregationOptions.builder()
				.collation(Collation.of(currentLocale).numericOrdering(true)).build();
		MatchOperation matchApplication = Aggregation.match(commonCriteria);
		MatchOperation matchFilter = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		List<FacetOperation> facets = new ArrayList<>();
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			facets.add(Aggregation.facet(matchApplication, matchFilter).as("output"));
		} else {
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
				} else {
					sort = sort.descending();
				}
				FacetOperation unwindSortFacet = Aggregation
						.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
				facets.add(unwindSortFacet);
			}
		} else {
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
						.and("$" + output + "grouping").as("grouping").and("$" + output + "userId").as("userId"))
				.as("output");

		facets.add(unWindAndProjectFieldsOperation);

		Aggregation aggregation = Aggregation.newAggregation(facets).withOptions(options);
		Aggregation countAggregation = null;
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			countAggregation = Aggregation.newAggregation(matchApplication, matchFilter,
					Aggregation.count().as("count"));
		} else {
			countAggregation = Aggregation.newAggregation(matchApplication, Aggregation.count().as("count"));
		}
		List<Document> result = mongoTemplate.aggregate(aggregation, "RecentGraphs", Document.class).getMappedResults();
		List<Document> count = mongoTemplate.aggregate(countAggregation, "RecentGraphs", Document.class)
				.getMappedResults();
		result = ValidationUtil.documentIdConverter((List<Document>) result.get(0).get("output"));
		if (sortParams.size() > 0 && sortParams != null) {
			result = sortAxisParameters(result, sortParams);
		}

		DataModel response = new DataModel();
		response.setData(result);
		if (!count.isEmpty()) {
			response.setCount((int) count.get(0).get("count"));
		}
		return response;
	}

	/*
	 * View RegularGraph
	 */
	@Override
	public DataListModel specificRegularGraphs(ArrayList<String> regularGraphIds) {
		MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
				.and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
				.and("$graphName").as("graphName").and("$category").as("category").and("$graphType").as("graphType")
				.and("$accessType").as("accessType").and("$grouping").as("grouping").and("$xAxis").as("xAxis")
				.and("$yAxis").as("yAxis").and("$zAxis").as("zAxis");
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
		} else {
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
		recentGraph.setGraphId(graphRequest.getGraphId());
		recentGraph.setAccessType(graphRequest.getAccessType());
		recentGraph.setApplicationName(graphRequest.getApplicationName());
		recentGraph.setGraphName(graphRequest.getGraphName());
		recentGraph.setAccessTime(DateUtil.getCurrentDateInUTC());
		recentGraph.setCategory(category);
		recentGraph.setGraphType(graphRequest.getGraphType());
		recentGraph.setUserId(graphRequest.getUserId());
		if (graphRequest.getGrouping().size() > 0) {
			recentGraph.setGrouping(graphRequest.getGrouping().get(0).getAxisParameter());
		} else {
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
	 * View RecentGraph
	 */

	@Override
	public DataListModel specificRecentGraphs(ArrayList<String> regularGraphIds) {
		MatchOperation matchGraphId = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
		ProjectionOperation projectGraphId = Aggregation.project().and("$graphId").as("graphId").and("$filter")
				.as("filter");
		Aggregation aggregationId = Aggregation.newAggregation(matchGraphId, projectGraphId);
		List<Document> result = mongoTemplate.aggregate(aggregationId, "RecentGraphs", Document.class)
				.getMappedResults();
		MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("_id").in(result.get(0).get("graphId")));
		MatchOperation deleteFlag = Aggregation.match(Criteria.where("deleteFlag").is(false));
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
				.and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
				.and("$graphName").as("graphName").and("$graphType").as("graphType").and("$category").as("category")
				.and("$accessType").as("accessType").and("$grouping").as("grouping").and("$xAxis").as("xAxis")
				.and("$yAxis").as("yAxis").and("$zAxis").as("zAxis");
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
			projectGraphId = Aggregation.project().and("$waferIds").as("waferIds").and("$filter").as("filter")
					.and("$waferArea").as("waferArea");
			aggregationId = Aggregation.newAggregation(matchId, projectGraphId);
			List<WaferIdModel> waferIdList = mongoTemplate
					.aggregate(aggregationId, "SavedGraphWaferIds", WaferIdModel.class).getMappedResults();
			List<String> waferList = new ArrayList<String>();
			if (waferIdList.size() > 0) {
				for (WaferIdModel waferModel : waferIdList) {
					graphRequest.setFilter(waferModel.getFilter());
					graphRequest.setWaferArea(waferModel.getWaferArea());
				}
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
		} else {
			testListModel.setDataList(results);
			message.setSuccess(false);
			message.setCode(204);
			testListModel.setStatus(message);
			return testListModel;
		}
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
		} else {
			return null;
		}
	}

	/*
	 * View SavedGraph
	 */
	@Override
	public DataListModel specificSavedGraphs(ArrayList<String> regularGraphIds) {
		MatchOperation matchRegularGraphId = Aggregation.match(Criteria.where("_id").in(regularGraphIds));
		ProjectionOperation projectFieldSelectionOperation = Aggregation.project().and("_id").as("_id")
				.and("$applicationName").as("applicationName").and("$lastUpdatedDate").as("lastUpdatedDate")
				.and("$graphName").as("graphName").and("$graphType").as("graphType").and("$legendList").as("legendList")
				.and("$category").as("category").and("$accessType").as("accessType").and("$grouping").as("grouping")
				.and("$xAxis").as("xAxis").and("$yAxis").as("yAxis").and("$zAxis").as("zAxis");
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
			ProjectionOperation projectGraphId = Aggregation.project().and("$waferIds").as("waferIds").and("$filter")
					.as("filter").and("$waferArea").as("waferArea");
			Aggregation aggregationId = Aggregation.newAggregation(matchId, projectGraphId);
			List<WaferIdModel> waferIdList = mongoTemplate
					.aggregate(aggregationId, "SavedGraphWaferIds", WaferIdModel.class).getMappedResults();
			List<String> waferList = new ArrayList<String>();
			if (waferIdList.size() > 0) {
				for (WaferIdModel waferModel : waferIdList) {
					graphRequest.setFilter(waferModel.getFilter());
					graphRequest.setWaferArea(waferModel.getWaferArea());
				}
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
		} else {
			testListModel.setDataList(results);
			message.setSuccess(false);
			message.setCode(204);
			testListModel.setStatus(message);
			return testListModel;
		}
	}

	@Override
	public Message renameGraph(String id, String graphName) {
		RecentGraphSaveModel recentGraphModels = null;
		Message message = new Message();
		List<Document> recentGraphId = checkRecentGraphExists(id, "RecentGraphs");
		if (recentGraphId.size() > 0) {
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
			} else {
				message.setCode(302);
				message.setSuccess(false);
			}
		} else {
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
	 * checkRecentGraphExists
	 */
	private List<Document> checkRecentGraphExists(String id, String category) {
		MatchOperation matchGraphId = null;
		if (category.equalsIgnoreCase("GraphTemplates")) {
			ObjectId obj = new ObjectId(id);
			matchGraphId = Aggregation.match(Criteria.where("graphId").in(obj));
		} else {
			matchGraphId = Aggregation.match(Criteria.where("_id").in(id));
		}
		ProjectionOperation projectGraphId = Aggregation.project().and("$graphId").as("graphId");
		Aggregation aggregationId = Aggregation.newAggregation(matchGraphId, projectGraphId);
		List<Document> recentGraphId = mongoTemplate.aggregate(aggregationId, "RecentGraphs", Document.class)
				.getMappedResults();
		return recentGraphId;
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
		} else {
			message.setCode(302);
			message.setSuccess(false);
		}
	}

	/*
	 * deleteGraph
	 */
	@Override
	public GraphMessage deleteGraph(String id) {
		RegularGraphModel regularGraphModels = null;
		GraphMessage message = new GraphMessage();
		List<Document> recentGraphId = checkRecentGraphExists(id, "RecentGraphs");
		if (recentGraphId.size() > 0) {
			RecentGraphSaveModel recentGraphModel = null;
			Optional<RecentGraphSaveModel> recentOptional = recentGraphRepository.findById(id);
			recentGraphModel = recentOptional.get();
			recentGraphModel.setAccessTime(DateUtil.getCurrentDateInUTC());
			recentGraphModel.setDeleteFlag(true);
			recentGraphRepository.save((RecentGraphSaveModel) recentGraphModel);
			message.setStatus("Recent Graph deleted Successfully");
		} else {
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
		boolean flag = false;
		DataListModel datalist = new DataListModel();
		Message message = new Message();
		RegularGraphModel regularGraphModel = null;
		List<String> regularGraphModelDetails = new ArrayList<String>();

		RegularGraphModel regularGraphDetails = null;

		if (regularGraphInfo.get_id() != null) {
			Optional<RegularGraphModel> optionalObj = regularGraphRepository
					.findById(regularGraphInfo.get_id().toString());
			regularGraphModel = optionalObj.get();

			if ((regularGraphInfo.get_id() != null)
					&& (regularGraphModel.getCategory()==null || !regularGraphModel.getCategory().equals(regularGraphInfo.getCategory()))) {
				flag = true;
			}
		}

		try {
			if ((regularGraphInfo.get_id() == null) || flag == true) {
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
				} else {
					message.setCode(302);
					message.setSuccess(false);
				}
			} else if (regularGraphInfo.get_id() != null) {

				if (regularGraphModel.getGraphName().equals(regularGraphInfo.getGraphName())
						&& regularGraphModel.getAccessType().equals(regularGraphInfo.getAccessType())
						&& regularGraphModel.getCategory().equals(regularGraphInfo.getCategory())) {
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
				} else {
					regularGraphDetails = regularGraphRepository.findByGraphDetails(regularGraphInfo.getGraphName(),
							regularGraphInfo.getAccessType(), regularGraphInfo.getCategory());

					if (regularGraphDetails == null) {
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
					} else {
						message.setCode(302);
						message.setSuccess(false);
					}

				}

			}

		} catch (Exception e) {

			e.printStackTrace();

		}
		datalist.setStatus(message);
		return datalist;
	}

	/*
	 * Binding Payload Save Data to Database collection
	 */
	private void setParamToModel(GraphRequest regularGraphInfo, RegularGraphModel regularGraphModels) {
		regularGraphModels.setCreatedBy("User1");
		regularGraphModels.setDeleteFlag(false);
		regularGraphModels.setUpdatedBy("User1");
		regularGraphModels.setUserId(regularGraphInfo.getUserId());
		regularGraphModels.setDeleteFlag(regularGraphInfo.isDeleteFlag());
		regularGraphModels.setAccessType(regularGraphInfo.getAccessType());
		regularGraphModels.setCategory(regularGraphInfo.getCategory());
		regularGraphModels.setGraphType(regularGraphInfo.getGraphType());
		regularGraphModels.setGraphName(regularGraphInfo.getGraphName());
		regularGraphModels.setxAxis(regularGraphInfo.getxAxis());
		regularGraphModels.setyAxis(regularGraphInfo.getyAxis());
		regularGraphModels.setzAxis(regularGraphInfo.getzAxis());
		regularGraphModels.setGrouping(regularGraphInfo.getGrouping());
		regularGraphModels.setUserId(regularGraphInfo.getUserId());
	}

	/*
	 * setUpdatedWaferIds
	 */
	private void setUpdatedWaferIds(GraphRequest regularGraphInfo) {
		
		List<WaferIdModel> waferIdModels = null;
		ObjectId objectId = new ObjectId(regularGraphInfo.get_id());
		MatchOperation id = Aggregation.match(Criteria.where("graphId").in(objectId));
		ProjectionOperation projectGraph = Aggregation.project().and("$_id").as("id").and("$waferIds").as("waferIds");
		Aggregation aggregation = Aggregation.newAggregation(id, projectGraph);
		waferIdModels = mongoTemplate.aggregate(aggregation, "SavedGraphWaferIds", WaferIdModel.class)
				.getMappedResults();
		if (waferIdModels.size() > 0) {
			Optional<WaferIdModel> optionalObj = waferIdRepository.findById(waferIdModels.get(0).get_id().toString());
			WaferIdModel waferIdModel = optionalObj.get();
			waferIdModel.setWaferIds(regularGraphInfo.getWaferIds());
			waferIdModel.setFilter(regularGraphInfo.getFilter());
			waferIdModel.setWaferArea(regularGraphInfo.getWaferArea());
			waferIdModel.setElements(regularGraphInfo.getElements());
			waferIdRepository.save((WaferIdModel) waferIdModel);
		}
	}

	/*
	 * setSaveWaferIds
	 */
	private void setSaveWaferIds(GraphRequest regularGraphInfo, RegularGraphModel regularGraphModels) {
		WaferIdModel waferIdModel = new WaferIdModel();
		ObjectId _id = new ObjectId(regularGraphModels.get_id());
		waferIdModel.setGraphId(_id);
		waferIdModel.setWaferIds(regularGraphInfo.getWaferIds());
		waferIdModel.setFilter(regularGraphInfo.getFilter());
		waferIdModel.setWaferArea(regularGraphInfo.getWaferArea());
		waferIdModel.setElements(regularGraphInfo.getElements());
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
		}
	}

	@Override
	public DataModel getRegularGraphs(GraphModelJSON search) throws IOException {

		List<SortParam> sortParams = search.getSort();
		List<FilterParam> filterParams = search.getFilter();
		String userId = search.getUserId();
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
		} else {
			pageable = PageRequest.of(0, 25);
		}
		if (filterParams != null && filterParams.size() >= 1) {
			criteriaList = (generateCriteriaForFileds(filterParams));
		}
		Criteria commonCriteria = null;

		commonCriteria = Criteria.where("userId").is(userId).and("deleteFlag").ne(true).and("category").is("Regular");
		AggregationOptions options = AggregationOptions.builder()
				.collation(Collation.of(currentLocale).numericOrdering(true)).build();
		for (Criteria criteria : criteriaList) {
			if (criteria.getKey().equals("graphName")) {
				options = AggregationOptions.builder().collation(Collation.of(currentLocale).numericOrdering(false))
						.build();
				break;
			}
		}
		MatchOperation matchApplication = Aggregation.match(commonCriteria);
		MatchOperation matchFilter = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		List<FacetOperation> facets = new ArrayList<>();
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			facets.add(Aggregation.facet(matchApplication, matchFilter).as("output"));
		} else {
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
				} else {
					sort = sort.descending();
				}
				FacetOperation unwindSortFacet = Aggregation
						.facet(Aggregation.unwind("$output", false), Aggregation.sort(sort)).as("output");
				facets.add(unwindSortFacet);
			}
		} else {
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
						.and("$" + output + "category").as("category").and("$" + output + "grouping").as("grouping")
						.and("$" + output + "userId").as("userId"))
				.as("output");

		facets.add(unWindAndProjectFieldsOperation);
		Aggregation aggregation = Aggregation.newAggregation(facets).withOptions(options);
		Aggregation countAggregation = null;
		if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
			countAggregation = Aggregation.newAggregation(matchApplication, matchFilter,
					Aggregation.count().as("count"));
		} else {
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
			result = sortAxisParameters(result, sortParams);
		}

		DataModel response = new DataModel();
		response.setData(result);
		if (!count.isEmpty()) {
			response.setCount((int) count.get(0).get("count"));
		}
		return response;
	}

	@Override
	public List<Document> getDefects(GraphDefectFilterCriteria criteria) throws IOException {
		String collectionName = "WaferDefects";
		List<Document> defectData = null;
		List<Integer> cat = null;
		if (criteria.cat != null && criteria.cat.size() > 0) {
			cat = criteria.cat;
		}
		List<Integer> rbn = null;
		if (criteria.rbn != null && criteria.rbn.size() > 0) {
			rbn = criteria.rbn;
		}
		List<Integer> fbn = null;
		if (criteria.fbn != null && criteria.fbn.size() > 0) {
			fbn = criteria.fbn;
		}
		MatchOperation matchOperation = Aggregation.match(Criteria.where("waferId").in(criteria.waferIds));
		LookupOperation lookupOnInspectedWaferData = LookupOperation.newLookup().from("InspectedWaferData")
				.localField("waferId").foreignField("_id").as("inspectedWaferData");
		UnwindOperation unwWindOperationonInspectedWaferData = Aggregation.unwind("$inspectedWaferData", true);
		ProjectionOperation projectionOperation = Aggregation.project().and("$waferId").as("id")
				.and("$inspectedWaferData.lotRecord.waferRecord.waferNumber").as("waferId")
				.and("$inspectedWaferData.lotRecord.deviceId").as("deviceId").and("$inspectedWaferData.lotRecord.lotId")
				.as("lotId").and("$inspectedWaferData.lotRecord.stepId").as("process")
				.and("$inspectedWaferData.lotRecord.waferRecord.slotNumber").as("slotNo").andExclude("_id");
		Aggregation aggregation = null;
		MatchOperation catMatch = Aggregation.match(Criteria.where("classNumber").in(cat));
		MatchOperation rbnMatch = Aggregation.match(Criteria.where("roughBinNumber").in(rbn));
		MatchOperation fbnMatch = Aggregation.match(Criteria.where("fineBinNumber").in(fbn));
		if ((criteria.cat != null && criteria.cat.size() > 0) || (criteria.rbn != null && criteria.rbn.size() > 0)
				|| (criteria.fbn != null && criteria.fbn.size() > 0)) {
			if (cat != null && cat.size() > 0) {
				aggregation = Aggregation.newAggregation(matchOperation, catMatch, lookupOnInspectedWaferData,
						unwWindOperationonInspectedWaferData, projectionOperation);
			}
			if (rbn != null && rbn.size() > 0) {
				aggregation = Aggregation.newAggregation(matchOperation, rbnMatch, lookupOnInspectedWaferData,
						unwWindOperationonInspectedWaferData, projectionOperation);
			}
			if (fbn != null && fbn.size() > 0) {
				aggregation = Aggregation.newAggregation(matchOperation, fbnMatch, lookupOnInspectedWaferData,
						unwWindOperationonInspectedWaferData, projectionOperation);
			}
		} else {
			aggregation = Aggregation.newAggregation(matchOperation, lookupOnInspectedWaferData,
					unwWindOperationonInspectedWaferData, projectionOperation);
		}
		logger.info("Aggregation Query" + aggregation);
		defectData = mongoTemplate.aggregate(aggregation, collectionName, Document.class).getMappedResults();
		if (defectData != null) {
			for (Document obj : defectData) {
				ObjectId objId = (ObjectId) obj.get("id");
				obj.put("id", objId.toHexString());
			}
		}
		return defectData;
	}

	@Override
	public SidePanelResponseParam getSidePanelData(String[] waferID) {
		logger.info("Inside getSidePanelData ");
		SidePanelResponseParam response = new SidePanelResponseParam();
		try {
			ObjectId[] waferIds = new ObjectId[waferID.length];
			for (int i = 0; i < waferID.length; i++) {
				waferIds[i] = new ObjectId(waferID[i]);
			}
			MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
			ProjectionOperation projectFetchData = Aggregation.project().and("lotRecord.deviceId").as("deviceId")
					.and("lotRecord.lotId").as("lotId").and("lotRecord.waferRecord.slotNumber").as("slotNo")
					.and("lotRecord.stepId").as("process").and("lotRecord.waferRecord.waferNumber").as("waferId")
					.and("_id").as("id");
			ProjectionOperation excludeId = Aggregation.project().andExclude("_id");
			Aggregation aggregationMean = Aggregation.newAggregation(matchOperation, projectFetchData, excludeId);
			logger.info("Preparing aggregationMean getSidePanelData" + aggregationMean);
			List<Document> testResults = mongoTemplate.aggregate(aggregationMean, "InspectedWaferData", Document.class)
					.getMappedResults();
			logger.info("Fetching Data from Db done getSidePanelData ");
			if (testResults != null && testResults.size() > 0) {
				for (Document doc : testResults) {
					if (doc.containsKey("id")) {
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
			logger.info("Exception" + e.getMessage() + "in getSidePanelData");
			response.setData(new ArrayList<>());
			return response;
		}
	}

	@Override
	public GridDataResponse getMultipleWaferDefects(WaferDefectFilterCriteria request) throws IOException {
		List<Area> area = request.getAreas();
		SelectionType selection = request.getSelectionType();
		List<Criteria> criteriaListForSelection = null;

		if (SelectionType.NONE.value == selection.value) {
			return null;
		} else if (SelectionType.PARTIAL.value == selection.value) {
			criteriaListForSelection = generateQueryForSelection(area);
		}

		String collectionName2 = "WaferDefects";
		List<Document> defectResults = null;
		List<Document> waferDefects = new ArrayList<Document>();

		List<ObjectId> waferIds = request.getWaferIds();

		List<SortParamVdi> sortParams = request.getSort();

		LockParamVdi lock = request.getLock();
		List<FilterParamVdi> filterParams = request.getFilter();
		List<Criteria> criteriaList = new ArrayList<>();

		Sort sortCombinedQuery = FilterUtil.generateSortQueryDynamically(sortParams, lock, columnMappingDefectList);

		if (sortCombinedQuery == null) {
			sortCombinedQuery = Sort.by("waferId").ascending();
		}
		sortCombinedQuery = sortCombinedQuery.and(Sort.by("_id").ascending());

		if (filterParams != null && filterParams.size() >= 1) {
			criteriaList = generateCriteriaForMultipleWaferDefects(filterParams);
		}
		List<Integer> cat = null;
		if (request.getCat() != null && request.getCat().size() > 0) {
			cat = request.getCat();
		}
		List<Integer> rbn = null;
		if (request.getRbn() != null && request.getRbn().size() > 0) {
			rbn = request.getRbn();
		}
		List<Integer> fbn = null;
		if (request.getFbn() != null && request.getFbn().size() > 0) {
			fbn = request.getFbn();
		}
		List<Double> xSize = null;
		if (request.getxSize() != null && request.getxSize().size() > 0) {
			xSize = request.getxSize();
		}
		List<Double> ySize = null;
		if (request.getySize() != null && request.getySize().size() > 0) {
			ySize = request.getySize();
		}
		List<Double> dSize = null;
		if (request.getdSize() != null && request.getdSize().size() > 0) {
			dSize = request.getdSize();
		}
		List<Double> dArea = null;
		if (request.getdArea() != null && request.getdArea().size() > 0) {
			dArea = request.getdArea();
		}
		Boolean adder = null;
		if (request.getAdder() != null) {
			adder = request.getAdder();
		}
		if (waferIds == null || waferIds.size() == 0) {
			return null;
		}

		Criteria matchCriteria = Criteria.where("waferId").in(waferIds);
		if (adder != null)
			if (adder == true) {
				matchCriteria = matchCriteria.andOperator(Criteria.where("adder").is(adder));
			} else {
				adder = true;
				matchCriteria = matchCriteria.norOperator(Criteria.where("adder").is(adder));
			}
		if (criteriaListForSelection != null)
			matchCriteria = matchCriteria
					.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()]));

		MatchOperation matchforWaferDefects2 = Aggregation.match(matchCriteria);
		MatchOperation catMatch = Aggregation.match(Criteria.where("classNumber").in(cat));
		MatchOperation rbnMatch = Aggregation.match(Criteria.where("roughBinNumber").in(rbn));
		MatchOperation fbnMatch = Aggregation.match(Criteria.where("fineBinNumber").in(fbn));
		MatchOperation xSizeMatch = Aggregation.match(Criteria.where("xSize").in(xSize));
		MatchOperation ySizeMatch = Aggregation.match(Criteria.where("ySize").in(ySize));
		MatchOperation dSizeMatch = Aggregation.match(Criteria.where("dSize").in(dSize));
		MatchOperation dAreaMatch = Aggregation.match(Criteria.where("defectArea").in(dArea));
		MatchOperation matchFilter = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));

		ProjectionOperation projectForWaferDefects = Aggregation.project().and("$waferId").as("waferId").and("$lotId")
				.as("lotId").and("$waferNumber").as("waferNumber").and("$stepId").as("process").and("$defectId")
				.as("defect").and("$test").as("test").and("$classNumber").as("cat").and("$roughBinNumber").as("rbn")
				.and("$fineBinNumber").as("fbn").and("$xSize").as("xSize").and("$ySize").as("ySize").and("$dSize")
				.as("dSize").and("$clusterNumber").as("cn").and("$xRel").as("xRel").and("$yRel").as("yRel")
				.and("$xIndex").as("xIndex").and("$yIndex").as("yIndex").and("$imageCount").as("imgCount")
				.and("$defectArea").as("dArea").and("$adder").as("adder");

		Aggregation aggregationResult2 = null;
		Aggregation aggregationCount = null;

		CountOperation countOp = Aggregation.count().as("count");

		if (cat != null && cat.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, catMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, catMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, catMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, catMatch, countOp);
			}
		} else if (rbn != null && rbn.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, rbnMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, rbnMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, rbnMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, rbnMatch, countOp);
			}
		} else if (fbn != null && fbn.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, fbnMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, fbnMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, fbnMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, fbnMatch, countOp);
			}
		} else if (xSize != null && xSize.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, xSizeMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, xSizeMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, xSizeMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, xSizeMatch, countOp);
			}
		} else if (ySize != null && ySize.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, ySizeMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, ySizeMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, ySizeMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, ySizeMatch, countOp);
			}
		} else if (dSize != null && dSize.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, dSizeMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, dSizeMatch, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, dSizeMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, dSizeMatch, countOp);
			}
		} else if (dArea != null && dArea.size() >= 0) {
			if (filterParams != null && filterParams.size() > 0 && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, dAreaMatch, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, dAreaMatch, matchFilter, countOp);
			} else {

				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, dAreaMatch,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, dAreaMatch, countOp);
			}
		}

		else {
			if (matchFilter != null && criteriaList != null && criteriaList.size() > 0) {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2, matchFilter,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, matchFilter, countOp);
			} else {
				aggregationResult2 = Aggregation.newAggregation(matchforWaferDefects2,
						Aggregation.sort(sortCombinedQuery), projectForWaferDefects);
				aggregationCount = Aggregation.newAggregation(matchforWaferDefects2, countOp);
			}
		}
		logger.info("AggregationQuery for WaferDefects: " + aggregationResult2);
		logger.info("Query for Count: " + aggregationCount);

		List<Document> total = mongoTemplate.aggregate(aggregationCount, collectionName2, Document.class)
				.getMappedResults();
		int totalCount = 0;
		if (total != null && total.size() > 0) {
			totalCount = (int) total.get(0).get("count");
		}
		defectResults = mongoTemplate.aggregate(aggregationResult2, collectionName2, Document.class).getMappedResults();
		waferDefects.addAll(defectResults);

		for (Document obj : waferDefects) {
			ObjectId oid2 = (ObjectId) obj.get("waferId");
			obj.put("waferId", oid2.toHexString());
			obj.put("_id", ((ObjectId) obj.get("_id")).toHexString());
			if (obj.get("imgCount") == null) {
				obj.put("imgCount", 0);
			}
		}
		GridDataResponse response = new GridDataResponse();
		response.setData(waferDefects);
		response.setCount(totalCount);
		return response;
	}

	private List<Criteria> generateQueryForSelection(List<Area> areas) {
		List<Criteria> criteriaList = new ArrayList<Criteria>();

		for (Area area : areas) {
			Criteria criteria = Criteria.where("radius").lt(area.getRadius().getTo()).gt(area.getRadius().getFrom());
			if (area.getAngle() != null) {
				criteria = criteria.and("angle").lt(area.getAngle().getTo()).gt(area.getAngle().getFrom());
			}
			criteriaList.add(criteria);
		}

		return criteriaList;
	}

	private List<Criteria> generateCriteriaForMultipleWaferDefects(List<FilterParamVdi> filterParams)
			throws IOException {
		List<Criteria> criteriaList = new ArrayList<Criteria>();
		String fieldName;

		Map<String, String> jsonObject = FilterUtil.getMasterColumnSchema(columnMappingDefectList);

		for (FilterParamVdi searchFilter : filterParams) {
			Criteria criteria = null;
			fieldName = searchFilter.getField().trim();
			switch (fieldName) {

			case "defect":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "lotId":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForStringArrayFields(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForString(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "process":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForStringArrayFields(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForString(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "test":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "cat":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "waferNumber":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForStringArrayFields(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForString(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "rbn":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "fbn":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}

				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "xSize":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "ySize":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "dSize":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "cn":
				if (searchFilter.getIsAdvanceSearch() == 1) {
					criteria = FilterUtil.generateCriteriaForNumberArrayFileds(searchFilter, fieldName, jsonObject);
				} else {
					criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				}
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "xRel":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "yRel":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "xIndex":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "yIndex":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "imgCount":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "dArea":
				criteria = FilterUtil.generateCriteriaForNumberFileds(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;
			case "adder":
				criteria = FilterUtil.generateCriteriaForBooleanAndNull(searchFilter, fieldName, jsonObject);
				if (criteria != null) {
					criteriaList.add(criteria);
				}
				break;

			default:
				break;
			}
		}
		return criteriaList;
	}

}
