package app.common.dto;

import java.util.List;

/** UserListResponse. */
public record UserListResponse(List<UserDto> users) implements Response {}
