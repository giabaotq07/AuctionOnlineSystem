// common/src/main/java/app/common/dto/FetchItemImageRequest.java
package app.common.dto;

/** Yêu cầu client gửi lên để lấy ảnh item theo đường dẫn trên server. */
public record FetchItemImageRequest(int itemId, String imagePath) implements Request {}
