package org.mule.examples.springboot.sapdataretrieval;

/** Typed BAPI inputs, one per Mule "Transform Map to Xml&lt;BAPI_…&gt;" DataWeave script. */
public final class SapRequests {

    private SapRequests() {
    }

    /** {@code BAPI_CUSTOMER_FIND}: {@code MAX_CNT}, {@code PL_HOLD=X}, {@code SELOPT_TAB} on {@code KNA1.NAME1}. */
    public record CustomerFind(String maxCount, String namePattern) {
        public static CustomerFind of(String maxCount, String name) {
            return new CustomerFind(maxCount, name == null ? "*" : name + "*");
        }
    }

    /** {@code BAPI_MATERIAL_GETLIST}: {@code MAXROWS}, {@code MATERIALSHORTDESCSEL} {@code I CP <name>*}. */
    public record MaterialGetList(String maxRows, String descriptionPattern) {
        public static MaterialGetList of(String maxRows, String name) {
            return new MaterialGetList(maxRows, name == null ? "*" : name + "*");
        }
    }

    /** {@code BAPI_SALESORDER_GETLIST}: {@code CUSTOMER_NUMBER}, {@code SALES_ORGANIZATION}. */
    public record SalesOrderGetList(String customerNumber, String salesOrganization) {
    }
}
