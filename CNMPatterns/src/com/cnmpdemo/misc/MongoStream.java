package com.cnmpdemo.misc;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;

public class MongoStream {

    public static MongoCollection<Document> dbCollection;
    public static final String collectionName = "15sec-Aggregates";

    private static BasicDBObject query = new BasicDBObject();

    public static void main(String[] args) {
        new MongoStream().collectionNames();
    }

    public void collectionNames() {
        MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://35.200.24.49:27017"));

        String BTC_NAME = "BTC";

        try {
            MongoDatabase database = mongoClient.getDatabase("trades");

            MongoIterable <String> collections = database.listCollectionNames();
            for (String collectionName: collections) {
                if (collectionName.endsWith(BTC_NAME))
                    System.out.println(collectionName);
            }

            dbCollection = database.getCollection(collectionName);
            query.put("_id", "ADABTC");

            System.out.println(dbCollection.find(query).first().getDouble("high"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mongoClient.close();
        }
    }
}