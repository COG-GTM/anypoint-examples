package org.mule.examples.springboot.parity;

/** How a body is compared. */
public enum BodyFormat {
    /** Semantic JSON comparison (key order ignored; array order per {@code jsonCompareMode}). */
    JSON,
    /** XMLUnit similarity: whitespace, comments and namespace prefixes ignored. */
    XML,
    /** Exact string match, or a regular expression when the expected value starts with {@code ~}. */
    TEXT,
    /** Body must be empty. */
    NONE,
    /** Body is not compared. */
    IGNORE
}
