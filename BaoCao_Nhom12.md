# BÁO CÁO BÀI TẬP LỚN — NHÓM 12
## Hệ thống Đấu Giá Trực Tuyến (Online Auction System)

---

## 1. GIỚI THIỆU MỤC TIÊU VÀ PHẠM VI

### Mục tiêu
Xây dựng ứng dụng đấu giá trực tuyến theo mô hình **Client–Server**, trong đó nhiều người dùng có thể đăng ký tài khoản, đăng bán sản phẩm, tham gia đặt giá theo thời gian thực và theo dõi kết quả đấu giá. Hệ thống vận dụng đầy đủ các nguyên lý **OOP**, **Design Patterns**, xử lý **đồng thời** và kiến trúc phần mềm chuẩn mực theo yêu cầu học phần.

### Phạm vi thực hiện
- **Server**: Tiếp nhận kết nối TCP từ nhiều client đồng thời, xử lý nghiệp vụ đấu giá (đặt giá, quản lý phiên, thanh toán), đồng bộ dữ liệu với MySQL, phát sóng cập nhật giá và chat theo thời gian thực.
- **Client**: Giao diện JavaFX cho phép đăng ký/đăng nhập, duyệt và tìm kiếm sản phẩm, đặt giá thủ công và tự động, xem biểu đồ giá, chat trực tiếp trong phòng đấu giá, quản lý tài khoản và sản phẩm.
- **Tính năng tuỳ chọn**: Auto-Bidding (bot tự động), Anti-sniping (chống đặt giá phút chót), Bid History Visualization (biểu đồ diễn biến giá).
- **Hạ tầng**: Maven multi-module, JUnit 5, CI/CD qua GitHub Actions, cơ sở dữ liệu MySQL (Aiven Cloud) với connection pool HikariCP.

---

## 2. KIẾN TRÚC TỔNG THỂ HỆ THỐNG

### Sơ đồ kiến trúc

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (JavaFX)                          │
│                                                                   │
│  ┌────────────┐  ┌──────────────────┐  ┌─────────────────────┐  │
│  │  View      │  │   Controller     │  │   Model (local)     │  │
│  │  (FXML)    │←→│  (8 Controller)  │←→│  User, Auction, ... │  │
│  └────────────┘  └────────┬─────────┘  └─────────────────────┘  │
│                            │                                      │
│               ┌────────────┴──────────────┐                      │
│               │      ServerConnection     │  ← Singleton         │
│               │   (TCP Socket, port 9999) │                      │
│               └────────────┬──────────────┘                      │
│                             │ Socket #2 (broadcast listener)      │
└─────────────────────────────┼───────────────────────────────────-┘
                              │  JSON over TCP (line-delimited)
