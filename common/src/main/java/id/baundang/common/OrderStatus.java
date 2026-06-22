package id.baundang.common;

public enum OrderStatus {

    /** Order created, awaiting payment. */
    PENDING,

    /** Payment confirmed by payment gateway. */
    PAID,

    /** Buyer has submitted a revision request. */
    IN_REVISION,

    /** Invitation delivered and accepted. */
    COMPLETED,

    /** Order cancelled before completion. */
    CANCELLED
}
