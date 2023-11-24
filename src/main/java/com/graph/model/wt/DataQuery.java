package com.graph.model.wt;

import java.util.List;

public class DataQuery {
private String _id;
private String queryName;
private List<FilterParam> filterParams;

public String get_id() {
    return _id;
}
public void set_id(String _id) {
    this._id = _id;
}
public String getQueryName() {
    return queryName;
}
public void setQueryName(String queryName) {
    this.queryName = queryName;
}
public List<FilterParam> getFilterParams() {
    return filterParams;
}
public void setFilterParams(List<FilterParam> filterParams) {
    this.filterParams = filterParams;
}


}
