package com.querylens.model;

import lombok.Data;

@Data
public class QueryRequest {
    private String sql;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
