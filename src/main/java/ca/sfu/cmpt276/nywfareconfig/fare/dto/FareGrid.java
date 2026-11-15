package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.util.List;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareCell;
import ca.sfu.cmpt276.nywfareconfig.tenant.model.Tenant;

// Represents the unified 2D matrix structure consumed directly by the Thymeleaf template.
public class FareGrid {
    private Tenant tenant;
    private String contextSelectorLabel;
    private String selectedFilterValue;
    private List<String> columnHeaders;
    private List<String> rowHeaders;
    private List<List<FareCell>> rows;

    public FareGrid() {
    }

    public FareGrid(Tenant tenant, String contextSelectorLabel, String selectedFilterValue, List<String> columnHeaders,
            List<String> rowHeaders, List<List<FareCell>> rows) {
        this.tenant = tenant;
        this.contextSelectorLabel = contextSelectorLabel;
        this.selectedFilterValue = selectedFilterValue;
        this.columnHeaders = columnHeaders;
        this.rowHeaders = rowHeaders;
        this.rows = rows;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getContextSelectorLabel() {
        return contextSelectorLabel;
    }

    public void setContextSelectorLabel(String contextSelectorLabel) {
        this.contextSelectorLabel = contextSelectorLabel;
    }

    public String getSelectedFilterValue() {
        return selectedFilterValue;
    }

    public void setSelectedFilterValue(String selectedFilterValue) {
        this.selectedFilterValue = selectedFilterValue;
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
