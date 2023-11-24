package com.graph.model.wt;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "UserPreferences")
public class UserPreferenceModel {

	@Id
	ObjectId id;

	private String userId;
	private RegularGraphList regularGraphList;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public RegularGraphList getRegularGraphList() {
		return regularGraphList;
	}

	public void setRegularGraphList(RegularGraphList regularGraphList) {
		this.regularGraphList = regularGraphList;
	}
}
