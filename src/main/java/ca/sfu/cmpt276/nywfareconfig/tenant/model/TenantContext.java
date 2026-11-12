package ca.sfu.cmpt276.nywfareconfig.tenant.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;

// Stores the currently selected tenant and selection state in the user's session.
@Component
@SessionScope
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Tenant selectedTenant;
    private String selectedOrigin;
    private String selectedProduct;

    public TenantContext() {
    }

    public TenantContext(Tenant selectedTenant, String selectedOrigin, String selectedProduct) {
        this.selectedTenant = selectedTenant;
        this.selectedOrigin = selectedOrigin;
        this.selectedProduct = selectedProduct;
    }

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

    public boolean isSecondarySelected() {
        return selectedOrigin != null || selectedProduct != null;
    }
}