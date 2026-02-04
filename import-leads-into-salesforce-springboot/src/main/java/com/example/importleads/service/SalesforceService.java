package com.example.importleads.service;

import com.example.importleads.config.SalesforceConfig;
import com.example.importleads.exception.SalesforceException;
import com.example.importleads.model.Lead;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SalesforceService {

    private static final Logger logger = LoggerFactory.getLogger(SalesforceService.class);
    private static final int BATCH_SIZE = 200;

    private final SalesforceConfig salesforceConfig;
    private PartnerConnection connection;

    public SalesforceService(SalesforceConfig salesforceConfig) {
        this.salesforceConfig = salesforceConfig;
    }

    @PostConstruct
    public void init() {
        connect();
    }

    public void connect() {
        try {
            ConnectorConfig config = new ConnectorConfig();
            config.setUsername(salesforceConfig.getUsername());
            config.setPassword(salesforceConfig.getPasswordWithToken());
            config.setAuthEndpoint(salesforceConfig.getLoginUrl() + "/services/Soap/u/61.0");
            
            logger.info("Connecting to Salesforce as user: {}", salesforceConfig.getUsername());
            connection = Connector.newConnection(config);
            logger.info("Successfully connected to Salesforce. Session ID: {}", 
                    connection.getSessionHeader().getSessionId().substring(0, 20) + "...");
        } catch (ConnectionException e) {
            logger.error("Failed to connect to Salesforce: {}", e.getMessage());
            throw new SalesforceException("Failed to connect to Salesforce", e);
        }
    }

    public Set<String> findExistingLeadEmails(List<Lead> leads) {
        Set<String> existingEmails = new HashSet<>();
        
        if (leads.isEmpty()) {
            return existingEmails;
        }

        List<String> emails = leads.stream()
                .map(Lead::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .toList();

        if (emails.isEmpty()) {
            return existingEmails;
        }

        StringBuilder queryBuilder = new StringBuilder("SELECT Id, Email FROM Lead WHERE Email IN (");
        for (int i = 0; i < emails.size(); i++) {
            if (i > 0) {
                queryBuilder.append(", ");
            }
            queryBuilder.append("'").append(escapeSOQL(emails.get(i))).append("'");
        }
        queryBuilder.append(")");

        try {
            logger.debug("Querying Salesforce for existing leads: {}", queryBuilder);
            QueryResult result = connection.query(queryBuilder.toString());
            
            if (result.getRecords() != null) {
                for (SObject record : result.getRecords()) {
                    String email = (String) record.getField("Email");
                    if (email != null) {
                        existingEmails.add(email.toLowerCase());
                    }
                }
            }
            
            logger.info("Found {} existing leads in Salesforce", existingEmails.size());
        } catch (ConnectionException e) {
            logger.error("Failed to query Salesforce for existing leads: {}", e.getMessage());
            throw new SalesforceException("Failed to query Salesforce", e);
        }

        return existingEmails;
    }

    public int createLeads(List<Lead> leads) {
        if (leads.isEmpty()) {
            logger.info("No leads to create");
            return 0;
        }

        int totalCreated = 0;
        List<List<Lead>> batches = partitionList(leads, BATCH_SIZE);

        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<Lead> batch = batches.get(batchIndex);
            logger.info("Processing batch {}/{} with {} leads", batchIndex + 1, batches.size(), batch.size());

            SObject[] sObjects = new SObject[batch.size()];
            for (int i = 0; i < batch.size(); i++) {
                Lead lead = batch.get(i);
                SObject sObject = new SObject();
                sObject.setType("Lead");
                sObject.setField("Company", lead.getCompany());
                sObject.setField("Email", lead.getEmail());
                sObject.setField("FirstName", lead.getFirstName());
                sObject.setField("LastName", lead.getLastName());
                sObjects[i] = sObject;
            }

            try {
                SaveResult[] results = connection.create(sObjects);
                for (int i = 0; i < results.length; i++) {
                    SaveResult result = results[i];
                    if (result.isSuccess()) {
                        batch.get(i).setSalesforceId(result.getId());
                        totalCreated++;
                        logger.debug("Created lead: {} with ID: {}", batch.get(i).getEmail(), result.getId());
                    } else {
                        StringBuilder errors = new StringBuilder();
                        for (com.sforce.soap.partner.Error error : result.getErrors()) {
                            errors.append(error.getMessage()).append("; ");
                        }
                        logger.error("Failed to create lead {}: {}", batch.get(i).getEmail(), errors);
                    }
                }
            } catch (ConnectionException e) {
                logger.error("Failed to create leads batch: {}", e.getMessage());
                throw new SalesforceException("Failed to create leads in Salesforce", e);
            }
        }

        logger.info("Successfully created {} leads in Salesforce", totalCreated);
        return totalCreated;
    }

    private String escapeSOQL(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "\\'");
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public boolean isConnected() {
        return connection != null;
    }
}
