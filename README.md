> 🌐 [Phiên bản Tiếng Việt](README.md)

# 🏷️ Hệ Thống Đấu Giá Trực Tuyến — Nhóm 12

Ứng dụng đấu giá theo mô hình **Client-Server** thời gian thực, xây dựng bằng Java thuần với giao tiếp qua TCP Socket. Người dùng có thể đăng sản phẩm, tham gia đặt giá thủ công hoặc tự động, theo dõi biến động giá trực tiếp và nhắn tin trong phòng chat của từng phiên đấu giá.

---

## 📋 Mô Tả Bài Toán

Hệ thống cho phép:
- **Người bán (Seller):** đăng sản phẩm, đặt giá khởi điểm và thời gian kết thúc, theo dõi phiên đấu giá của mình.
- **Người mua (Bidder):** xem danh sách sản phẩm, đặt giá thủ công hoặc bật chế độ Auto-Bid, xem lịch sử và biểu đồ giá.
- **Thời gian thực:** giá tự động cập nhật trên tất cả client đang xem phiên ngay khi có người đặt giá mới, nhờ cơ chế Observer (Radio Socket).
- **Phòng chat:** người tham gia phiên có thể nhắn tin công khai, lịch sử chat được lưu vào database.

---

## 🛠️ Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Giao diện | JavaFX 17.0.2 + FXML |
| Giao tiếp mạng | TCP Socket (java.net) |
| Serialization | Gson 2.10.1 |
| Database | MySQL (cloud — Aiven) |
| Connection Pool | HikariCP 5.1.0 |
| Bảo mật mật khẩu | jBCrypt 0.4 |
| Lưu trữ ảnh | Cloudinary |
| Build tool | Apache Maven 3.x |

---

## ⚙️ Yêu Cầu Môi Trường

