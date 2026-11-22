package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.util.List;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;

// Represents the unified 2D matrix structure consumed directly by the Thymeleaf template.
public class FareGrid {
    private List<CatalogOption> columnHeaders;
    private List<CatalogOption> rowHeaders;
    private List<List<FareCell>> rows;

    public FareGrid() {
    }

    public FareGrid(List<CatalogOption> columnHeaders, List<CatalogOption> rowHeaders, List<List<FareCell>> rows) {
        this.columnHeaders = columnHeaders;
        this.rowHeaders = rowHeaders;
        this.rows = rows;
    }

    public List<CatalogOption> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<CatalogOption> columnHeaders) {
        this.columnHeaders = columnHeaders;
    }

    public List<CatalogOption> getRowHeaders() {
        return rowHeaders;
    }

    public void setRowHeaders(List<CatalogOption> rowHeaders) {
        this.rowHeaders = rowHeaders;
    }

    public List<List<FareCell>> getRows() {
        return rows;
    }

    public void setRows(List<List<FareCell>> rows) {
        this.rows = rows;
    }

}
