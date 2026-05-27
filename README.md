# Online Auction System (Hệ thống Đấu giá Trực tuyến)

Một ứng dụng Client-Server được xây dựng bằng ngôn ngữ **Java** hỗ trợ đấu giá trực tuyến theo thời gian thực (real-time). Dự án được thiết kế theo kiến trúc phân lớp chuẩn hóa, áp dụng nghiêm ngặt các nguyên lý Lập trình Hướng đối tượng (OOP) và nhiều mẫu thiết kế (Design Patterns) kinh điển.

Dự án này là Bài tập lớn môn **Lập trình nâng cao (LTNC)**.

---

## 📂 Cấu trúc thư mục dự án

Dự án được tổ chức dưới dạng **Maven Multi-Module** nhằm phân tách rạch ròi các vai trò nghiệp vụ:

```
AuctionOnlineSystem/
├── common/                  # Module chứa các tài nguyên dùng chung giữa Client và Server
│   └── src/main/java/app/common/
│       ├── dto/             # Các Data Transfer Objects để đóng gói dữ liệu truyền qua Socket
│       ├── exceptions/      # Định nghĩa các ngoại lệ tùy chỉnh của hệ thống
│       └── models/          # Các thực thể dữ liệu nghiệp vụ (User, Item, Auction, Bid...)
├── server/                  # Module Server (quản lý kết nối Socket, Database DAO, Service logic)
│   ├── src/main/
│   │   ├── java/app/server/
│   │   │   ├── command/     # Triển khai các Command xử lý Request nhận từ Client
│   │   │   ├── dao/         # Data Access Object thực hiện các câu lệnh SQL
│   │   │   ├── database/    # Cấu hình kết nối DB (HikariCP) & Transaction Manager
│   │   │   ├── network/     # Quản lý Socket kết nối TCP, ClientHandler & Session
│   │   │   └── service/     # Logic nghiệp vụ (Auto-Bid, Anti-Snipe, Scheduler...)
│   │   └── resources/
│   │       ├── schema.sql   # Script khởi tạo cơ sở dữ liệu MySQL
│   │       └── logback.xml  # Cấu hình ghi nhận nhật ký hoạt động (Log) của Server
├── client/                  # Module Client (Giao diện JavaFX + AtlantaFX)
│   ├── src/main/
│   │   ├── java/app/client/
│   │   │   ├── command/     # Triển khai các Command xử lý Response nhận từ Server
│   │   │   ├── controllers/ # Các Controller điều khiển giao diện màn hình JavaFX
│   │   │   ├── manager/     # Quản lý luồng Client request, Navigation, User session
│   │   │   └── store/       # Cache lưu trữ trạng thái hiển thị cục bộ phía Client
│   │   └── resources/
│   │       ├── app/views/   # Định nghĩa giao diện qua file FXML và Style CSS
│   │       └── application.properties # File cấu hình mặc định (IP Server) cho Client
├── pom.xml                  # Root POM quản lý các thư viện phụ thuộc và cấu hình build chung
└── DESIGN_PATTERNS.md       # Tài liệu mô tả chi tiết các Design Patterns được sử dụng
```

---

## ⚡ Các tính năng chính

### 1. Tính năng Cơ bản
* **Quản lý & Phân quyền Người dùng**: Hỗ trợ đăng ký, đăng nhập và phân quyền rõ ràng cho 3 vai trò: **Admin** (Quản trị viên), **Seller** (Người bán), và **Bidder** (Người đấu giá).
* **Quản lý Sản phẩm (Items)**: Người bán có thể đăng sản phẩm lên sàn, phân loại sản phẩm (`Electronics`, `Art`, `Vehicle` kế thừa từ lớp trừu tượng `Item`), cập nhật thông tin và hình ảnh.
* **Quản lý Phiên đấu giá (Auctions)**: Hỗ trợ tạo phòng đấu giá mới, lập lịch tự động kích hoạt phòng theo thời gian cấu hình, đóng phiên đấu giá tự động khi hết giờ.
* **Đấu giá Real-time qua Socket**: Broadcast trực tiếp mức giá thầu mới nhất đến toàn bộ các client đang xem phòng đấu giá ngay lập tức mà không cần tải lại trang.
* **Xử lý Ngoại lệ chặt chẽ**: Toàn bộ nghiệp vụ đặt giá được kiểm duyệt nghiêm ngặt qua lớp kiểm tra giá trị (ví dụ: giá cọc, mức tăng tối thiểu, trạng thái phòng đấu giá, số dư ví).
* **Đồng thời an toàn (Concurrency Control)**: Sử dụng kỹ thuật khóa dòng dữ liệu **Row Locking** (`SELECT ... FOR UPDATE` thông qua [TransactionManager](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/java/app/server/database/TransactionManager.java)) để đảm bảo không bị lỗi race condition khi nhiều client cùng đặt giá ở một thời điểm mili-giây.
* **Cấu hình IP linh hoạt (New)**: Tách biệt địa chỉ host của Server cho phép Client kết nối động thông qua **giao diện nhập liệu** hoặc qua **file cấu hình `application.properties`**.

