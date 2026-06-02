package com.auction.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.dao.BidTransactionDAO;
import com.auction.exception.AuctionException;
import com.auction.model.Admin;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.service.AuctionServiceImpl;
import com.auction.service.IAuctionService;
import com.auction.util.AuctionManager;
import com.auction.util.ItemManager;
import com.auction.util.PasswordUtil;
import com.auction.util.UserManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Gson gson;
    private IAuctionService auctionService;
    private BidTransactionDAO bidTransactionDAO;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gson = new Gson();
        this.auctionService = new AuctionServiceImpl();
        this.bidTransactionDAO = new BidTransactionDAO();
    }

    @Override
    public void run() {
        java.io.PrintWriter outRef = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("📥 Server nhận: " + inputLine);

                Map<String, Object> request = gson.fromJson(inputLine, new TypeToken<Map<String, Object>>(){}.getType());
                String action = (String) request.get("action");
                Object payload = request.get("payload");

                Map<String, Object> responseMap = new HashMap<>();

                if ("REGISTER".equals(action)) {
                    responseMap = handleRegister(payload);
                } else if ("LOGIN".equals(action)) {
                    responseMap = handleLogin(payload);
                } else if ("ADD_ITEM".equals(action)) {
                    responseMap = handleAddItem(payload);
                } else if ("GET_AUCTIONS".equals(action)) {
                    responseMap = handleGetAuctions(payload);
                } else if ("PLACE_BID".equals(action)) {
                    responseMap = handlePlaceBid(payload);
                } else if ("GET_BID_HISTORY".equals(action)) {
                    responseMap = handleGetBidHistory(payload);
                } else if ("REGISTER_AUTO_BID".equals(action)) {
                    responseMap = handleRegisterAutoBid(payload);
                } else if ("GET_MY_AUCTION_RESULTS".equals(action)) {
                    responseMap = handleGetMyAuctionResults(payload);
                } else if ("GET_MY_PRODUCTS".equals(action)) {
                    responseMap = handleGetMyProducts(payload);
                } else if ("CONFIRM_PAYMENT".equals(action)) {
                    responseMap = handleConfirmPayment(payload);
                } else if ("CANCEL_PAYMENT".equals(action)) {
                    responseMap = handleCancelPayment(payload);
                } else if ("UPDATE_PHONE".equals(action)) {
                    responseMap = handleUpdatePhone(payload);
                } else if ("UPDATE_EMAIL".equals(action)) {
                    responseMap = handleUpdateEmail(payload);
                } else if ("SEND_CHAT".equals(action)) {
                    responseMap = handleSendChat(payload);
                } else if ("SUBSCRIBE_PRICE".equals(action)) {
                    System.out.println("🎧 Một Client vừa đăng ký nghe Đài phát thanh giá!");
                    BroadcastManager.addObserver(out);
                    // Bắt nó chờ mãi mãi ở đây để giữ cáp mạng không bị đứt
                    while(in.read() != -1) { }
                    return; // Nếu đứt mạng thì thoát hàm run()


                } else {
                    responseMap.put("success", false);
                    responseMap.put("message", "Hành động không hợp lệ!");
                }

                String jsonResponse = gson.toJson(responseMap);
                out.println(jsonResponse);
                System.out.println("📤 Server trả lời: " + jsonResponse);
            }
        } catch (Exception e) {
            System.err.println("❌ Ngắt kết nối với Client: " + e.getMessage());
        } finally {
            // ========================================================
            // 4. CHỖ NÀY LÀ ĐOẠN EM HỎI ĐÂY:
            // Rất quan trọng: Xóa cái loa đi khi đứt cáp (Dùng biến outRef)
            // ========================================================
            if (outRef != null) {
                BroadcastManager.removeObserver(outRef);
            }
        }

    }

    private Map<String, Object> handleRegister(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, String> userData = gson.fromJson(gson.toJson(payload), new TypeToken<Map<String, String>>(){}.getType());
            String username    = userData.get("username");       // tên đăng nhập
            String email       = userData.getOrDefault("email", userData.getOrDefault("account_name", "")); // email thật
            String displayName = userData.getOrDefault("account_name", username); // họ tên hiển thị
            String password    = userData.get("password");
            String role        = userData.get("role");
            String phone       = userData.getOrDefault("phone", "");

            boolean isDuplicate = UserManager.getInstance().getAllUsers().stream()
                    .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));

            if (isDuplicate) {
                response.put("success", false);
                response.put("message", "Tên đăng nhập '" + username + "' đã tồn tại!");
            } else {
                String hashedPassword = PasswordUtil.hash(password);
                User newUser;
                if ("SELLER".equals(role)) {
                    newUser = new Seller(0, username, email, hashedPassword);
                } else {
                    newUser = new Bidder(0, username, email, hashedPassword);
                }
                newUser.setDisplayName(displayName);
                newUser.setPhone(phone);
                UserManager.getInstance().addUser(newUser);
                response.put("success", true);
                response.put("message", "Đăng ký tài khoản thành công!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi xử lý đăng ký: " + e.getMessage());
        }
        return response;
    }

    private Map<String, Object> handleLogin(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, String> loginData = gson.fromJson(gson.toJson(payload), new TypeToken<Map<String, String>>(){}.getType());
            String username = loginData.get("username");
            String password = loginData.get("password");

            User matchedUser = null;
            for (User u : UserManager.getInstance().getAllUsers()) {
                // Dùng BCrypt verify thay vì so sánh plain text
                if (u.getUsername().equals(username) && PasswordUtil.verify(password, u.getPasswordHash())) {
                    matchedUser = u;
                    break;
                }
            }

            if (matchedUser != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id",          matchedUser.getId());
                userInfo.put("username",    matchedUser.getUsername());
                userInfo.put("displayName", matchedUser.getDisplayName()); // họ tên hiển thị
                userInfo.put("email",       matchedUser.getEmail());        // email thật
                userInfo.put("phone",       matchedUser.getPhone() != null ? matchedUser.getPhone() : "");

                if (matchedUser instanceof Seller) userInfo.put("role", "SELLER");
                else if (matchedUser instanceof Admin) userInfo.put("role", "ADMIN");
                else userInfo.put("role", "BIDDER");

                response.put("success", true);
                response.put("message", "Đăng nhập thành công!");
                response.put("data", userInfo);
            } else {
                response.put("success", false);
                response.put("message", "Sai tài khoản hoặc mật khẩu!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi đăng nhập: " + e.getMessage());
        }
        return response;
    }

    // ĐÃ SỬA: Tự động gom Sản phẩm vào một Phiên đấu giá mới
    private Map<String, Object> handleAddItem(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> itemData = gson.fromJson(gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());

            String name = (String) itemData.get("name");
            String description = (String) itemData.get("description");
            double price = ((Number) itemData.get("startingPrice")).doubleValue();
            String imagePath = (String) itemData.getOrDefault("imagePath", "");
            int sellerId = ((Number) itemData.getOrDefault("sellerId", -1)).intValue();
            // Lấy thời gian đấu giá, mặc định 1440 phút (1 ngày) nếu không truyền
            long durationMinutes = itemData.get("durationMinutes") != null
                    ? ((Number) itemData.get("durationMinutes")).longValue()
                    : 1440L;

            // 1. Lưu sản phẩm vào kho
// Lấy category từ Client gửi lên (nếu không có thì mặc định là Other)
            String category = (String) itemData.getOrDefault("category", "Other");

            // 1. Dùng Nhà máy để đúc ra sản phẩm cụ thể thay vì dùng new Item()
            Item newItem = com.auction.util.ItemFactory.createItem(category, 0, name, description, price, imagePath);
            ItemManager.getInstance().addItem(newItem);

            // 2. Tạo phiên đấu giá theo thời gian người dùng chọn
            Auction newAuction = new Auction(
                    0,
                    sellerId,
                    newItem.getId(),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMinutes(durationMinutes),
                    newItem.getStartingPrice()
            );
            newAuction.setStatus("RUNNING");
            AuctionManager.getInstance().addAuction(newAuction);

            response.put("success", true);
            response.put("message", "Đăng bán và mở phiên đấu giá thành công!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi thêm sản phẩm: " + e.getMessage());
        }
        return response;
    }

    // ĐÃ SỬA: Ghép thông tin Auction và Item lại với nhau để gửi cho Client
    private Map<String, Object> handleGetAuctions(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
                Item item = ItemManager.getInstance().getItem(auction.getItemId());
                if (item != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", auction.getId());
                    map.put("sellerId", auction.getSellerId());
                    map.put("name", item.getName());
                    map.put("description", item.getDescription());
                    map.put("itemImagePath", item.getImagePath());

                    map.put("category", item.getCategory());
                    map.put("startingPrice", item.getStartingPrice());
                    map.put("currentHighestBid", auction.getCurrentHighestBid());
                    map.put("status", auction.getStatus());

                    String formattedEnd = auction.getEndTime().toString().replace("T", " ");
                    if (formattedEnd.contains(".")) formattedEnd = formattedEnd.substring(0, formattedEnd.indexOf("."));
                    map.put("endTime", formattedEnd);

                    if (auction.getPaymentDeadline() != null) {
                        String deadline = auction.getPaymentDeadline().toString().replace("T", " ");
                        if (deadline.contains(".")) deadline = deadline.substring(0, deadline.indexOf("."));
                        map.put("paymentDeadline", deadline);
                    }

                    resultList.add(map);
                }
            }

            response.put("success", true);
            response.put("message", "Tải danh sách đấu giá thành công!");
            response.put("data", resultList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /** THÊM MỚI: Xử lý đặt giá từ Client */
    private Map<String, Object> handlePlaceBid(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> bidData = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());

            int    auctionId = ((Number) bidData.get("auctionId")).intValue();
            int    bidderId  = ((Number) bidData.get("bidderId")).intValue();
            double amount    = ((Number) bidData.get("amount")).doubleValue();

            // Kiểm tra quyền: Seller không được tự đặt giá sản phẩm của mình
            Auction targetAuction = AuctionManager.getInstance().getAuction(auctionId);
            if (targetAuction != null && targetAuction.getSellerId() == bidderId) {
                response.put("success", false);
                response.put("message", "Bạn không thể đặt giá sản phẩm của chính mình!");
                return response;
            }

            // Gọi service — đã có synchronized + anti-sniping + auto-bid bên trong
            auctionService.placeManualBid(auctionId, bidderId, amount);

            response.put("success", true);
            response.put("message", "Đặt giá thành công! Giá hiện tại: " + amount + "$");

        } catch (AuctionException e) {
            // Lỗi nghiệp vụ (giá thấp, phiên đóng...) — trả về message rõ ràng cho Client
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi đặt giá: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /** THÊM MỚI: Trả về lịch sử đặt giá của một phiên */
    private Map<String, Object> handleGetBidHistory(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int auctionId = ((Number) data.get("auctionId")).intValue();

            List<BidTransaction> bids = bidTransactionDAO.getBidsByAuctionId(auctionId);
            List<Map<String, Object>> resultList = new ArrayList<>();

            for (BidTransaction bid : bids) {
                Map<String, Object> map = new HashMap<>();
                map.put("bidderId", bid.getBidderId());

                // Tra cứu tên tài khoản theo bidderId
                User bidder = UserManager.getInstance().getUser(bid.getBidderId());
                String bidderName = (bidder != null) ? bidder.getUsername() : "Bidder #" + bid.getBidderId();
                map.put("bidderName", bidderName);

                map.put("amount",   bid.getAmount());
                String time = bid.getBidTime().toString().replace("T", " ");
                if (time.contains(".")) time = time.substring(0, time.indexOf("."));
                map.put("bidTime", time);
                resultList.add(map);
            }

            response.put("success", true);
            response.put("message", "Tải lịch sử đặt giá thành công!");
            response.put("data", resultList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi tải lịch sử: " + e.getMessage());
        }
        return response;
    }

    /** THÊM MỚI: Đăng ký Auto-Bid cho một phiên */
    private Map<String, Object> handleRegisterAutoBid(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());

            int    auctionId = ((Number) data.get("auctionId")).intValue();
            int    bidderId  = ((Number) data.get("bidderId")).intValue();
            double maxAmount = ((Number) data.get("maxAmount")).doubleValue();
            double increment = ((Number) data.get("increment")).doubleValue();

            boolean ok = auctionService.registerAutoBid(auctionId, bidderId, maxAmount, increment);

            if (ok) {
                response.put("success", true);
                response.put("message", "Đã bật Auto-Bid thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể bật Auto-Bid: phiên không tồn tại hoặc đã kết thúc.");
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi đăng ký Auto-Bid: " + e.getMessage());
        }
        return response;
    }

    /**
     * THÊM MỚI: Trả về kết quả các phiên đấu giá đã kết thúc liên quan đến user.
     * Bao gồm: phiên user thắng, phiên user tham gia nhưng thua, phiên user đăng bán.
     */
    private Map<String, Object> handleGetMyAuctionResults(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int userId = ((Number) data.get("userId")).intValue();

            List<Map<String, Object>> resultList = new ArrayList<>();

            for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
                if (!"FINISHED".equals(auction.getStatus())) continue;

                Item item = ItemManager.getInstance().getItem(auction.getItemId());
                if (item == null) continue;

                boolean isWinner = auction.getCurrentWinnerId() == userId;
                boolean isSeller = auction.getSellerId() == userId;
                // Kiểm tra user có từng đặt giá trong phiên này không
                boolean participated = auction.getBidHistory().stream()
                        .anyMatch(b -> b.getBidderId() == userId);

                if (!isWinner && !isSeller && !participated) continue;

                Map<String, Object> map = new HashMap<>();
                map.put("auctionId",    auction.getId());
                map.put("itemName",     item.getName());
                map.put("finalPrice",   auction.getCurrentHighestBid());
                map.put("winnerId",     auction.getCurrentWinnerId());
                map.put("isWinner",     isWinner);
                map.put("isSeller",     isSeller);

                String endTime = auction.getEndTime().toString().replace("T", " ");
                if (endTime.contains(".")) endTime = endTime.substring(0, endTime.indexOf("."));
                map.put("endTime", endTime);

                resultList.add(map);
            }

            response.put("success", true);
            response.put("message", "Tải kết quả thành công!");
            response.put("data", resultList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi tải kết quả: " + e.getMessage());
        }
        return response;
    }

    /**
     * Trả về sản phẩm của user: đang bán, đang bid, đã bán, đã bid.
     */
    private Map<String, Object> handleGetMyProducts(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int userId = ((Number) data.get("userId")).intValue();

            List<Map<String, Object>> selling   = new ArrayList<>(); // đang bán (RUNNING)
            List<Map<String, Object>> bidding   = new ArrayList<>(); // đang bid (RUNNING)
            List<Map<String, Object>> soldDone  = new ArrayList<>(); // đã bán (FINISHED)
            List<Map<String, Object>> bidDone   = new ArrayList<>(); // đã bid (FINISHED)

            for (Auction auction : AuctionManager.getInstance().getAllAuctions()) {
                Item item = ItemManager.getInstance().getItem(auction.getItemId());
                if (item == null) continue;

                boolean isSeller = auction.getSellerId() == userId;
                boolean hasBid   = auction.getBidHistory().stream()
                        .anyMatch(b -> b.getBidderId() == userId);
                boolean isRunning  = "RUNNING".equals(auction.getStatus());
                boolean isFinished = "FINISHED".equals(auction.getStatus());
                boolean isPending  = "PENDING_PAYMENT".equals(auction.getStatus());
                boolean isCancelled = "CANCELLED".equals(auction.getStatus());

                if (!isSeller && !hasBid) continue;

                Map<String, Object> map = new HashMap<>();
                map.put("auctionId",        auction.getId());
                map.put("itemName",         item.getName());
                map.put("itemImagePath",    item.getImagePath());
                map.put("currentHighestBid", auction.getCurrentHighestBid());
                map.put("startingPrice",    item.getStartingPrice());
                map.put("status",           auction.getStatus());
                map.put("isWinner",         auction.getCurrentWinnerId() == userId);
                String endTime = auction.getEndTime().toString().replace("T", " ");
                if (endTime.contains(".")) endTime = endTime.substring(0, endTime.indexOf("."));
                map.put("endTime", endTime);

                // Gửi kèm paymentDeadline nếu đang PENDING_PAYMENT
                if (auction.getPaymentDeadline() != null) {
                    String deadline = auction.getPaymentDeadline().toString().replace("T", " ");
                    if (deadline.contains(".")) deadline = deadline.substring(0, deadline.indexOf("."));
                    map.put("paymentDeadline", deadline);
                }

                if (isSeller && isRunning)   selling.add(map);
                if (isSeller && isFinished)  soldDone.add(map);
                if (isSeller && isPending)   soldDone.add(map);   // Seller thấy trong tab đã bán
                if (isSeller && isCancelled) soldDone.add(map);

                if (!isSeller && hasBid && isRunning)   bidding.add(map);
                if (!isSeller && hasBid && isFinished)  bidDone.add(map);
                if (!isSeller && hasBid && isPending)   bidDone.add(map);  // Winner thấy trong tab đã đấu giá
                if (!isSeller && hasBid && isCancelled) bidDone.add(map);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("selling",  selling);
            result.put("bidding",  bidding);
            result.put("soldDone", soldDone);
            result.put("bidDone",  bidDone);

            response.put("success", true);
            response.put("message", "Tải sản phẩm thành công!");
            response.put("data", result);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi tải sản phẩm: " + e.getMessage());
        }
        return response;
    }

    /** Xác nhận thanh toán thành công */
    private Map<String, Object> handleConfirmPayment(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int auctionId = ((Number) data.get("auctionId")).intValue();
            int userId    = ((Number) data.get("userId")).intValue();

            Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy phiên đấu giá!");
                return response;
            }
            synchronized (auction) {
                if (!"PENDING_PAYMENT".equals(auction.getStatus())) {
                    response.put("success", false);
                    response.put("message", "Phiên này không ở trạng thái chờ thanh toán.");
                    return response;
                }
                if (auction.getCurrentWinnerId() != userId) {
                    response.put("success", false);
                    response.put("message", "Bạn không phải người thắng phiên này!");
                    return response;
                }
                // Kiểm tra còn trong hạn không
                if (auction.getPaymentDeadline() != null
                        && java.time.LocalDateTime.now().isAfter(auction.getPaymentDeadline())) {
                    auction.setStatus("CANCELLED");
                    AuctionManager.getInstance().updateAuctionInDB(auction);
                    response.put("success", false);
                    response.put("message", "Hết thời hạn thanh toán! Phiên đã bị hủy.");
                    return response;
                }
                auction.setStatus("FINISHED");
                AuctionManager.getInstance().updateAuctionInDB(auction);
                System.out.println("💳 Phiên " + auctionId + " đã được thanh toán bởi user " + userId);
            }
            response.put("success", true);
            response.put("message", "Thanh toán thành công! Cảm ơn bạn đã tham gia đấu giá.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi xử lý thanh toán: " + e.getMessage());
        }
        return response;
    }

    /** Hủy thanh toán — người thắng chủ động từ bỏ */
    private Map<String, Object> handleCancelPayment(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int auctionId = ((Number) data.get("auctionId")).intValue();
            int userId    = ((Number) data.get("userId")).intValue();

            Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy phiên đấu giá!");
                return response;
            }
            synchronized (auction) {
                if (!"PENDING_PAYMENT".equals(auction.getStatus())) {
                    response.put("success", false);
                    response.put("message", "Phiên này không ở trạng thái chờ thanh toán.");
                    return response;
                }
                if (auction.getCurrentWinnerId() != userId) {
                    response.put("success", false);
                    response.put("message", "Bạn không phải người thắng phiên này!");
                    return response;
                }
                auction.setStatus("CANCELLED");
                AuctionManager.getInstance().updateAuctionInDB(auction);
                System.out.println("❌ Phiên " + auctionId + " bị hủy bởi user " + userId);
            }
            response.put("success", true);
            response.put("message", "Đã hủy. Sản phẩm sẽ không được bàn giao.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi hủy thanh toán: " + e.getMessage());
        }
        return response;
    }

    /** Cập nhật số điện thoại cho user */
    private Map<String, Object> handleUpdatePhone(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int    userId = ((Number) data.get("userId")).intValue();
            String phone  = (String) data.get("phone");

            if (phone == null || phone.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Số điện thoại không được để trống!");
                return response;
            }

            boolean ok = UserManager.getInstance().updatePhone(userId, phone.trim());
            if (ok) {
                response.put("success", true);
                response.put("message", "Cập nhật số điện thoại thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server: " + e.getMessage());
        }
        return response;
    }

    /** Gửi tin nhắn chat vào phòng chat của phiên đấu giá */
    private Map<String, Object> handleSendChat(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int    auctionId  = ((Number) data.get("auctionId")).intValue();
            String senderName = (String) data.getOrDefault("senderName", "Ẩn danh");
            String content    = (String) data.get("content");

            if (content == null || content.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Nội dung tin nhắn không được để trống!");
                return response;
            }

            // Kiểm tra phiên có tồn tại không
            Auction auction = AuctionManager.getInstance().getAuction(auctionId);
            if (auction == null) {
                response.put("success", false);
                response.put("message", "Phiên đấu giá không tồn tại!");
                return response;
            }

            // Phát tin nhắn đến tất cả client đang subscribe
            BroadcastManager.broadcastChatMessage(auctionId, senderName, content.trim());

            response.put("success", true);
            response.put("message", "Tin nhắn đã được gửi!");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server khi gửi tin nhắn: " + e.getMessage());
        }
        return response;
    }

    /** Cập nhật email cho user */
    private Map<String, Object> handleUpdateEmail(Object payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(payload), new TypeToken<Map<String, Object>>(){}.getType());
            int    userId = ((Number) data.get("userId")).intValue();
            String email  = (String) data.get("email");

            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email không được để trống!");
                return response;
            }

            boolean ok = UserManager.getInstance().updateEmail(userId, email.trim());
            if (ok) {
                response.put("success", true);
                response.put("message", "Cập nhật email thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy tài khoản!");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi server: " + e.getMessage());
        }
        return response;
    }
}