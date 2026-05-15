# Design Patterns - He thong Dau gia Truc tuyen

Tai lieu nay chi liet ke cac design pattern thuc su ro rang dang duoc ap dung trong du an. Cac ky thuat nhu thread pool, transaction, concurrency control, client-server layering, exception hierarchy... khong duoc dua vao danh sach chinh vi chung nghieng ve kien truc hoac ky thuat trien khai hon la design pattern OOP.

---

## 1. Singleton Pattern

**Y nghia:** Dam bao mot class chi co mot instance dung chung trong vong doi ung dung.

**Noi ap dung:**

* `app.database.DatabaseConnection`: Quan ly duy nhat mot `HikariDataSource` dung chung cho toan bo ung dung.
* `app.network.Client`: Moi client JavaFX dung mot instance socket chinh de ket noi server va quan ly listener.
* `app.network.Server`: Quan ly server socket, service graph va thread pool thong qua `Server.getInstance()`.
* `app.models.DataStore`: Luu state cuc bo phia client nhu user hien tai, danh sach phien dau gia va phien dang xem.
* `app.controllers.manager.NavigationManager`: Quan ly `primaryStage`, controller hien tai va dieu huong man hinh JavaFX.

**Ghi chu:** `JsonUtil`, `PasswordUtils`, `AlertUtils` la static utility classes, khong phai Singleton dung nghia vi khong quan ly mot instance co state.

---

## 2. Factory Pattern / Simple Factory

**Y nghia:** Dong goi logic tao object, giup code goi khong can biet class con cu the.

**Noi ap dung:**

* `app.models.ItemFactory`: Tao subclass cua `Item` theo `ItemType`, vi du `Electronics`, `Art`, `Vehicle`.
* `app.models.UserFactory`: Tao subclass cua `User` theo `UserRole`, vi du `Admin`, `Seller`, `Bidder`.
* `app.models.PacketReq` va `app.models.PacketRes`: Dung static factory methods nhu `of(...)`, `success(...)`, `error(...)` de chuan hoa cach tao packet request/response.

**Loi ich trong du an:**

* Giam viec rai `new Electronics(...)`, `new Bidder(...)` o nhieu noi.
* Khi them loai item hoac role moi, logic tao object tap trung tai factory.

---

## 3. Command Pattern

**Y nghia:** Dong goi moi request thanh mot object co the thuc thi qua cung mot contract.

**Noi ap dung:**

* `app.network.Command` dinh nghia method `execute(ClientHandler clientHandler, PacketReq packet)`.
* Cac command cu the nhu `LoginCommand`, `RegisterCommand`, `PlaceBidCommand`, `DepositCommand`, `CreateAuctionCommand`, `CancelAuctionCommand`, `ChatCommand`, `FetchAuctionsCommand`.
* `app.network.ClientHandler` tao bang dang ky command theo `PacketType`, sau do dispatch request den command tuong ung.

**Loi ich trong du an:**

* Server khong bi don logic xu ly request vao mot `switch` hoac `if-else` lon.
* Them request moi chi can them DTO, `PacketType`, command moi va dang ky command.

---

## 4. Observer Pattern / Pub-Sub Variant

**Y nghia:** Cho phep nhieu thanh phan dang ky lang nghe su kien. Khi packet tuong ung xuat hien, publisher thong bao den cac listener da subscribe.

**Noi ap dung:**

* `app.network.PacketListener<T>` la callback interface.
* `app.network.Client` quan ly `Map<PacketType, CopyOnWriteArrayList<PacketListener<?>>>`.
* `Client.subscribe(...)`, `Client.unsubscribe(...)`, `Client.notifyListeners(...)` tao co che pub-sub phia client.
* Cac controller nhu `FirstScene`, `LiveController`, `MyHistoryController`, `DepositController`, `RegisterController`, `LoginController`, `MessController`, `AuctionController` dang ky listener theo tung `PacketType`.
* `app.network.Server.broadcast(...)` gui packet den nhieu client dang online de cap nhat realtime gia thau, chat, danh sach phien dau gia va vi.

**Loi ich trong du an:**

