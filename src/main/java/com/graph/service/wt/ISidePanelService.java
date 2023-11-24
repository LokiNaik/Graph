package com.graph.service.wt;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import com.graph.model.wt.DataListModel;
import com.graph.model.wt.SidePanelResponseParam;
import com.graph.model.wt.TestListResponse;
import com.graph.model.wt.UserPreferenceDetails;

public interface ISidePanelService {

	DataListModel getListOfWaferIds(ObjectId[] testIds);

    List<JSONObject> getAxisSettings(String graphType);

    JSONObject getAxisParameters(String applicationName);

    public boolean updateUserPreference(UserPreferenceDetails userPreferenceDetails);

    SidePanelResponseParam getSidePanelData(String[] waferID);

    TestListResponse getTestList(ObjectId[] waferIds);

    Document getBinList(ObjectId[] waferIds);
}
