package ca.sfu.cmpt276.nywfareconfig.fare.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public class FareDraftBatchRequest {
    
    @Valid 
    @NotEmpty 
    private List<FareChangeRequest> changes;

    public FareDraftBatchRequest() {
    }
    
    public FareDraftBatchRequest(@Valid @NotEmpty List<FareChangeRequest> changes) {
        this.changes = changes;
    }

    public List<FareChangeRequest> getChanges() {
        return changes;
    }

    public void setChanges(List<FareChangeRequest> changes) {
        this.changes = changes;
    }
}