### 2. Tính năng Nâng cao (Điểm cộng)
* **Auto-Bidding (Đấu giá tự động)**: Người tham gia có thể cấu hình giá trần tối đa (`maxBid`) và bước tăng giá (`increment`). Hệ thống sẽ tự động đặt giá thay họ mỗi khi có người trả giá cao hơn cho đến khi đạt ngưỡng giới hạn nhờ [AutoBidService](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/java/app/server/service/AutoBidService.java).
* **Anti-Sniping (Gia hạn phiên phút chót)**: Khi phát hiện có lượt đấu giá mới trong vòng 30 giây cuối cùng, hệ thống sẽ tự động gia hạn thêm 60 giây (tối đa 5 lần) thông qua [AntiSnipeService](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/java/app/server/service/AntiSnipeService.java), tạo môi trường cạnh tranh công bằng.
* **Biểu đồ biến thiên giá trực quan**: Tích hợp vẽ biểu đồ đường (Line Chart) động hiển thị lịch sử biến động giá trực tiếp trên Canvas JavaFX trong thời gian thực.

---

## 🛠️ Kiến trúc & Design Patterns

Hệ thống được thiết kế dựa trên kiến trúc **Client-Server phân lớp** kết hợp mô hình **MVC** và áp dụng **10 Design Patterns** cốt lõi:

