package app.common.dto;

/** UploadImageRequest. */
public record UploadImageRequest(int itemId, String base64Data, String originalFileName)
    implements Request {}
