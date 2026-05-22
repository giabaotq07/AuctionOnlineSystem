// common/src/main/java/app/common/dto/FetchItemImageResponse.java
package app.common.dto;

/** Server trả về: Base64 của file ảnh. Client decode trực tiếp, không đụng filesystem. */
public record FetchItemImageResponse(int itemId, String base64Data) implements Response {}