1. **Singleton**: Quản lý các instance duy nhất và toàn cục như [DatabaseConnection](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/java/app/server/database/DatabaseConnection.java), [NavigationManager](file:///d:/demoTemp/AuctionOnlineSystem/client/src/main/java/app/client/manager/NavigationManager.java), [Client](file:///d:/demoTemp/AuctionOnlineSystem/client/src/main/java/app/client/Client.java), và [Server](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/java/app/server/network/Server.java).
2. **Factory Method**: Tạo các đối tượng con cụ thể của sản phẩm thông qua [ItemFactory](file:///d:/demoTemp/AuctionOnlineSystem/common/src/main/java/app/common/models/ItemFactory.java).
3. **Command**: Đóng gói các yêu cầu xử lý client-server thành các đối tượng độc lập (`LoginCommand`, `PlaceBidCommand`,...) tránh việc dùng câu lệnh switch-case dài.
4. **Observer**: Lắng nghe dữ liệu gói tin Socket và cập nhật đồng bộ UI tự động cho các view đang hoạt động.
5. **DAO (Data Access Object)**: Cô lập logic truy xuất CSDL khỏi logic nghiệp vụ của các Service.
6. **DTO (Data Transfer Object)**: Chuẩn hóa dữ liệu gửi/nhận qua Socket dạng JSON bằng thư viện Gson.
7. **Proxy**: [AuctionDetailProxy](file:///d:/demoTemp/AuctionOnlineSystem/client/src/main/java/app/client/manager/AuctionDetailProxy.java) làm trung gian cache và tải chậm (lazy load) chi tiết phòng đấu giá khi cần thiết.
8. **MVC (Model-View-Controller)**: Phân tách rõ ràng giữa View (FXML/CSS), Controller (JavaFX Controllers) và Model (Entities).
9. **Registry / Dispatcher**: Ánh xạ nhanh và định tuyến chính xác các gói tin Request/Response đến Command xử lý.
10. **Facade**: Cung cấp [ClientRequestService](file:///d:/demoTemp/AuctionOnlineSystem/client/src/main/java/app/client/manager/ClientRequestService.java) rút gọn các thao tác mạng phức tạp thành các phương thức gọi hàm đơn giản phía Client.

*(Xem chi tiết triển khai cụ thể các pattern tại file tài liệu [DESIGN_PATTERNS.md](file:///d:/demoTemp/AuctionOnlineSystem/DESIGN_PATTERNS.md)).*

---

## 💻 Yêu cầu hệ thống

Trước khi chạy dự án, hãy đảm bảo máy tính của bạn đã được cài đặt:
* **Java Development Kit (JDK)**: Phiên bản **25** trở lên (Có thể cấu hình chạy trên phiên bản thấp hơn bằng cách thay đổi giá trị `<java.version>` trong file `pom.xml`).
* **Database**: MySQL Server 8.0 trở lên.
* **Build Tool**: Apache Maven 3.8 trở lên.

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy

### Bước 1: Khởi tạo Cơ sở dữ liệu MySQL

1. Tạo một database mới trong MySQL:
   ```sql
   CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Thực thi file script [schema.sql](file:///d:/demoTemp/AuctionOnlineSystem/server/src/main/resources/schema.sql) để khởi tạo các bảng dữ liệu:
   ```bash
   mysql -u root -p auction_db < server/src/main/resources/schema.sql
   ```
   *Lưu ý: Mật khẩu mặc định hệ thống kết nối là `25122007`, bạn có thể thay đổi bằng cấu hình tham số JVM khi chạy ở bước sau.*

### Bước 2: Build & Đóng gói dự án (Fat JAR)

Mở terminal tại thư mục gốc của dự án và chạy:
```powershell
# Biên dịch và đóng gói thành Fat JAR
.\mvnw clean package -DskipTests
```

Sau khi quá trình build hoàn tất, các file thực thi `.jar` sẽ được tạo ra tại:
* Server module: `server/target/server-1.0-SNAPSHOT.jar`
* Client module: `client/target/client-1.0-SNAPSHOT.jar`

*(Tùy chọn) Định dạng chuẩn hóa code trước khi commit:*
```powershell
.\mvnw spotless:apply
```

### Bước 3: Cấu hình Địa chỉ IP của Server dành cho Client

Để Client kết nối đến Server ở xa hoặc trong cùng mạng LAN, hệ thống hỗ trợ 2 cơ chế cấu hình linh hoạt:

1. **Cấu hình qua file text (Không cần build lại code)**:
   - Tạo file có tên `application.properties` đặt ngay **cạnh file `.jar`** của Client sau khi đóng gói.
   - Điền địa chỉ IP của máy chủ Server vào file:
     ```properties
     server.host=192.168.1.45
     ```
   - Khi khởi chạy file JAR Client, chương trình sẽ tự động đọc file này làm địa chỉ kết nối mặc định.

2. **Thay đổi trực tiếp trên giao diện (GUI)**:
   - Ở màn hình khởi động "Kết nối Server", hệ thống cung cấp một ô nhập liệu **ĐỊA CHỈ IP SERVER**. Bạn có thể chỉnh sửa địa chỉ này (ví dụ: `192.168.1.45` hoặc IP Public của Server trên Cloud) trực tiếp trước khi nhấn nút **KẾT NỐI**.

---

### Bước 4: Khởi chạy ứng dụng

#### 1. Khởi chạy Server
Chạy file JAR Server (kèm cấu hình CSDL nếu cần):
```powershell
# Chạy với CSDL mặc định
java -jar server/target/server-1.0-SNAPSHOT.jar

# Chạy kết nối CSDL từ xa (Remote DB)
java -Ddb.url="jdbc:mysql://<IP_DATABASE>:<PORT>/auction_db" -Ddb.user="<TÊN_ĐĂNG_NHẬP>" -Ddb.password="<MẬT_KHẨU>" -jar server/target/server-1.0-SNAPSHOT.jar
```

#### 2. Khởi chạy Client (Có thể mở nhiều Client cùng lúc)
Chạy file JAR Client từ terminal:
```powershell
java -jar client/target/client-1.0-SNAPSHOT.jar
```

---

## 📄 Báo cáo & Video Demo
* 📄 **Tài liệu Báo cáo PDF**: [2026-Bài tập lớn.pdf](file:///d:/demoTemp/AuctionOnlineSystem/2026-B%C3%A0i%20t%E1%BA%ADp%20l%E1%BB%9Bn.pdf) (Báo cáo tóm tắt dưới 5 trang mô tả kiến trúc, sơ đồ UML lớp).
* 🎥 **Video Demo ứng dụng**: [Đường dẫn Video Demo](https://github.com/giabaotq07/AuctionOnlineSystem) *(Nhóm chỉnh sửa đường dẫn thực tế tại đây trước khi nộp)*.
