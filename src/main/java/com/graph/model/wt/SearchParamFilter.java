package com.graph.model.wt;

public class SearchParamFilter {
		private String[] waferID;
		private int pageNo;
		private int pageSize;
		private String noInspection;
		private String dimension;
		private String measure;
		private String aggregationType;
		private String sorting;
		private int BinNumber;
		private String[] testID;
		
		public int getBinNumber() {
			return BinNumber;
		}
		public void setBinNumber(int binNumber) {
			BinNumber = binNumber;
		}
		public String[] getWaferID() {
			return waferID;
		}
		public void setWaferID(String[] waferID) {
			this.waferID = waferID;
		}
		public int getPageNo() {
			return pageNo;
		}
		public void setPageNo(int pageNo) {
			this.pageNo = pageNo;
		}
		public int getPageSize() {
			return pageSize;
		}
		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}
		public String getDimension() {
			return dimension;
		}
		public void setDimension(String dimension) {
			this.dimension = dimension;
		}
		public String getMeasure() {
			return measure;
		}
		public void setMeasure(String measure) {
			this.measure = measure;
		}
		public String getAggregationType() {
			return aggregationType;
		}
		public void setAggregationType(String aggregationType) {
			this.aggregationType = aggregationType;
		}
		public String getSorting() {
			return sorting;
		}
		public void setSorting(String sorting) {
			this.sorting = sorting;
		}
		public String getNoInspection() {
			return noInspection;
		}
		public void setNoInspection(String noInspection) {
			this.noInspection = noInspection;
		}
		public String[] getTestID() {
			return testID;
		}
		public void setTestID(String[] testID) {
			this.testID = testID;
		}
	
}
