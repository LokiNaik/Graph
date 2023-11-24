package com.graph.model.wt;

public class SortParam {

	private String field;
	private boolean asc;
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public boolean isAsc() {
		return asc;
	}
	public void setAsc(boolean asc) {
		this.asc = asc;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
	        return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    SortParam other = (SortParam) obj;
	    if (field == null) {
	        if (other.field != null)
	            return false;
	    } else if (!field.equals(other.field))
	        return false;
	    return true;
}
}

