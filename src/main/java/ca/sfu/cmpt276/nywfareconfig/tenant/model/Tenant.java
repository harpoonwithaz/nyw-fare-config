package ca.sfu.cmpt276.nywfareconfig.tenant.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ca.sfu.cmpt276.nywfareconfig.fare.model.GridMode;

public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String displayName;
    private String defaultCurrency;
    private String defaultLocale;
    private List<String> supportedLocales;

    private GridMode gridMode;
    private boolean active = true;

    public Tenant() {
        this.supportedLocales = new ArrayList<String>();
    }

    public Tenant(String tenantCode, String name) {
        this.code = tenantCode;
        this.displayName = name;
    }

    public Tenant(String code, String displayName, String defaultCurrency, String defaultLocale, GridMode gridMode,
            boolean active, List<String> supportedLocales) {
        this.code = code;
        this.displayName = displayName;
        this.defaultCurrency = defaultCurrency;
        this.defaultLocale = defaultLocale;
        this.gridMode = gridMode;
        this.active = active;
        this.supportedLocales = supportedLocales;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(List<String> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(code, tenant.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}