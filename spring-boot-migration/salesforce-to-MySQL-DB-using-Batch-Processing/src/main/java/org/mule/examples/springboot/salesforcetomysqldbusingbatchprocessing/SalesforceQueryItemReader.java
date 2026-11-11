package org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing;

import org.mule.examples.springboot.salesforcetomysqldbusingbatchprocessing.salesforce.SalesforceClient;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Batch input: runs the poll's SOQL query when the step opens and records the next watermark, the MAX of
 * {@code selectorField} over every returned record ({@code selector="MAX"}), in the step execution
 * context under {@link #NEXT_WATERMARK}. No key is written when nothing was returned (the watermark keeps
 * its value) or when the query failed (the step fails before reading).
 */
public class SalesforceQueryItemReader implements ItemStreamReader<Map<String, Object>> {

    public static final String NEXT_WATERMARK = "watermark.next";

    private final SalesforceClient salesforce;
    private final String soql;
    private final String selectorField;
    private Iterator<Map<String, Object>> records;

    public SalesforceQueryItemReader(SalesforceClient salesforce, String soql, String selectorField) {
        this.salesforce = salesforce;
        this.soql = soql;
        this.selectorField = selectorField;
    }

    @Override
    public void open(ExecutionContext context) {
        List<Map<String, Object>> result = salesforce.query(soql);
        result.stream().map(r -> r.get(selectorField)).filter(Objects::nonNull).map(Object::toString)
                .max(String::compareTo)
                .ifPresent(max -> context.putString(NEXT_WATERMARK, max));
        records = result.iterator();
    }

    @Override
    public Map<String, Object> read() {
        return records != null && records.hasNext() ? records.next() : null;
    }
}
