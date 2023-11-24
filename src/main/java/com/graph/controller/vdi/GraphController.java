package com.graph.controller.vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.graph.model.common.DataListExceptionModel;
import com.graph.model.common.MessageadvGraph;
import com.graph.model.common.RegularGraphRequest;
import com.graph.model.vdi.GraphDefectFilterCriteria;
import com.graph.model.vdi.GridDataResponse;
import com.graph.model.vdi.HistogramRequest;
import com.graph.model.vdi.LineGraphRequest;
import com.graph.model.vdi.LineGraphResponse;
import com.graph.model.vdi.ScatterPlotRequest;
import com.graph.model.vdi.WaferDefectFilterCriteria;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.GraphMessage;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.GraphRequest;
import com.graph.model.wt.Message;
import com.graph.model.wt.SidePanelRequestParam;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.service.vdi.IGraphService;
import com.graph.service.vdi.IHistogramService;
import com.graph.service.vdi.IScatterService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/VDI/v1")
public class GraphController {
	@Autowired
	private IGraphService iiGraphService;
	
	@Autowired
	private IScatterService iScatterService;
	
	@Autowired
	private IHistogramService iHistogramService;


	public static final Logger logger = LoggerFactory.getLogger(GraphController.class);

	@PostMapping("/graph/line")
	public ResponseEntity<?> lineGraphApi(@RequestBody LineGraphRequest requestParam) throws IOException {
		LineGraphResponse resultList = null;
		try {
			if (requestParam.waferID != null && requestParam.waferID.size() > 0) {
				resultList = iiGraphService.getGraphData(requestParam);
			}
			if (resultList == null) {
				return new ResponseEntity<>(resultList, HttpStatus.NO_CONTENT);
			} else {
				return new ResponseEntity<>(resultList, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error("Exception occured", e);
			return new ResponseEntity<>("Exception occured", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/graph/defects")
	public ResponseEntity<?> GraphDefects(@RequestBody GraphDefectFilterCriteria criteria) {
		List<Document> resultList = null;
		try {
			resultList = iiGraphService.getDefects(criteria);
			if (resultList == null) {
				return new ResponseEntity<>(resultList, HttpStatus.NO_CONTENT);
			} else {
				return new ResponseEntity<>(resultList, HttpStatus.OK);
			}

		} catch (Exception e) {
			logger.error("Exception occured", e);
			return new ResponseEntity<>("Exception occured", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = {"/graph/lineGraphPlot","/graph/barGraphPlot"}, method = RequestMethod.POST)
	public ResponseEntity<?> lineGraphPlotAPI(@RequestBody LineGraphRequest request) {
		try {
			logger.info("Inside lineGraphPlotAPI()");
			LineGraphResponse list = iiGraphService.getLineGraphDetails(request);
			if (list == null) {
				return new ResponseEntity<>(list, HttpStatus.NO_CONTENT);
			} else {
				return new ResponseEntity<>(list, HttpStatus.OK);
			}
		} catch (IOException e) {
			logger.error("Exception occuured: in lineGraphPlotAPI()" + e.getMessage());
			DataListExceptionModel res = new DataListExceptionModel();
			res.setData(new ArrayList<>());
			MessageadvGraph message = new MessageadvGraph();
			message.setStatus(e.getMessage());
			res.setMessage(message);
			return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
		} catch (Exception e) {
			logger.error("Exception occuured: in testNamesListAPI()" + e.getMessage());
			return new ResponseEntity<>("Exception occured", HttpStatus.NO_CONTENT);
		}
	}
	
	@PostMapping(value = "/regularGraph")
	public ResponseEntity<?> regularGraphsAPI(@RequestBody GraphModelJSON search) {
		DataModel datas = null;
		try {
			datas = iiGraphService.getRegularGraphs(search);
			if (datas == null) {
				logger.info("regularGraphsAPI :Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/savedGraph")
	public ResponseEntity<?> savedGraphsAPI(@RequestBody GraphModelJSON search) {
		DataModel datas = null;
		try {
			datas = iiGraphService.getSavedGraphs(search);
			if (datas == null) {
				logger.info("savedGraphsAPI:Null reponse, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/recentGraph")
	public ResponseEntity<?> recentGraphsAPI(@RequestBody GraphModelJSON search) {
		DataModel datas = null;
		try {
			datas = iiGraphService.getRecentGraphs(search);
			if (datas == null) {
				logger.info("recentGraphsAPI:Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/specificRegularGraph/{regularGraphIds}")
	public ResponseEntity<?> specificRegularGraphsAPI(
			@PathVariable("regularGraphIds") ArrayList<String> regularGraphIds) {
		DataListModel datas = null;
		try {
			datas = iiGraphService.specificRegularGraphs(regularGraphIds);
			if (datas == null) {
				logger.info("specificRegularGraphsAPI:Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/specificRecentGraph/{recentGraphIds}")
	public ResponseEntity<?> specificRecentGraphsAPI(
			@PathVariable("recentGraphIds") ArrayList<String> regularGraphIds) {
		DataListModel datas = null;
		try {
			datas = iiGraphService.specificRecentGraphs(regularGraphIds);
			if (datas == null) {
				logger.info("specificRecentGraphsAPI:Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/specificSavedGraph/{savedGraphIds}")
	public ResponseEntity<?> specificSavedGraphsAPI(@PathVariable("savedGraphIds") ArrayList<String> savedGraphIds) {
		DataListModel datas = null;
		try {
			datas = iiGraphService.specificSavedGraphs(savedGraphIds);
			if (datas == null) {
				logger.info("specificSavedGraphsAPI:Null response, before returning the final response");
				return new ResponseEntity<>(datas, HttpStatus.OK);
			} else {
				logger.info("Before returning the final response : " + datas);
				return new ResponseEntity<>(datas, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occuured: in getRegularGraphs() : " + e.getMessage());
			return new ResponseEntity<>(datas, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/renameGraph")
	public Message renameGraphAPI(@RequestBody RegularGraphRequest request) {
		Message result = new Message();
		try {
			logger.info("Inside renameRegularGraphDetails()");
			result = iiGraphService.renameGraph(request.getSelected_id(), request.getGraphName());
		} catch (Exception e) {
			logger.error("Exception occuured: in renameRegularGraphDetails()" + e.getMessage());
		}
		return result;
	}

	@PostMapping(value = "/deleteGraph/{selectedId}")
	public GraphMessage deleteGraphAPI(@PathVariable("selectedId") String id) {
		GraphMessage result = new GraphMessage();
		try {
			logger.info("Inside deleteRegularGraphDetails()");
			result = iiGraphService.deleteGraph(id);
		} catch (Exception e) {
			logger.error("Exception occuured: in deleteRegularGraphDetails()" + e.getMessage());
		}
		return result;
	}

	@PostMapping("/saveGraph")
	public DataListModel saveGraphDetailsAPI(@RequestBody GraphRequest regularGraphInfo) {
		DataListModel dataListModel = new DataListModel();
		try {
			logger.info("Inside saverGraphDetails()");
			dataListModel = iiGraphService.saveGraphDetails(regularGraphInfo);
		} catch (Exception e) {
			logger.error("Exception occuured: in saveRegularGraphDetails()" + e.getMessage());
		}
		return dataListModel;
	}
	
	@PostMapping("/sidePanel")
	public ResponseEntity<?> sidePanelApi(@RequestBody SidePanelRequestParam sidePanelParam) throws IOException {
		SidePanelResponseParam resultList = null;
		logger.info("Inside sidePanelApi() Controller ");
		try {
			if (sidePanelParam.getWaferID() != null) {
				resultList = iiGraphService.getSidePanelData(sidePanelParam.getWaferID());
			}
			if (resultList == null) {
				return new ResponseEntity<>(resultList, HttpStatus.NO_CONTENT);
			} else {
				return new ResponseEntity<>(resultList, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error("Exception occuured: in sidePanelApi()" + e.getMessage());
			return new ResponseEntity<>("Exception occured", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/scatterPlot")
	public ResponseEntity<?> scatterPlotAPI(@RequestBody  HistogramRequest  request) throws IOException {
		Map<String, Object> response = new HashMap<>();
		try {
			LineGraphResponse list = iScatterService.getScatterPlotDetails(request);

			if (list.getData().size() > 0) {
				return new ResponseEntity<>(list, HttpStatus.OK);

			} else {
				response.put("data", new ArrayList<>());
				response.put("statuscode", String.valueOf(HttpStatus.NO_CONTENT.value()));
				return ResponseEntity.ok().body(response);
			}
		} catch (IOException e) {
			DataListExceptionModel res = new DataListExceptionModel();
			res.setData(new ArrayList<>());
			MessageadvGraph message = new MessageadvGraph();
			message.setStatus(e.getMessage());
			res.setMessage(message);
			return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
		} catch (Exception e) {
			response.put("data", new ArrayList<>());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/histogram")
	public ResponseEntity<?> histogramAPI(@RequestBody HistogramRequest request) throws IOException {
		Map<String, Object> response = new HashMap<>();
		try {
			LineGraphResponse list = iHistogramService.getHistogramDetails(request);

			if (list.getData().size() > 0) {
				return new ResponseEntity<>(list, HttpStatus.OK);

			} else {
				response.put("data", new ArrayList<>());
				response.put("statuscode", String.valueOf(HttpStatus.NO_CONTENT.value()));
				return ResponseEntity.ok().body(response);
			}
		} catch (IOException e) {
			DataListExceptionModel res = new DataListExceptionModel();
			res.setData(new ArrayList<>());
			MessageadvGraph message = new MessageadvGraph();
			message.setStatus(e.getMessage());
			res.setMessage(message);
			return new ResponseEntity<>(res, HttpStatus.NOT_IMPLEMENTED);
		} catch (Exception e) {
			response.put("data", new ArrayList<>());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping(value = "/defects")
	public ResponseEntity<?> getMultipleWaferDefects(@RequestBody WaferDefectFilterCriteria request) {
		GridDataResponse resultList = null;
		try {
			resultList = iiGraphService.getMultipleWaferDefects(request);
			if (resultList == null) {
				return new ResponseEntity<>(resultList, HttpStatus.NO_CONTENT);
			} else {
				return new ResponseEntity<>(resultList, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error("Exception occured", e);
			return new ResponseEntity<>("Exception occured", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	

}
