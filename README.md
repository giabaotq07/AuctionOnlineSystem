# Online Auction System — Hệ thống Đấu giá Trực tuyến

Ứng dụng **Client-Server** viết bằng **Java** hỗ trợ đấu giá trực tuyến theo thời gian thực (real-time) qua kết nối **TCP Socket**. Hệ thống cho phép nhiều người dùng đồng thời tham gia đặt giá, theo dõi biến động giá tức thì, tự động gia hạn phiên và đấu giá tự động — tất cả không cần tải lại trang.

Dự án là **Bài tập lớn môn Lập trình nâng cao (LTNC)** được xây dựng theo kiến trúc phân lớp chuẩn, áp dụng OOP nghiêm ngặt và **10 Design Patterns** kinh điển.

---

## 📋 Mô tả bài toán & Phạm vi hệ thống

| Khía cạnh | Chi tiết |
|---|---|
| **Bài toán** | Xây dựng sàn đấu giá trực tuyến nhiều người dùng, thời gian thực |
| **Phạm vi** | Đăng ký / đăng nhập; quản lý sản phẩm & phiên đấu giá; đặt giá realtime; ví điện tử; chat; thông báo |
| **Kiến trúc** | Client-Server phân lớp + MVC + 10 Design Patterns |
| **Giao tiếp** | TCP Socket, giao thức JSON tuỳ chỉnh (PacketReq / PacketRes) |
| **Phân quyền** | 3 vai trò: **Admin**, **Seller** (Người bán), **Bidder** (Người đấu giá) |

---

## 🛠️ Công nghệ sử dụng & Yêu cầu cài đặt

### Công nghệ chính

| Thành phần | Công nghệ / Thư viện |
|---|---|
| Ngôn ngữ | Java 25 (tương thích JDK 17+) |
| Giao diện Client | JavaFX 21 + AtlantaFX (theme hiện đại) |
| Cơ sở dữ liệu | MySQL 8.0 |
| Kết nối DB | HikariCP (Connection Pool) |
| Serialization | Gson (JSON) |
| Build | Apache Maven 3.8 (Maven Wrapper đi kèm) |
| Logging | Logback / SLF4J |
| Kiểm thử | JUnit 5 + JaCoCo |

### Yêu cầu cài đặt

Đảm bảo đã cài đặt trên máy:

- **JDK 17** trở lên (khuyến nghị JDK 25). Kiểm tra: `java -version`
- **MySQL Server 8.0** trở lên và đang chạy
- **Apache Maven 3.8+** *(hoặc dùng `mvnw` đi kèm dự án, không cần cài Maven)*
- *(Tuỳ chọn)* Git để clone repository

---

## 📂 Cấu trúc thư mục & Các module chính

Dự án được tổ chức dạng **Maven Multi-Module**:

```
AuctionOnlineSystem/
├── common/                      # Tài nguyên dùng chung Client & Server
│   └── src/main/java/app/common/
│       ├── dto/                 # Data Transfer Objects (JSON DTOs trao đổi qua Socket)
│       ├── enums/               # Hệ thống Enum phân quyền, trạng thái, và loại gói tin
│       ├── exception/           # Định nghĩa ngoại lệ tuỳ chỉnh của hệ thống
│       ├── mapper/              # Bộ chuyển đổi dữ liệu (Mapper) giữa các đối tượng
│       ├── models/              # Thực thể miền (Domain Models): User, Item, Auction, Bid, Wallet...
│       └── protocol/            # Giao thức gói tin mạng PacketReq/PacketRes qua JSON
│
├── server/                      # Module Server
│   └── src/main/java/app/server/
│       ├── command/             # Command handlers xử lý Request từ Client
│       ├── dao/                 # Data Access Objects (MySQL Database CRUD)
│       ├── database/            # HikariCP Connection Pool & TransactionManager
│       ├── network/             # Server socket, ClientHandler, và SessionManager
│       └── service/             # Business logic: AutoBid, AntiSnipe, AuctionScheduler
│
├── client/                      # Module Client (JavaFX)
│   └── src/main/java/app/client/
│       ├── command/             # Command handlers xử lý Response từ Server
│       ├── controllers/         # JavaFX Controllers điều khiển màn hình (AtlantaFX)
│       ├── manager/             # NavigationManager, ClientRequestService, UserManager, NotificationCenter
│       └── store/               # Lưu trữ trạng thái cục bộ (AuctionStore, LiveAuctionSessionStore, ItemStore...)
│
├── UMLdiagram/                  # Sơ đồ UML lớp (.puml + ảnh PNG xuất ra)
├── pom.xml                      # Root POM — quản lý dependency & cấu hình build multi-module
├── DESIGN_PATTERNS.md           # Tài liệu mô tả 10 Design Patterns thực tế đã áp dụng
└── REPORT_BIG.pdf               # Báo cáo PDF tóm tắt dự án (≤ 5 trang)
```

