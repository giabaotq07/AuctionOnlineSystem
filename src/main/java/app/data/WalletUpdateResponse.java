package app.data;

import app.enums.OperationStatus;

public record WalletUpdateResponse(OperationStatus status, String message, UserData user)
    implements Response {}
