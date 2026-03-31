package Common;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

public abstract class Entity implements Serializable {
    // Sử dụng protected để các lớp con (Item, User) truy cập được
    protected String id;
    protected LocalDateTime createdAt;

    // Constructor tự động sinh ID - Tiện cho việc tạo mới đối tượng
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Constructor nhận ID - Tiện cho việc load dữ liệu cũ từ DB/File
    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    // Thêm setter này để sau này Mapping dữ liệu dễ hơn
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public abstract void printInfo();
}