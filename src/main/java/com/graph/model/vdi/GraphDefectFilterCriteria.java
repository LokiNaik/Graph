package com.graph.model.vdi;

import java.awt.geom.Area;
import java.util.List;

import org.bson.types.ObjectId;

import com.graph.model.wt.FilterParam;
import com.graph.model.wt.LockParam;
import com.graph.model.wt.SortParam;

public class GraphDefectFilterCriteria {
	public List<ObjectId> waferIds;
	public List<Integer> cat;
	public List<Integer> rbn;
	public List<Integer> fbn;
	public List<Double> xSize;
	public List<Double> ySize;
	public List<Double> dSize;
	public List<Double> dArea;
	public String pageSize;
	public String pageNo;
	public String[] selectedColumn;
	public List<FilterParam> filter;
	public List<SortParam> sort;
	public LockParam lock;
	public Boolean adder;
	public SelectionType selectionType;
	public List<ObjectId> getWaferIds() {
		return waferIds;
	}
	public List<Integer> getCat() {
		return cat;
	}
	public List<Integer> getRbn() {
		return rbn;
	}
	public List<Integer> getFbn() {
		return fbn;
	}
	public List<Double> getxSize() {
		return xSize;
	}
	public List<Double> getySize() {
		return ySize;
	}
	public List<Double> getdSize() {
		return dSize;
	}
	public List<Double> getdArea() {
		return dArea;
	}
	public String getPageSize() {
		return pageSize;
	}
	public String getPageNo() {
		return pageNo;
	}
	public String[] getSelectedColumn() {
		return selectedColumn;
	}
	public List<FilterParam> getFilter() {
		return filter;
	}
	public List<SortParam> getSort() {
		return sort;
	}
	public LockParam getLock() {
		return lock;
	}
	public Boolean getAdder() {
		return adder;
	}
	public SelectionType getSelectionType() {
		return selectionType;
	}
	public List<Area> getAreas() {
		return areas;
	}
	public List<ObjectId> getDefectIds() {
		return defectIds;
	}
	public List<Area> areas;
	public List<ObjectId> defectIds;

}
