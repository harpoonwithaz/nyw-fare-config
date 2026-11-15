package ca.sfu.cmpt276.nywfareconfig.tenant.model;

import java.io.Serializable;
import java.util.Objects;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String displayName;
    private GridMode gridMode;
    private boolean active = true;

    public Tenant() {}

    public Tenant(String code, String displayName, GridMode gridMode, boolean active) {
        this.code = code;
        this.displayName = displayName;
        this.gridMode = gridMode;
        this.active = active;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public GridMode getGridMode() { return gridMode; }
    public void setGridMode(GridMode gridMode) { this.gridMode = gridMode; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(code, tenant.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}