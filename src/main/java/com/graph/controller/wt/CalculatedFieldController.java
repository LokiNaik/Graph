package com.graph.controller.wt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.graph.model.wt.DataListModel;
import com.graph.service.wt.ICalculatedFieldService;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/WT/v1/calculatedFields")

public class CalculatedFieldController {

	@Autowired
	private ICalculatedFieldService graphService;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@RequestMapping(value = "/loadCalculatedField", method = RequestMethod.POST)
	    public ResponseEntity<DataListModel> calculatedFieldAPI() {
		   DataListModel results = null;
		   try {
			  results = graphService.getCalculatedFields();
			  if (results == null) {
				 logger.info("getListOfWaferIds:Null response, before returning the final response");
				 return new ResponseEntity<>(results, HttpStatus.OK);
			  }
			  else {
				 logger.info("Before returning the final response : " + results);
				 return new ResponseEntity<>(results, HttpStatus.OK);
			  }
		   }
		   catch (Exception e) {
			  logger.error("Exception occuured: in getCalculatedFields() : " + e.getMessage());
			  return new ResponseEntity<>(results, HttpStatus.INTERNAL_SERVER_ERROR);
		   }
	    }

}
