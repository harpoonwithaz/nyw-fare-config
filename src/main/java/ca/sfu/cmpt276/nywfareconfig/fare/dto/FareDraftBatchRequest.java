package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public class FareDraftBatchRequest {
    
    @NotEmpty 
    private List<@Valid FareChangeRequest> changes;

    public FareDraftBatchRequest() {
    }
    
    public FareDraftBatchRequest(
        @NotEmpty List<@Valid FareChangeRequest> changes) {
        this.changes = changes;
    }

    public List<FareChangeRequest> getChanges() {
        return changes;
    }

    public void setChanges(List<FareChangeRequest> changes) {
        this.changes = changes;
    }
}
