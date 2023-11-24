package com.graph.controller.wt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graph.model.common.DataListExceptionModel;
import com.graph.model.common.MessageadvGraph;
import com.graph.model.wt.BinListRequest;
import com.graph.model.wt.GraphCommonRequest;
import com.graph.model.wt.GraphCommonResponse;
import com.graph.model.wt.ComparisonHistogramTestValueRequest;
import com.graph.model.wt.ComparisonHistogramTestValueResponse;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.MESTestAvgMedRequest;
import com.graph.model.wt.RegularHistoTestValue;
import com.graph.model.wt.RegularHistogramRequest;
import com.graph.model.wt.RegularHistogramResponse;
import com.graph.model.wt.ScatterPlotRequest;
import com.graph.model.wt.ScatterPlotResponse;
import com.graph.model.wt.SearchParamFilter;
import com.graph.model.wt.SidePanelRequestParam;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.model.wt.TestListRequest;
import com.graph.model.wt.TestListResponse;
import com.graph.model.wt.TestStatusDetails;
import com.graph.model.wt.TestValueDetails;
import com.graph.model.wt.UserPreferenceDetails;
import com.graph.service.wt.IGraphService;
import com.graph.service.wt.ISidePanelService;
import com.graph.constants.wt.ExceptionGraphConstant;
import com.graph.constants.wt.AdvancedGraphConstants;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/WT/v1")
public class AdvanceGraphController {

    @Autowired
    private IGraphService graphService;
    
