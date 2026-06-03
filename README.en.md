🌐 [Phiên bản Tiếng Việt](README.md)

# 🏷️ Online Auction System — Group 12

A real-time **Client-Server** auction application built in pure Java, communicating over TCP Sockets. Users can list products, place bids manually or automatically, track live price changes, and chat with other participants inside each auction session.

---

## 📋 Project Overview

The system supports:
- **Seller:** list products with a starting price and auction end time, monitor their own auction sessions.
- **Bidder:** browse products, place manual bids or enable Auto-Bid, view bid history and price charts.
- **Real-time updates:** prices refresh instantly on every connected client the moment a new bid is placed, powered by an Observer pattern (Radio Socket).
- **Live chat room:** participants in an auction session can send public messages; full chat history is persisted in the database.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 17.0.2 + FXML |
| Networking | TCP Socket (java.net) |
| Serialization | Gson 2.10.1 |
| Database | MySQL (cloud — Aiven) |
| Connection Pool | HikariCP 5.1.0 |
| Password Security | jBCrypt 0.4 |
| Image Storage | Cloudinary |
| Build Tool | Apache Maven 3.x |

---

## ⚙️ Prerequisites

- **JDK 17** or higher ([Adoptium](https://adoptium.net/) or Oracle JDK)
- **Apache Maven 3.6+**
- Internet connection (to download Maven dependencies on first run and to reach the Aiven cloud database)
- No local MySQL installation required — the database is already hosted on the cloud

---

## 📁 Project Structure

```
btl-nhom-12/
├── dev/
│   ├── pom.xml                  ← Parent POM (multi-module)
│   ├── auction_server/          ← Server module
│   │   ├── pom.xml
│   │   └── src/main/java/com/auction/
│   │       ├── Main.java                ← Server entry point (port 9999)
│   │       ├── dao/                     ← Database access (User, Item, Auction, BidTransaction, Chat)
│   │       ├── model/                   ← Domain entities (User, Item, Auction, BidTransaction...)
│   │       ├── network/                 ← ClientHandler, BroadcastManager
│   │       ├── service/                 ← Business logic (bidding, auto-bid, anti-sniping...)
│   │       ├── util/                    ← Singleton Managers, ItemFactory, PasswordUtil
│   │       └── exception/               ← Custom exceptions
│   └── auction_client/          ← Client module (JavaFX)
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/auction/client/
│           │   ├── Launcher.java        ← Client entry point
│           │   ├── Main.java            ← JavaFX scene manager
│           │   ├── controller/          ← HomeController, LoginController, SellingProductController...
│           │   ├── model/               ← AppData, client-side models
│           │   ├── network/             ← ServerConnection (Singleton TCP), Response
│           │   └── session/             ← UserSession
│           └── resources/view/          ← FXML layout files
└── README.md
```

---

## 🚀 Build & Run Commands

### Step 1 — Build the entire project

Run from the `dev/` directory (the one containing the parent `pom.xml`):

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

> The first run will download all Maven dependencies (~2–3 minutes). Subsequent builds are much faster.

---

### Step 2 — Start the Server

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

> The server prints `🌐 Server is listening on port 9999 ...` when it starts successfully.

---

### Step 3 — Start the Client

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

> ⚠️ **Linux/macOS:** Pass `-Djavafx.platform=linux` or `-Djavafx.platform=mac` during the build step:
> ```bash
> mvn clean package -DskipTests -Djavafx.platform=linux
> ```

---

## 📋 Startup Order

```
1. Finish the build  (mvn clean package -DskipTests)
2. Start the Server first
3. Wait for the line  "✅ System initialized successfully"
4. Launch the Client  (multiple windows supported — see section below)
5. Register an account or log in
```

> ❗ **Always start the Server before the Client.** The client will report a connection error if the server is not running.

---

## 💻 Running Multiple Clients on One Machine

Each client is an independent JavaFX process, so you can open as many windows as you need on a single machine to simulate concurrent users.

### Option 1 — Multiple terminals, one command each

Open **N separate terminals** and run the client launch command in each one:

**Windows:**
```bash
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar;target/dependency/*" com.auction.client.Launcher
```

**Linux / macOS:**
```bash
java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar:target/dependency/*" com.auction.client.Launcher
```

Each execution opens a fully independent application window.

---

### Option 2 — Batch script (Windows)

Create `run-clients.bat` inside `dev/auction_client/`:

```bat
@echo off
set CMD=java --module-path "target/dependency" --add-modules javafx.controls,javafx.fxml -cp "target/auction_client-1.0-SNAPSHOT.jar;target/dependency/*" com.auction.client.Launcher

start "Client 1" cmd /k %CMD%
start "Client 2" cmd /k %CMD%
start "Client 3" cmd /k %CMD%
```

Double-click `run-clients.bat` — three client windows will open simultaneously.

---

### Option 3 — Shell script (Linux / macOS)

Create `run-clients.sh` inside `dev/auction_client/`:

```bash
#!/bin/bash
CMD="java --module-path target/dependency --add-modules javafx.controls,javafx.fxml \
     -cp target/auction_client-1.0-SNAPSHOT.jar:target/dependency/* \
     com.auction.client.Launcher"

for i in 1 2 3; do
  $CMD &
done
```

Run it:
```bash
chmod +x run-clients.sh
./run-clients.sh
```

---

### Notes for multi-client testing

- Each client should **log in with a different account** to properly simulate competing bidders.
- The server supports **up to 50 simultaneous connections** (thread pool size = 50).
- Clients viewing the same auction session will receive price updates and chat messages **in real time**.

---

## ✅ Completed Features

### Authentication & Accounts
- [x] User registration (Seller / Bidder roles)
- [x] Login / Logout
- [x] BCrypt password hashing
- [x] View and update personal profile (email, phone number)

### Products & Auction Sessions
- [x] List a new product with images (Cloudinary upload, multi-image gallery)
- [x] Product categories (Arts, Electronics, Real Estate, Fashion, Other)
- [x] Create auction sessions with a custom duration
- [x] Browse active and ended auctions
- [x] Filter by category and status
- [x] **Product search** by name, description, and category (real-time, client-side)
- [x] Automatic session closure when time expires (30-second scheduler)

### Bidding
- [x] Manual bidding
- [x] **Auto-Bid** — automatically outbid competitors up to a set maximum, with a configurable step
- [x] **Anti-Sniping** — session automatically extends if a bid is placed in the last 30 seconds
- [x] Permission check: sellers cannot bid on their own items
- [x] Bid history view
- [x] Price trend chart

### Real-Time (Observer Pattern)
- [x] Instant price updates pushed to all clients watching a session
- [x] **Live chat room** — public messaging inside each auction session
- [x] Chat history persisted to database and reloaded on session entry

### Payments & Results
- [x] Confirm payment after winning
- [x] Cancel payment (session moves to CANCELLED status)
- [x] Automatic payment deadline enforcement
- [x] View products currently selling / bidding on / completed

### Technical
- [x] Client-Server architecture over TCP Socket
- [x] Thread pool (50 threads) for concurrent client handling
- [x] HikariCP connection pool (3–10 MySQL connections)
- [x] Automatic schema migration on server startup
- [x] Singleton pattern (UserManager, ItemManager, AuctionManager, ServerConnection)
- [x] Factory pattern (ItemFactory creates items by category)
- [x] CI/CD pipeline with GitHub Actions

---

## 🔗 Resources

- 📄 **Project Report (PDF):** [Add report link here]
- 🎥 **Demo Video:** [Add video link here]

---

## 👥 Group 12 — Members

| No. | Full Name      | Student ID |
|-----|----------------|------------|
| 1   | Nguyễn Hải Nam | 25021903   |
| 2   | Lê Trọng Tùng  | 25022004   |
| 3   | Trần Minh Sơn  | 25021974   |
| 4   | Phạm Tuấn Minh | 25021886   |
