package com.graph.service.wt;

import java.io.IOException;

import java.text.DecimalFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.constants.wt.AdvancedGraphConstants;
import com.graph.constants.wt.ExceptionGraphConstant;
import com.graph.model.wt.BoxPlotTestValueWaferIds;
import com.graph.model.wt.GraphCommonRequest;
import com.graph.model.wt.GraphCommonResponse;
import com.graph.model.wt.ComparisonHistogramTestValueRequest;
import com.graph.model.wt.ComparisonHistogramTestValueResponse;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.MESTestAvgMedRequest;
import com.graph.model.wt.Message;
import com.graph.model.wt.RegularHistoTestValue;
import com.graph.model.wt.RegularHistogramRequest;
import com.graph.model.wt.RegularHistogramResponse;
import com.graph.model.wt.ScatterPlotRequest;
import com.graph.model.wt.ScatterPlotResponse;
import com.graph.model.wt.SelectedTestDetails;
import com.graph.model.wt.TestStatus;
import com.graph.model.wt.TestStatusDetails;
import com.graph.model.wt.TestValueDetails;
import com.graph.model.wt.WaferDateTime;
import com.graph.model.wt.XaxisDetails;
import com.graph.model.wt.XaxisResponseDetails;
import com.graph.model.wt.XaxisSetting;
import com.graph.model.wt.YaxisDetails;
import com.graph.model.wt.YaxisResponseDetails;
import com.graph.model.wt.YaxisSetting;
import com.graph.model.wt.YaxisTestDetails;
import com.graph.model.wt.YaxisTestSetting;
import com.graph.model.wt.groupResponseDetails;
import com.graph.utils.DateUtil;
import com.graph.utils.CriteriaUtil;