    @Autowired 
    private ISidePanelService sidePanelService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PostMapping(value = "/graph_waferids")
    public ResponseEntity<DataListModel> graphWaferIdsAPI(@RequestBody SearchParamFilter request) {
	   DataListModel results = null;
	   try {
		  logger.info("Getting wafer ids");
		  String[] testID = request.getTestID();
		  ObjectId[] testIds = new ObjectId[testID.length];
		 
		  for ( int i = 0; i < testID.length; i++) {
			 testIds[i] = new ObjectId(testID[i]);
		  }
		  results = sidePanelService.getListOfWaferIds(testIds);
		  if (results == null) {
			 logger.info("getListOfWaferIds:Null response, before returning the final response");
			 return new ResponseEntity<>(results, HttpStatus.OK);
		  }
		  else {
			 logger.info(ExceptionGraphConstant.RETURNING_RESPONSE , results);
			 return new ResponseEntity<>(results, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.EXCEPTION_WAFERID , e.getMessage());
		  return new ResponseEntity<>(results, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @GetMapping(value = "/graphAxisSettings")
    public ResponseEntity<?> axisSettingsAPI() {
	   List<JSONObject> graphTypeModel = null;
	   try {
		  graphTypeModel = sidePanelService.getAxisSettings("WT");
		  if (!graphTypeModel.isEmpty()) {
			 logger.info("AxisSettingsAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(graphTypeModel, HttpStatus.OK);
		  }
		  else {
			 logger.info(ExceptionGraphConstant.RETURNING_RESPONSE, graphTypeModel);
			 return new ResponseEntity<>(graphTypeModel, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.AXIS_SETTING,e.getMessage());
		  return new ResponseEntity<>(graphTypeModel, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @GetMapping(value = "/graphAxisParameters")
    public ResponseEntity<?> axisParametersAPI() {
	   JSONObject axisModel = null;
	   try {
		  axisModel = sidePanelService.getAxisParameters("WT");
		  if (axisModel == null) {
			 logger.info("AxisParametersAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(axisModel, HttpStatus.OK);
		  }
		  else {
			 logger.error(ExceptionGraphConstant.RETURNING_RESPONSE,axisModel);
			 return new ResponseEntity<>(axisModel, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.AXIS_PARAM, e.getMessage());
		  return new ResponseEntity<>(axisModel, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }




    @PostMapping("/user/preference")
    public ResponseEntity<?> updateUserPrefernce(@RequestBody UserPreferenceDetails userPreferenceDetails) {
	   boolean status = false;
	   try {
		  status = sidePanelService.updateUserPreference(userPreferenceDetails);
		  if (status) {
			 return new ResponseEntity<>(HttpStatus.OK);
		  }
		  else {
			 return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		  }
	   }
	   catch (Exception e) {
		  logger.error("Exception occured : ", e);
		  return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping("/sidePanel")
    public ResponseEntity<?> sidePanelApi(@RequestBody SidePanelRequestParam sidePanelParam) {
	   SidePanelResponseParam resultList = null;
	   logger.info("Inside sidePanelApi() Controller ");
	   try {
		  if (sidePanelParam.getWaferID() != null) {
			 resultList = sidePanelService.getSidePanelData(sidePanelParam.getWaferID());
		  }
		  if (resultList == null) {
			 return new ResponseEntity<>(resultList, HttpStatus.NO_CONTENT);
		  }
		  else {
			 return new ResponseEntity<>(resultList, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.SIDEPANEL_API,e.getMessage());
		  return new ResponseEntity<>(resultList, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/testList")
    public ResponseEntity<?> testNamesListAPI(@RequestBody TestListRequest request) {
	   TestListResponse list = null;
	   try {
		  logger.info("Inside testNamesListAPI() Controller ");
		  String[] waferID = request.getWaferID();
		  ObjectId[] waferIds = new ObjectId[waferID.length];
		  
		  for (int i = 0; i < waferID.length; i++) {
			 waferIds[i] = new ObjectId(waferID[i]);
		  }
		   list = sidePanelService.getTestList(waferIds);

		  if (list == null) {
			 return new ResponseEntity<>(list, HttpStatus.NO_CONTENT);
		  }
		  else {
			 return new ResponseEntity<>(list, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.TESTNAME_API,e.getMessage());
		  return new ResponseEntity<>(list, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping(value = "/binList")
    public ResponseEntity<?> binListAPI(@RequestBody BinListRequest request) {
	   Document list  = null;
	   try {
		  logger.info("Inside binListAPI() Controller ");
		  String[] waferID = request.getWaferID();
		  ObjectId[] waferIds = new ObjectId[waferID.length];
		  
		  for (int i=0;  i < waferID.length; i++) {
			 waferIds[i] = new ObjectId(waferID[i]);
		  }
		   list = sidePanelService.getBinList(waferIds);

		  if (list == null) {
			 return new ResponseEntity<>(list, HttpStatus.NO_CONTENT);
		  }
		  else {
			 return new ResponseEntity<>(list, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.BINLIST_API, e.getMessage());
		  return new ResponseEntity<>(list, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping(value = { "/boxPlot", "/barChart", "/comparisonScatterPlot"})
    public ResponseEntity<?> commonGraphAPI(@RequestBody GraphCommonRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside comparisonHistogram() Controller ");

		  GraphCommonResponse list = graphService.getCommonGraphDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO, e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping(value = {"/lineGraph" })
    public ResponseEntity<?> lineGraphAPI(@RequestBody GraphCommonRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside lineGraphAPI() Controller ");

		  GraphCommonResponse list = graphService.getLineGraphDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.LINE_GRAPH, e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.LINE_GRAPH, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

   @PostMapping(value = { "/comparisonHistogram" })
    public ResponseEntity<?> comparisonHistogramAPI(@RequestBody GraphCommonRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside comparisonHistogram() Controller ");

		  GraphCommonResponse list = graphService.getComparisionHistogramDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO , e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO,e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping(value = "/comparisonHistogram/testValue")
    public ResponseEntity<?> comparisonHistogramtestValueAPI(@RequestBody ComparisonHistogramTestValueRequest request) {

	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside comparisonHistogramtestValueAPI() Controller ");

		  ComparisonHistogramTestValueResponse list = graphService.getHistogramTestValueDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO_TESTVALUE,e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.COMPARISION_HISTO_TESTVALUE,e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }

    @PostMapping("/testValuesAvgMed")
    public ResponseEntity<?> getMESTestValuesAvgMed(@RequestBody MESTestAvgMedRequest request) {
	   DataListModel result = null;
	   try {

		  logger.info("Inside the getMESTestValuesService()");
		 

		  result = graphService.getMESTestValuesAvgMedService(request);

		  if (result == null) {
			 return new ResponseEntity<>(new DataListModel(new ArrayList<>()), HttpStatus.OK);
		  }
		  else {
			 return new ResponseEntity<>(result, HttpStatus.OK);
		  }

	   }

	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.TESTVALUE_AVG_MEDIAN,e.getMessage());
		  return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/regularHistogram")
    public ResponseEntity<?> regularHistogramAPI(@RequestBody RegularHistogramRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside regularHistogramAPI() Controller ");

		  RegularHistogramResponse list = graphService.getRegularHistoDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE, String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.REGULAR_HISTO,e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.REGULAR_HISTO, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }
    
    @PostMapping(value = "/scatterPlot")
    public ResponseEntity<?> scatterPlotAPI(@RequestBody ScatterPlotRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside scatterPlot() Controller ");

		  ScatterPlotResponse list = graphService.getScatterPlotDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put(AdvancedGraphConstants.WAFER_DATE_TIME, list.getWaferDateTime());
			 response.put(AdvancedGraphConstants.STATUS_CODE,String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.SCATTER_PLOT,e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.SCATTER_PLOT, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }
    
    
    
    @PostMapping(value = "/regularHistogram/testValues")
    public ResponseEntity<?> regularHistogramTestValueAPI(@RequestBody RegularHistogramRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside regularHistogramAPI() Controller ");

		  RegularHistoTestValue list = graphService.getRegularHistoTestValueDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put("waferDateTime", list.getWaferDateTime());
			 response.put("statuscode", String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.REGULAR_HISTO_TESTVALUE, e.getMessage());
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.REGULAR_HISTO_TESTVALUE, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }
    
    @PostMapping(value = "/boxPlot/testValue")
    public ResponseEntity<?> boxPlotValueAPI(@RequestBody GraphCommonRequest request) {
	   Map<String, Object> response = new HashMap<>();
	   try {
		  logger.info("Inside boxPlotTestValueAPI() Controller ");

		  GraphCommonResponse list = graphService.getBoxPlotTestValueDetails(request);

		  if (!list.getData().isEmpty()) {
			 return new ResponseEntity<>(list, HttpStatus.OK);

		  }
		  else {
			 response.put("data", new ArrayList<>());
			 response.put("waferDateTime", list.getWaferDateTime());
			 response.put("statuscode", String.valueOf(HttpStatus.NO_CONTENT.value()));
			 return ResponseEntity.ok().body(response);
		  }
	   }
	   catch (IOException e) {
		  logger.error(ExceptionGraphConstant.COMBINATION_NOT_SUPPORT);
		  DataListExceptionModel res = new DataListExceptionModel();
		  res.setData(new ArrayList<>());
		  MessageadvGraph message = new MessageadvGraph();
		  message.setStatus(e.getMessage());
		  res.setMessage(message);
		  return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
	   }
	   catch (Exception e) {
		  response.put("data", new ArrayList<>());
		  logger.error(ExceptionGraphConstant.BOXPLOT_TESTVALUE, e.getMessage());
		  return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	   }

    }
    
    

    @PostMapping("/waferIDFortestValue")
    public ResponseEntity<?> waferIdForTestValueAPI(@RequestBody TestValueDetails request) {
    	DataListModel result=null;
	   try {
		      logger.info("Inside waferIdForTestValueAPI() Controller ");
			  result = graphService.getWaferIdsOnTestValues(request);
			  return new ResponseEntity<>(result, HttpStatus.OK);
	   }
	   catch (Exception e) {
		  logger.error("Exception occured : ", e);
		  return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
	

    }
    
    
    @PostMapping("/getTestStatus")
    public ResponseEntity<?>getTestStatusAPI(@RequestBody TestStatusDetails request)
    {
    	DataListModel result=null;
    	try {
    		logger.info("Inside getTestStatusAPI() Controller ");
    		result=graphService.getTestStatus(request);
    		return new ResponseEntity<>(result,HttpStatus.OK);
    	}
    	catch(Exception e)
    	{
    		logger.error("Exception occured : ", e);
  		  return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    }
}
