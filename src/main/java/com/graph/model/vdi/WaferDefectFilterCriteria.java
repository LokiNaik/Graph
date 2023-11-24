package com.graph.model.vdi;

import java.util.List;

import org.bson.types.ObjectId;

import com.graph.enums.SelectionType;

public class WaferDefectFilterCriteria {
    private List<ObjectId> waferIds;
    private List<Integer> cat;
    private List<Integer> rbn;
    private List<Integer> fbn;
    private List<Double> xSize;
    private List<Double> ySize;
    private List<Double> dSize;
    private List<Double> dArea;
    private String[] selectedColumn;
    private List<FilterParamVdi> filter;
    private List<SortParamVdi> sort;
    private LockParamVdi lock;
    private Boolean adder;
    private SelectionType selectionType;
    private List<Area> areas;
    private List<ObjectId> defectIds;

    public String[] getSelectedColumn() {
	   return selectedColumn;
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

    public List<ObjectId> getWaferIds() {
	   return waferIds;
    }

    public List<FilterParamVdi> getFilter() {
	   return filter;
    }

    public void setFilter(List<FilterParamVdi> filter) {
	   this.filter = filter;
    }

    public List<SortParamVdi> getSort() {
	   return sort;
    }

    public void setSort(List<SortParamVdi> sort) {
	   this.sort = sort;
    }

    public LockParamVdi getLock() {
	   return lock;
    }

    public void setLock(LockParamVdi lock) {
	   this.lock = lock;
    }

    public List<Area> getAreas() {
	   return areas;
    }

    public SelectionType getSelectionType() {
	   return selectionType;
    }

    public Boolean getAdder() {
	   return adder;
    }

    public List<ObjectId> getDefectIds() {
	   return defectIds;
    }

}