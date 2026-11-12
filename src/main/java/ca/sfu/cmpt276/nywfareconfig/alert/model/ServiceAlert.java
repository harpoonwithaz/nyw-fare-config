package ca.sfu.cmpt276.nywfareconfig.alert.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "service_alerts")
public class ServiceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "origin_code", nullable = false)
    private String originCode;

    @Column(name = "destination_code", nullable = false)
    private String destinationCode;

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

    public ServiceAlert(String tenantId) {
        this.tenantId = tenantId;
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

    public String getTenantId() {
        return tenantId;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
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

    public void setOriginCode(String originCode) {
        this.originCode = originCode;
    }

    public void setDestinationCode(String destinationCode) {
        this.destinationCode = destinationCode;
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