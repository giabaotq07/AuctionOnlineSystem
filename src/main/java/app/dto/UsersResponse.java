package app.dto;

import java.util.List;

/** UsersResponse. */
public record UsersResponse(List<UserData> users) implements Response {}
