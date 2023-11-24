package com.graph.service.wt;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.annotation.RequestBody;

import com.graph.model.wt.AutoGraph;
import com.graph.model.wt.GraphCommonRequest;
import com.graph.model.wt.GraphCommonResponse;
import com.graph.model.wt.ComparisonHistogramTestValueRequest;
import com.graph.model.wt.ComparisonHistogramTestValueResponse;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.GraphInformationResponse;
import com.graph.model.wt.HyperLinkModel;
import com.graph.model.wt.MESTestAvgMedRequest;
import com.graph.model.wt.RegularHistoTestValue;
import com.graph.model.wt.RegularHistogramRequest;
import com.graph.model.wt.RegularHistogramResponse;
import com.graph.model.wt.ScatterPlotRequest;
import com.graph.model.wt.ScatterPlotResponse;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.model.wt.TestListResponse;
import com.graph.model.wt.UserPreferenceDetails;
import com.graph.model.wt.AppHyperLink;

public interface IHyperLinkService {



	DataListModel saveHyperLink(HyperLinkModel request) throws FileNotFoundException;

	List<JSONObject> getHyperLink(String request); 

}
