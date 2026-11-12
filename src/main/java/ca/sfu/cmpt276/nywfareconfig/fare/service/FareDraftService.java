package ca.sfu.cmpt276.nywfareconfig.fare.service;

import org.springframework.stereotype.Service;

// Manages the persistence lifecycle of FareDraft entities in PostgreSQL,
// including upserting modified cells, computing old-vs-new price deltas,
// reverting individual/all drafts, and transitioning PENDING rows to COMMITTED.
@Service 
public class FareDraftService {
    
}
