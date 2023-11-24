package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

@Document("AutoGraphs")
public class SaveAutoGraph {
	private String _id;
	 private String category;
	private String autographName;
	 private String accessType;
	 private List<?> autoSlides;
	 private Date createdAt;
	 private String createdBy;
	 private String updatedBy;
	 private boolean deleteFlag;
	 private Date lastUpdatedDate;  

	
	 
	 public SaveAutoGraph() {
		super();
		// TODO Auto-generated constructor stub
	}



	public String get_id() {
		return _id;
	}



	public void set_id(String _id) {
		this._id = _id;
	}



	public String getCategory() {
		return category;
	}



	public void setCategory(String category) {
		this.category = category;
	}


	public String getAutographName() {
		return autographName;
	}



	public void setAutographName(String autographName) {
		this.autographName = autographName;
	}



	public String getAccessType() {
		return accessType;
	}



	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}



	public List<?> getAutoSlides() {
		return autoSlides;
	}



	public void setAutoSlides(List<?> autoSlides) {
		this.autoSlides = autoSlides;
	}



	public Date getCreatedAt() {
		return createdAt;
	}



	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}



	public String getCreatedBy() {
		return createdBy;
	}



	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}



	public String getUpdatedBy() {
		return updatedBy;
	}



	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}



	public boolean isDeleteFlag() {
		return deleteFlag;
	}



	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}



	public Date getLastUpdatedDate() {
		return lastUpdatedDate;
	}



	public void setLastUpdatedDate(Date lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}



	
	
}
