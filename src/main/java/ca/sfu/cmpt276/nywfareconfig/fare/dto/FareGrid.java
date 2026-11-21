package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.util.List;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;

// Represents the unified 2D matrix structure consumed directly by the Thymeleaf template.
public class FareGrid {
    private List<String> columnHeaders;
    private List<String> rowHeaders;
    private List<List<FareCell>> rows;

    public FareGrid() {
    }

    public FareGrid(List<String> columnHeaders, List<String> rowHeaders, List<List<FareCell>> rows) {
        this.columnHeaders = columnHeaders;
        this.rowHeaders = rowHeaders;
        this.rows = rows;
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders;
    }

    public List<String> getRowHeaders() {
        return rowHeaders;
    }

    public void setRowHeaders(List<String> rowHeaders) {
        this.rowHeaders = rowHeaders;
    }

    public List<List<FareCell>> getRows() {
        return rows;
    }

    public void setRows(List<List<FareCell>> rows) {
        this.rows = rows;
    }

}
