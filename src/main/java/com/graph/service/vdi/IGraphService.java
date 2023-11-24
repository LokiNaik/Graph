package com.graph.service.vdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.stereotype.Component;
import com.graph.model.vdi.GraphDefectFilterCriteria;
import com.graph.model.vdi.GridDataResponse;
import com.graph.model.vdi.LineGraphRequest;
import com.graph.model.vdi.LineGraphResponse;
import com.graph.model.vdi.WaferDefectFilterCriteria;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.DataModel;
import com.graph.model.wt.GraphMessage;
import com.graph.model.wt.GraphModelJSON;
import com.graph.model.wt.GraphRequest;
import com.graph.model.wt.Message;
import com.graph.model.wt.SidePanelResponseParam;

@Component
public interface IGraphService {
	public LineGraphResponse getLineGraphDetails(LineGraphRequest request) throws IOException;

	public LineGraphResponse getGraphData(LineGraphRequest requestParam) throws IOException;
	
    public DataModel getSavedGraphs(GraphModelJSON search) throws IOException;

    public DataModel getRecentGraphs(GraphModelJSON search) throws IOException;

    public DataListModel specificRegularGraphs(ArrayList<String> regularGraphIds);

    public DataListModel specificRecentGraphs(ArrayList<String> regularGraphIds);

    public DataListModel specificSavedGraphs(ArrayList<String> savedGraphIds);

    public Message renameGraph(String selected_id, String graphName);

    public GraphMessage deleteGraph(String id);

    public DataListModel saveGraphDetails(GraphRequest regularGraphInfo);

    public DataModel getRegularGraphs(GraphModelJSON search) throws IOException;

    public List<Document> getDefects(GraphDefectFilterCriteria criteria)  throws IOException;

	public SidePanelResponseParam getSidePanelData(String[] waferID);

	public GridDataResponse getMultipleWaferDefects(WaferDefectFilterCriteria request) throws IOException;
}