* Controller co the phan ung voi packet realtime ma khong can polling lien tuc.
* Cac man hinh khac nhau co the lang nghe cung mot loai packet theo nhu cau rieng.

---

## 5. DAO Pattern

**Y nghia:** Tach logic truy cap database khoi business logic.

**Noi ap dung:**

* Interface trong `app.dao`: `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidDAO`, `AutoBidDAO`, `ChatDAO`, `NotificationDAO`.
* Implementation trong `app.dao.impl`: `MySqlUserDAO`, `MySqlItemDAO`, `MySqlAuctionDAO`, `MySqlBidDAO`, `MySqlAutoBidDAO`.
* `app.dao.BaseDAO` gom helper dung chung nhu `withConnection(...)`, `executeUpdate(...)`, `setParameters(...)`, `runInTransaction(...)`.

**Loi ich trong du an:**

* Service khong chua SQL truc tiep.
* Co the thay doi cach luu tru du lieu bang cach thay implementation DAO.
* Cac thao tac database duoc gom theo tung nhom domain ro rang.

---

## 6. DTO Pattern

**Y nghia:** Dong goi du lieu de truyen qua mang hoac giua cac tang ma khong keo theo behavior nghiep vu.

**Noi ap dung:**

* Package `app.data` chua cac `record` lam request/response DTO, vi du `LoginRequest`, `LoginResponse`, `CreateAuctionRequest`, `AuctionDetailResponse`, `PlaceBidRequest`, `PlaceBidResponse`, `ChatRequest`, `ChatResponse`.
* Marker interfaces `Request` va `Response` phan biet du lieu vao/ra.
* `PacketType` anh xa moi packet type voi class request/response tuong ung.
* `PacketReq` va `PacketRes` serialize/deserialize payload bang Gson.

**Loi ich trong du an:**

* Client-server trao doi JSON nhat quan.
* Domain model khong bi gui nguyen trang qua socket.
* Response tra ve client chi gom du lieu can thiet.

---

## 7. Mapper Pattern

**Y nghia:** Chuyen doi giua domain model va DTO/view model de giam phu thuoc truc tiep giua tang nghiep vu va du lieu tra ve client.

**Noi ap dung:**

* `app.service.AuctionMapper`: Chuyen `Auction` thanh `AuctionSummary` va `AuctionDetail`.
* `app.data.UserData`: Tao DTO tu `User`.
* `app.data.ItemData`: Tao DTO tu `Item`.

**Loi ich trong du an:**

* Man hinh danh sach khong can nhan toan bo du lieu chi tiet.
* Response khong expose truc tiep moi field noi bo cua domain object.
* Logic tinh/chon du lieu hien thi duoc tap trung tai mapper.

---

## 8. MVC Pattern

**Y nghia:** Tach giao dien, du lieu/domain model va logic dieu khien man hinh.

**Noi ap dung:**

* **View:** `src/main/resources/app/views/` chua cac file FXML.
* **Controller:** `app.controllers` chua controller JavaFX nhu `FirstScene`, `LiveController`, `LoginController`, `RegisterController`, `DepositController`, `MessController`.
* **Model:** `app.models` chua domain object nhu `Auction`, `Item`, `Wallet`, `User`, `BidTransaction`, `Session`, `DataStore`.
* **Dieu huong:** `NavigationManager` load FXML, gan CSS va thay scene tren `Stage`.

**Loi ich trong du an:**

* UI, state/domain model va logic dieu khien man hinh duoc tach rieng.
* Controller khong truy cap database truc tiep ma gui request qua client-server protocol.

---

## 9. Registry / Dispatcher Pattern

**Y nghia:** Dung mot bang dang ky de anh xa key sang handler tuong ung, tu do dispatch request ma khong can chuoi `if-else` dai.

**Noi ap dung:**

* `ClientHandler.createCommands(...)` tao `EnumMap<PacketType, Command>`.
* `ClientHandler.handlePacket(...)` lay command bang `commands.get(type)` roi goi `execute(...)`.
* `PacketType` dong vai tro key chung cho protocol client-server.

**Loi ich trong du an:**

* De nhin toan bo request server ho tro.
* Moi command doc lap, giam rui ro sua request nay anh huong request khac.

---