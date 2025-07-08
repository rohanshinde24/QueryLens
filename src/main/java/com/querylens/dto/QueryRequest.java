// src/main/java/com/querylens/dto/QueryRequest.java
package com.querylens.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for /analyze.
 */
public class QueryRequest {

    @NotBlank
    private String sql;

    public QueryRequest() {}

    public QueryRequest(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
