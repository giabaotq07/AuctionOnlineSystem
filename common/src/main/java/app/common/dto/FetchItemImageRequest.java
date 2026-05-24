// common/src/main/java/app/common/dto/FetchItemImageRequest.java
package app.common.dto;

/** Yêu cầu client gửi lên để lấy ảnh item. Chỉ cần gửi id, server sẽ tra soát DB. */
public record FetchItemImageRequest(int itemId) implements Request {}
