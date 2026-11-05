package ca.sfu.cmpt276.nywfareconfig.fare.model;

import java.io.Serializable;

// Represents the selected tenant, such as NYW_SANDBOX or METRA, and identifies its grid mode.
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String displayName;
    private GridMode gridMode;
    private boolean active;

    public Tenant() {}
    
    public Tenant(String id, String displayName, GridMode gridMode, boolean active) {
        this.id = id;
        this.displayName = displayName;
        this.gridMode = gridMode;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public GridMode getGridMode() {
        return gridMode;
    }

    public void setGridMode(GridMode gridMode) {
        this.gridMode = gridMode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


}
