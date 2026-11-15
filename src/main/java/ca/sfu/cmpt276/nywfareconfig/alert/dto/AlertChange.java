package ca.sfu.cmpt276.nywfareconfig.alert.dto;

import ca.sfu.cmpt276.nywfareconfig.alert.model.ServiceAlert;

public record AlertChange(ServiceAlert proposed, ServiceAlert current) {

    public boolean isNew() {
        return current == null;
    }
}