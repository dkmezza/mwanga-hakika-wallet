package com.mwanga.wallet.requisition;

public enum RequisitionStatus {
    /** Awaiting admin review. */
    PENDING,

    /** Admin approved — wallet credited, transaction created. */
    APPROVED,

    /** Admin rejected — wallet unchanged. */
    REJECTED
}
