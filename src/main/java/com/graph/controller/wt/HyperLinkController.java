package com.graph.controller.wt;

import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.HyperLinkModel;
import com.graph.model.wt.Message;
import com.graph.service.wt.IHyperLinkService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/advancedGraph/WT/v1")
public class HyperLinkController {

	@Autowired
	private IHyperLinkService graphService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@RequestMapping(value = "/hyperLink", method = RequestMethod.POST)
	public DataListModel getHyperLinkAPI(@RequestParam("createdBy") String request) {
		DataListModel dataListModel = new DataListModel();
		List<JSONObject> results = null;
		try {
			results = graphService.getHyperLink(request);

			if (results.size() < 1) {
				logger.info("getHyperLinkAPI():Null response, before returning the final response");
				Message message = new Message();
				message.setSuccess(false);
				message.setCode(322);
				dataListModel.setStatus(message);
				dataListModel.setDataList(results);
			} else {
				logger.info("Before returning the final response : " + results);
				Message message = new Message();
				message.setSuccess(true);
				message.setCode(200);
				dataListModel.setStatus(message);
				dataListModel.setDataList(results);
			}
		}

		catch (Exception e) {
			logger.error("Exception occuured: in getHyperLinkAPI() : " + e.getMessage());
		}
		return dataListModel;
	}

	@PostMapping(path = "/saveHyperLink")
	public DataListModel saveHyperLink(@RequestBody HyperLinkModel request) {
		DataListModel dataListModel = new DataListModel();
		try {
			logger.info("Inside saveHyperLink()");
			dataListModel = graphService.saveHyperLink(request);
		} catch (Exception e) {
			logger.error("Exception occuured: in saveHyperLink()" + e.getMessage());
		}
		return dataListModel;
	}
}
