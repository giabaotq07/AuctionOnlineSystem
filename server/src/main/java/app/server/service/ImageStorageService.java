package app.server.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lưu ảnh Base64 vào thư mục server_data/images, trả về relative path. */
public class ImageStorageService {
  private static final Logger logger = LoggerFactory.getLogger(ImageStorageService.class);

  // Dùng Paths.get(user.dir, ...) để tương thích Fat JAR trên mọi OS
  private static final Path IMAGE_DIR =
      Paths.get(System.getProperty("user.dir"), "server_data", "images");

  private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

  /** Khởi tạo thư mục lưu ảnh nếu chưa tồn tại. */
  public ImageStorageService() {
    try {
      Files.createDirectories(IMAGE_DIR);
      logger.info("Image storage directory ready: {}", IMAGE_DIR.toAbsolutePath());
    } catch (IOException e) {
      logger.error("Cannot create image directory: {}", IMAGE_DIR, e);
    }
  }

  /**
   * Decode Base64 và lưu file ảnh.
   *
   * @param base64Data chuỗi Base64 của file ảnh
   * @param originalFileName tên file gốc, dùng để lấy extension (vd: "photo.jpg")
   * @return relative path dưới dạng "server_data/images/<uuid>.<ext>" để lưu vào DB
   * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
   * @throws IOException nếu ghi file thất bại
   */
  public String save(String base64Data, String originalFileName)
      throws IllegalArgumentException, IOException {
    if (base64Data == null || base64Data.isBlank()) {
      throw new IllegalArgumentException("Dữ liệu ảnh trống.");
    }
    // Tách phần data nếu có header "data:image/jpeg;base64,..."
    String pureBase64 = stripDataUriPrefix(base64Data);

    byte[] imageBytes;
    try {
      imageBytes = Base64.getDecoder().decode(pureBase64);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Dữ liệu Base64 không hợp lệ.", e);
    }

    if (imageBytes.length > MAX_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "File ảnh vượt quá 5MB. Kích thước thực: " + (imageBytes.length / 1024 / 1024) + "MB");
    }

    String ext = extractSafeExtension(originalFileName);
    String uniqueName = UUID.randomUUID().toString() + "." + ext;

    // Đường dẫn tuyệt đối để ghi file
    Path absolutePath = IMAGE_DIR.resolve(uniqueName);
    Files.write(absolutePath, imageBytes);
    logger.info("Saved image: {}", absolutePath);

    // Trả về relative path (dùng forward slash để tương thích DB + mọi OS)
    return "server_data/images/" + uniqueName;
  }

  /** Xoá file ảnh cũ theo relative path (gọi khi update ảnh). */
  public void deleteIfExists(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return;
    }
    try {
      Path target =
          Paths.get(
              System.getProperty("user.dir"), relativePath.replace("/", java.io.File.separator));
      Files.deleteIfExists(target);
      logger.info("Deleted old image: {}", target);
    } catch (IOException e) {
      logger.warn("Could not delete old image: {}", relativePath, e);
    }
  }

  private String stripDataUriPrefix(String base64Data) {
    int commaIndex = base64Data.indexOf(',');
    if (commaIndex >= 0) {
      return base64Data.substring(commaIndex + 1);
    }
    return base64Data;
  }

  private String extractSafeExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "jpg";
    }
    String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    // Whitelist — chỉ cho phép các định dạng ảnh hợp lệ
    return switch (ext) {
      case "jpg", "jpeg", "png", "gif", "webp" -> ext;
      default -> "jpg";
    };
  }

  /**
   * Đọc file ảnh theo relative path và trả về chuỗi Base64. Dùng Paths.get(user.dir, ...) — KHÔNG
   * hardcode absolute path.
   *
   * @param relativePath vd: "server_data/images/uuid.jpg"
   * @throws IOException nếu file không tồn tại hoặc không đọc được
   */
  public String readAsBase64(String relativePath) throws IOException {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("Đường dẫn ảnh trống.");
    }
    Path filePath =
        Paths.get(
            System.getProperty("user.dir"), relativePath.replace("/", java.io.File.separator));

    if (!Files.exists(filePath)) {
      throw new IOException("File ảnh không tồn tại trên server: " + filePath.toAbsolutePath());
    }
    byte[] imageBytes = Files.readAllBytes(filePath);
    return Base64.getEncoder().encodeToString(imageBytes);
  }
}
