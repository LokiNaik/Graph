package com.graph.service.vdi;

import java.io.IOException;

import org.springframework.stereotype.Component;
import com.graph.model.vdi.HistogramRequest;
import com.graph.model.vdi.LineGraphResponse;

@Component
public interface IScatterService {
	public LineGraphResponse getScatterPlotDetails(HistogramRequest request) throws IOException;
}