---

## 📦 Vị trí các file `.jar`

Nằm trong Mục release của GitHub: [https://github.com/giabaotq07/AuctionOnlineSystem]

---

## 🚀 Hướng dẫn chạy Server / Client theo thứ tự

### Bước 1 — Khởi tạo cơ sở dữ liệu MySQL

```sql
-- Tạo database
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
-- Nạp schema (chạy từ thư mục gốc dự án)
mysql -u root -p auction_db < server/src/main/resources/schema.sql
```

> **Lưu ý:** Mật khẩu DB mặc định trong cấu hình là `25122007`. Xem Bước 3 để thay đổi.

---

### Bước 2 — Build & đóng gói dự án (Fat JAR)

```powershell
# Chạy từ thư mục gốc dự án
.\mvnw clean package -DskipTests
```

Kết quả: file JAR xuất hiện tại `server/target/` và `client/target/`.

---

### Bước 3 — Cấu hình địa chỉ IP Server cho Client *(tuỳ chọn)*

**Cách 1 — Qua file cấu hình** (không cần build lại):

Tạo file `application.properties` đặt cùng thư mục với `client-1.0-SNAPSHOT.jar`:

```properties
server.host=192.168.1.100
```

**Cách 2 — Qua giao diện** (GUI):

Ở màn hình khởi động "Kết nối Server", nhập IP vào ô **ĐỊA CHỈ IP SERVER** rồi nhấn **KẾT NỐI**.

---

### Bước 4 — Khởi chạy Server *(chạy trước)*

```powershell
# Kết nối DB mặc định (localhost, password: 25122007)
java -jar server/target/server-1.0-SNAPSHOT.jar

# Kết nối DB tuỳ chỉnh (remote / password khác)
java -Ddb.url="jdbc:mysql://<IP_DB>:<PORT>/auction_db" `
     -Ddb.user="<USER>" `
     -Ddb.password="<PASSWORD>" `
     -jar server/target/server-1.0-SNAPSHOT.jar
```

Server khởi động thành công khi console hiển thị: `Server started on port 12345`.

---

### Bước 5 — Khởi chạy Client *(chạy sau Server, có thể mở nhiều cửa sổ)*

```powershell
java -jar client/target/client-1.0-SNAPSHOT.jar
```

> Có thể mở nhiều cửa sổ Client đồng thời để kiểm thử đa người dùng.

---

## ✅ Danh sách chức năng đã hoàn thành

### Chức năng cơ bản

- [x] **Đăng ký / Đăng nhập** — xác thực BCrypt, phân 3 vai trò Admin / Seller / Bidder
- [x] **Quản lý sản phẩm** — Seller đăng sản phẩm, phân loại (`Electronics`, `Art`, `Vehicle`), cập nhật ảnh
- [x] **Quản lý phiên đấu giá** — tạo phòng, lập lịch kích hoạt, tự động đóng khi hết giờ
- [x] **Đặt giá real-time** — broadcast tức thì đến toàn bộ client đang xem phòng qua TCP Socket
- [x] **Ví điện tử** — nạp tiền, kiểm tra số dư, đặt cọc khi tham gia đấu giá
- [x] **Chat trong phòng đấu giá** — nhắn tin realtime giữa những người tham gia
- [x] **Thông báo hệ thống** — thông báo khi phiên kết thúc, khi bị vượt giá
- [x] **Lịch sử đấu giá** — xem lại các phiên đã tham gia, kết quả thắng/thua
- [x] **Concurrency Control** — Row Locking (`SELECT … FOR UPDATE`) tránh race condition khi nhiều người đặt giá đồng thời
- [x] **Cấu hình IP linh hoạt** — qua file `application.properties` hoặc nhập trực tiếp trên GUI

### Chức năng nâng cao 

- [x] **Auto-Bidding** — đặt giá tự động theo giá trần & bước tăng cấu hình sẵn (`AutoBidService`)
- [x] **Anti-Sniping** — tự động gia hạn thêm 60 giây nếu có lượt đặt giá trong 30 giây cuối (tối đa 5 lần)
- [x] **Biểu đồ biến thiên giá** — Line Chart động vẽ trực tiếp trên Canvas JavaFX theo thời gian thực
- [x] **10 Design Patterns** — Singleton, Factory, Command, Observer, DAO, DTO, Proxy, MVC, Registry/Dispatcher, Facade

---

## 📄 Báo cáo PDF & Video Demo

- 📄 **Báo cáo PDF**: [REPORT_BIG.pdf](./REPORT_BIG.pdf) — Báo cáo tóm tắt ≤ 5 trang: mục tiêu, kiến trúc tổng thể, UML, các chức năng đạt được.
- 🎥 **Video Demo**: [https://www.youtube.com/watch?v=qxf3Nleffd0](https://www.youtube.com/watch?v=qxf3Nleffd0)
