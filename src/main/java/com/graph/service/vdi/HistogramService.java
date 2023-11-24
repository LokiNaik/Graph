package com.graph.service.vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import com.graph.model.vdi.Area;
import com.graph.model.vdi.HistogramRequest;
import com.graph.model.vdi.LineGraphResponse;
import com.graph.model.vdi.SelectedTestDetails;
import com.graph.model.vdi.XaxisDetailsHisto;
import com.graph.model.vdi.XaxisResponseDetails;
import com.graph.model.vdi.YaxisDetailsHisto;
import com.graph.model.vdi.YaxisSettingHisto;
import com.graph.model.wt.YaxisResponseDetails;
import com.graph.model.wt.groupResponseDetails;

@Service
public class HistogramService implements IHistogramService {
	@Autowired
	@Qualifier(value = "secondaryMongoTemplate")
	MongoTemplate mongoTemplate;

	public static final Logger logger = LoggerFactory.getLogger(HistogramService.class);

	@Override
	public LineGraphResponse getHistogramDetails(HistogramRequest request) throws IOException {
		LineGraphResponse res = new LineGraphResponse();
		List<Document> testResults = null;
		List<ObjectId> waferIds = request.getWaferID();
		String group = request.getGrouping().getAxisParameter();
		XaxisDetailsHisto xaxis = request.getxAxis();
		YaxisDetailsHisto yaxis = request.getyAxis();
		int XaxisCount = xaxis.getAxisCount();
		int YaxisCount = yaxis.getAxisCount();
		List<YaxisSettingHisto> dimensionDetails = xaxis.getSettings();
		List<YaxisSettingHisto> measureDetails = yaxis.getSettings();
		String DimensionName = dimensionDetails.get(0).getName();
		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		Object minVal = measureDetails.get(0).getMin();
		Object maxVal = measureDetails.get(0).getMax();
		Object minValX = dimensionDetails.get(0).getMin();
		Object maxValX = dimensionDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		List<Area> areas = request.getAreas();
		List<Area> discardedAreas = request.getDiscardedAreas();

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
			res.setData(new ArrayList<>());
			return res;
		}

		testResults = plothistogram(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName, group,
				selectedBin, testName, testNumber, minVal, maxVal, minValX, maxValX, Aggregator, areas, discardedAreas);

