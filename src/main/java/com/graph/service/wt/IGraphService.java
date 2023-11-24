package com.graph.service.wt;

import java.io.IOException;

import com.graph.model.wt.ComparisonHistogramTestValueRequest;
import com.graph.model.wt.ComparisonHistogramTestValueResponse;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.GraphCommonRequest;
import com.graph.model.wt.GraphCommonResponse;
import com.graph.model.wt.MESTestAvgMedRequest;
import com.graph.model.wt.RegularHistoTestValue;
import com.graph.model.wt.RegularHistogramRequest;
import com.graph.model.wt.RegularHistogramResponse;
import com.graph.model.wt.ScatterPlotRequest;
import com.graph.model.wt.ScatterPlotResponse;
import com.graph.model.wt.TestStatusDetails;
import com.graph.model.wt.TestValueDetails;

public interface IGraphService {

	GraphCommonResponse getCommonGraphDetails(GraphCommonRequest request) throws IOException;

	ComparisonHistogramTestValueResponse getHistogramTestValueDetails(ComparisonHistogramTestValueRequest request)
			throws NoSuchFieldException, IOException;

	ScatterPlotResponse getScatterPlotDetails(ScatterPlotRequest request) throws IOException;

	GraphCommonResponse getComparisionHistogramDetails(GraphCommonRequest request) throws IOException;

	DataListModel getMESTestValuesAvgMedService(MESTestAvgMedRequest request);

	RegularHistogramResponse getRegularHistoDetails(RegularHistogramRequest request) throws IOException;

	GraphCommonResponse getLineGraphDetails(GraphCommonRequest request) throws IOException;

	RegularHistoTestValue getRegularHistoTestValueDetails(RegularHistogramRequest request) throws IOException;

	GraphCommonResponse getBoxPlotTestValueDetails(GraphCommonRequest request) throws IOException;

	DataListModel getWaferIdsOnTestValues(TestValueDetails waferIds) throws IOException;

	DataListModel getTestStatus(TestStatusDetails request) throws IOException;

}
