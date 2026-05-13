package app.data;

import java.util.List;

public record UsersResponse(boolean success, String message, List<UserData> users)
    implements Response {}
