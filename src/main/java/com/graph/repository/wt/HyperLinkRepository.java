package com.graph.repository.wt;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.graph.model.wt.AppHyperLink;

public interface HyperLinkRepository extends MongoRepository<AppHyperLink, String> {

}