		if (testResults == null) {
			res.setData(new ArrayList<>());
			return res;
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

	private List<Document> plothistogram(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin,
			String testName, int testNumber, Object minVal, Object maxVal, Object minValX, Object maxValX,
			String Aggregator, List<Area> areas, List<Area> discardedAreas) throws IOException {
		List<Document> testResults = null;
		Aggregation aggregationMean = null;
		String collectionName = null;
		MatchOperation matchOperation = null;
		LookupOperation lookupOnInspectedWaferData, lookupOnWaferDefects, lookupOnEdx = null;
		UnwindOperation unwWindOperationonInspectedWaferData, unwWindOperationonWaferdefects,
				unwWindOperationonEdx = null;
		ProjectionOperation projectionOperation = null, projectionForZeroArea = null, projectionForNullArea = null,
				projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea;
		GroupOperation groupOperation = null, groupOperationForDensity, groupOperationForArea = null;
		AddFieldsOperation addFieldsdiscardedarea, addFieldsOperationDensity = null;
		MatchOperation crfMatch = null;
		MatchOperation edxMatch = null;
		List<Criteria> criteriaListForSelection = null;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;
		float discardedArea = 0;
		boolean flag = false;
		boolean edxflag = true;
		AggregationOperation FilterMinMax = null;

		Set<String> set1 = new HashSet<String>(Arrays.asList("tDefect", "N-Die", "D-Die"));
		Set<String> set2 = new HashSet<String>(Arrays.asList("Sx", "Sy", "D-Size", "A-Size"));
		Set<String> set3 = new HashSet<String>(Arrays.asList("aDefect"));
		Set<String> set4 = new HashSet<String>(Arrays.asList("Area", "Density"));

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}

		String DimensionX = generateFieldNameForCriteria(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		if (minValX == null && maxValX == null) {
			flag = true;
		} else if (Aggregator.equalsIgnoreCase("count")) {
			if (!(minValX == null) && maxValX == null) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)));
			} else if (minValX == null && !(maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxValX)));
			} else if (!(minValX == null) && !(maxValX == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX).lt(maxValX)));
			} else {
				if (Aggregator.equalsIgnoreCase("count")) {
					FilterMinMax = Aggregation
							.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minVal).lt(maxVal)));
				} else {
					FilterMinMax = Aggregation.match(new Criteria());
				}
			}
		} else {
			FilterMinMax = Aggregation.match(new Criteria());
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

		collectionName = "WaferDefects";
		matchOperation = Aggregation.match(Criteria.where("waferId").in(waferIds));

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

		lookupOnInspectedWaferData = LookupOperation.newLookup().from("InspectedWaferData").localField("waferId")
				.foreignField("_id").as("inspectedWaferData");
		unwWindOperationonInspectedWaferData = Aggregation.unwind("$inspectedWaferData", true);

		if (set1.contains(dimensionName) || set2.contains(dimensionName)) {
			groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").push(DimensionX).as("yObj")
					.addToSet("$waferId").as("waferIds");
			projectionOperation = Aggregation.project().and("_id." + group).as("group").and("$yObj").as("xAxis")
					.andArrayOf(new Document("$size", "$yObj")).as("yAxis").and("$waferIds").as("waferIds")
					.andExclude("_id");
		} else if (set3.contains(dimensionName)) {
			groupOperation = Aggregation.group("$waferId").push("$adder").as("xObj").addToSet("$waferId")
					.as("waferIds");
			projectionOperation = Aggregation.project().and("_id." + group).as("group")
					.andArrayOf(new Document("$size", "$xObj")).as("xAxis").andArrayOf(new Document("$size", "$xObj"))
					.as("yAxis").and("$waferIds").as("waferIds").andExclude("_id");

			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectionOperation, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectionOperation, FilterMinMax);
				}
			}
			logger.info("Histogram" + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}

			for (Document document : testResults) {
				List DimensionArray = (List) document.get("xAxis");
				List<ObjectId> wafers = (List) document.get("waferIds");
				int addervalue = (int) DimensionArray.get(0);
				if (addervalue == 0) {
					document.clear();
				} else {
					List<Integer> adderList = new ArrayList<>();
					for (int i = 0; i < addervalue; i++) {
						adderList.add(addervalue);
					}
					document.put("xAxis", adderList);
					List waferList = new ArrayList<>();
					for (int i = 0; i < wafers.size(); i++) {
						String hexString = wafers.get(i).toHexString();
						ObjectId objectId = new ObjectId(hexString);
						waferList.add(hexString);
					}
					document.put("waferIds", waferList);
				}
			}

			List<Document> tempDoc = new ArrayList<>();
			boolean emptyDocFlag = true;
			for (Document document : testResults) {
				if (!document.isEmpty()) {
					tempDoc.add(document);
					testResults = tempDoc;
					emptyDocFlag = false;
				}
			}
			if (emptyDocFlag == true) {
				testResults = null;
			}
			return testResults;
		} else if (set4.contains(dimensionName)) {
			collectionName = "InspectedWaferData";
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
			if (crfValue.equals("classNumber")) {
				crfValue = "waferDefects.classNumber";
			}
			if (crfValue.equals("roughBinNumber")) {
				crfValue = "waferDefects.roughBinNumber";
			}
			if (crfValue.equals("fineBinNumber")) {
				crfValue = "waferDefects.fineBinNumber";
			}
			criteriaForEdx = generateQueryForEdxset4(elements, edxflag);
			if (areas == null || areas.isEmpty()) {
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
			} else {
				criteriaListForSelection = generateQueryForSelectionArea(areas);
				crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx)
						.orOperator(criteriaListForSelection.toArray(new Criteria[criteriaListForSelection.size()])));
			}
			lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
					.as("edxObj");

			if (edxflag == true) {
				criteriaForEdxMatch = generateQueryforEdxMatchset4(elements);
				edxMatch = Aggregation.match(new Criteria()
						.orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
			}

			lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
					.foreignField("waferId").as("waferDefects");
			unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);

			addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea).build();
			groupOperation = Aggregation.group("$_id", DimensionX).first(DimensionX).as("xObj")
					.first("$lotRecord.sampleSize.x").as("diameter").count().as("yObj");
			projectionOperation = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj").as("yAxis")
					.and("$waferIds").as("waferIds").andExclude("_id");

			projectionForZeroArea = Aggregation.project().andInclude("_id", "xObj", "yObj", "diameter")
					.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
							.then(new Document("$multiply",
									Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
							.otherwise("$xObj"))
					.as("area");
			projectionForNullArea = Aggregation
					.project().andInclude("_id", "xObj", "yObj").and(
							ConditionalOperators.ifNull("$area")
									.then(new Document("$multiply",
											Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
					.as("area");
			groupOperationForArea = Aggregation.group("area").first("area").as("xObj").sum("yObj").as("yObj")
					.addToSet("_id._id").as("waferIds");

			if (dimensionName.equals("Density")) {

				addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea)
						.build();
				groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("xObj").count().as("yObj")
						.first("$discardedArea").as("discardedArea").first("$_id").as("waferId");

				projectionForZeroDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea", "tDefect",
								"waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$xObj"))
						.as("area");
				projectionForNullDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "tDefect", "discardedArea", "waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
				projectdiscardedarea = Aggregation.project()
						.and(ConditionalOperators
								.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
								.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
								.otherwise("$area"))
						.as("xObj").andInclude("yObj", "waferId");

				addFieldsOperationDensity = Aggregation.addFields()
						.addFieldWithValue("defDensity",
								new Document("$round",
										Arrays.asList(new Document("$divide", Arrays.asList("$yObj", "$xObj")), 16L)))
						.build();

				groupOperationForDensity = Aggregation.group("defDensity").first("defDensity").as("xObj").sum("yObj")
						.as("yObj").addToSet("waferId").as("waferIds");
				projectionOperation = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj")
						.as("yAxis").and("$waferIds").as("waferIds").andExclude("_id");
				if (edxflag == false) {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, groupOperationForDensity, projectionOperation);

					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								groupOperationForDensity, projectionOperation);
					}

				} else {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								groupOperationForDensity, projectionOperation);

					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, groupOperationForDensity,
								projectionOperation);
					}
				}
			}

			else {
				if (edxflag == false) {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, groupOperationForArea, projectionOperation);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, groupOperationForArea, projectionOperation, FilterMinMax);
					}
				} else {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, groupOperationForArea,
								projectionOperation);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, groupOperationForArea,
								projectionOperation, FilterMinMax);
					}
				}
			}

			logger.info("Histogram" + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}

			for (Document document : testResults) {
				List DimensionArrayX = (List) document.get("xAxis");
				double addervalue = (double) DimensionArrayX.get(0);
				List DimensionArrayY = (List) document.get("yAxis");
				int areaCount = (int) DimensionArrayY.get(0);
				List<Double> adderList = new ArrayList<>();
				for (int i = 0; i < areaCount; i++) {
					adderList.add(addervalue);
				}
				document.put("xAxis", adderList);
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

		if (set2.contains(dimensionName)) {
			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectionOperation, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectionOperation, FilterMinMax);
				}
			}
		} else {
			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectionOperation, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectionOperation);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectionOperation, FilterMinMax);
				}
			}
		}

		logger.info("Histogram" + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
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

	private List<Criteria> generateQueryforEdxMatchset4(List<String> elements) {
		Criteria criteria = new Criteria();
		List<Criteria> criteriaList = new ArrayList<>();
		criteriaList.add(Criteria.where("edxObj.elementsPresent.elements").in(elements));
		criteria.and("waferDefects.edx").exists(false);
		criteriaList.add(criteria);
		return criteriaList;
	}

	private Criteria generateQueryForEdxset4(List<String> elements, boolean edxflag) {
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

	private List<Criteria> generateQueryForSelectionArea(List<Area> areas) {
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

	private String generateFieldNameForCriteria(String fieldName) {
		switch (fieldName) {
		case "N-Die":
			return "inspectedWaferData.waferInspectionSummary.nDie";
		case "D-Die":
			return "inspectedWaferData.waferInspectionSummary.nDefDie";
		case "tDefect":
			return "inspectedWaferData.waferInspectionSummary.nDefect";
		case "Area":
			return "waferInspectionSummary.testArea";
		case "Density":
			return "waferInspectionSummary.testArea";
		case "aDefect":
			return "adder";
		case "D-Size":
			return "dSize";
		case "A-Size":
			return "defectArea";
		case "Sy":
			return "ySize";
		case "Sx":
			return "xSize";
		default:
			return null;
		}
	}
}
