package app.common.dto;

/** UploadAvatarRequest. */
public record UploadAvatarRequest(String base64Data, String originalFileName) implements Request {}
