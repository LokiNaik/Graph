package com.graph.model.vdi;
import java.util.List;
import org.bson.types.ObjectId;

public class WaferSummaryList {
	private List<ObjectId> waferIds;
	private String type;
	
	public List<ObjectId> getWaferIds() {
		return waferIds;
	}
	
	public String getType() {
		return type;
	}
	
}