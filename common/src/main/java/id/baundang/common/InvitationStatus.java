package id.baundang.common;

public enum InvitationStatus {

    /** Invitation is being built; not yet publicly accessible. */
    DRAFT,

    /** Invitation is live and accessible via its slug URL. */
    ACTIVE,

    /** Wedding date has passed; invitation is no longer served. */
    EXPIRED
}
