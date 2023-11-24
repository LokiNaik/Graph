package com.graph.controller.wt;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graph.constants.wt.ExceptionGraphConstant;
import com.graph.model.common.RegularGraphRequest;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.GraphMessage;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.GraphRequest;
import com.graph.model.wt.Message;
import com.graph.service.wt.ICommonGraphService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/WT/v1")

public class CommonGraphController {

	@Autowired
	private ICommonGraphService graphService;
	private final Logger logger = LoggerFactory.getLogger(getClass());

    
    @PostMapping(value = "/regularGraph")
    public ResponseEntity<?> regularGraphsAPI(@RequestBody GraphModelJSON search) {
	   DataModel datas = null;
	   
	   try {
		   
		  datas = graphService.getRegularGraphs(search);
		  if (datas == null) {
			 logger.info("regularGraphsAPI :Null response, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			   logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}",datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		  logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/savedGraph")
    public ResponseEntity<?> savedGraphsAPI(@RequestBody GraphModelJSON search) {
	   DataModel datas = null;
	   try {
		  datas = graphService.getSavedGraphs(search);
		  if (datas == null) {
			 logger.info("savedGraphsAPI:Null reponse, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			  logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}",datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		   logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/recentGraph")
    public ResponseEntity<?> recentGraphsAPI(@RequestBody GraphModelJSON search) {
	   DataModel datas = null;
	   try {
		  datas = graphService.getRecentGraphs(search);
		  if (datas == null) {
			 logger.info("recentGraphsAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			  logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}",datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		   logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/specificRegularGraph/{regularGraphIds}")
    public ResponseEntity<?> specificRegularGraphsAPI(
		  @PathVariable("regularGraphIds") List<String> regularGraphIds) {
	   DataListModel datas = null;
	   try {
		  datas = graphService.specificRegularGraphs(regularGraphIds);
		  if (datas == null) {
			 logger.info("specificRegularGraphsAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			  logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}",datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		   logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/specificRecentGraph/{recentGraphIds}")
    public ResponseEntity<?> specificRecentGraphsAPI(
		  @PathVariable("recentGraphIds") List<String> regularGraphIds) {
	   DataListModel datas = null;
	   try {
		  datas = graphService.specificRecentGraphs(regularGraphIds);
		  if (datas == null) {
			 logger.info("specificRecentGraphsAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			  logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}",datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		   logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }

    @PostMapping(value = "/specificSavedGraph/{savedGraphIds}")
    public ResponseEntity<?> specificSavedGraphsAPI(@PathVariable("savedGraphIds") List<String> savedGraphIds) {
	   DataListModel datas = null;
	   try {
		  datas = graphService.specificSavedGraphs(savedGraphIds);
		  if (datas == null) {
			 logger.info("specificSavedGraphsAPI:Null response, before returning the final response");
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
		  else {
			  logger.info(ExceptionGraphConstant.RETURNING_RESPONSE,datas);
			 return new ResponseEntity<>(datas, HttpStatus.OK);
		  }
	   }
	   catch (Exception e) {
		   logger.error(ExceptionGraphConstant.EXCEPTION_REGULAR_GRAPH+" {}" , e.getMessage());
		  return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
	   }
    }
	/*
	 * 
	 */
	  @PostMapping(value = "/renameGraph")
	    public Message renameGraphAPI(@RequestBody RegularGraphRequest request) {
		   Message result = new Message();
		   try {
			  logger.info("Inside renameRegularGraphDetails()");
			  result = graphService.renameGraph(request.getSelected_id(), request.getGraphName());
		   }
		   catch (Exception e) {
			   logger.error(ExceptionGraphConstant.EXCEPTION_RENAME_REGULAR_GRAPH+" {}" , e.getMessage());
			  
		   }
		   return result;
	    }

	    @PostMapping(value = "/deleteGraph/{selectedId}")
	    public GraphMessage deleteGraphAPI(@PathVariable("selectedId") String id) {
		   GraphMessage result = new GraphMessage();
		   try {
			  logger.info("Inside deleteRegularGraphDetails()");
			  result = graphService.deleteGraph(id);
		   }
		   catch (Exception e) {
			   logger.error(ExceptionGraphConstant.EXCEPTION_DELETE_REGULAR_GRAPH+" {}" , e.getMessage());
		   }
		   return result;
	    }

	    @PostMapping("/saveGraph")
	    public DataListModel saveGraphDetailsAPI(@RequestBody GraphRequest regularGraphInfo) {
	    	
	    	DataListModel dataListModel = new DataListModel();
		   try {
			  logger.info("Inside saverGraphDetails()");
			  dataListModel = graphService.saveGraphDetails(regularGraphInfo);
		   }
		   catch (Exception e) {
			   logger.error(ExceptionGraphConstant.EXCEPTION_SAVE_REGULAR_GRAPH+" {}" , e.getMessage());
		   }
		   return dataListModel;
	    }

}