- **JDK 17** trở lên ([Adoptium](https://adoptium.net/) hoặc Oracle JDK)
- **Apache Maven 3.6+**
- Kết nối Internet (để tải dependency Maven lần đầu và kết nối database Aiven)
- Không cần cài MySQL cục bộ — database đã được host sẵn trên cloud

---

## 📁 Cấu Trúc Thư Mục

```
btl-nhom-12/
├── dev/
│   ├── pom.xml                  ← Parent POM (multi-module)
│   ├── auction_server/          ← Module Server
│   │   ├── pom.xml
│   │   └── src/main/java/com/auction/
│   │       ├── Main.java                ← Entry point server (port 9999)
│   │       ├── dao/                     ← Truy vấn database (User, Item, Auction, BidTransaction, Chat)
│   │       ├── model/                   ← Các entity (User, Item, Auction, BidTransaction...)
│   │       ├── network/                 ← ClientHandler, BroadcastManager
│   │       ├── service/                 ← Business logic (đặt giá, auto-bid, anti-sniping...)
│   │       ├── util/                    ← Singleton Manager, ItemFactory, PasswordUtil
│   │       └── exception/               ← Custom exceptions
│   └── auction_client/          ← Module Client (JavaFX)
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/auction/client/
│           │   ├── Launcher.java        ← Entry point client
│           │   ├── Main.java            ← Quản lý scene JavaFX
│           │   ├── controller/          ← HomeController, LoginController, SellingProductController...
│           │   ├── model/               ← AppData, model phía client
│           │   ├── network/             ← ServerConnection (Singleton TCP), Response
│           │   └── session/             ← UserSession
│           └── resources/view/          ← Các file FXML giao diện
└── README.md
```

---

## 🚀 Câu Lệnh Build & Chạy

### Bước 1 — Build toàn bộ project

Chạy từ thư mục `dev/` (thư mục chứa parent `pom.xml`):

**Windows (CMD / PowerShell):**
```bash
cd dev
mvn clean package -DskipTests
```

**Linux / macOS:**
```bash
cd dev
mvn clean package -DskipTests
```

> Lần đầu chạy Maven sẽ tải dependencies (~2-3 phút). Các lần sau sẽ nhanh hơn.

---

### Bước 2 — Chạy Server

**Windows:**
```bash
cd dev/auction_server
java -cp "target/auction-server.jar;target/libs/*" com.auction.Main
```

**Linux / macOS:**
```bash
cd dev/auction_server
java -cp "target/auction-server.jar:target/libs/*" com.auction.Main
```

> Server sẽ in ra `🌐 Server đang lắng nghe tại port 9999 ...` khi khởi động thành công.

---

### Bước 3 — Chạy Client

**Windows:**
```bash
cd dev/auction_client
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar;target/dependency/*" com.auction.client.Launcher
```

**Linux / macOS:**
```bash
cd dev/auction_client
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar:target/dependency/*" com.auction.client.Launcher
```

> ⚠️ **Linux/macOS:** Cần truyền thêm `-Djavafx.platform=linux` hoặc `-Djavafx.platform=mac` khi build:
> ```bash
> mvn clean package -DskipTests -Djavafx.platform=linux
> ```

---

## 📋 Hướng Dẫn Chạy Server/Client Theo Thứ Tự

```
1. Build xong (mvn clean package -DskipTests)
2. Khởi động Server trước
3. Chờ thấy dòng "✅ Khởi tạo hệ thống thành công"
4. Mở Client (có thể mở nhiều cửa sổ — xem hướng dẫn bên dưới)
5. Đăng ký tài khoản hoặc đăng nhập
```

> ❗ **Luôn khởi động Server trước.** Client sẽ báo lỗi kết nối nếu Server chưa chạy.

---

## 💻 Chạy Nhiều Client Trên Cùng Một Máy

Vì mỗi Client là một tiến trình JavaFX độc lập, bạn có thể mở bao nhiêu cửa sổ tùy ý trên cùng một máy để mô phỏng nhiều người dùng đồng thời.

### Cách 1 — Mở nhiều terminal, mỗi terminal chạy một lệnh

Mở **N terminal riêng biệt**, mỗi terminal chạy lại lệnh khởi động client:

**Windows:**
```bash
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar;target/dependency/*" com.auction.client.Launcher
```

**Linux / macOS:**
```bash
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar:target/dependency/*" com.auction.client.Launcher
```

Mỗi lần chạy sẽ xuất hiện một cửa sổ ứng dụng mới, hoàn toàn độc lập.

---

### Cách 2 — Script tự động mở nhiều client (Windows)

Tạo file `run-clients.bat` trong thư mục `dev/auction_client/`:

```bat
@echo off
set CMD=java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar;target/dependency/*" com.auction.client.Launcher

start "Client 1" cmd /k %CMD%
start "Client 2" cmd /k %CMD%
start "Client 3" cmd /k %CMD%
```

Chạy: `run-clients.bat` — 3 cửa sổ client sẽ mở đồng thời.

---

### Cách 3 — Script tự động mở nhiều client (Linux / macOS)

Tạo file `run-clients.sh` trong thư mục `dev/auction_client/`:

```bash
#!/bin/bash
CMD="java --module-path target/dependency --add-modules javafx.controls,javafx.fxml \
     -cp target/auction_client-1.0-SNAPSHOT.jar:target/dependency/* \
     com.auction.client.Launcher"

for i in 1 2 3; do
  $CMD &
done
```

Chạy:
```bash
chmod +x run-clients.sh
./run-clients.sh
```

---

### Lưu ý khi chạy nhiều client

- Mỗi client cần **đăng nhập bằng tài khoản khác nhau** để mô phỏng đúng kịch bản đấu giá
- Server hỗ trợ **tối đa 50 kết nối đồng thời** (thread pool size = 50)
- Các client cùng xem một phiên sẽ nhận cập nhật giá và tin nhắn chat **theo thời gian thực**

---

## ✅ Danh Sách Chức Năng Đã Hoàn Thành

### Xác thực & Tài khoản
- [x] Đăng ký tài khoản (Seller / Bidder)
- [x] Đăng nhập / Đăng xuất
- [x] Mã hóa mật khẩu bằng BCrypt
- [x] Xem và cập nhật thông tin cá nhân (email, số điện thoại)

### Sản phẩm & Phiên Đấu Giá
- [x] Đăng sản phẩm mới kèm ảnh (upload Cloudinary, hỗ trợ nhiều ảnh)
- [x] Phân loại sản phẩm (Nghệ thuật, Điện tử, Bất động sản, Thời trang, Khác)
- [x] Tạo phiên đấu giá với thời gian tùy chọn
- [x] Xem danh sách phiên đấu giá đang diễn ra và đã kết thúc
- [x] Lọc theo danh mục và trạng thái
- [x] **Tìm kiếm sản phẩm** theo tên, mô tả, danh mục (realtime)
- [x] Tự động đóng phiên khi hết thời gian (Scheduler 30 giây)

### Đặt Giá
- [x] Đặt giá thủ công
- [x] **Auto-Bid** — tự động đặt giá theo bước tăng đến mức tối đa
- [x] Cơ chế **Anti-Sniping** — tự động gia hạn phiên nếu có bid trong 30 giây cuối
- [x] Kiểm tra quyền: Seller không tự đặt giá sản phẩm của mình
- [x] Xem lịch sử đặt giá
- [x] Biểu đồ biến động giá

### Thời Gian Thực (Observer Pattern)
- [x] Cập nhật giá tức thì trên tất cả client đang xem phiên
- [x] **Phòng chat trực tuyến** — nhắn tin trong phiên đấu giá
- [x] Lịch sử chat được lưu vào database và tải lại khi vào phòng

### Thanh Toán & Kết Quả
- [x] Xác nhận thanh toán sau khi thắng
- [x] Hủy thanh toán (phiên chuyển sang CANCELLED)
- [x] Kiểm tra deadline thanh toán tự động
- [x] Xem sản phẩm đang bán / đang đấu giá / đã kết thúc

### Kỹ Thuật
- [x] Kiến trúc Client-Server với TCP Socket
- [x] Thread pool (50 luồng) xử lý đồng thời nhiều client
- [x] Connection pool HikariCP (3-10 kết nối MySQL)
- [x] Schema migration tự động khi server khởi động
- [x] Singleton pattern (UserManager, ItemManager, AuctionManager, ServerConnection)
- [x] Factory pattern (ItemFactory tạo item theo danh mục)
- [x] CI/CD với GitHub Actions

---

## 🔗 Tài Nguyên

- 📄 **Báo cáo PDF:** [Thêm link báo cáo vào đây]
- 🎥 **Video Demo:** [Thêm link video vào đây]

---

## 👥 Thành Viên Nhóm 12

| STT | Họ và tên      | MSSV |
|-----|----------------|------|
| 1   | Nguyễn Hải Nam | 25021903 |
| 2   | Lê Trọng Tùng  | 25022004 |
| 3   | Trần Minh Sơn  | 25021974 |
| 4   | Phạm Tuấn Minh | 25021886 |
