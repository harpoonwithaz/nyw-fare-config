package ca.sfu.cmpt276.nywfareconfig.fare.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

/**
 * Stores the currently selected tenant and selection state in the user's session.
 */
@Component
@SessionScope
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Tenant selectedTenant;
    private String selectedOrigin;
    private String selectedProduct;

    public TenantContext() {
        // Default constructor required for Spring session proxy creation
    }

    public TenantContext(Tenant selectedTenant, String selectedOrigin, String selectedProduct) {
        this.selectedTenant = selectedTenant;
        this.selectedOrigin = selectedOrigin;
        this.selectedProduct = selectedProduct;
    }

    /**
     * Updates the tenant and resets secondary filters.
     * Prevents state bleed when switching between OD mode (origin) and Zonal mode (product).
     */
    public void resetForTenant(Tenant tenant) {
        this.selectedTenant = tenant;
        this.selectedOrigin = null;
        this.selectedProduct = null;
    }

    /**
     * Wipes all state back to initial empty context.
     */
    public void clear() {
        this.selectedTenant = null;
        this.selectedOrigin = null;
        this.selectedProduct = null;
    }

    public boolean hasTenant() {
        return this.selectedTenant != null;
    }

    public boolean hasTenant(String tenantId) {
        return this.selectedTenant != null && this.selectedTenant.getId().equalsIgnoreCase(tenantId);
    }

    // Getters and Setters

    public Tenant getSelectedTenant() {
        return selectedTenant;
    }

    public void setSelectedTenant(Tenant selectedTenant) {
        this.selectedTenant = selectedTenant;
    }

    public String getSelectedOrigin() {
        return selectedOrigin;
    }

    public void setSelectedOrigin(String selectedOrigin) {
        this.selectedOrigin = selectedOrigin;
    }

    public String getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(String selectedProduct) {
        this.selectedProduct = selectedProduct;
    }
}