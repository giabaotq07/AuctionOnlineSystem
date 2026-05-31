# Design Patterns - Hệ thống Đấu giá Trực tuyến

Tài liệu này chỉ liệt kê các design pattern thực sự rõ ràng đang được áp dụng trong dự án. Các kỹ thuật như thread pool, transaction, concurrency control, client-server layering, exception hierarchy... không được đưa vào danh sách chính vì chúng nghiêng về kiến trúc hoặc kỹ thuật triển khai hơn là design pattern OOP.

---

## 1. Singleton Pattern
**Ý nghĩa:** Đảm bảo một class chỉ có một instance dùng chung trong vòng đời ứng dụng.

**Nơi áp dụng:**
* `app.server.database.DatabaseConnection`: Quản lý duy nhất một `HikariDataSource` dùng chung cho toàn bộ ứng dụng.
* `app.client.Client`: Mỗi client JavaFX duy trì duy nhất một socket connection đến server và nhận phản hồi.
* `app.server.network.Server`: Quản lý server socket, quản trị vòng đời và thread pool xử lý kết nối.
* `app.client.manager.UserManager`: Lưu trữ trạng thái thông tin người dùng đang đăng nhập (`currentUser`) và cache ảnh đại diện.
* `app.client.store.AuctionStore` (và các Store khác như `ItemStore`, `BidStore`, `UserListStore`): Quản lý bộ nhớ đệm (cache) cục bộ phía Client cho các preview và chi tiết đấu giá.
* `app.client.manager.ClientNotificationCenter`: Hub đơn nhất để điều phối và đăng ký nhận các gói tin thông báo từ server.
* `app.client.manager.NavigationManager`: Quản lý `primaryStage`, FXML loader và điều hướng màn hình JavaFX.

**Ghi chú:** `JsonUtil`, `PasswordUtils`, `AlertUtils` là static utility classes, không phải Singleton đúng nghĩa vì không quản lý trạng thái instance.

---

## 2. Factory Pattern / Simple Factory
**Ý nghĩa:** Đóng gói logic tạo object, giúp code gọi không cần biết class con cụ thể.

**Nơi áp dụng:**
* `app.common.models.ItemFactory`: Tạo subclass của `Item` theo `ItemType` động (như `Electronics`, `Art`, `Vehicle`).
* `app.common.protocol.PacketReq` và `app.common.protocol.PacketRes`: Sử dụng các phương thức static factory như `of(...)`, `success(...)`, `error(...)` để chuẩn hóa cách khởi tạo các gói tin DTO gửi/nhận qua Socket.

**Lợi ích trong dự án:**
* Giảm thiểu việc lạm dụng toán tử `new` rải rác trong mã nguồn.
* Đóng gói logic khởi tạo, dễ mở rộng danh mục sản phẩm mới chỉ bằng việc thêm nhánh xử lý trong `ItemFactory`.

---

## 3. Command Pattern
**Ý nghĩa:** Đóng gói mỗi request thành một object có thể thực thi qua cùng một contract.

**Nơi áp dụng:**
* `app.server.command.Command` định nghĩa method `execute(ClientHandler clientHandler, PacketReq packet)` cho request phía server.
* Các command server cụ thể như `LoginCommand`, `RegisterCommand`, `PlaceBidCommand`, `DepositCommand`, `CreateAuctionCommand`, `CancelAuctionCommand`, `SetAutoBidCommand`, `FetchAuctionSummariesCommand`.
* `app.server.network.ClientHandler` tạo bảng đăng ký `EnumMap<RequestType, Command>`, sau đó dispatch request đến command tương ứng.
* `app.client.command.Command` định nghĩa method `execute(PacketRes packet)` cho response phía client.
* `app.client.Client` tạo bảng đăng ký `EnumMap<ResponseType, Command>`, sau đó dispatch response đến command tương ứng.