┌─────────────────────────────┼────────────────────────────────────┐
│                         SERVER                                    │
│                              │                                    │
│  ┌───────────────────────────▼─────────────────────────────┐     │
│  │              network/ClientHandler (Runnable)            │     │
│  │          Thread Pool — 50 threads (ExecutorService)      │     │
│  └──────────────────────────┬──────────────────────────────┘     │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐    │
│  │  service/AuctionServiceImpl  ←  IAuctionService           │    │
│  │  (placeManualBid, registerAutoBid, processExpiredAuctions)│    │
│  └──────────────────────────┬───────────────────────────────┘    │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐    │
│  │  util/ AuctionManager | UserManager | ItemManager         │    │
│  │        (In-Memory Cache — ConcurrentHashMap)              │    │
│  └──────────────────────────┬───────────────────────────────┘    │
│                              │                                    │
│  ┌───────────────────────────▼──────────────────────────────┐    │
│  │  dao/  AuctionDAO | UserDAO | ItemDAO                     │    │
│  │        BidTransactionDAO | ChatMessageDAO                 │    │
│  │        (PreparedStatement + HikariCP Connection Pool)     │    │
│  └──────────────────────────┬───────────────────────────────┘    │
│                              │                                    │
│                    ┌─────────▼──────────┐                        │
│                    │   MySQL (Aiven)     │                        │
│                    └────────────────────┘                        │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │  network/BroadcastManager (Observer pattern)            │     │
│  │  CopyOnWriteArrayList<PrintWriter> observers            │     │
│  │  → broadcastPriceUpdate() | broadcastChatMessage()      │     │
│  └─────────────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────────────┘
```

### Mô tả kiến trúc
Hệ thống chia làm hai module Maven độc lập (`auction_server`, `auction_client`), giao tiếp qua **TCP Socket** với giao thức **JSON line-by-line**.

**Server** được tổ chức thành 4 tầng rõ ràng: tầng mạng (`network`), tầng nghiệp vụ (`service`), tầng cache in-memory (`util`) và tầng truy xuất dữ liệu (`dao`). Khi khởi động, toàn bộ dữ liệu từ MySQL được nạp lên RAM (ConcurrentHashMap) giúp các thao tác đặt giá chỉ cần đọc/ghi RAM — tốc độ cao, tránh lock DB. Mỗi kết nối client được xử lý bởi một `ClientHandler` (Runnable) trong thread pool 50 luồng. Cập nhật giá và chat được đẩy về client theo cơ chế **push (Observer)** qua `BroadcastManager`.

**Client** tuân theo **MVC**: FXML là View, 8 Controller điều phối logic UI, `ServerConnection` (Singleton) là layer mạng duy nhất. Cập nhật UI từ background thread được đảm bảo bởi `Platform.runLater()`.

---

## 3. CÁC CHỨC NĂNG ĐẠT ĐƯỢC

### 3.1 Thiết kế lớp và cây kế thừa (0.5đ — Bắt buộc)

**Chức năng:** Xây dựng hệ thống phân cấp class đầy đủ cho toàn bộ đối tượng trong hệ thống.

**Cây kế thừa:**
```
Entity (abstract — int id)
├── User (abstract — username, email, passwordHash, phone, displayName)
│   ├── Bidder  — autoBidEnabled, maxAutoBidAmount, autoBidIncrement
│   ├── Seller  — rating
│   └── Admin
└── Item (abstract — name, description, startingPrice, imagePath)
    ├── Electronics
    ├── Arts
    ├── Fashion
    ├── RealEstate
    └── OtherItem
