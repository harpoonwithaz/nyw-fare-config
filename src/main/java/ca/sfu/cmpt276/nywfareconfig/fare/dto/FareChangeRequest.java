package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import ca.sfu.cmpt276.nywfareconfig.fare.model.FareAction;
import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// Inbound DTO bound from browser AJAX/form payloads in FareController
public class FareChangeRequest {

    @NotBlank(message = "Tenant code is required.")
    @Size(max = 50, message = "Tenant code must not exceed 50 characters.")
    private String tenantCode;

    @NotNull(message = "Grid mode is required.")
    private GridMode gridMode;

    @NotBlank(message = "Origin identifier is required.")
    @Size(max = 50, message = "Origin identifier must not exceed 50 characters.")
    private String originIdentifier;

    @NotBlank(message = "Destination identifier is required.")
    @Size(max = 50, message = "Destination identifier must not exceed 50 characters.")
    private String destIdentifier;

    @NotBlank(message = "Product code is required.")
    @Size(max = 50, message = "Product code must not exceed 50 characters.")
    private String productCode;

    @DecimalMin(value = "0.00", inclusive = true, message = "Fare must be $0.00 or greater.")
    @DecimalMax(value = "9999.99", inclusive = true, message = "Fare cannot exceed $9999.99.")
    @Digits(integer = 6, fraction = 2, message = "Fare must have at most 2 decimal places.")
    private BigDecimal newPrice;

    @NotNull (message = "Target status must be VALUE or NOT_SOLD.")
    private FareStatus targetStatus;

    @NotNull(message = "Invalid action type.")
    private FareAction actionType;

    @Size(max = 50, message = "Tier code must not exceed 50 characters.")
    private String tierCode; // Optional; populated for ZONAL tier actions

    public FareChangeRequest() {
    }

    public FareChangeRequest(
            String tenantCode,
            GridMode gridMode,
            String originIdentifier,
            String destIdentifier,
            String productCode,
            BigDecimal newPrice,
            FareStatus targetStatus,
            FareAction actionType,
            String tierCode) {
        this.tenantCode = tenantCode;
        this.gridMode = gridMode;
        this.originIdentifier = originIdentifier;
        this.destIdentifier = destIdentifier;
        this.productCode = productCode;
        this.newPrice = newPrice;
        this.targetStatus = targetStatus;
        this.actionType = actionType;
        this.tierCode = tierCode;
    }

    /**
     * Requires a non-null price whenever the cell is sellable (ACTIVE),
     * while allowing null when toggling a zonal intersection to NOT_SOLD.
     */
    @AssertTrue(message = "Proposed price is required when target status is ACTIVE.")
    public boolean isPriceValidForTargetStatus() {
        if (targetStatus == FareStatus.NOT_SOLD) {
            return true;
        }
        return newPrice != null;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public GridMode getGridMode() {
        return gridMode;
    }

    public void setGridMode(GridMode gridMode) {
        this.gridMode = gridMode;
    }

    public String getOriginIdentifier() {
        return originIdentifier;
    }

    public void setOriginIdentifier(String originIdentifier) {
        this.originIdentifier = originIdentifier;
    }

    public String getDestIdentifier() {
        return destIdentifier;
    }

    public void setDestIdentifier(String destIdentifier) {
        this.destIdentifier = destIdentifier;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public FareStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(FareStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public FareAction getActionType() {
        return actionType;
    }

    public void setActionType(FareAction actionType) {
        this.actionType = actionType;
    }

    public String getTierCode() {
        return tierCode;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }
}