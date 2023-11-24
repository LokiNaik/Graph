package com.graph.repository.wt;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.graph.model.wt.UserPreferenceModel;

public interface UserPreferenceRepository extends MongoRepository<UserPreferenceModel, String> {

	UserPreferenceModel findByUserId(String userId);
}
