package com.fps.svmes.services.impl;

import com.fps.svmes.services.MongoService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import com.mongodb.client.model.ReplaceOptions;


@Service
public class MongoServiceImpl implements MongoService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabaseName;

    @Override
    public void createCollection(String collectionName) {
        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName);
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return mongoTemplate.collectionExists(collectionName);
    }

    @Override
    public void insertOne(String collectionName, Document document) {
        mongoTemplate.getCollection(collectionName).insertOne(document);
    }

    @Override
    public void replaceOne(String collectionName, Document filter, Document newDoc) {
        MongoDatabase db = mongoClient.getDatabase(mongoDatabaseName); // ✅ your real DB name
        MongoCollection<Document> collection = db.getCollection(collectionName);
        collection.replaceOne(filter, newDoc, new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteOne(String collectionName, Document filter) {
        MongoDatabase db = mongoClient.getDatabase(mongoDatabaseName);
        MongoCollection<Document> collection = db.getCollection(collectionName);
        collection.deleteOne(filter);
    }

    @Override
    public Document findOne(String collectionName, Document filter) {
        MongoDatabase db = mongoClient.getDatabase(mongoDatabaseName);
        MongoCollection<Document> collection = db.getCollection(collectionName);
        return collection.find(filter).first();
    }

}
