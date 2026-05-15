package app.dto;

import app.enums.OperationStatus;

/** WalletUpdateResponse. */
public record WalletUpdateResponse(OperationStatus status, String message, UserData user)
    implements Response {}
