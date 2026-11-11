package org.mule.examples.springboot.importingacsvfileintomongodb;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Body of flow {@code csv-to-mongodbFlow1} after the file is read: enricher + {@code mongo:exists-collection},
 * choice {@code mongo:create-collection}, then {@code foreach} {@code mongo:save-object-from-map} into
 * {@code customers}.
 */
@Component
class CustomerImporter {

    static final String COLLECTION = "customers";
    private static final Logger log = LoggerFactory.getLogger(CustomerImporter.class);

    private final MongoTemplate mongo;

    CustomerImporter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    void importCsv(String csv) {
        List<Map<String, Object>> customers = CustomerCsv.parse(csv);
        log.info("saving objects: {}", customers);
        boolean existsCollection = mongo.collectionExists(COLLECTION);
        if (!existsCollection) {
            mongo.createCollection(COLLECTION);
        } else {
            log.info("Customer Collection already exists.");
        }
        for (Map<String, Object> customer : customers) {
            mongo.insert(new Document(customer), COLLECTION);
        }
    }
}
