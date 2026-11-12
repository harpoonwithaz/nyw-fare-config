package ca.sfu.cmpt276.nywfareconfig.alert.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "service_notices")
public class ServiceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_notice_routes", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "route_key", nullable = false, length = 80)
    private Set<String> routeKeys = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 40)
    private TriggerType triggerType = TriggerType.WEEKENDS;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "product_code", length = 40)
    private String productCode;

    @Column(name = "message_en", nullable = false, length = 500)
    private String messageEn;

    @Column(name = "message_es", length = 500)
    private String messageEs;

    @Column(name = "confirm_label_en", length = 40)
    private String confirmLabelEn;

    @Column(name = "confirm_label_es", length = 40)
    private String confirmLabelEs;

    @Column(name = "dismiss_label_en", length = 40)
    private String dismissLabelEn;

    @Column(name = "dismiss_label_es", length = 40)
    private String dismissLabelEs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.DRAFT;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ServiceAlert() {
    }

    public ServiceAlert(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public Set<String> getRouteKeys() {
        return routeKeys;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public String getMessageEs() {
        return messageEs;
    }

    public String getConfirmLabelEn() {
        return confirmLabelEn;
    }

    public String getConfirmLabelEs() {
        return confirmLabelEs;
    }

    public String getDismissLabelEn() {
        return dismissLabelEn;
    }

    public String getDismissLabelEs() {
        return dismissLabelEs;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setRouteKeys(Set<String> keys) {
        this.routeKeys.clear();
        this.routeKeys.addAll(keys);
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public void setMessageEn(String messageEn) {
        this.messageEn = messageEn;
    }

    public void setMessageEs(String messageEs) {
        this.messageEs = messageEs;
    }

    public void setConfirmLabelEn(String confirmLabelEn) {
        this.confirmLabelEn = confirmLabelEn;
    }

    public void setConfirmLabelEs(String confirmLabelEs) {
        this.confirmLabelEs = confirmLabelEs;
    }

    public void setDismissLabelEn(String dismissLabelEn) {
        this.dismissLabelEn = dismissLabelEn;
    }

    public void setDismissLabelEs(String dismissLabelEs) {
        this.dismissLabelEs = dismissLabelEs;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}