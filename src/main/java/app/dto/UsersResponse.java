package app.dto;

import java.util.List;

/** UsersResponse. */
public record UsersResponse(boolean success, String message, List<UserData> users)
    implements Response {}