@Service
public class AdvanceGraphService implements IGraphService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;
	@Value("${currentlocale}")
	private String currentLocale;

	@Autowired(required = true)
	private IAutographService autoGraphService;
	@Autowired
	private OneXGraphDetailsService oneXGraphDetails;
	@Autowired
	private TwoXGraphDetailsService twoXGraphDetails;
	@Autowired
	private RegularHistogramService regularHistogramService;
	@Autowired
	private ScatterPlotService scatterPlotService;
	@Autowired
	private ScatterPlotTestMeasureService scatterPlotTestMeasureService;
	@Autowired
	private TestValueService testValueService;
	@Autowired
	private WaferDateTimeService waferDateTimeService;

	@Value("${settings.path}")
	private String filePath;
	@Value("${settings.folder}")
	private String settingsFolder;
	@Value("${maximumLimitBoxPlot}")
	private String maximumLimitBox;

	private final static Logger logger = LoggerFactory.getLogger(AdvanceGraphService.class);

	private static String GROUP_DUT = "";

	public static final String MinMax = null;

	/*
	 * Listing ComparisionHistogramDetails
	 */
	@Override
	public GraphCommonResponse getCommonGraphDetails(GraphCommonRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		XaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();
		int XaxisCount = xaxis.getAxisCount();
		int YaxisCount = yaxis.getAxisCount();
		GraphCommonResponse res = null;
		if (XaxisCount == 1) {
			res = getOneXDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		} else {
			res = getTwoXDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		}
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);
			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private GraphCommonResponse getTwoXDetails(GraphCommonRequest request, XaxisDetails xaxis, YaxisDetails yaxis,
			int xaxisCount, int yaxisCount, List<FilterParam> filterParams) throws IOException {
		

		GraphCommonResponse res = new GraphCommonResponse();
		logger.info("Inside getComparisionHistogramDetails service ");
		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String granuality2X = dimensionDetails.get(1).getGranularity();
		String DimensionName2X = dimensionDetails.get(1).getName();

		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria(AdvancedGraphConstants.EMPTY);
		}
		if (MeasureName.equals(AdvancedGraphConstants.TEST_VALUE)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (group.equals(AdvancedGraphConstants.TEST)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO) && Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
				
		}
		if ((MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD))
				&& Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if (MeasureName.equals(AdvancedGraphConstants.FAIL_RATIO) && Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if (group.equals(DimensionName) && !group.equals(AdvancedGraphConstants.WAFER_NUMBER)
				|| group.equals(DimensionName2X) && !group.equals(AdvancedGraphConstants.WAFER_NUMBER)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info(ExceptionGraphConstant.PREPARING_QUERY_GETCOMP_HISTO);
		logger.info(ExceptionGraphConstant.PREPARING_QUERY_GETCOMP_HISTO);
		if (!group.equalsIgnoreCase("Bin")
				&& !(DimensionName.equals("DUT") || group.equals("DUT") || DimensionName2X.equals("DUT"))
				&& testName != null && !"".equals(testName) && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {

			testResults = twoXGraphDetails.getTwoXTestRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, granuality2X,
					DimensionName2X, filterParams);
		} else if ((granuality != null && !"".equals(granuality)) || (granuality2X != null && !"".equals(granuality2X))) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		} else {
			if (!group.equalsIgnoreCase("Bin")
					&& (DimensionName.equals("DUT") || group.equals("DUT") || DimensionName2X.equals("DUT"))) {
				GROUP_DUT = group;
				if (GROUP_DUT.equals("DUT")) {
					String dimension = DimensionName;
					String groupingVal = group;
					DimensionName = groupingVal;
					group = dimension;
				}
				testResults = twoXGraphDetails.getTwoXDUTService(waferIds, group,GROUP_DUT, DimensionName, MeasureName,
						selectedBin, minVal, maxVal, Aggregator, granuality2X, DimensionName2X, filterParams);
			} else if (group.equalsIgnoreCase("Bin")) {
				testResults = twoXGraphDetails.getGroupByBin2X(waferIds, MeasureName, group, DimensionName, testName,
						testNumber, DimensionName2X, minVal, maxVal, Aggregator, filterParams);
			} else {
				testResults = twoXGraphDetails.getTwoXCommonDimensionsAndMeasure(waferIds, DimensionName, MeasureName,
						group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, DimensionName2X,
						filterParams);
			}
		}
		List<ObjectId> listWafers = new ArrayList<>();

		List<String> waferIdList = new ArrayList<>();

		for (Document obj : testResults) {
			listWafers = (List<ObjectId>) obj.get(AdvancedGraphConstants.WAFER_ID);
			for (ObjectId id : listWafers) {
				waferIdList = new ArrayList<String>();
				waferIdList.add(id.toHexString());
				obj.put(AdvancedGraphConstants.WAFER_ID, waferIdList);
			}
		}
		if (!testResults.isEmpty()) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();

			res.setxAxisCount(xaxisCount);
			res.setyAxisCount(yaxisCount);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if (searchValues != null && !searchValues.isEmpty() && Boolean.TRUE.equals(isSearchValueExcluded)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && Boolean.TRUE.equals(!isSearchValueExcluded)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			}
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			List DimensionArray = (List) documentDimension.get("xAxis");
			String Dimension = String.valueOf(DimensionArray.get(0));
			String Dimension2X = String.valueOf(DimensionArray.get(1));
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XaxisResponseDetails XResDetails2X = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (granuality != null && !"".equals(granuality)) {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("Number");
				} else {
					XResDetails.setType("String");
				}
			}
			XResDetails2X.setAxisName(DimensionName2X);
			if (granuality != null && !"".equals(granuality)) {
				XResDetails2X.setType("DateTime");
			} else {
				if (isNumeric((Dimension2X))) {
					XResDetails2X.setType("Number");
				} else {
					XResDetails2X.setType("String");
				}
			}
			Dimens.add(XResDetails);
			Dimens.add(XResDetails2X);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			res.setGroupDetails(groupResDetails);
		} else {
			res.setData(new ArrayList<>());
		}

		return res;
	}

	private GraphCommonResponse getOneXDetails(GraphCommonRequest request, XaxisDetails xaxis, YaxisDetails yaxis,
			int XaxisCount, int YaxisCount, List<FilterParam> filterParams) throws IOException {
		

		GraphCommonResponse res = new GraphCommonResponse();
		logger.info("Inside getComparisonHistogramDetails service ");
		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		if (MeasureName.equals(AdvancedGraphConstants.TEST_VALUE)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
	
		if (MeasureName.equals("FailRatio")) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info(ExceptionGraphConstant.PREPARING_QUERY_GETCOMP_HISTO);
		if (DimensionName.equals("chipArea")) {
			if (!MeasureName.equals(AdvancedGraphConstants.YIELD)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		if (group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = oneXGraphDetails.getGroupByTestForComparison(waferIds, DimensionName, MeasureName, group,
					testName, testNumber, minVal, maxVal, Aggregator, selectedBin, false, filterParams);
		}

		else if (!group.equalsIgnoreCase("Bin") && !(DimensionName.equals("DUT") || group.equals("DUT"))
				&& testName != null && !"".equals(testName) && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = oneXGraphDetails.getTestRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
		} else if (granuality != null && !"".equals(granuality)) {
			if (group.equalsIgnoreCase("Bin") || group.equalsIgnoreCase("DUT") || DimensionName.equals("DUT")) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
			testResults = oneXGraphDetails.getDateTimeRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
		} else {
			if (!group.equalsIgnoreCase("Bin") && (DimensionName.equals("DUT") || group.equals("DUT"))) {
				testResults = oneXGraphDetails.getDUTService(waferIds, group, DimensionName, MeasureName, selectedBin,
						minVal, maxVal, Aggregator, filterParams);
			} else if (group.equalsIgnoreCase("Bin")) {
				testResults = oneXGraphDetails.getGroupByBin(waferIds, MeasureName, group, DimensionName, testName,
						testNumber, minVal, maxVal, Aggregator, filterParams);
			} else {
				testResults = oneXGraphDetails.getCommonDimensionsAndMeasure(waferIds, DimensionName, MeasureName,
						group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, filterParams);
			}
		}
		List<ObjectId> listWafers = new ArrayList<>();

		List<String> waferIdList = new ArrayList<>();

		for (Document obj : testResults) {
			listWafers = (List<ObjectId>) obj.get(AdvancedGraphConstants.WAFER_ID);
			for (ObjectId id : listWafers) {
				waferIdList = new ArrayList<String>();
				waferIdList.add(id.toHexString());
				obj.put(AdvancedGraphConstants.WAFER_ID, waferIdList);
			}
		}
		if (testResults.size() > 0) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			res.setxAxisCount(XaxisCount);
			res.setyAxisCount(YaxisCount);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if ((searchValues != null && !searchValues.isEmpty() && isSearchValueExcluded == null)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && Boolean.TRUE.equals(isSearchValueExcluded)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && !isSearchValueExcluded == true) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}
			}
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			List DimensionArray = (List) documentDimension.get("xAxis");

			String Dimension = String.valueOf(DimensionArray.get(0));
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (granuality != null && !"".equals(granuality)) {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("Number");
				} else {
					XResDetails.setType("String");
				}
			}
			Dimens.add(XResDetails);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			if (!"mesWafer._id".equals(group) || group.isEmpty()) {
				res.setGroupDetails(groupResDetails);
			}
		} else {
			res.setData(new ArrayList<>());
		}

		return res;
	}

	private static boolean isNumeric(String string) {
		int intValue;
		if (string == null || string.equals("")) {
			return false;
		}
		try {
			intValue = Integer.parseInt(string);
			return true;
		} catch (NumberFormatException e) {
			logger.info("Input String cannot be parsed to Integer");
		}
		return false;
	}

	/*
	 * List HistogramTestValueDetails
	 */
	@Override
	public ComparisonHistogramTestValueResponse getHistogramTestValueDetails(
			ComparisonHistogramTestValueRequest request) throws NoSuchFieldException, IOException {
		
		ComparisonHistogramTestValueResponse res = null;
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		XaxisDetails xaxis = request.getxAxis();
		YaxisTestDetails yaxis = request.getyAxis();
		int count = xaxis.getAxisCount();
		if (count == 1) {
			 res = getOneXTestValueDetails(request, xaxis, yaxis, filterParams);
			
		} else {
			 res = getTwoXTestValueDetails(request, xaxis, yaxis, filterParams);
			
		}
		
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);

			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private ComparisonHistogramTestValueResponse getTwoXTestValueDetails(ComparisonHistogramTestValueRequest request,
			XaxisDetails xaxis, YaxisTestDetails yaxis, List<FilterParam> filterParams) throws IOException {
		ComparisonHistogramTestValueResponse res = new ComparisonHistogramTestValueResponse();

		String CollectionName = null;
		Aggregation aggregationMean = null;
		logger.info("Inside getComparsionHistogramDetails service ");
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisTestSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String granuality2X = dimensionDetails.get(1).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();
		String Dimension2XName = dimensionDetails.get(1).getName();

		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		double classVal = measureDetails.get(0).getClassval();
		double scaleVal = measureDetails.get(0).getScaleval();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (DimensionName.equals("DUT") || Dimension2XName.equals("DUT")) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		 if (granuality2X != null && granuality2X != "" || granuality != null && granuality != "") {
	  		  throw new IOException("Selected combination is not supported");
	  	   }
		DecimalFormat df = new DecimalFormat("#.#");
		List<Criteria> criteriaList = new ArrayList<>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where(AdvancedGraphConstants.WAFER_ID).in(waferIds));

		}
		if (testNumber > 0) {
			Criteria testCriteria = (Criteria.where("TestBoundSummary.tn").is(testNumber));
			criteriaListForMinMax.add(testCriteria);
			Criteria testNameCriteria = (Criteria.where("TestBoundSummary.tm").is(testName));
			criteriaListForMinMax.add(testNameCriteria);
		}
		if (waferIds.length > 0) {
			aggregationMean = Aggregation
					.newAggregation(unwind -> new Document("$unwind", "$TestBoundSummary"),
							match -> new Document("$match", new Document("$and",
									Arrays.asList(new Document(AdvancedGraphConstants.WAFER_ID, new Document("$in", Arrays.asList(waferIds))),
											new Document("TestBoundSummary.tn", testNumber),
											new Document("TestBoundSummary.tm", testName)

									))),
							grouping -> new Document("$group", new Document("_id",
									new Document("min", "$TestBoundSummary.min").append("max", "$TestBoundSummary.max"))
											.append("minVal", new Document("$min", "$TestBoundSummary.min"))
											.append("maxVal", new Document("$max", "$TestBoundSummary.max")))

					).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		} else {
			aggregationMean = autoGraphService.getAutoCompTestMinMaxAgg(testName, testNumber, criteriaList);
		}
		List<Document> keyResultsMean = mongoTemplate.aggregate(aggregationMean, "MESBoundValues", Document.class)
				.getMappedResults();
		if (!keyResultsMean.isEmpty()) {
			List<Double> newMaxList = new ArrayList<>();
			List<Double> newMinList = new ArrayList<>();
			for (Document document : keyResultsMean) {
				double MaxValue = Double.parseDouble(document.get("maxVal").toString());
				double MinValue = Double.parseDouble(document.get("minVal").toString());
				newMaxList.add(MaxValue);
				newMinList.add(MinValue);
			}
			double min = Collections.min(newMinList);
			double max = Collections.max(newMaxList);
			maxVal = Math.ceil(max);

			if (minVal <= 0) {
				minVal = Math.floor(min);
			}

			if (classVal <= 0) {
				classVal = (((double) maxVal - (double) minVal) / 10);
			}
			if (scaleVal <= 0) {
				scaleVal = (((double) maxVal - (double) minVal) / 10);
			}
			maxVal = (maxVal + classVal);
			List<Document> GroupingtestValues = testValueService.gettingTestValueFromCollectionFor2X(testNumber,
					testName, waferIds, classVal, maxVal, df, minVal, DimensionName, group, Dimension2XName,
					filterParams);
			GroupingtestValues = testValueService.GroupTestValueBasedOnYAxis(GroupingtestValues);

			if (GroupingtestValues.size() > 0) {
				if (waferIds.length == 0) {
					WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(GroupingtestValues);
					res.setWaferDateTime(waferDateTime);
				}
				res.setxAxisCount(2);
				res.setyAxisCount(1);
				List<?> searchValues = request.getGrouping().getSearchValues();
				Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
				if (searchValues != null && searchValues.size() > 0 && isSearchValueExcluded) {
					if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
								.collect(Collectors.toList());
					} else {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
										.substring(0, groupItem.get("group").toString().indexOf("_"))))
								.collect(Collectors.toList());
					}

				} else if (searchValues != null && searchValues.size() > 0 && !isSearchValueExcluded) {
					if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
								.collect(Collectors.toList());
					} else {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
										.substring(0, groupItem.get("group").toString().indexOf("_"))))
								.collect(Collectors.toList());
					}

				}
				res.setData(GroupingtestValues);
				XaxisResponseDetails XResDetails = new XaxisResponseDetails();
				XaxisResponseDetails XResDetails2X = new XaxisResponseDetails();
				XResDetails.setAxisName(DimensionName);
				XResDetails2X.setAxisName(Dimension2XName);
				Document documentDimension = GroupingtestValues.get(0);
				List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
				List DimensionArray = (List) documentDimension.get("xAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				String Dimension2X = String.valueOf(DimensionArray.get(1));
				if (granuality != null && !"".equals(granuality)) {
					XResDetails.setType("DateTime");
				} else {
					if (isNumeric(Dimension)) {
						XResDetails.setType("Number");
					} else {
						XResDetails.setType("String");
					}
				}
				Dimens.add(XResDetails);

				if (granuality != null && !"".equals(granuality)) {
					XResDetails2X.setType("DateTime");
				} else {
					if (isNumeric(Dimension2X)) {
						XResDetails2X.setType("Number");
					} else {
						XResDetails2X.setType("String");
					}
				}
				Dimens.add(XResDetails2X);

				res.setxAxisDetails(Dimens);
				YaxisResponseDetails yResDetails = new YaxisResponseDetails();
				yResDetails.setAxisName(MeasureName);
				yResDetails.setType("Object");
				res.setyAxisDetails(yResDetails);
				groupResponseDetails groupResDetails = new groupResponseDetails();
				groupResDetails.setAxisName(group);
				if (!"mesWafer._id".equals(group) || group.isEmpty()) {
					res.setGroupDetails(groupResDetails);
				}
				res.setClassValue(classVal);
				res.setMaxValue(maxVal);
				res.setScaleValue(scaleVal);
				res.setMinValue(minVal);
			}
		} else {
			res.setData(new ArrayList<>());
		}
		return res;
	}

	private ComparisonHistogramTestValueResponse getOneXTestValueDetails(ComparisonHistogramTestValueRequest request,
			XaxisDetails xaxis, YaxisTestDetails yaxis, List<FilterParam> filterParams) throws IOException {
		ComparisonHistogramTestValueResponse res = new ComparisonHistogramTestValueResponse();

		logger.info("Inside getComparisonHistogramDetails service ");
		String CollectionName = null;
		Aggregation aggregationMean = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisTestSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		double classVal = measureDetails.get(0).getClassval();
		double scaleVal = measureDetails.get(0).getScaleval();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (DimensionName.equals("DUT") || DimensionName.equals("chipArea")) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		DecimalFormat df = new DecimalFormat("#.#");
		List<Criteria> criteriaList = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where(AdvancedGraphConstants.WAFER_ID).in(waferIds));

		}

		if (testNumber > 0) {
			Criteria testCriteria = (Criteria.where("TestBoundSummary.tn").is(testNumber));
			criteriaListForMinMax.add(testCriteria);
			Criteria testNameCriteria = (Criteria.where("TestBoundSummary.tm").is(testName));
			criteriaListForMinMax.add(testNameCriteria);
		}

		if (waferIds.length > 0) {
			aggregationMean = Aggregation
					.newAggregation(unwind -> new Document("$unwind", "$TestBoundSummary"),
							match -> new Document("$match", new Document("$and",
									Arrays.asList(new Document(AdvancedGraphConstants.WAFER_ID, new Document("$in", Arrays.asList(waferIds))),
											new Document("TestBoundSummary.tn", testNumber),
											new Document("TestBoundSummary.tm", testName)

									))),
							grouping -> new Document("$group", new Document("_id",
									new Document("min", "$TestBoundSummary.min").append("max", "$TestBoundSummary.max"))
											.append("minVal", new Document("$min", "$TestBoundSummary.min"))
											.append("maxVal", new Document("$max", "$TestBoundSummary.max")))

					).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());

		} else {
			aggregationMean = autoGraphService.getAutoCompTestMinMaxAgg(testName, testNumber, criteriaList);
		}

		List<Document> keyResultsMean = mongoTemplate.aggregate(aggregationMean, "MESBoundValues", Document.class)
				.getMappedResults();

		if (!keyResultsMean.isEmpty()) {
			List<Double> newMaxList = new ArrayList<>();
			List<Double> newMinList = new ArrayList<>();
			for (Document document : keyResultsMean) {
				double MaxValue = Double.parseDouble(document.get("maxVal").toString());
				double MinValue = Double.parseDouble(document.get("minVal").toString());
				newMaxList.add(MaxValue);
				newMinList.add(MinValue);
			}
			double min = Collections.min(newMinList);
			double max = Collections.max(newMaxList);
			maxVal = Math.ceil(max);

			if (minVal <= 0) {
				minVal = Math.floor(min);
			}

			if (classVal <= 0) {
				classVal = (((double) maxVal - (double) minVal) / 10);
			}
			if (scaleVal <= 0) {
				scaleVal = (((double) maxVal - (double) minVal) / 10);
			}
			maxVal = (maxVal + classVal);
			List<Document> GroupingtestValues = testValueService.gettingTestValueFromCollection(testNumber, testName,
					waferIds, classVal, maxVal, df, minVal, DimensionName, group, granuality, filterParams);
			GroupingtestValues = testValueService.GroupTestValueBasedOnYAxis(GroupingtestValues);

			if ((!GroupingtestValues.isEmpty()) && GroupingtestValues.size() > 0) {
				if (waferIds.length == 0) {
					WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(GroupingtestValues);
					res.setWaferDateTime(waferDateTime);
				}
				res.setxAxisCount(1);
				res.setyAxisCount(1);
				List<?> searchValues = request.getGrouping().getSearchValues();
				Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
				if (searchValues != null && searchValues.size() > 0 && isSearchValueExcluded) {
					if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
								.collect(Collectors.toList());
					} else {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
										.substring(0, groupItem.get("group").toString().indexOf("_"))))
								.collect(Collectors.toList());
					}

				} else if (searchValues != null && searchValues.size() > 0 && !isSearchValueExcluded) {
					if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
								.collect(Collectors.toList());
					} else {
						GroupingtestValues = GroupingtestValues.stream()
								.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
										.substring(0, groupItem.get("group").toString().indexOf("_"))))
								.collect(Collectors.toList());
					}

				}
				res.setData(GroupingtestValues);
				XaxisResponseDetails XResDetails = new XaxisResponseDetails();
				XResDetails.setAxisName(DimensionName);
				Document documentDimension = GroupingtestValues.get(0);
				List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();

				List DimensionArray = (List) documentDimension.get("xAxis");
				String Dimension = String.valueOf(DimensionArray.get(0));
				if (granuality != null && !"".equals(granuality)) {
					XResDetails.setType("DateTime");
				} else {
					if (isNumeric(Dimension)) {
						XResDetails.setType("Number");
					} else {
						XResDetails.setType("String");
					}
				}
				Dimens.add(XResDetails);
				res.setxAxisDetails(Dimens);
				YaxisResponseDetails yResDetails = new YaxisResponseDetails();
				yResDetails.setAxisName(MeasureName);
				yResDetails.setType("Object");
				res.setyAxisDetails(yResDetails);
				groupResponseDetails groupResDetails = new groupResponseDetails();
				groupResDetails.setAxisName(group);
				if (!"mesWafer._id".equals(group) || group.isEmpty()) {
					res.setGroupDetails(groupResDetails);
				}
				res.setClassValue(classVal);
				res.setMaxValue(maxVal);
				res.setScaleValue(scaleVal);
				res.setMinValue(minVal);
			} else {
				res.setData(new ArrayList<>());
			}
		} else {
			res.setData(new ArrayList<>());
		}
		return res;
	}

	/*
	 * List scatterPlotDetails
	 */
	@Override
	public ScatterPlotResponse getScatterPlotDetails(ScatterPlotRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}

		YaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();

		ScatterPlotResponse res = new ScatterPlotResponse();

		logger.info("Inside scatterPlotDetails service ");

		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<YaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String DimensionName = dimensionDetails.get(0).getName();
		int dimensionselectedBin = dimensionDetails.get(0).getSelectedBin();
		SelectedTestDetails testDimension = dimensionDetails.get(0).getSelectedTest();
		String dimensionTestName = testDimension.getTestName();
		int dimensionTestNumber = testDimension.getTestNumber();
		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		double min = dimensionDetails.get(0).getMin();
		double max = dimensionDetails.get(0).getMax();
		int measureselectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info("Preparing Query scatterPlotDetails service ");
		if (DimensionName.equalsIgnoreCase("DUT")) {
			throw new IOException("Selected Combination is not supported.");
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}

		if (MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals("FailRatio")) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST) && !group.equalsIgnoreCase("DUT") && !group.equalsIgnoreCase("Bin")
				&& ((testName != null && !"".equals(testName)) || ((dimensionTestName != null && !"".equals(dimensionTestName))))) {
			testResults = scatterPlotTestMeasureService.getTestRelatedMeasureForScatter(waferIds, DimensionName, MeasureName,
					group, testName, testNumber, dimensionTestName, dimensionTestNumber, minVal, maxVal, Aggregator,
					dimensionselectedBin, measureselectedBin, min, max, filterParams);

		} else if (group.equalsIgnoreCase("Bin")) {

			testResults = scatterPlotService.getGroupByBinForScatter(waferIds, DimensionName, MeasureName, minVal,
					maxVal, Aggregator, min, max, filterParams);
		} else if (group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {

			testResults = scatterPlotTestMeasureService.getGroupByTestdMeasureForScatter(waferIds, DimensionName, MeasureName,
					group, testName, testNumber, dimensionTestName, dimensionTestNumber, minVal, maxVal, Aggregator,
					dimensionselectedBin, measureselectedBin, min, max, filterParams);
		}

		else {
			if (group.equalsIgnoreCase("DUT")) {
				testResults = scatterPlotService.getGroupByDUTForScatter(waferIds, DimensionName, MeasureName, group,
						testName, testNumber, dimensionTestName, dimensionTestNumber, minVal, maxVal, Aggregator,
						dimensionselectedBin, measureselectedBin, min, max, filterParams);

			} else {

				testResults = scatterPlotService.getCommonDimensionsAndMeasureForScatter(waferIds, DimensionName,
						MeasureName, group, measureselectedBin, dimensionselectedBin, testName, testNumber, minVal,
						maxVal, Aggregator, min, max, filterParams);
			}

		}
		if (testResults.size() > 0) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			res.setxAxisCount(1);
			res.setyAxisCount(1);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if ((searchValues != null && searchValues.size() > 0 && isSearchValueExcluded == null)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && searchValues.size() > 0 && isSearchValueExcluded == true) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && searchValues.size() > 0 && !isSearchValueExcluded == true) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}
			}
			res.setData(testResults);
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			XResDetails.setType("Number");
			res.setxAxisDetails(XResDetails);
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
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);

			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	@Override
	public GraphCommonResponse getComparisionHistogramDetails(GraphCommonRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		ArrayList<String> list = new ArrayList<String>();

		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		XaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();
		int XaxisCount = xaxis.getAxisCount();
		int YaxisCount = yaxis.getAxisCount();
		GraphCommonResponse res = null;
		if (XaxisCount == 1) {
			res = getOneXHistoDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		} else {
			res = getTwoXDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		}
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);

			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private GraphCommonResponse getOneXHistoDetails(GraphCommonRequest request, XaxisDetails xaxis, YaxisDetails yaxis,
			int XaxisCount, int YaxisCount, List<FilterParam> filterParams) throws IOException {
		

		GraphCommonResponse res = new GraphCommonResponse();
		logger.info("Inside getComparisonHistogramDetails service ");
		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		if (MeasureName.equals(AdvancedGraphConstants.TEST_VALUE)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals("FailRatio")) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info(ExceptionGraphConstant.PREPARING_QUERY_GETCOMP_HISTO);
		if (DimensionName.equals("chipArea")) {
			if (!MeasureName.equals(AdvancedGraphConstants.YIELD)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		if (group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = oneXGraphDetails.getGroupByTestForComparison(waferIds, DimensionName, MeasureName, group,
					testName, testNumber, minVal, maxVal, Aggregator, selectedBin, false, filterParams);
		}

		else if (!group.equalsIgnoreCase("Bin") && !(DimensionName.equals("DUT") || group.equals("DUT"))
				&& testName != null && !"".equals(testName) && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = oneXGraphDetails.getTestRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
		} else if (granuality != null && !"".equals(granuality)) {
			if (group.equalsIgnoreCase("Bin") || group.equalsIgnoreCase("DUT") || DimensionName.equals("DUT")) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
			testResults = oneXGraphDetails.getDateTimeRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
		} else {
			if (!group.equalsIgnoreCase("Bin") && (DimensionName.equals("DUT") || group.equals("DUT"))) {
				testResults = oneXGraphDetails.getDUTService(waferIds, group, DimensionName, MeasureName, selectedBin,
						minVal, maxVal, Aggregator, filterParams);
			} else if (group.equalsIgnoreCase("Bin")) {
				testResults = oneXGraphDetails.getGroupByBin(waferIds, MeasureName, group, DimensionName, testName,
						testNumber, minVal, maxVal, Aggregator, filterParams);
			} else {
				testResults = oneXGraphDetails.getCommonDimensionsAndMeasureHisto(waferIds, DimensionName, MeasureName,
						group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, filterParams);
			}
		}

		List<ObjectId> listWafers = new ArrayList<>();
		List<String> waferIdList = new ArrayList<>();

		for (Document obj : testResults) {
			listWafers = (List<ObjectId>) obj.get(AdvancedGraphConstants.WAFER_ID);
			for (ObjectId id : listWafers) {
				waferIdList = new ArrayList<String>();
				waferIdList.add(id.toHexString());
				obj.put(AdvancedGraphConstants.WAFER_ID, waferIdList);
			}
		}

		if (testResults.size() > 0) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			res.setxAxisCount(XaxisCount);
			res.setyAxisCount(YaxisCount);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if ((searchValues != null && searchValues.size() > 0 && isSearchValueExcluded == null)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && searchValues.size() > 0 && isSearchValueExcluded == true) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && searchValues.size() > 0 && !isSearchValueExcluded == true) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}
			}

			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			List DimensionArray = (List) documentDimension.get("xAxis");
			String Dimension = String.valueOf(DimensionArray.get(0));
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (granuality != null && granuality != "") {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("Number");
				} else {
					XResDetails.setType("String");
				}
			}
			Dimens.add(XResDetails);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			if (group != "mesWafer._id" || group.isEmpty()) {
				res.setGroupDetails(groupResDetails);
			}
		} else {
			res.setData(new ArrayList<>());
		}

		return res;
	}

	@Override
	public DataListModel getMESTestValuesAvgMedService(MESTestAvgMedRequest request) {
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}

		DataListModel results = null;
		String[] waferID = request.getWaferID();
		int testNumber = request.getTestNumber();
		String testName = request.getTestName();
		String dimension1X = request.getDimension();
		String dimension2X = request.getDimension2X();
		String yearMonth = request.getGranularity();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}

		double minVal = request.getMin();
		double maxVal = request.getMax();

		if (dimension1X != null && dimension2X != null) {
			results = testValueService.getAvgMedian2X(waferIds, testNumber, testName, dimension1X, dimension2X,
					yearMonth, minVal, maxVal, filterParams);
		} else {
			results = testValueService.getAvgMedian1X(waferIds, testNumber, testName, dimension1X, yearMonth, minVal,
					maxVal, filterParams);
		}
		return results;
	}

	@Override
	public RegularHistogramResponse getRegularHistoDetails(RegularHistogramRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}

		YaxisTestDetails xaxis = request.getxAxis();
		int XaxisCount = xaxis.getAxisCount();
		RegularHistogramResponse res = null;
		if (XaxisCount == 1) {
			res = getOneXRegularHistoDetails(request, xaxis, XaxisCount, filterParams);
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);
			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private RegularHistogramResponse getOneXRegularHistoDetails(RegularHistogramRequest request, YaxisTestDetails xaxis,
			int xaxisCount, List<FilterParam> filterParams) throws IOException {
		YaxisTestDetails yaxis = request.getxAxis();

		RegularHistogramResponse res = new RegularHistogramResponse();

		logger.info("Inside scatterPlotDetails service");

		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<?> searchValues = request.getGrouping().getSearchValues();
		Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();

		List<YaxisTestSetting> dimensionDetails = xaxis.getSettings();
		String DimensionName = dimensionDetails.get(0).getName();
		int dimensionselectedBin = dimensionDetails.get(0).getSelectedBin();
		SelectedTestDetails testDimension = dimensionDetails.get(0).getSelectedTest();
		String dimensionTestName = testDimension.getTestName();
		int dimensionTestNumber = testDimension.getTestNumber();
		double min = dimensionDetails.get(0).getMin();
		String aggregator = dimensionDetails.get(0).getAggregator();
		double max = dimensionDetails.get(0).getMax();
		double scale = dimensionDetails.get(0).getScaleval();
		double classVal = dimensionDetails.get(0).getClassval();
		if (isSearchValueExcluded == null) {
			isSearchValueExcluded = false;
		}
		if (group == null) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		}
		if (group.equals("deviceGroup")) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}

		logger.info("Preparing Query scatterPlotDetails service ");

		if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST) && !group.equalsIgnoreCase("Bin")
				&& !(DimensionName.equals("DUT") || group.equals("DUT")) && dimensionTestName != null
				&& dimensionTestName != "" && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = regularHistogramService.getTestRelatedMeasureForRegularHisto(waferIds, DimensionName, group,
					dimensionselectedBin, dimensionTestName, dimensionTestNumber, min, max, scale, classVal, aggregator,
					filterParams);

		} else if (group.equals("DUT")) {
			testResults = regularHistogramService.getDutServiceForRegularHisto(waferIds, DimensionName, group,
					dimensionselectedBin, min, max, scale, classVal, aggregator, searchValues, isSearchValueExcluded,
					filterParams);
		} else if (group.equals("Bin")) {
			testResults = regularHistogramService.getBinForRegularHisto(waferIds, DimensionName, group,
					dimensionselectedBin, min, max, scale, classVal, aggregator, searchValues, isSearchValueExcluded,
					dimensionTestName, dimensionTestNumber, filterParams);
		} else if (group.equals(AdvancedGraphConstants.TEST)) {
			testResults = regularHistogramService.getTestServiceForRegularHisto(waferIds, DimensionName, group,
					dimensionselectedBin, min, max, scale, classVal, aggregator, searchValues, isSearchValueExcluded,
					filterParams);
		} else {
			testResults = regularHistogramService.getCommonDimensionsAndMeasureForRegularHisto(waferIds, DimensionName,
					group, dimensionselectedBin, min, max, scale, classVal, aggregator, filterParams);
		}
		if (testResults.size() > 0) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			res.setxAxisCount(xaxisCount);
			res.setData(testResults);
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			XResDetails.setType("Number");
			Dimens.add(XResDetails);
			res.setxAxisDetails(Dimens);

		} else {
			res.setData(new ArrayList<>());
		}

		return res;

	}

	@Override
	public GraphCommonResponse getLineGraphDetails(GraphCommonRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		XaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();
		int XaxisCount = xaxis.getAxisCount();
		int YaxisCount = yaxis.getAxisCount();
		GraphCommonResponse res = null;
		if (XaxisCount == 1) {
			res = getOneXLineDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		} else {
			res = getTwoXLineDetails(request, xaxis, yaxis, XaxisCount, YaxisCount, filterParams);
		}
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);
			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private GraphCommonResponse getOneXLineDetails(GraphCommonRequest request, XaxisDetails xaxis, YaxisDetails yaxis,
			int XaxisCount, int YaxisCount, List<FilterParam> filterParams) throws IOException {
		

		GraphCommonResponse res = new GraphCommonResponse();
		logger.info("Inside getComparisonHistogramDetails service");
		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();

		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info(ExceptionGraphConstant.PREPARING_QUERY_GETCOMP_HISTO);
		if (DimensionName.equals("chipArea") && !MeasureName.equals(AdvancedGraphConstants.YIELD)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO) && Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if ((MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD)) && Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if (MeasureName.equals("FailRatio") && Aggregator.equals(AdvancedGraphConstants.SUM)) {
			
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			
		}
		if (group == null || group.isEmpty()) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			testResults = oneXGraphDetails.getGroupByTestForComparison(waferIds, DimensionName, MeasureName, group,
					testName, testNumber, minVal, maxVal, Aggregator, selectedBin, false, filterParams);
		}

		else if (!group.equalsIgnoreCase("Bin") && !(DimensionName.equals("DUT") || group.equals("DUT"))
				&& testName != null && testName != "" && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			if (MeasureName.equals(AdvancedGraphConstants.TEST_VALUE)) {

				testResults = testValueService.getTestValueMeasure(waferIds, DimensionName, MeasureName, group,
						selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
			} else {
				testResults = oneXGraphDetails.getTestRelatedMeasure(waferIds, DimensionName, MeasureName, group,
						selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
			}
		} else if (granuality != null && !"".equals(granuality)) {
			if (group.equalsIgnoreCase("Bin") || group.equalsIgnoreCase("DUT") || DimensionName.equals("DUT")) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
			testResults = oneXGraphDetails.getDateTimeRelatedMeasure(waferIds, DimensionName, MeasureName, group,
					selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, filterParams);
		} else {
			if (!group.equalsIgnoreCase("Bin") && (DimensionName.equals("DUT") || group.equals("DUT"))) {

				if (group.equals("DUT")) {
					if (!(DimensionName.equals("LotId") || DimensionName.equals("OriginalLotID")
							|| DimensionName.equals(AdvancedGraphConstants.WAFER_NUMBER) || DimensionName.equals("Device"))) {
						throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
					}
				}
				testResults = oneXGraphDetails.getDUTService(waferIds, group, DimensionName, MeasureName, selectedBin,
						minVal, maxVal, Aggregator, filterParams);
			} else if (group.equalsIgnoreCase("Bin")) {
				if (!(DimensionName.equals("LotId") || DimensionName.equals("OriginalLotID")
						|| DimensionName.equals(AdvancedGraphConstants.WAFER_NUMBER) || DimensionName.equals("Device"))) {
					throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
				}
				testResults = oneXGraphDetails.getGroupByBin(waferIds, MeasureName, group, DimensionName, testName,
						testNumber, minVal, maxVal, Aggregator, filterParams);
			} else {
				testResults = oneXGraphDetails.getCommonDimensionsAndMeasure(waferIds, DimensionName, MeasureName,
						group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, filterParams);
			}
		}
		if (!testResults.isEmpty()) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			res.setxAxisCount(XaxisCount);
			res.setyAxisCount(YaxisCount);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if ((searchValues != null && !searchValues.isEmpty() && isSearchValueExcluded == null)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && isSearchValueExcluded) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && !isSearchValueExcluded.equals(true)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}
			}
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			List DimensionArray = (List) documentDimension.get("xAxis");
			String Dimension = String.valueOf(DimensionArray.get(0));
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (granuality != null && granuality != "") {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("Number");
				} else {
					XResDetails.setType("String");
				}
			}
			Dimens.add(XResDetails);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			if (group != "mesWafer._id" || group.isEmpty()) {
				res.setGroupDetails(groupResDetails);
			}
		} else {
			res.setData(new ArrayList<>());
		}

		return res;
	}

	private GraphCommonResponse getTwoXLineDetails(GraphCommonRequest request, XaxisDetails xaxis, YaxisDetails yaxis,
			int xaxisCount, int yaxisCount, List<FilterParam> filterParams) throws IOException {
		

		GraphCommonResponse res = new GraphCommonResponse();
		logger.info("Inside getComparisonHistogramDetails service");
		List<Document> testResults = null;
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();

		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String granuality2X = dimensionDetails.get(1).getGranularity();
		String DimensionName2X = dimensionDetails.get(1).getName();

		String Aggregator = measureDetails.get(0).getAggregator();
		String MeasureName = measureDetails.get(0).getName();
		if (group == null || group.isEmpty()) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (group.equals(AdvancedGraphConstants.TEST)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (MeasureName.equals(AdvancedGraphConstants.BIN_RATIO)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals(AdvancedGraphConstants.YIELD) || MeasureName.equals(AdvancedGraphConstants.LASTYIELD)) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (MeasureName.equals("FailRatio")) {
			if (Aggregator.equals(AdvancedGraphConstants.SUM)) {
				throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
			}
		}
		if (group.equals(DimensionName) && !group.equals(AdvancedGraphConstants.WAFER_NUMBER)
				|| group.equals(DimensionName2X) && !group.equals(AdvancedGraphConstants.WAFER_NUMBER)) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		int selectedBin = measureDetails.get(0).getSelectedBin();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		logger.info("Preparing Query getComparisonHistogramDetails service ");

		if (!group.equalsIgnoreCase("Bin")
				&& !(DimensionName.equals("DUT") || group.equals("DUT") || DimensionName2X.equals("DUT"))
				&& testName != null && testName != "" && !group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
			if (MeasureName.equals(AdvancedGraphConstants.TEST_VALUE)) {

				testResults = testValueService.getTestValueMeasure2X(waferIds, DimensionName, MeasureName, group,
						selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, granuality2X,
						DimensionName2X, filterParams);
			} else {
				testResults = twoXGraphDetails.getTwoXTestRelatedMeasure(waferIds, DimensionName, MeasureName, group,
						selectedBin, testName, testNumber, minVal, maxVal, Aggregator, granuality, granuality2X,
						DimensionName2X, filterParams);
			}
		} else if ((granuality != null && !"".equals(granuality)) || (granuality2X != null && !"".equals(granuality2X))) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		} else {
			if (!group.equalsIgnoreCase("Bin")
					&& (DimensionName.equals("DUT") || group.equals("DUT") || DimensionName2X.equals("DUT"))) {
				GROUP_DUT = group;
				if (GROUP_DUT.equals("DUT")) {
					String dimension = DimensionName;
					String groupingVal = group;
					DimensionName = groupingVal;
					group = dimension;
				}
				testResults = twoXGraphDetails.getTwoXDUTService(waferIds, group,GROUP_DUT, DimensionName, MeasureName,
						selectedBin, minVal, maxVal, Aggregator, granuality2X, DimensionName2X, filterParams);
			} else if (group.equalsIgnoreCase("Bin")) {
				testResults = twoXGraphDetails.getGroupByBin2X(waferIds, MeasureName, group, DimensionName, testName,
						testNumber, DimensionName2X, minVal, maxVal, Aggregator, filterParams);
			} else {
				testResults = twoXGraphDetails.getTwoXCommonDimensionsAndMeasure(waferIds, DimensionName, MeasureName,
						group, selectedBin, testName, testNumber, minVal, maxVal, Aggregator, DimensionName2X,
						filterParams);
			}
		}
		if (!testResults.isEmpty()) {
			if (waferIds.length == 0) {
				WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(testResults);
				res.setWaferDateTime(waferDateTime);
			}
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();

			res.setxAxisCount(xaxisCount);
			res.setyAxisCount(yaxisCount);
			List<?> searchValues = request.getGrouping().getSearchValues();
			Boolean isSearchValueExcluded = request.getGrouping().getIsSearchValueExcluded();
			if (searchValues != null && !searchValues.isEmpty() && Boolean.TRUE.equals(isSearchValueExcluded)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> !searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			} else if (searchValues != null && !searchValues.isEmpty() && Boolean.TRUE.equals(!isSearchValueExcluded)) {
				if (!group.equalsIgnoreCase(AdvancedGraphConstants.TEST)) {
					testResults = testResults.stream()
							.filter(groupItem -> searchValues.contains(groupItem.get("group").toString()))
							.collect(Collectors.toList());
				} else {
					testResults = testResults
							.stream().filter(groupItem -> searchValues.contains(groupItem.get("group").toString()
									.substring(0, groupItem.get("group").toString().indexOf("_"))))
							.collect(Collectors.toList());
				}

			}
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			List DimensionArray = (List) documentDimension.get("xAxis");
			String Dimension = String.valueOf(DimensionArray.get(0));
			String Dimension2X = String.valueOf(DimensionArray.get(1));
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XaxisResponseDetails XResDetails2X = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);
			if (granuality != null && granuality != "") {
				XResDetails.setType("DateTime");
			} else {
				if (isNumeric((Dimension))) {
					XResDetails.setType("Number");
				} else {
					XResDetails.setType("String");
				}
			}
			XResDetails2X.setAxisName(DimensionName2X);
			if (granuality != null && granuality != "") {
				XResDetails2X.setType("DateTime");
			} else {
				if (isNumeric((Dimension2X))) {
					XResDetails2X.setType("Number");
				} else {
					XResDetails2X.setType("String");
				}
			}
			Dimens.add(XResDetails);
			Dimens.add(XResDetails2X);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			res.setGroupDetails(groupResDetails);
		} else {
			res.setData(new ArrayList<>());
		}

		return res;
	}

	@Override
	public RegularHistoTestValue getRegularHistoTestValueDetails(RegularHistogramRequest request) throws IOException {
		List<FilterParam> filterParams = null;
		RegularHistoTestValue res = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		YaxisTestDetails xaxis = request.getxAxis();
		int count = xaxis.getAxisCount();
		if (count == 1) {
			res = getOneXRegularTestValueDetails(request, xaxis, filterParams);

		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}

		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);

			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private RegularHistoTestValue getOneXRegularTestValueDetails(RegularHistogramRequest request,
			YaxisTestDetails xaxis, List<FilterParam> filterParams) throws IOException {
		RegularHistoTestValue res = new RegularHistoTestValue();

		logger.info("Inside getComparsionHistogramDetails service ");
		String[] waferID = request.getWaferID();
		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<YaxisTestSetting> measureDetails = xaxis.getSettings();

		String CollectionName = null;
		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		double classVal = measureDetails.get(0).getClassval();
		double scaleVal = measureDetails.get(0).getScaleval();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}

		DecimalFormat df = new DecimalFormat("#.#");
		List<Criteria> criteriaList = new ArrayList<>();
		List<Criteria> criteriaListforwafers = new ArrayList<>();
		List<Criteria> criteriaListForMinMax = new ArrayList<>();
		if (waferIds.length == 0) {
			if (filterParams != null && filterParams.size() >= 1) {
				criteriaList = (CriteriaUtil.generateCriteriaForFileds(filterParams, CollectionName));
			}
		} else {
			criteriaList.add(Criteria.where("wd").in(waferIds));

		}
		if (testNumber > 0) {
			Criteria testCriteria = (Criteria.where("tn").is(testNumber));
			// criteriaList.add(testCriteria);
			Criteria testNameCriteria = (Criteria.where("tm").is(testName));
			// criteriaList.add(testNameCriteria);
			criteriaListForMinMax.add(testCriteria);
			criteriaListForMinMax.add(testNameCriteria);
		}
		Aggregation aggregationMean = null;
		MatchOperation matchForMinMax = Aggregation.match(
				new Criteria().andOperator(criteriaListForMinMax.toArray(new Criteria[criteriaListForMinMax.size()])));

		LookupOperation lookupProductOperation = LookupOperation.newLookup().from("ProductInfo")
				.localField("mesWafer.productId").foreignField("_id").as("ProductInfo");
		UnwindOperation unwindProductInfo = Aggregation.unwind("$ProductInfo", false);
		MatchOperation match = Aggregation
				.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
		GroupOperation groupOperation = Aggregation.group().min("$mv").as("minVal").max("$mv").as("maxVal");
		ProjectionOperation projectFetchData = Aggregation.project().and("$mesWafer._id").as("waferId")
				.andExclude("_id");

		if (waferIds.length == 0) {
			List<Criteria> resultWafer = autoGraphService.getWafersFromQueryCriteria(criteriaListforwafers,
					lookupProductOperation, unwindProductInfo, match, projectFetchData);

			MatchOperation match1 = Aggregation
					.match(new Criteria().andOperator(resultWafer.toArray(new Criteria[resultWafer.size()])));
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match1, groupOperation);

		} else {
			aggregationMean = Aggregation.newAggregation(matchForMinMax, match, groupOperation);
		}
		List<Document> keyResultsMean = mongoTemplate.aggregate(aggregationMean, "TestResults", Document.class)
				.getMappedResults();
		if (!keyResultsMean.isEmpty()) {
			List<Double> newMaxList = new ArrayList<>();
			List<Double> newMinList = new ArrayList<>();
			for (Document document : keyResultsMean) {
				double MaxValue = Double.parseDouble(document.get("maxVal").toString());
				double MinValue = Double.parseDouble(document.get("minVal").toString());
				newMaxList.add(MaxValue);
				newMinList.add(MinValue);
			}
			double min = Collections.min(newMinList);
			double max = Collections.max(newMaxList);

			if (maxVal <= 0) {
				maxVal = Math.ceil(max);
				maxVal = (maxVal + classVal);
			}
			if (minVal <= 0) {
				minVal = Math.floor(min);
			}

			if (classVal <= 0) {
				classVal = (((double) maxVal - (double) minVal) / 10);
			}
			if (scaleVal <= 0) {
				scaleVal = (((double) maxVal - (double) minVal) / 10);
			}

			List<Document> GroupingtestValues = testValueService.gettingRegularHistpTestFromCollection(testNumber,
					testName, waferIds, classVal, maxVal, df, minVal, group, filterParams);
			GroupingtestValues = testValueService.GroupRegularHistoTestValueBasedOnYAxis(GroupingtestValues, classVal);

			if (GroupingtestValues.size() > 0) {
				if (waferIds.length == 0) {
					WaferDateTime waferDateTime = waferDateTimeService.getWaferDateResponse(GroupingtestValues);
					res.setWaferDateTime(waferDateTime);
				}
				res.setxAxisCount(1);
				res.setData(GroupingtestValues);
				List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
				XaxisResponseDetails XResDetails = new XaxisResponseDetails();
				XResDetails.setAxisName(MeasureName);
				XResDetails.setType("Number");
				Dimens.add(XResDetails);
				res.setxAxisDetails(Dimens);
				res.setClassValue(classVal);
				res.setMaxValue(maxVal);
				res.setScaleValue(scaleVal);
				res.setMinValue(minVal);
			} else {
				res.setData(new ArrayList<>());
			}

		}

		else {
			res.setData(new ArrayList<>());
		}

		return res;

	}

	@Override
	public GraphCommonResponse getBoxPlotTestValueDetails(GraphCommonRequest request) throws IOException {
		GraphCommonResponse res = new GraphCommonResponse();
		List<FilterParam> filterParams = null;
		if (request.getDataQueryId() != null) {
			List<Document> results = waferDateTimeService.getQueryList(request.getDataQueryId());
			filterParams = CriteriaUtil.getDataFromQueryList(results.get(0));
		}
		XaxisDetails xaxis = request.getxAxis();
		YaxisDetails yaxis = request.getyAxis();
		int count = xaxis.getAxisCount();
		if (count == 1) {
			res = getOneXTestValueBoxDetails(request, xaxis, yaxis, filterParams);

		} else {
			res = getTwoXTestValueBoxDetails(request, xaxis, yaxis, filterParams);
		}
		if (request.getDataQueryId() != null) {
			WaferDateTime waferDateTime = waferDateTimeService.handleWaferDates(filterParams);

			if (waferDateTime.getWaferStartTime() != null && waferDateTime.getWaferEndTime() != null) {
				res.setWaferDateTime(waferDateTime);
			} else if (res.getWaferDateTime() == null) {

				waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(LocalDateTime.now()));
				res.setWaferDateTime(waferDateTime);
			}

		}
		return res;
	}

	private GraphCommonResponse getTwoXTestValueBoxDetails(GraphCommonRequest request, XaxisDetails xaxis,
			YaxisDetails yaxis, List<FilterParam> filterParams) throws IOException {
		GraphCommonResponse res = new GraphCommonResponse();

		logger.info("Inside getOneXTestValueBoxDetails service ");
		String[] waferID = request.getWaferID();
		int maxLimit = Integer.parseInt(maximumLimitBox);
		if (waferID.length > maxLimit) {
			throw new IOException("Selected data has exceeded the maximum limit");
		}

		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();
		String DimensionName2X = dimensionDetails.get(1).getName();

		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		String granuality2X = dimensionDetails.get(1).getGranularity();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (DimensionName.equals("DUT") || DimensionName2X.equals("DUT")) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}

		List<Document> testResults = testValueService.gettingTestValueBoxPlot2X(testNumber, testName, waferIds, maxVal,
				minVal, DimensionName, DimensionName2X, group, granuality, granuality2X, filterParams);

		if (testResults.size() > 0) {

			res.setxAxisCount(2);
			res.setyAxisCount(1);
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			String Dimension = String.valueOf(documentDimension.get("Grouping"));
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XaxisResponseDetails XResDetails2X = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);

			if (granuality != null && granuality != "") {
				XResDetails.setType("DateTime");
			} else {

				String dimensionType = getDimenisonType(Dimension);
				XResDetails.setType(dimensionType);

			}

			XResDetails2X.setAxisName(DimensionName2X);
			if (granuality != null && granuality != "") {
				XResDetails2X.setType("DateTime");
			} else {
				if (isNumeric((DimensionName2X))) {
					XResDetails2X.setType("Number");
				} else {
					XResDetails2X.setType("String");
				}
			}

			Dimens.add(XResDetails);
			Dimens.add(XResDetails2X);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			if (group != "mesWafer._id" || group.isEmpty()) {
				res.setGroupDetails(groupResDetails);
			}
		} else {
			res.setData(new ArrayList<>());
		}

		return res;

	}

	private GraphCommonResponse getOneXTestValueBoxDetails(GraphCommonRequest request, XaxisDetails xaxis,
			YaxisDetails yaxis, List<FilterParam> filterParams) throws IOException {
		GraphCommonResponse res = new GraphCommonResponse();

		logger.info("Inside getOneXTestValueBoxDetails service ");
		String[] waferID = request.getWaferID();
		int maxLimit = Integer.parseInt(maximumLimitBox);
		if (waferID.length > maxLimit) {
			throw new IOException("Selected data has exceeded the maximum limit");
		}

		ObjectId[] waferIds = new ObjectId[waferID.length];
		for (int i = 0; i < waferID.length; i++) {
			waferIds[i] = new ObjectId(waferID[i]);
		}
		String group = request.getGrouping().getAxisParameter();
		List<XaxisSetting> dimensionDetails = xaxis.getSettings();
		List<YaxisSetting> measureDetails = yaxis.getSettings();
		String granuality = dimensionDetails.get(0).getGranularity();
		String DimensionName = dimensionDetails.get(0).getName();

		String MeasureName = measureDetails.get(0).getName();
		double minVal = measureDetails.get(0).getMin();
		double maxVal = measureDetails.get(0).getMax();
		SelectedTestDetails test = measureDetails.get(0).getSelectedTest();
		String testName = test.getTestName();
		int testNumber = test.getTestNumber();
		if (group == null || group.isEmpty()) {
			group = CriteriaUtil.generateFieldNameForCriteria("empty");
		} else {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}
		if (DimensionName.equals("DUT")) {
			throw new IOException(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		}

		List<Document> testResults = testValueService.gettingTestValueBoxPlot(testNumber, testName, waferIds, maxVal,
				minVal, DimensionName, group, granuality, filterParams);

		if (testResults.size() > 0) {

			res.setxAxisCount(1);
			res.setyAxisCount(1);
			res.setData(testResults);
			Document documentDimension = testResults.get(0);
			String Dimension = String.valueOf(documentDimension.get("Grouping"));
			List<XaxisResponseDetails> Dimens = new ArrayList<XaxisResponseDetails>();
			List<YaxisResponseDetails> Measure = new ArrayList<YaxisResponseDetails>();
			XaxisResponseDetails XResDetails = new XaxisResponseDetails();
			XResDetails.setAxisName(DimensionName);

			if (granuality != null && granuality != "") {
				XResDetails.setType("DateTime");
			} else {

				String dimensionType = getDimenisonType(Dimension);
				XResDetails.setType(dimensionType);

			}
			Dimens.add(XResDetails);
			res.setxAxisDetails(Dimens);
			YaxisResponseDetails yResDetails = new YaxisResponseDetails();
			yResDetails.setAxisName(MeasureName);
			yResDetails.setType("Number");
			Measure.add(yResDetails);
			res.setyAxisDetails(Measure);
			groupResponseDetails groupResDetails = new groupResponseDetails();
			groupResDetails.setAxisName(group);
			if (group != "mesWafer._id" || group.isEmpty()) {
				res.setGroupDetails(groupResDetails);
			}
		} else {
			res.setData(new ArrayList<>());
		}

		return res;

	}

	public String getDimenisonType(String Dimension) {
		if (isNumeric(Dimension)) {
			return "Number";
		} else {
			return "String";
		}
	}

	/*
	 * api to match with selected test value and return waferId as output
	 */
	@Override
	public DataListModel getWaferIdsOnTestValues(TestValueDetails request) throws IOException {
		Document result = null;
		Aggregation aggregationval = null;
		double testValue = request.getTestValue();
		ObjectId[] waferIds = request.getWaferID();
		String testName = request.getTestName();
		int testNumber = request.getTestNumber();
		if (waferIds.length > 0) {
			aggregationval = Aggregation.newAggregation(
					match -> new Document("$match",
							new Document("$and",
									Arrays.asList(new Document("wd", new Document("$in", Arrays.asList(waferIds))),
											new Document("tn", testNumber), new Document("tm", testName)))),

					groupfield -> new Document("$group",
							new Document("_id", "$wd").append("value", new Document("$addToSet", "$mv"))),
					matchZero -> new Document("$match",
							new Document("value", new Document("$in", Arrays.asList(testValue)))),
					projectExcludeId -> new Document("$project", new Document("_id", 1L))

			).withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		}
		logger.info("Preparing Query gettingTestValueBoxPlot" + aggregationval);
		List<Document> keyResultsTestValue = mongoTemplate.aggregate(aggregationval, "TestResults", Document.class)
				.getMappedResults();
		Message message = new Message();
		DataListModel testListModel = new DataListModel();
		BoxPlotTestValueWaferIds boxPlotWafers = new BoxPlotTestValueWaferIds();
		List<String> waferID = new ArrayList<>();
		List<BoxPlotTestValueWaferIds> listWaferID = new ArrayList<>();
		if (keyResultsTestValue.size() > 0) {
			for (Document obj : keyResultsTestValue) {
				ObjectId objId = (ObjectId) obj.get("_id");
				obj.put(AdvancedGraphConstants.WAFER_ID, objId.toHexString());
				obj.remove("_id", objId);
				waferID.add(objId.toHexString());
			}
			boxPlotWafers.setWaferIDs(waferID);
			listWaferID.add(boxPlotWafers);
			testListModel.setDataList(listWaferID);

			message.setSuccess(true);
			message.setCode(200);
			testListModel.setStatus(message);
		} else {
			testListModel.setDataList(listWaferID);
			message.setSuccess(false);
			message.setCode(204);
			testListModel.setStatus(message);
			return testListModel;
		}
		return testListModel;
	}
	
	/*
	 * api to fetch testPassCount and testFailCount from TestResults
	 */
	@Override
	public DataListModel getTestStatus(TestStatusDetails request) throws IOException {
		Document result = null;
		Aggregation aggregationval = null;
		ObjectId[] waferIds = request.getWaferIds();
		String testName = request.getTestName();
		int testNumber = request.getTestNumber();
		if (waferIds.length > 0) {

			aggregationval = Aggregation.newAggregation(

					match -> new Document("$match",
							new Document("$and",
									Arrays.asList(new Document("wd", new Document("$in", Arrays.asList(waferIds))),
											new Document("tn", testNumber), new Document("tm", testName)))),
					addFied -> new Document("$addFields",
							new Document("testPass", new Document("$cond", Arrays.asList("$tp", 1L, 0L)))
									.append("testFail", new Document("$cond", Arrays.asList("$tp", 0L, 1L)))),
					groupOperation -> new Document("$group",
							new Document("_id", new Document("wd", "$wd").append("tn", "$tn").append("tm", "$tm"))
									.append("tpCount", new Document("$sum", "$testPass"))
									.append("tfCount", new Document("$sum", "$testFail"))),
					projectOperation -> new Document("$project",
							new Document("_id", 1L).append("tpCount", 1L).append("tfCount", 1L)))
					.withOptions(AggregationOptions.builder().allowDiskUse(Boolean.TRUE).build());
		}
		logger.info("Preparing Query gettingTestStatus" + aggregationval);
		List<Document> keyResultsTestValue = mongoTemplate.aggregate(aggregationval, "TestResults", Document.class)
				.getMappedResults();
		boolean status = false;
		Message message = new Message();
		DataListModel testListModel = new DataListModel();
		TestStatus testStatus = new TestStatus();
		List<TestStatus> testStatusList = new ArrayList<>();
		int count=0;
		if (keyResultsTestValue.size() > 0) {
			for (Document obj : keyResultsTestValue) {
				Long testPassCount = (Long) obj.get("tpCount");
				Long testFailCount = (Long) obj.get("tfCount");
				if (testPassCount == 0 && testFailCount > 0) {
					count++;
				}
			}
			if (keyResultsTestValue.size()==count) {
				testStatus.setIsTestStatus(false);
				testStatusList.add(testStatus);
				testListModel.setDataList(testStatusList);
			} else {
				testStatus.setIsTestStatus(true);
				testStatusList.add(testStatus);
				testListModel.setDataList(testStatusList);
			}
			message.setSuccess(true);
			message.setCode(200);
			testListModel.setStatus(message);
		} else {
			testListModel.setDataList(testStatusList);
			message.setSuccess(false);
			message.setCode(204);
			testListModel.setStatus(message);
			return testListModel;
		}
		return testListModel;
	}
}
