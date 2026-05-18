package app.dto;

import java.util.List;

/** UserListResponse. */
public record UserListResponse(List<UserData> users) implements Response {}
