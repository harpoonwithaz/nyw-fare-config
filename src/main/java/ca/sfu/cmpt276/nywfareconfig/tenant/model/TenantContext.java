package ca.sfu.cmpt276.nywfareconfig.tenant.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

import java.io.Serializable;

// Stores the currently selected tenant and selection state in the user's session.
@Component
@SessionScope
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Tenant selectedTenant;
    private String secondaryFilter;

    public TenantContext() {
    }

    public TenantContext(Tenant selectedTenant, String secondaryFilter) {
        this.selectedTenant = selectedTenant;
        this.secondaryFilter = secondaryFilter;
    }

    public Tenant getSelectedTenant() {
        return selectedTenant;
    }

    public void setSelectedTenant(Tenant selectedTenant) {
        this.selectedTenant = selectedTenant;
    }

    public String getSecondaryFilter() {
        return secondaryFilter;
    }

    public void setSecondaryFilter(String secondaryFilter) {
        this.secondaryFilter = secondaryFilter;
    }

}