**Lợi ích trong dự án:**
* Server không bị dồn logic xử lý request vào một `switch` hoặc `if-else` lớn.
* Thêm request/response mới chỉ cần thêm DTO, `RequestType`/`ResponseType`, command mới và đăng ký command.

---

## 4. Observer Pattern / Pub-Sub Variant
**Ý nghĩa:** Cho phép các thành phần đăng ký nhận sự kiện và cập nhật trạng thái tự động khi có dữ liệu mới.

**Nơi áp dụng:**
* `app.client.manager.ClientNotificationCenter` (Singleton): Đóng vai trò là trung tâm phát tán sự kiện (Publisher Hub) phía Client. Quản lý danh sách các listener (`messageListeners`, `chatListeners`, `updateListeners`, `userListListeners`) sử dụng functional interfaces như `Consumer<String>`, `Consumer<ChatResponse>` hoặc `Runnable`.
* Các JavaFX Controller (như `LiveController`, `MessController`, `AuctionController`...): Đăng ký lắng nghe các sự kiện mạng thông qua `addChatListener(...)`, `addUpdateListener(...)` để cập nhật trực tiếp giao diện JavaFX khi nhận được phản hồi.
* Phía Server: `app.server.network.Server.broadcast(PacketRes packet)` gửi thông báo tới tất cả các `ClientHandler` để đồng bộ hóa cập nhật giá thầu, tin nhắn chat, số dư ví và trạng thái phiên tức thời.

**Lợi ích trong dự án:**
* Giao diện UI phản ứng nhanh chóng với dữ liệu thời gian thực từ mạng mà không cần dùng cơ chế Polling tốn băng thông.
* Giảm sự phụ thuộc trực tiếp (tight coupling) giữa tầng mạng Socket và tầng hiển thị UI.

---

## 5. DAO Pattern
**Ý nghĩa:** Tách logic truy cập database khỏi business logic.

**Nơi áp dụng:**
* Interface trong `app.server.dao`: `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidDAO`, `AutoBidDAO`, `ChatDAO`, `NotificationDAO`.
* Implementation trong `app.server.dao.impl`: `MySqlUserDAO`, `MySqlItemDAO`, `MySqlAuctionDAO`, `MySqlBidDAO`, `MySqlAutoBidDAO`.
* `app.server.dao.BaseDAO` gồm helper dùng chung như `withConnection(...)`, `executeUpdate(...)`, `setParameters(...)`, `runInTransaction(...)`.

**Lợi ích trong dự án:**
* Service không chứa SQL trực tiếp.
* Có thể thay đổi cách lưu trữ dữ liệu bằng cách thay implementation DAO.
* Các thao tác database được gom theo từng nhóm domain rõ ràng.

---

## 6. DTO Pattern
**Ý nghĩa:** Đóng gói dữ liệu để truyền qua mạng hoặc giữa các tầng mà không kéo theo behavior nghiệp vụ.

**Nơi áp dụng:**
* Package `app.common.dto` chứa các record làm request/response DTO, ví dụ `LoginRequest`, `LoginResponse`, `CreateAuctionRequest`, `AuctionDetailResponse`, `PlaceBidRequest`, `PlaceBidResponse`, `ChatRequest`, `ChatResponse`.
* `AuctionPreview`, `ItemPreview`, `UserPreview` là DTO projection nhẹ cho màn danh sách/lịch sử, không kéo theo bid history hoặc dữ liệu nhạy cảm.
* Marker interfaces `Request` và `Response` phân biệt dữ liệu vào/ra.
* `RequestType` và `ResponseType` ánh xạ mỗi packet type với class request/response tương ứng.
* `PacketReq` và `PacketRes` serialize/deserialize payload bằng Gson.

**Lợi ích trong dự án:**
* Client-server trao đổi JSON nhất quán.
* Màn danh sách nhận payload nhẹ, còn màn chi tiết có thể nhận domain aggregate đầy đủ khi cần.
* Response trả về client đúng theo nhu cầu từng luồng, tránh gửi thừa dữ liệu.

