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
import org.springframework.data.mongodb.core.aggregation.Fields;
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
public class ScatterService implements IScatterService {
	@Autowired
	@Qualifier(value = "secondaryMongoTemplate")
	MongoTemplate mongoTemplate;

	public static final Logger logger = LoggerFactory.getLogger(ScatterService.class);

	@Override
	public LineGraphResponse getScatterPlotDetails(HistogramRequest request) throws IOException {
		logger.info("Inside ScatterPlot");
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
		
		Set<String> dieSet = new HashSet<String>(Arrays.asList("N-Die", "D-Die"));

		//
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
			if(!(dieSet.contains(DimensionName)&&dieSet.contains(MeasureName))) {
				if (!(crfFilter == null)) {
					res.setData(new ArrayList<>());
					return res;
				}
			}
		}
//

		if (DimensionName.equals(MeasureName)) {
			throw new IOException("Combination not supported");
		} else if (MeasureName.equals("Area") || MeasureName.equals("Density")) {
			testResults = scatterPlotForArea(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName, group, selectedBin,
					testName, testNumber, minVal, maxVal, minValX, maxValX, Aggregator, areas, discardedAreas);
		} else if (MeasureName.equals("D-Die") || MeasureName.equals("N-Die")) {
			testResults = scatterPlotForDie(waferIds,  crfFilter, crfValue, elements, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, minValX, maxValX, Aggregator, areas,
					discardedAreas);
		} else {
			testResults = scatterPlot1(waferIds, crfFilter, crfValue, elements, DimensionName, MeasureName, group, selectedBin,
					testName, testNumber, minVal, maxVal, minValX, maxValX, Aggregator, areas, discardedAreas);
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

	private List<Document> scatterPlotForDie(List<ObjectId> waferIds,List<Integer> crfFilter, String crfValue,
			List<String> elements,  String dimensionName, String measureName, String group, int selectedBin, String testName,
			int testNumber, Object minVal, Object maxVal, Object minValX, Object maxValX, String Aggregator,
			List<Area> areas, List<Area> discardedAreas) throws IOException {
		String collectionName = null;
		MatchOperation edxMatch=null, matchOperation = null;
		AggregationOperation FilterMinMax = null;
		GroupOperation groupOperation = null, groupOperation1 = null, groupOperationForArea = null,
				groupOperationForDensity = null;
		ProjectionOperation projectFetchData = null, projectionForZeroArea = null, projectionForNullArea = null,
				projectionOperation1 = null, projectionForZeroDensity = null, projectionForNullDensity = null,
				projectdiscardedarea = null;
		boolean flag = false;
		Aggregation aggregationMean = null;
		List<Document> testResults = null;
		LookupOperation lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonWaferdefects = null;
		String DimensionX, DimensionY = null;
		MatchOperation crfMatch = null;
		AddFieldsOperation addFieldsOperationDensity = null, addFieldsdiscardedarea = null;
		List<Criteria> criteriaListForSelection = null;
		float discardedArea = 0;
		LookupOperation lookupOnEdx, lookupOnInspectedWaferData=null;
		UnwindOperation unwWindOperationonEdx, unwWindOperationonInspectedWaferData=null;
		boolean edxflag = true;
		Criteria criteriaForEdx = null;
		List<Criteria> criteriaForEdxMatch = null;

		Set<String> uniqueCase = new HashSet<String>(Arrays.asList("aDefect", "tDefect"));
		Set<String> uniqueCase1 = new HashSet<String>(Arrays.asList("N-Die", "D-Die"));
		Set<String> uniqueCase2 = new HashSet<String>(Arrays.asList("Area", "Density"));
		
		if(!(elements==null)) {
			if (elements.size() == 1 && elements.contains("emptyElements")) {
				edxflag = false;
			}
		}
		
		
		if (uniqueCase2.contains(dimensionName)) {
			DimensionX = generateFieldNameForCriteria(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}
			DimensionY = generateFieldNameForCriteria(measureName);
			if (DimensionY == null) {
				throw new IOException("Selected Dimension is not supported.");
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
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
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
		
		} else {
			if(!(elements==null)) {
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
				
			}
		}

		if ((minVal == null && maxVal == null) && (minValX == null && maxValX == null)) {
			flag = true;
		} else if (Aggregator.equalsIgnoreCase("count")) {
			if ((!(minVal == null) && maxVal == null) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((minVal == null && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
			} else if ((!(minValX == null) && maxValX == null) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)));
			} else if ((minValX == null && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(maxValX)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX)));
			} else if ((!(minValX == null) && !(maxValX == null)) && (!(minVal == null) && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").gte(minVal))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && !(maxVal == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").lte(maxVal))));
			} else {
				if (Aggregator.equalsIgnoreCase("count")) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
							.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX))));
				} else {
					FilterMinMax = Aggregation.match(new Criteria());
				}
			}
		} else {
			FilterMinMax = Aggregation.match(new Criteria());
		}
		projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj").as("yAxis")
				.andExclude("_id").and("$waferId").as("waferIds");

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
		if (uniqueCase1.contains(dimensionName)) {
			collectionName = "InspectedWaferData";
		
			matchOperation = Aggregation.match(Criteria.where("_id").in(waferIds));
			DimensionX = generateFieldNameForArea(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}
			DimensionY = generateFieldNameForArea(measureName);
			if (DimensionY == null) {
				throw new IOException("Selected Dimension is not supported.");
			}

			groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").sum(DimensionY).as("yObj")
							.addToSet("_id").as("waferId");
			if (flag == true) {
				aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFetchData);
			} else {
				aggregationMean = Aggregation.newAggregation(matchOperation, groupOperation, projectFetchData,
						FilterMinMax);
			}
		} else if (uniqueCase2.contains(dimensionName)) {
			collectionName = "InspectedWaferData";
			lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
					.foreignField("waferId").as("waferDefects");
			unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);
			//
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

			
			if (dimensionName.equals("Area")) {
				groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("xObj").first(DimensionY)
						.as("yObj");

				projectionForZeroArea = Aggregation
						.project().andInclude("_id", "xObj", "yObj",
								"diameter")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$xObj"))
						.as("area");
				projectionForNullArea = Aggregation.project().andInclude("_id", "xObj", "yObj")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
				groupOperationForArea = Aggregation.group("area").first("area").as("xObj").sum("yObj").as("yObj")
										.addToSet("_id").as("waferId");

				projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj").as("yAxis")
						.andExclude("_id").and("$waferId").as("waferIds");
//
				if(edxflag == false) {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation,
								projectionForZeroArea, projectionForNullArea, groupOperationForArea, projectFetchData);
				} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects,crfMatch, groupOperation,
								projectionForZeroArea, projectionForNullArea, groupOperationForArea, projectFetchData,
								FilterMinMax);
				}
					
				}else {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch,lookupOnEdx, unwWindOperationonEdx, edxMatch, groupOperation,
								projectionForZeroArea, projectionForNullArea, groupOperationForArea, projectFetchData);
				} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects,crfMatch,lookupOnEdx, unwWindOperationonEdx, edxMatch, groupOperation,
								projectionForZeroArea, projectionForNullArea, groupOperationForArea, projectFetchData,
								FilterMinMax);
				}
				}
				
			} else {
				addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea)
						.build();
				groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("xObj").first(DimensionY)
						.as("yObj").first("$discardedArea").as("discardedArea").first("$_id").as("waferId");
				projectionForZeroDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea",
								"tDefect","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$xObj"))
						.as("area");
				projectionForNullDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "discardedArea", "tDefect","waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
				projectdiscardedarea = Aggregation.project()
						.and(ConditionalOperators
								.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
								.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
								.otherwise("$area"))
						.as("area").andInclude("tDefect","waferId").andInclude("yObj");

				addFieldsOperationDensity = Aggregation.addFields().addFieldWithValue("defDensity",
						new Document("$round",
								Arrays.asList(new Document("$divide", Arrays.asList("$tDefect", "$area")), 16L)))
						.build();

				groupOperationForDensity = Aggregation.group("defDensity").first("defDensity").as("xObj").sum("yObj")
						.as("yObj").addToSet("waferId").as("waferId");
				projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj").as("yAxis")
						.andExclude("_id").and("$waferId").as("waferIds");
				
				if (edxflag == false) {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, groupOperationForDensity, projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, groupOperationForDensity, projectFetchData, FilterMinMax);
					}

				} else {
					if (flag == true) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								groupOperationForDensity, projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								groupOperationForDensity, projectFetchData, FilterMinMax);
					}
				}
			}

		} else {
			
			collectionName = "WaferDefects";
			lookupOnInspectedWaferData = LookupOperation.newLookup().from("InspectedWaferData")
					.localField("waferId").foreignField("_id").as("inspectedWaferData");
			unwWindOperationonInspectedWaferData = Aggregation.unwind("$inspectedWaferData", false);
			
			DimensionX = generateFieldNameForCriteria(dimensionName);
			if (DimensionX == null) {
				throw new IOException("Selected Dimension is not supported.");
			}
			DimensionY = generateFieldName(measureName);
			if (DimensionY == null) {
				throw new IOException("Selected Dimension is not supported.");
			}
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


			if (dimensionName.equals("tDefect")) {
				groupOperation = Aggregation.group("$waferId").count().as("xObj").first(DimensionY).as("yObj")
						.addToSet("waferId").as("waferId");
			} else if (dimensionName.equals("aDefect")) {
				groupOperation = Aggregation.group("$waferId").push(DimensionX).as("xObj").first(DimensionY).as("yObj")
						.addToSet("waferId").as("waferId");;
				projectFetchData = Aggregation.project().andArrayOf(new Document("$size", "$xObj")).as("xAxis")
						.andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");
			} else {
				groupOperation = Aggregation.group("$_id", DimensionX).first(DimensionX).as("xObj").first(DimensionY)
						.as("yObj").addToSet("waferId").as("waferId");
			}
			
			if (edxflag == false) {
				if (flag == true) {
					if (uniqueCase.contains(dimensionName)) {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch,
								lookupOnInspectedWaferData, unwWindOperationonInspectedWaferData, groupOperation,
								projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch,
								lookupOnInspectedWaferData, unwWindOperationonInspectedWaferData, groupOperation,
								projectFetchData);
					}

				} else {
					if (uniqueCase.contains(dimensionName)) {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch,
								lookupOnInspectedWaferData, unwWindOperationonInspectedWaferData, groupOperation,
								projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch,
								lookupOnInspectedWaferData, unwWindOperationonInspectedWaferData, groupOperation,
								projectFetchData, FilterMinMax);
					}
				}

			} else {
				if (flag == true) {
					if (uniqueCase.contains(dimensionName)) {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
								unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
								unwWindOperationonInspectedWaferData, groupOperation, projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
								unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
								unwWindOperationonInspectedWaferData, groupOperation, projectFetchData);
					}

				} else {
					if (uniqueCase.contains(dimensionName)) {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
								unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
								unwWindOperationonInspectedWaferData, groupOperation, projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
								unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
								unwWindOperationonInspectedWaferData, groupOperation, projectFetchData, FilterMinMax);
					}
				}

			}

		}

		logger.info("Aggregation Query for ScatterPlot " + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}
		if (Aggregator.equalsIgnoreCase("Avg")) {
			int wSize = waferIds.size();
			for (Document document : testResults) {
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i = Double.parseDouble(Dimension);
				double total = i / wSize;
				if (minVal == null && maxVal == null) {
					document.put("yAxis", Arrays.asList(total));
				} else if (!(minVal == null) && maxVal == null) {
					if (total >= Double.parseDouble(minVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else if (!(maxVal == null) && minVal == null) {
					if (total <= Double.parseDouble(maxVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else {
					if (total <= Double.parseDouble(maxVal.toString())
							&& total >= Double.parseDouble(minVal.toString())) {
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

	private List<Document> scatterPlotForArea(List<ObjectId> waferIds,List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin, String testName,
			int testNumber, Object minVal, Object maxVal, Object minValX, Object maxValX, String Aggregator,
			List<Area> areas, List<Area> discardedAreas) throws IOException {
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData = null, projectionForZeroArea = null, projectionForNullArea = null,
				projectionOperation1 = null, projectionForZeroDensity = null, projectionForNullDensity = null,
				projectdiscardedarea = null, projectFetchData1 = null;
		GroupOperation groupOperationForArea = null, groupOperation = null, groupOperationForDensity = null;
		List<Document> testResults = null;
		MatchOperation edxMatch=null, matchOperation = null;
		String collectionName = null;
		LookupOperation lookupOnEdx, lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonEdx,  unwWindOperationonWaferdefects = null;
		MatchOperation crfMatch = null;
		Criteria criteriaForEdx = null;
		boolean edxflag = true;
		boolean flag = false;
		AddFieldsOperation addFieldsOperationDensity = null, addFieldsdiscardedarea = null,
				addFieldsOperationForAdder = null;
		AggregationOperation FilterMinMax = null;
		String DimensionX;
		List<Criteria> criteriaListForSelection = null;
		List<Criteria> criteriaForEdxMatch = null;
		float discardedArea = 0;

		if (elements.size() == 1 && elements.contains("emptyElements")) {
			edxflag = false;
		}
		
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

		if (dimensionName.equals("N-Die") || dimensionName.equals("D-Die")) {
			DimensionX = generateFieldNameForCriteria(dimensionName);
		} else {
			DimensionX = generateFieldNameForArea(dimensionName);
		}
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		String DimensionY = generateFieldNameForArea(measureName);
		if (DimensionY == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		String[] parts = DimensionX.split("\\.");
		String key = null;
		for (String end : parts) {
			key = end;
		}
		if ((minVal == null && maxVal == null) && (minValX == null && maxValX == null)) {
			flag = true;
		} else if (Aggregator.equalsIgnoreCase("count")) {
			if ((!(minVal == null) && maxVal == null) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((minVal == null && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
			} else if ((!(minValX == null) && maxValX == null) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)));
			} else if ((minValX == null && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(maxValX)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX)));
			} else if ((!(minValX == null) && !(maxValX == null)) && (!(minVal == null) && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").gte(minVal))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && !(maxVal == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").lte(maxVal))));
			} else {
				if (Aggregator.equalsIgnoreCase("count")) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
							.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX))));
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

		lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
				.foreignField("waferId").as("waferDefects");
		unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);
		

		if (measureName.equals("Area")) {
			if (dimensionName.equals("tDefect")) {
				groupOperation = Aggregation.group("$_id").count().as("xObj").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("yObj").count().as("tDefect")
						.addToSet("_id").as("waferId");
				projectionOperation1 = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$area")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

			} else if (dimensionName.equals("aDefect")) {

				groupOperation = Aggregation.group("$_id").push(DimensionX).as("xObj").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("yObj").count().as("tDefect")
						.addToSet("_id").as("waferId");
				projectionOperation1 = Aggregation.project().andArrayOf(new Document("$size", "$xObj")).as("xAxis")
						.andArrayOf("$area").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

			} else if (dimensionName.equals("Density")) {
				addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea)
						.build();
				groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("xObj")
						.first("$waferInspectionSummary.testArea").as("yObj").first("$discardedArea")
						.as("discardedArea").first("$_id").as("waferId");
				projectionForZeroDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea",
								"tDefect","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$yObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$yObj"))
						.as("area");
				projectionForNullDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "tDefect", "discardedArea","waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
				projectdiscardedarea = Aggregation.project()
						.and(ConditionalOperators
								.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
								.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
								.otherwise("$area"))
						.as("area").andInclude("tDefect").andInclude("yObj","waferId");

				addFieldsOperationDensity = Aggregation.addFields().addFieldWithValue("defDensity",
						new Document("$round",
								Arrays.asList(new Document("$divide", Arrays.asList("$tDefect", "$area")), 16L)))
						.build();

				groupOperationForDensity = Aggregation.group("defDensity").first("defDensity").as("xObj").sum("area")
						.as("yObj").addToSet("waferId").as("waferIds");
				projectFetchData1 = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj")
						.as("yAxis").andExclude("_id").and("$waferIds").as("waferIds");

			}else {
				groupOperation = Aggregation.group("$_id", DimensionX).first(DimensionX).as("xObj")
						.first("$lotRecord.sampleSize.x").as("diameter").first("$waferInspectionSummary.testArea")
						.as("yObj").count().as("tDefect").first("$_id").as("waferId");
				groupOperationForArea = Aggregation.group("$_id").first("xObj").as("xObj").sum("area").as("yObj")
										.addToSet("waferId").as("waferId");
				projectionOperation1 = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");
			}

			if (!dimensionName.equals("Density")) {

				projectionForZeroArea = Aggregation
						.project().andInclude("_id", "xObj", "yObj", "diameter",
								"tDefect","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$yObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$yObj"))
						.as("area");

				projectionForNullArea = Aggregation.project().andInclude("_id", "xObj", "tDefect","waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
			}
		}
		else {
			/*
			 * matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects,  catMatch, addFieldsdiscardedarea,
								groupOperation, projectionForZeroDensity, projectionForNullDensity,
								projectdiscardedarea, addFieldsOperationDensity, addFieldsOperationForAdder,
								projectFetchData
			 * 
			 */
			addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea).build();
			groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
					.as("diameter").first("$waferInspectionSummary.testArea").as("xObj").first("$discardedArea")
					.as("discardedArea").first("$_id").as("waferId");

			projectionForZeroDensity = Aggregation.project()
					.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea", "tDefect","waferId")
					.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$yObj").equalToValue(0))
							.then(new Document("$multiply",
									Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
							.otherwise("$yObj"))
					.as("area");
			projectionForNullDensity = Aggregation
					.project().andInclude("_id", "xObj", "yObj", "tDefect", "discardedArea","waferId").and(
							ConditionalOperators.ifNull("$area")
									.then(new Document("$multiply",
											Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
					.as("area");
			projectdiscardedarea = Aggregation.project()
					.and(ConditionalOperators.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
							.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
							.otherwise("$area"))
					.as("area").andInclude("tDefect","waferId").andInclude("xObj");
			addFieldsOperationDensity = Aggregation.addFields()
					.addFieldWithValue("defDensity",
							new Document("$round",
									Arrays.asList(new Document("$divide", Arrays.asList("$tDefect", "$area")), 16L)))
					.build();
			addFieldsOperationForAdder = Aggregation.addFields()
					.addFieldWithValue("product", new Document("$size", "$xObj")).build();

			if (dimensionName.equals("tDefect")) {

				groupOperation = Aggregation.group("$_id").count().as("xObj").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("yObj").count().as("tDefect")
						.first("$discardedArea").as("discardedArea").addToSet("_id").as("waferId");;
				projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$defDensity")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

			} else if (dimensionName.equals("aDefect")) {

				groupOperation = Aggregation.group("$_id").push(DimensionX).as("xObj").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("yObj").count().as("tDefect")
						.first("$discardedArea").as("discardedArea").addToSet("_id").as("waferId");
				projectFetchData = Aggregation.project().andArrayOf("$product").as("xAxis").andArrayOf("$defDensity")
						.as("yAxis").andExclude("_id")
						.and("$waferId").as("waferIds");

			} else if (dimensionName.equals("Area")) {

				groupOperation = Aggregation.group("$_id").count().as("tDefect").first("$lotRecord.sampleSize.x")
						.as("diameter").first("$waferInspectionSummary.testArea").as("xObj")
						.first("$waferInspectionSummary.testArea").as("yObj").count().as("tDefect")
						.first("$discardedArea").as("discardedArea").addToSet("_id").as("waferId");
				projectFetchData = Aggregation.project().andArrayOf("$area").as("xAxis").andArrayOf("$defDensity")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

			}

			else {

				groupOperation = Aggregation.group("$_id", DimensionX).count().as("tDefect")
						.first("$lotRecord.sampleSize.x").as("diameter").first(DimensionX).as("xObj")
						.first("$waferInspectionSummary.testArea").as("yObj").first("$discardedArea")
						.as("discardedArea").addToSet("_id").as("waferId");
				projectFetchData = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$defDensity")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

			}

		}

		if (measureName.equals("Area")) {
			if (edxflag == false) {
				if (flag == true) {
					if (dimensionName.equals("tDefect") || dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, projectionOperation1);

					} else if (dimensionName.equals("Density")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, groupOperationForDensity, projectFetchData1);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, groupOperationForArea, projectionOperation1);
					}
				} else {
					if (dimensionName.equals("tDefect") || dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, projectionOperation1, FilterMinMax);
					} else if (dimensionName.equals("Density")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, projectionOperation1, groupOperationForDensity,
								projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
								projectionForNullArea, groupOperationForArea, projectionOperation1, FilterMinMax);
					}
				}

			} else {
				if (flag == true) {
					if (dimensionName.equals("tDefect") || dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, projectionOperation1);

					} else if (dimensionName.equals("Density")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								groupOperationForDensity, projectFetchData1);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, groupOperationForArea,
								projectionOperation1);
					}
				} else {
					if (dimensionName.equals("tDefect") || dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, projectionOperation1,
								FilterMinMax);
					} else if (dimensionName.equals("Density")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								projectionOperation1, groupOperationForDensity, projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								groupOperation, projectionForZeroArea, projectionForNullArea, groupOperationForArea,
								projectionOperation1, FilterMinMax);
					}
				}
			}

		} else {
			if (edxflag == false) {
				if (flag == true) {
					if (dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, addFieldsOperationForAdder, projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, projectFetchData);
					}
				} else {
					if (dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, addFieldsOperationForAdder, projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
								projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
								addFieldsOperationDensity, projectFetchData, FilterMinMax);
					}
				}

			} else {
				if (flag == true) {
					if (dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								addFieldsOperationForAdder, projectFetchData);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								projectFetchData);
					}
				} else {
					if (dimensionName.equals("aDefect")) {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								addFieldsOperationForAdder, projectFetchData, FilterMinMax);
					} else {
						aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
								unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx, edxMatch,
								addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
								projectionForNullDensity, projectdiscardedarea, addFieldsOperationDensity,
								projectFetchData, FilterMinMax);
					}
				}
			}
		}

		logger.info("Aggregation Query for ScatterPlot " + aggregationMean);
		testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
		if (testResults == null || testResults.isEmpty()) {
			return null;
		}

		if (Aggregator.equalsIgnoreCase("Avg")) {
			int wSize = waferIds.size();
			for (Document document : testResults) {
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i = Double.parseDouble(Dimension);
				double total = i / wSize;
				if (minVal == null && maxVal == null) {
					document.put("yAxis", Arrays.asList(total));
				} else if (!(minVal == null) && maxVal == null) {
					if (total >= Double.parseDouble(minVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else if (!(maxVal == null) && minVal == null) {
					if (total <= Double.parseDouble(maxVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else {
					if (total <= Double.parseDouble(maxVal.toString())
							&& total >= Double.parseDouble(minVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				}

			}
		}

		for (Document document : testResults) {
			List DimensionArrayX = (List) document.get("xAxis");
			double addervalue = Double.valueOf(DimensionArrayX.get(0).toString());
			List DimensionArrayY = (List) document.get("yAxis");
			double areaCount = Double.valueOf(DimensionArrayY.get(0).toString());
			List<Double> adderList = new ArrayList<>();
			adderList.add(addervalue);
			document.put("xAxis", adderList);
			document.put("yAxis",  Arrays.asList(areaCount));

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

	private List<Document> scatterPlot1(List<ObjectId> waferIds, List<Integer> crfFilter, String crfValue,
			List<String> elements, String dimensionName, String measureName, String group, int selectedBin, String testName,
			int testNumber, Object minVal, Object maxVal, Object minValX, Object maxValX, String Aggregator,
			List<Area> areas, List<Area> discardedAreas) throws IOException {
		Aggregation aggregationMean = null;
		ProjectionOperation projectFetchData, projectionForZeroArea = null, projectionForNullArea = null,
				projectionOperation, projectionOperation1 = null, projectionForZeroDensity = null,
				projectionForNullDensity = null, projectdiscardedarea = null;
		GroupOperation groupOperation1 = null, groupOperation = null;
		List<Document> testResults = null;
		MatchOperation edxMatch=null, matchOperation = null;
		String collectionName = null;
		LookupOperation lookupOnEdx, lookupOnInspectedWaferData, lookupOnWaferDefects = null;
		UnwindOperation unwWindOperationonEdx, unwWindOperationonInspectedWaferData, unwWindOperationonWaferdefects = null;
		List<Criteria> criteriaForEdxMatch = null;
		MatchOperation crfMatch = null;
		boolean flag = false;
		List<Criteria> criteriaListForSelection = null;
		Criteria criteriaForEdx = null;
		boolean edxflag = true;
		AddFieldsOperation addFieldsOperation = null, addFieldsOperationForAdder = null, addFieldsdiscardedarea = null;
		AggregationOperation FilterMinMax = null;
		float discardedArea = 0;
		Set<String> uniqueCase = new HashSet<String>(Arrays.asList("aDefect", "tDefect"));
		Set<String> uniqueCase1 = new HashSet<String>(Arrays.asList("N-Die", "D-Die"));
		Set<String> uniqueCase2 = new HashSet<String>(Arrays.asList("Area", "Density"));

		collectionName = "WaferDefects";
		matchOperation = Aggregation.match(Criteria.where("waferId").in(waferIds));

		String DimensionX = generateFieldName(dimensionName);
		if (DimensionX == null) {
			throw new IOException("Selected Dimension is not supported.");
		}

		String DimensionY = generateFieldNameForCriteria(measureName);
		if (DimensionY == null) {
			throw new IOException("Selected Dimension is not supported.");
		}
		if ((minVal == null && maxVal == null) && (minValX == null && maxValX == null)) {
			flag = true;
		} else if (Aggregator.equalsIgnoreCase("count")) {
			if ((!(minVal == null) && maxVal == null) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").gte(minVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((minVal == null && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(
						Criteria.where("yAxis").lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && maxVal == null) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)));
			} else if ((minVal == null && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").lte(maxVal)));
			} else if ((!(minValX == null) && maxValX == null) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)));
			} else if ((minValX == null && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").lte(maxValX)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && maxValX == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal).lte(maxVal)));
			} else if ((!(minVal == null) && !(maxVal == null)) && (!(minValX == null) && maxValX == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX))));
			} else if ((!(minVal == null) && !(maxVal == null)) && (minValX == null && !(maxValX == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
						.lte(maxVal).andOperator(Criteria.where("xAxis").lte(maxValX))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && maxVal == null)) {
				FilterMinMax = Aggregation
						.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX)));
			} else if ((!(minValX == null) && !(maxValX == null)) && (!(minVal == null) && maxVal == null)) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").gte(minVal))));
			} else if ((!(minValX == null) && !(maxValX == null)) && (minVal == null && !(maxVal == null))) {
				FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("xAxis").gte(minValX)
						.lte(maxValX).andOperator(Criteria.where("yAxis").lte(maxVal))));
			} else {
				if (Aggregator.equalsIgnoreCase("count")) {
					FilterMinMax = Aggregation.match(new Criteria().andOperator(Criteria.where("yAxis").gte(minVal)
							.lte(maxVal).andOperator(Criteria.where("xAxis").gte(minValX).lte(maxValX))));
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

		criteriaForEdx = generateQueryForEdx(elements, edxflag);
		if (areas == null || areas.isEmpty()) {
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));

		} else {
			criteriaListForSelection = generateQueryForSelectionAreas1(areas);
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

		if (uniqueCase.contains(dimensionName)) {
			if (dimensionName.equals("aDefect")) {
				if (measureName.equals("tDefect")) {
					groupOperation = Aggregation.group("$waferId").push("$adder").as("xObj").count().as("yObj")
										.addToSet("waferId").as("waferId");
					projectFetchData = Aggregation.project().and("_id." + group).as("group")
							.andArrayOf(new Document("$size", "$xObj")).as("xAxis").andArrayOf("$yObj").as("yAxis")
							.andExclude("_id").and("$waferId").as("waferIds");
				} else {
					groupOperation = Aggregation.group("$waferId").push("$adder").as("xObj").sum(DimensionY).as("yObj")
									.addToSet("waferId").as("waferId");;
					projectFetchData = Aggregation.project().and("_id." + group).as("group")
							.andArrayOf(new Document("$size", "$xObj")).as("xAxis").andArrayOf("$yObj").as("yAxis")
							.andExclude("_id").and("$waferId").as("waferIds");
				}
			} else {
				if (measureName.equals("aDefect")) {
					groupOperation = Aggregation.group("$waferId").count().as("xObj").push("$adder").as("yObj")
							.addToSet("waferId").as("waferId");
					projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf(new Document("$size", "$yObj")).as("yAxis").andExclude("_id")
							.and("$waferId").as("waferIds");
				} else {
					groupOperation = Aggregation.group("$waferId").count().as("xObj").sum(DimensionY).as("yObj")
							.addToSet("waferId").as("waferId");
					projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj")
							.as("xAxis").andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");;
				}
			}
			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, FilterMinMax);
				}
			}
			
			logger.info("Aggregation Query for ScatterPlot " + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}
		} else if (uniqueCase1.contains(dimensionName)) {
			lookupOnInspectedWaferData = LookupOperation.newLookup().from("InspectedWaferData")
					.localField("waferId").foreignField("_id").as("inspectedWaferData");
			unwWindOperationonInspectedWaferData = Aggregation.unwind("$inspectedWaferData", true);

			if (measureName.equals("aDefect")) {
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").push("$adder").as("yObj")
								.addToSet("waferId").as("waferId");
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf(new Document("$size", "$yObj")).as("yAxis").andExclude("_id").and("$waferId").as("waferIds");
			} else if (measureName.equals("tDefect")) {
//				addFieldsOperation = Aggregation.addFields()
//						.addFieldWithValue("yAxis", new Document("$first", "$yAxis")).addFieldWithValue("group", "null")
//						.build();
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").count().as("yObj")
						.addToSet("waferId").as("waferId");;
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");
			} else {
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").sum(DimensionY).as("yObj")
						.addToSet("waferId").as("waferId");;
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");
//				addFieldsOperation = Aggregation.addFields()
//						.addFieldWithValue("yAxis", new Document("$first", "$yAxis")).addFieldWithValue("group", "null")
//						.build();
			}
			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectFetchData, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, lookupOnInspectedWaferData,
							unwWindOperationonInspectedWaferData, groupOperation, projectFetchData, FilterMinMax);
				}
			}
			
			logger.info("Aggregation Query for ScatterPlot " + aggregationMean);
			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}
		} else if (uniqueCase2.contains(dimensionName)) {
			collectionName = "InspectedWaferData";
			DimensionX=generateFieldNameForCriteria(dimensionName);
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
			criteriaForEdx = generateQueryForEdxlookup(elements, edxflag);
			crfMatch = Aggregation.match(Criteria.where(crfValue).in(crfFilter).andOperator(criteriaForEdx));
			if (edxflag == true) {
				criteriaForEdxMatch = generateQueryforEdxMatchlookup(elements);
				edxMatch = Aggregation.match(new Criteria()
						.orOperator(criteriaForEdxMatch.toArray(new Criteria[criteriaForEdxMatch.size()])));
			}
			lookupOnEdx = LookupOperation.newLookup().from("EDX").localField("waferDefects.edx").foreignField("_id")
					.as("edxObj");
			unwWindOperationonEdx = Aggregation.unwind("$edxObj", true);

			String[] parts = DimensionX.split("\\.");
			String key = null;
			for (String end : parts) {
				key = end;
			}

			lookupOnWaferDefects = LookupOperation.newLookup().from("WaferDefects").localField("_id")
					.foreignField("waferId").as("waferDefects");
			unwWindOperationonWaferdefects = Aggregation.unwind("$waferDefects", false);
			

			if (dimensionName.equals("Area")) {
				if (measureName.equals("aDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id").and(key, DimensionX))
							.first(DimensionX).as("xObj").first("$lotRecord.sampleSize.x").as("diameter")
							.push("$waferDefects.adder").as("yObj").first("_id").as("waferId");
					projectionOperation = Aggregation.project().andInclude("_id", "area","waferId").and("$area").as("xObj")
							.and("$product").as("yObj");
				} else if (measureName.equals("tDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id").and(key, DimensionX))
							.first(DimensionX).as("xObj").first("$lotRecord.sampleSize.x").as("diameter").count()
							.as("yObj").first("_id").as("waferId");
					projectionOperation = Aggregation.project().andInclude("_id", "area","waferId").and("$area").as("xObj")
							.and("$yObj").as("yObj");
				} else {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id").and(key, DimensionX))
							.first(DimensionX).as("xObj").first("$lotRecord.sampleSize.x").as("diameter")
							.sum("$waferDefects." + DimensionY).as("yObj").first("_id").as("waferId");
					projectionOperation = Aggregation.project().andInclude("_id", "area","waferId").and("$area").as("xObj")
							.and("$yObj").as("yObj");
				}

				projectionForZeroArea = Aggregation
						.project().andInclude("_id", "xObj", "yObj",
								"diameter","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$xObj"))
						.as("area");

				projectionForNullArea = Aggregation.project().andInclude("_id", "xObj", "yObj","waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");

				addFieldsOperation = Aggregation.addFields()
						.addFieldWithValue("product", new Document("$size", "$yObj")).build();

				groupOperation1 = Aggregation.group("xObj").first("xObj").as("xObj").sum("yObj").as("yObj")
									.addToSet("waferId").as("waferId");

				projectionOperation1 = Aggregation.project().andArrayOf("$xObj").as("xAxis").andArrayOf("$yObj")
						.as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

				if (measureName.equals("aDefect")) {
					if (edxflag == false) {
						if (flag == true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
									projectionForNullArea, addFieldsOperation, projectionOperation, groupOperation1,
									projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
									projectionForNullArea, addFieldsOperation, projectionOperation, groupOperation1,
									projectionOperation1, FilterMinMax);
						}

					} else {
						if (flag == true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, groupOperation, projectionForZeroArea, projectionForNullArea,
									addFieldsOperation, projectionOperation, groupOperation1, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, groupOperation, projectionForZeroArea, projectionForNullArea,
									addFieldsOperation, projectionOperation, groupOperation1, projectionOperation1,
									FilterMinMax);
						}
					}

				} else {
					if (edxflag == false) {
						if (flag == true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
									projectionForNullArea, projectionOperation, groupOperation1, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, groupOperation, projectionForZeroArea,
									projectionForNullArea, projectionOperation, groupOperation1, projectionOperation1,
									FilterMinMax);
						}
					} else {
						if (flag == true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, groupOperation, projectionForZeroArea, projectionForNullArea,
									projectionOperation, groupOperation1, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, groupOperation, projectionForZeroArea, projectionForNullArea,
									projectionOperation, groupOperation1, projectionOperation1, FilterMinMax);
						}
					}
				}

				logger.info("Preparing aggregationMean scatterDetails" + aggregationMean);
				testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
						.getMappedResults();
			}

			else if (dimensionName.equals("Density")) {
				if (measureName.equals("aDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id")).first(DimensionX).as("xObj")
							.first("$lotRecord.sampleSize.x").as("diameter").count().as("tDefect")
							.push("$waferDefects.adder").as("yObj").first("$discardedArea").as("discardedArea")
							.addToSet("_id").as("waferId");

					projectionOperation1 = Aggregation.project().andArrayOf("$defDensity").as("xAxis")
							.andArrayOf("$product").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");

				} else if (measureName.equals("tDefect")) {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id")).first(DimensionX).as("xObj")
							.first("$lotRecord.sampleSize.x").as("diameter").count().as("tDefect").count().as("yObj")
							.first("$discardedArea").as("discardedArea").addToSet("_id").as("waferId");
					projectionOperation1 = Aggregation.project().andArrayOf("$defDensity").as("xAxis")
							.andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");;

				} else {
					groupOperation = Aggregation.group(Fields.fields().and("id", "$_id")).first(DimensionX).as("xObj")
							.first("$lotRecord.sampleSize.x").as("diameter").count().as("tDefect")
							.sum("$waferDefects." + DimensionY).as("yObj").first("$discardedArea").as("discardedArea")
							.addToSet("_id").as("waferId");
					projectionOperation1 = Aggregation.project().andArrayOf("$defDensity").as("xAxis")
							.andArrayOf("$yObj").as("yAxis").andExclude("_id").and("$waferId").as("waferIds");;

				}
				addFieldsdiscardedarea = Aggregation.addFields().addFieldWithValue("discardedArea", discardedArea)
						.build();
				projectionForZeroDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea",
								"tDefect","waferId")
						.and(ConditionalOperators.when(ComparisonOperators.Eq.valueOf("$xObj").equalToValue(0))
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d)))
								.otherwise("$xObj"))
						.as("area");

				projectionForNullDensity = Aggregation.project()
						.andInclude("_id", "xObj", "yObj", "diameter", "discardedArea", "tDefect","waferId")
						.and(ConditionalOperators.ifNull("$area")
								.then(new Document("$multiply",
										Arrays.asList("$diameter", "$diameter", 3.141592653589793d, 0.00025d))))
						.as("area");
				projectdiscardedarea = Aggregation.project()
						.and(ConditionalOperators
								.when(ComparisonOperators.Gt.valueOf("$discardedArea").greaterThanValue(0))
								.then(new Document("$subtract", Arrays.asList("$area", "$discardedArea")))
								.otherwise("$area"))
						.as("area").andInclude("tDefect","waferId").andInclude("yObj");

				addFieldsOperation = Aggregation.addFields().addFieldWithValue("defDensity",
						new Document("$round",
								Arrays.asList(new Document("$divide", Arrays.asList("$tDefect", "$area")), 16L)))
						.build();
				addFieldsOperationForAdder = Aggregation.addFields()
						.addFieldWithValue("product", new Document("$size", "$yObj")).build();

				if (measureName.equals("aDefect")) {
					if (edxflag == false) {
						if (flag = true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
									projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
									addFieldsOperation, addFieldsOperationForAdder, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
									projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
									addFieldsOperation, addFieldsOperationForAdder, projectionOperation1, FilterMinMax);
						}

					} else {
						if (flag = true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
									projectionForNullDensity, projectdiscardedarea, addFieldsOperation,
									addFieldsOperationForAdder, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
									projectionForNullDensity, projectdiscardedarea, addFieldsOperation,
									addFieldsOperationForAdder, projectionOperation1, FilterMinMax);
						}
					}

				} else {
					if (edxflag == false) {
						if (flag = true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
									projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
									addFieldsOperation, projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, addFieldsdiscardedarea, groupOperation,
									projectionForZeroDensity, projectionForNullDensity, projectdiscardedarea,
									addFieldsOperation, projectionOperation1, FilterMinMax);
						}
					} else {
						if (flag = true) {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
									projectionForNullDensity, projectdiscardedarea, addFieldsOperation,
									projectionOperation1);
						} else {
							aggregationMean = Aggregation.newAggregation(matchOperation, lookupOnWaferDefects,
									unwWindOperationonWaferdefects, crfMatch, lookupOnEdx, unwWindOperationonEdx,
									edxMatch, addFieldsdiscardedarea, groupOperation, projectionForZeroDensity,
									projectionForNullDensity, projectdiscardedarea, addFieldsOperation,
									projectionOperation1, FilterMinMax);
						}
					}
				}

				logger.info("Preparing aggregationMean scatterDetails" + aggregationMean);
				testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class)
						.getMappedResults();
			}

		} else {
			if (measureName.equals("aDefect")) {
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").push("$adder").as("yObj")
									.addToSet("waferId").as("waferId");
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf(new Document("$size", "$yObj")).as("yAxis").andExclude("_id")
						.and("$waferId").as("waferIds");

			} else if (measureName.equals("tDefect")) {
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").count().as("yObj")
						.addToSet("waferId").as("waferId");;
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf("$yObj").as("yAxis").andExclude("_id")
						.and("$waferId").as("waferIds");;

			} else {
				groupOperation = Aggregation.group(DimensionX).first(DimensionX).as("xObj").sum(DimensionY).as("yObj")
								.addToSet("waferId").as("waferId");;
				projectFetchData = Aggregation.project().and("_id." + group).as("group").andArrayOf("$xObj").as("xAxis")
						.andArrayOf("$yObj").as("yAxis").andExclude("_id")
						.and("$waferId").as("waferIds");;
			}

			if (edxflag == false) {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, groupOperation,
							projectFetchData, FilterMinMax);
				}
			} else {
				if (flag == true) {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData);
				} else {
					aggregationMean = Aggregation.newAggregation(matchOperation, crfMatch, lookupOnEdx,
							unwWindOperationonEdx, edxMatch, groupOperation, projectFetchData, FilterMinMax);
				}
			}

			logger.info("Aggregation Query for ScatterPlot " + aggregationMean);

			testResults = mongoTemplate.aggregate(aggregationMean, collectionName, Document.class).getMappedResults();
			if (testResults == null || testResults.isEmpty()) {
				return null;
			}
		}

		if (Aggregator.equalsIgnoreCase("Avg")) {
			int wSize = waferIds.size();
			for (Document document : testResults) {
				List DimensionArray = (List) document.get("yAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				double i = Double.parseDouble(Dimension);
				double total = i / wSize;
				if (minVal == null && maxVal == null) {
					document.put("yAxis", Arrays.asList(total));
				} else if (!(minVal == null) && maxVal == null) {
					if (total >= Double.parseDouble(minVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else if (!(maxVal == null) && minVal == null) {
					if (total <= Double.parseDouble(maxVal.toString())) {
						document.put("yAxis", Arrays.asList(total));
					} else {
						document.clear();
					}
				} else {
					if (total <= Double.parseDouble(maxVal.toString())
							&& total >= Double.parseDouble(minVal.toString())) {
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
	private List<Criteria> generateQueryForSelectionAreas1(List<Area> areas) {

		   List<Criteria> criteriaList = new ArrayList<Criteria>();

		   for (Area area : areas) {
			  Criteria criteria = Criteria.where("radius").lt(area.getRadius().getTo())
					.gt(area.getRadius().getFrom());
			  if (area.getAngle() != null) {
				 criteria = criteria.and("angle").lt(area.getAngle().getTo()).gt(area.getAngle().getFrom());
			  }
			  criteriaList.add(criteria);
		   }
		   return criteriaList;
	    }


	private String generateFieldNameForCriteria(String fieldName) {
		switch (fieldName) {
		case "D-Size":
			return "dSize";
		case "A-Size":
			return "defectArea";
		case "Sy":
			return "ySize";
		case "Sx":
			return "xSize";
		case "tDefect":
			return "tDefect";
		case "aDefect":
			return "adder";
		case "N-Die":
			return "waferInspectionSummary.nDie";
		case "D-Die":
			return "waferInspectionSummary.nDefDie";
		case "Area":
			return "waferInspectionSummary.testArea";
		case "Density":
			return "waferInspectionSummary.testArea";
		default:
			return null;
		}
	}
	
	private String generateFieldName(String fieldName) {
		switch (fieldName) {
		case "D-Size":
			return "dSize";
		case "A-Size":
			return "defectArea";
		case "Sy":
			return "ySize";
		case "Sx":
			return "xSize";
		case "tDefect":
			return "tDefect";
		case "aDefect":
			return "adder";
		case "N-Die":
			return "inspectedWaferData.waferInspectionSummary.nDie";
		case "D-Die":
			return "inspectedWaferData.waferInspectionSummary.nDefDie";
		case "Area":
			return "inspectedWaferData.waferInspectionSummary.testArea";
		case "Density":
			return "inspectedWaferData.waferInspectionSummary.testArea";
		default:
			return null;
		}
	}

	private String generateFieldNameForArea(String fieldName) {
		switch (fieldName) {
		case "D-Size":
			return "waferDefects.dSize";
		case "A-Size":
			return "waferDefects.defectArea";
		case "Sy":
			return "waferDefects.ySize";
		case "Sx":
			return "waferDefects.xSize";
		case "tDefect":
			return "tDefect";
		case "aDefect":
			return "waferDefects.adder";
		case "Area":
			return "waferInspectionSummary.testArea";
		case "Density":
			return "waferInspectionSummary.testArea";
		case "N-Die":
			return "waferInspectionSummary.nDie";
		case "D-Die":
			return "waferInspectionSummary.nDefDie";
		default:
			return null;
		}
	}
}
