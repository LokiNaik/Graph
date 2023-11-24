package com.graph.service.wt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.GraphMessage;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.GraphRequest;
import com.graph.model.wt.Message;

public interface ICommonGraphService {
	
    DataModel getRegularGraphs(GraphModelJSON request) throws IOException;

    DataModel getSavedGraphs(GraphModelJSON request) throws IOException;

    DataModel getRecentGraphs(GraphModelJSON request) throws IOException;

	public DataListModel saveGraphDetails(GraphRequest productInfo);

	Message renameGraph(String id, String graphName);

	GraphMessage deleteGraph(String id);

	DataListModel specificRegularGraphs(List<String> regularGraphIds);

	DataListModel specificRecentGraphs(List<String> recentGraphIds);

	DataListModel specificSavedGraphs(List<String> savedGraphIds);
}