---

## 7. Proxy Pattern
**Ý nghĩa:** Cung cấp một object đại diện cho object thật, kiểm soát lúc nào cần tải hoặc truy cập object thật.

**Nơi áp dụng:**
* `app.client.manager.AuctionDetailProxy`: Giữ `auctionId`, trả preview đã có trong cache và chỉ fetch `Auction` detail đầy đủ khi màn live cần.
* `LiveAuctionSessionStore`: Lưu proxy của phiên đang xem để `LiveController` không phải tự quản lý chi tiết lazy loading.

**Lợi ích trong dự án:**
* First scene, all auctions và history chỉ nhận `AuctionPreview` nhẹ.
* Live screen vẫn hiển thị được preview trước, sau đó proxy tải full `Auction` có item, seller, winner và bid history.
* Refresh danh sách không ghi đè mất detail đã cache.

---

## 8. MVC Pattern
**Ý nghĩa:** Tách giao diện, dữ liệu/domain model và logic điều khiển màn hình.

**Nơi áp dụng:**
* **View:** `src/main/resources/app/views/` chứa các file FXML bố cục giao diện.
* **Controller:** `app.client.controllers` chứa các controller JavaFX điều khiển logic hiển thị (như `FirstScene`, `LiveController`, `LoginController`, `RegisterController`, `DepositController`, `MessController`).
* **Model:** `app.common.models` chứa các thực thể nghiệp vụ (như `Auction`, `Item`, `Wallet`, `User`, `Bid`, `Account`, `AutoBid`).
* **Điều hướng:** `NavigationManager` chịu trách nhiệm tải tệp FXML, áp dụng stylesheet AtlantaFX/CSS và thay đổi màn hình (Scene) trên Stage.

**Lợi ích trong dự án:**
* UI, state/domain model và logic điều khiển màn hình được tách riêng.
* Controller không truy cập database trực tiếp mà gửi request qua client-server protocol.

---

## 9. Registry / Dispatcher Pattern
**Ý nghĩa:** Dùng một bảng đăng ký để ánh xạ key sang handler tương ứng, từ đó dispatch request mà không cần chuỗi `if-else` dài.

**Nơi áp dụng:**
* `ClientHandler.createCommands(...)` tạo `EnumMap<RequestType, app.server.command.Command>`.
* `ClientHandler.handlePacket(...)` lấy command bằng `commands.get(type)` rồi gọi `execute(...)`.
* `Client.createCommands(...)` tạo `EnumMap<ResponseType, app.client.command.Command>`.
* `Client.handlePacket(...)` lấy command bằng `commands.get(type)` rồi gọi `execute(...)`.
* `RequestType` và `ResponseType` đóng vai trò key cho protocol client-server.

**Lợi ích trong dự án:**
* Dễ nhìn toàn bộ request server hỗ trợ.
* Mỗi command độc lập, giảm rủi ro sửa request này ảnh hưởng request khác.

---

## 10. Facade Pattern
**Ý nghĩa:** Cung cấp một giao diện đơn giản cho client code, che giấu chi tiết phức tạp của subsystem phía sau.

**Nơi áp dụng:**
* `app.client.manager.ClientRequestService`: Cung cấp các method dễ dùng như `login(...)`, `createAuction(...)`, `placeBid(...)`, `deposit(...)`, `setAutoBid(...)`.
* Các controller JavaFX như `AuctionController`, `LiveController`, `DepositController`, `LoginController` chỉ gọi `ClientRequestService` thay vì tự tạo `PacketReq`, chọn `RequestType` và gọi `Client.sendRequest(...)`.

**Lợi ích trong dự án:**
* Controller không phụ thuộc trực tiếp vào chi tiết giao thức mạng.
* Logic đóng gói request tập trung trong một class.
* Khi cách gửi request thay đổi, phần lớn controller không cần sửa.
