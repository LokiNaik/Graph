package com.graph.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.graph.model.wt.AxisParameterModel;


public class ValidationUtil {

	public static boolean decimalValueValidation(double value, int length) {
		if (BigDecimal.valueOf(value).scale() <= length) {
			return true;
		}
		return false;
	}
	public static List<Document> documentIdConverter(List<Document> results) {
		for (Document obj : results) {
			ObjectId objId = (ObjectId) obj.get("_id");
			obj.put("_id", objId.toHexString());
		}
		return results;
	}
	public static List<Document> sortAxisParameter(List<Document> results, String fieldName, boolean asc) {
		
		for (Document obj : results) {
			List<AxisParameterModel> axes= (List<AxisParameterModel>) obj.get(fieldName);
			List<AxisParameterModel> sortedList = axes.stream()
			        .sorted(Comparator.comparing(AxisParameterModel::getAxisParameter))
			        .collect(Collectors.toList());
			if(!asc) {
				Collections.reverse(sortedList);
			}
		
			obj.put(fieldName, sortedList);
		}
		return results;
	}
	
	public static List<Document> axisConverter(List<Document> results, String axisName) {
		for (Document obj : results) {
			Document axis = (Document) obj.get(axisName);
			List<AxisParameterModel> axisList = new ArrayList<>();
			List<String> axisParameters = (List<String>) axis.get("axisParameter");
			if (axisParameters!=null) {
				for (String axisParam : axisParameters) {
					AxisParameterModel axisParameter = new AxisParameterModel();
					axisParameter.setAxisParameter(axisParam);
					axisList.add(axisParameter);
				}
			}
			obj.put(axisName, axisList);
		}
		return results;
	}
}
