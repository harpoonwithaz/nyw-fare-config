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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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

    // Message in the tenant's main language, kept for the list page and search.
    // The column name is from when alerts were English/Spanish only.
    @Column(name = "message_en", nullable = false, length = 500)
    private String primaryMessage;

    // Wording per language code (en, es, fr, ...), following the tenant's supported locales
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_notice_texts", joinColumns = @JoinColumn(name = "notice_id"))
    @MapKeyColumn(name = "language", length = 12)
    private Map<String, AlertText> texts = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.DRAFT;

    @Column(name = "replaces_alert_id")
    private Long replacesAlertId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "published_by")
    private String publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

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

    public String getPrimaryMessage() {
        return primaryMessage;
    }

    public Map<String, AlertText> getTexts() {
        return texts;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Long getReplacesAlertId() {
        return replacesAlertId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getArchivedBy() {
        return archivedBy;
    }

    public Instant getArchivedAt() {
        return archivedAt;
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

    public void setPrimaryMessage(String primaryMessage) {
        this.primaryMessage = primaryMessage;
    }

    public void setTexts(Map<String, AlertText> texts) {
        this.texts.clear();
        if (texts != null) {
            this.texts.putAll(texts);
        }
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public void setReplacesAlertId(Long replacesAlertId) {
        this.replacesAlertId = replacesAlertId;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setArchivedBy(String archivedBy) {
        this.archivedBy = archivedBy;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }
}