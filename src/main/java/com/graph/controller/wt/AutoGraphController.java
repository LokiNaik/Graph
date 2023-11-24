package com.graph.controller.wt;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.graph.constants.wt.ExceptionGraphConstant;
import com.graph.model.wt.AutoGraph;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.DataQueries;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.Message;
import com.graph.service.wt.IAutographService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/WT/v1")

public class AutoGraphController {

	@Autowired
	private IAutographService graphService;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@PostMapping(value = "/autoGraph")
	public ResponseEntity<?> autoGraphsAPI(@RequestBody GraphModelJSON search) {
		DataModel datas = null;
		try {
			datas = graphService.getAutoGraphs(search);
			if (datas == null) {
				logger.info("autoGraphsAPI :Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}", datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info(ExceptionGraphConstant.EXCEPTION_AUTO_GRAPH+" {}", e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/specificAutoGraph/{autoGraphIds}")
	public ResponseEntity<?> specificAutoGraphsAPI(@PathVariable("autoGraphIds") List<String> autoGraphIds) {
		DataListModel datas = null;
		try {
			datas = graphService.specificAutoGraphs(autoGraphIds);
			if (datas == null) {
				logger.info("specificAutoGraphs:Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info(ExceptionGraphConstant.RETURNING_RESPONSE+" {}", datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info(ExceptionGraphConstant.EXCEPTION_SPECIFIC_AUTO_GRAPH+" {}", e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping(value = "/loadQueryNamesList")
	public DataListModel loadQueryList() {
		DataListModel dataListModel = new DataListModel();
		List<Document> results = new ArrayList<>();
		try {
			results = graphService.getLoadQueryList();
			dataListModel.setDataList(results);
			Message message = new Message();
			message.setSuccess(true);
			message.setCode(200);
			dataListModel.setStatus(message);
		} catch (Exception e) {
			dataListModel.setDataList(results);
			Message message = new Message();
			message.setSuccess(false);
			message.setCode(204);
			dataListModel.setStatus(message);
			logger.info(ExceptionGraphConstant.EXCEPTION_LOAD_QUERY_LIST+" {}", e.getMessage());
		}
		return dataListModel;

	}

	@RequestMapping(value = "/loadDataQuery", method = RequestMethod.POST)
	public DataListModel loadDataQuery(@RequestBody DataQueries request) {
		DataListModel dataListModel = new DataListModel();
		List<Document> results = new ArrayList<>();
		try {
			results = graphService.getDataQuery(request);
			dataListModel.setDataList(results);
			Message message = new Message();
			message.setSuccess(true);
			message.setCode(200);
			dataListModel.setStatus(message);
		} catch (Exception e) {
			dataListModel.setDataList(results);
			Message message = new Message();
			message.setSuccess(false);
			message.setCode(204);
			dataListModel.setStatus(message);
			logger.info(ExceptionGraphConstant.EXCEPTION_GET_DATA_QUERY+" {}", e.getMessage());
		}
		return dataListModel;
	}

	@PostMapping("/saveDataQuery")
	public DataListModel saveDataQuery(@RequestBody DataQueries request) {
		DataListModel dataListModel = new DataListModel();
		try {
			logger.info("Inside saveDataQuery()");
			dataListModel = graphService.saveDataQuery(request);
		} catch (Exception e) {
			logger.info(ExceptionGraphConstant.EXCEPTION_SAVE_DATA_QUERY+" {}", e.getMessage());
		}
		return dataListModel;
	}

	@PostMapping("/saveAutoGraph")
	public DataListModel saveAutoGraph(@RequestBody AutoGraph request) {
		DataListModel dataListModel = new DataListModel();
		try {
			logger.info("Inside saveAutoGraph()");
			dataListModel = graphService.saveAutoGraph(request);
		} catch (Exception e) {
			logger.info(ExceptionGraphConstant.EXCEPTION_SAVE_AUTO_GRAPH+" {}", e.getMessage());
		}
		return dataListModel;
	}
}