Auction, BidTransaction (extend Entity)
```

**Hướng giải quyết:** Tách `Entity` là lớp gốc chứa `id`, mọi đối tượng nghiệp vụ đều kế thừa. `User` và `Item` là abstract, ép các lớp con phải triển khai `getCategory()` / `getRole()`. Lý do: đảm bảo tính mở rộng — thêm loại sản phẩm hay vai trò người dùng mới không cần sửa code hiện có.

---

### 3.2 Áp dụng OOP đầy đủ (1.0đ — Bắt buộc)

**Encapsulation:** Mọi field đều `private`, truy xuất qua getter/setter. Ví dụ: `Auction.currentHighestBid` chỉ được sửa bên trong `synchronized(auction)`.

**Inheritance:** Cây kế thừa 3 tầng (Entity → User/Item → các lớp cụ thể). Server và client có bản sao model riêng biệt, tách biệt deployment.

**Polymorphism:** `IAuctionService` — `AuctionServiceImpl` cho phép thay thế implementation dễ dàng. `ItemFactory.createItem()` trả về `Item` nhưng thực tế là subclass cụ thể; `getCategory()` được override ở mỗi loại sản phẩm.

**Abstraction:** `User`, `Item` là abstract class — không thể khởi tạo trực tiếp. `IAuctionService` là interface định nghĩa hợp đồng nghiệp vụ.

**Hướng giải quyết:** Áp dụng đúng 4 trụ cột OOP xuyên suốt, không chỉ ở model mà còn ở tầng service và network. Lý do: giúp code dễ đọc, dễ kiểm thử và dễ mở rộng.

---

### 3.3 Design Patterns (1.0đ — Bắt buộc)

| Pattern | Vị trí áp dụng |
|---|---|
| **Singleton** | `ServerConnection`, `UserSession`, `UserManager`, `AuctionManager`, `ItemManager` — Double-checked locking hoặc Initialization-on-demand holder |
| **Observer** | `BroadcastManager` + kênh `SUBSCRIBE_PRICE` — Server push giá và chat đến tất cả client đăng ký |
| **Factory Method** | `ItemFactory.createItem(category, ...)` — Tạo đúng subclass Item theo category string |
| **DAO** | `AuctionDAO`, `UserDAO`, `ItemDAO`, `BidTransactionDAO`, `ChatMessageDAO` — Tách hoàn toàn logic DB khỏi nghiệp vụ |
| **Strategy** | `IAuctionService` interface + `AuctionServiceImpl` — Dễ mock khi Unit Test |

**Hướng giải quyết:** Lựa chọn các pattern phù hợp với từng vấn đề cụ thể thay vì áp dụng cơ học. Singleton cho tài nguyên chia sẻ (kết nối, cache); Observer cho realtime update; Factory cho mở rộng loại sản phẩm. Lý do: giảm coupling, tăng cohesion, code dễ bảo trì.

---

### 3.4 Quản lý người dùng và sản phẩm (1.0đ — Bắt buộc)

**Người dùng:** Đăng ký (REGISTER) với BCrypt hash mật khẩu (cost 12), phân quyền Bidder/Seller/Admin. Đăng nhập (LOGIN) với BCrypt verify. Cập nhật thông tin (UPDATE_PHONE, UPDATE_EMAIL). Admin có thể xem danh sách, xóa user (cascade xóa auction, bids, chat liên quan).

**Sản phẩm:** Đăng bán (ADD_ITEM) → `ItemFactory` tạo đúng loại → lưu DB → tự động tạo phiên `Auction` RUNNING. Hỗ trợ gallery nhiều ảnh (Cloudinary URL, phân cách dấu phẩy). Quản lý theo tab: đang bán, đang đấu giá, đã bán, đã tham gia. Admin có thể xóa sản phẩm (cascade).

**Hướng giải quyết:** Mật khẩu không bao giờ lưu plain-text, dùng BCrypt để chống rainbow table attack. Dùng PreparedStatement cho mọi truy vấn DB để chống SQL injection. Lý do: bảo mật là yêu cầu không thể bỏ qua.

---

### 3.5 Chức năng đấu giá (1.0đ — Bắt buộc)

**Luồng đấu giá:** Người dùng xem danh sách phiên → vào phòng chi tiết → nhập giá → gửi `PLACE_BID` → server kiểm tra tính hợp lệ → cập nhật RAM + DB → broadcast giá mới → tất cả client nhận được ngay.

**Trạng thái phiên:** `OPEN → RUNNING → PENDING_PAYMENT (10 phút) → FINISHED` hoặc `CANCELLED`. Scheduler kiểm tra mỗi 30 giây.

**Thanh toán:** Sau khi phiên kết thúc, người thắng có 10 phút để xác nhận (CONFIRM_PAYMENT) hoặc từ chối (CANCEL_PAYMENT). Quá hạn → tự động CANCELLED.

**Hướng giải quyết:** Tách `processExpiredAuctions()` chạy độc lập qua `ScheduledExecutorService` — không ảnh hưởng đến luồng xử lý bid. Lý do: tránh bottleneck, phiên hết hạn được đóng chính xác.

---

### 3.6 Xử lý lỗi và ngoại lệ (1.0đ — Bắt buộc)

**Hệ thống ngoại lệ tùy chỉnh:** `AuctionException` (lớp cha) và `BidTooLowException` (lớp con) cho phép phân biệt loại lỗi nghiệp vụ cụ thể.

**Xử lý phân tầng:**
- Tầng service: ném `AuctionException`/`BidTooLowException` khi giá thấp, phiên đóng
- `ClientHandler`: bắt `AuctionException` → trả message rõ ràng về client; bắt `Exception` chung → log server
- `ServerConnection` (client): xử lý `SocketTimeoutException` (timeout 10s), `IOException` (đứt mạng), tự động reconnect
- Mọi Controller: hiển thị thông báo lỗi thân thiện trên UI (`priceCheck` label, `Alert` dialog)

**Hướng giải quyết:** Phân cấp exception theo domain giúp `catch` đúng loại lỗi mà không bắt quá rộng. Lý do: client nhận được thông báo có ý nghĩa thay vì "Server error" chung chung.

---

### 3.7 Xử lý đấu giá đồng thời — Concurrency (1.0đ — Bắt buộc)

**Cơ chế:**
- `synchronized(auction)`: mọi thao tác đọc-ghi trên một phiên đấu giá đều lock trên chính object đó — nhiều phiên vẫn chạy song song.
- `ConcurrentHashMap`: `AuctionManager`, `UserManager` dùng để đảm bảo thread-safe khi thêm/xóa.
- `CopyOnWriteArrayList`: `BroadcastManager` dùng để add/remove observer an toàn khi nhiều luồng đồng thời.
- Thread pool 50 luồng: mỗi client connection độc lập.
- `ExecutorService (newCachedThreadPool)`: Auto-Bid chạy bất đồng bộ, không block luồng bid chính.
- Trường `version` trên `Auction`: optimistic concurrency, reconcile dữ liệu khi restart server.

**Hướng giải quyết:** Lock ở mức object thay vì lock toàn bộ `AuctionManager` — tránh bottleneck khi có nhiều phiên đồng thời. Lý do: đảm bảo tính nhất quán mà không hy sinh hiệu năng.

---

### 3.8 Realtime Update — Observer/Socket (0.5đ — Bắt buộc)

Client mở **socket thứ hai** riêng biệt, gửi lệnh `SUBSCRIBE_PRICE`. Server lưu `PrintWriter` của client vào `BroadcastManager` (danh sách observers). Khi có bid mới hoặc tin nhắn chat, `broadcastPriceUpdate()` / `broadcastChatMessage()` đẩy JSON đến tất cả observers ngay lập tức. Client nhận broadcast trong background thread → `Platform.runLater()` cập nhật UI. Nếu client ngắt kết nối, observer bị tự động xóa khỏi danh sách.

**Hướng giải quyết:** Tách kênh broadcast khỏi kênh request-response để tránh xung đột protocol. Observer pattern cho phép server không cần biết số lượng client — chỉ cần gọi `broadcast()`. Lý do: kiến trúc linh hoạt, dễ thêm loại broadcast mới.

---

### 3.9 Kiến trúc Client–Server (0.5đ — Bắt buộc)

Đã trình bày chi tiết tại Mục 2. Tóm tắt: Server socket port 9999, giao thức JSON/TCP, thread pool 50, tách biệt hoàn toàn logic client và server (khác package, khác JAR). Client dùng Singleton `ServerConnection` quản lý một kết nối TCP duy nhất, auto-reconnect.

---

### 3.10 MVC — JavaFX + FXML (0.5đ — Bắt buộc)

8 cặp View (FXML) – Controller tương ứng: `login`, `signup`, `home`, `selling_product`, `add_product`, `my_products`, `profile`, `admin`. `Main.changeScene()` điều hướng bằng cách swap root Scene. Controller không chứa logic DB — gọi `ServerConnection.send()`. FXML định nghĩa hoàn toàn bố cục, Controller chỉ xử lý sự kiện và cập nhật dữ liệu.

---

### 3.11 Maven và Coding Convention (0.5đ — Bắt buộc)

Dự án cấu trúc multi-module Maven: `dev/auction_server/pom.xml` và `dev/auction_client/pom.xml`. Build bằng `mvn clean verify`. Đặt tên class, method, biến theo chuẩn Java (camelCase, PascalCase). Comment giải thích logic phức tạp (anti-sniping, đệ quy bot). Không có magic number — hằng số được đặt tên (`TIMEOUT_MS = 10_000`).

---

### 3.12 Unit Test — JUnit 5 (0.5đ — Bắt buộc)

File `AuctionServiceImplTest.java` với 3 test case:
1. **`testPlaceBid_TooLow_ShouldFail`**: `assertThrows(BidTooLowException.class)` — đảm bảo giá thấp bị chặn.
2. **`testPlaceBid_ValidAmount_ShouldSucceed`**: đặt giá hợp lệ → kiểm tra `currentHighestBid` và `currentWinnerId` được cập nhật đúng.
3. **`testPlaceBid_AntiSniping_ShouldExtendEndTime`**: giả lập phiên còn 10 giây → đặt giá → `assertTrue(endTime.isAfter(nearlyEndTime))` xác nhận thời gian được gia hạn.

---

### 3.13 CI/CD — GitHub Actions (0.5đ — Bắt buộc)

File `.github/workflows/ci.yml`: trigger trên push/PR vào `main`, `master`, `develop`, `dev`. Job `build-and-test` chạy `mvn -B clean verify` trên JDK 17 (Temurin), upload JAR artifacts (14 ngày). Job `deploy` (chỉ khi push lên main/master) đóng gói release bundle (30 ngày). `concurrency.cancel-in-progress: true` hủy run cũ khi có run mới.

---

### 3.14 Auto-Bidding (0.5đ — Tuỳ chọn)

Người dùng nhập giá tối đa và bước tăng. Server đăng ký `Bidder` vào danh sách `autoBidders` của phiên. Mỗi khi có bid thật → `notifyAutoBidObservers()` → submit task vào `ExecutorService` → sau 1.5s, bot kiểm tra giá và tự đặt bằng `processBid(..., isAutoBid=true)`. Cờ `isAutoBid` ngăn vòng lặp đệ quy vô hạn giữa các bot.

---

### 3.15 Anti-Sniping (0.5đ — Tuỳ chọn)

Trong `processBid()`: nếu thời điểm đặt giá nằm trong **30 giây cuối** trước khi phiên kết thúc, thời gian kết thúc được tự động **gia hạn thêm 60 giây**. Cơ chế này hoạt động trong `synchronized(auction)`, đảm bảo thread-safe và chính xác.

---

### 3.16 Bid History Visualization (0.5đ — Tuỳ chọn)

Giao diện phòng đấu giá hiển thị **danh sách lịch sử đặt giá** (tên bidder, mức giá, thời gian) và **biểu đồ đường (`LineChart`)** diễn biến giá theo thứ tự đặt. Cả hai được cập nhật **realtime** khi nhận broadcast giá mới — nhờ cơ chế Observer nội bộ (`dialogHistoryBox`, `dialogChartSeries`). Biểu đồ hỗ trợ auto-scale trục, dark theme CSS tùy chỉnh và label giá trên từng điểm dữ liệu.

---

## 4. PHÂN CHIA CÔNG VIỆC

| STT | Thành viên | Nhiệm vụ | Điểm barem tương ứng |
|---|---|---|---|
| 1 | **Tuấn Minh** | Thiết kế toàn bộ cây kế thừa (Entity, User, Item và các lớp con); Triển khai đầy đủ 4 nguyên lý OOP; Xây dựng tầng DAO (AuctionDAO, UserDAO, ItemDAO, BidTransactionDAO, ChatMessageDAO) và kết nối DB (HikariCP); Cấu hình Maven multi-module và coding convention | Thiết kế lớp (0.5) + OOP (1.0) + DAO/DB layer + Maven (0.5) |
| 2 | **Hải Nam** | Xây dựng `AuctionServiceImpl` với `processBid()` thread-safe; Cơ chế xử lý đồng thời (`synchronized`, `ConcurrentHashMap`, `CopyOnWriteArrayList`); Tính năng **Anti-sniping**; Xử lý lỗi và ngoại lệ (`AuctionException`, `BidTooLowException`); Scheduler `processExpiredAuctions()` và luồng thanh toán | Concurrency (1.0) + Xử lý lỗi (1.0) + Anti-sniping (0.5) |
| 3 | **Trọng Tùng** | Xây dựng `ClientHandler`, `ServerConnection` (Singleton, auto-reconnect); `BroadcastManager` (Observer pattern, push realtime); Tính năng **Auto-Bidding** (Observer + ExecutorService); Tầng network server (`Main.java`, thread pool, shutdown hook); Tất cả Design Patterns (Factory, Singleton, Observer, Strategy) | Kiến trúc Client–Server (0.5) + Realtime/Observer (0.5) + Design Patterns (1.0) + Auto-Bidding (0.5) |
| 4 | **Minh Sơn** | Toàn bộ giao diện JavaFX + FXML (8 màn hình); Tích hợp **Bid History Visualization** (danh sách + LineChart realtime); Quản lý người dùng và sản phẩm (UI + logic gọi server); Unit Test (JUnit 5, 3 test case); Cấu hình CI/CD (GitHub Actions) | MVC/JavaFX (0.5) + Quản lý user/sản phẩm (1.0) + Chức năng đấu giá UI (1.0) + Bid History Visualization (0.5) + Unit Test (0.5) + CI/CD (0.5) |

> *Ghi chú: Mỗi thành viên hỗ trợ lẫn nhau trong quá trình tích hợp và debug. Phân công trên phản ánh người chịu trách nhiệm chính từng hạng mục.*

---

*Tổng điểm barem đạt được: 10 điểm bắt buộc + 1.5 điểm tuỳ chọn (Auto-Bidding + Anti-sniping + Bid History Visualization)*
