package com.auction;

import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Seller;
import com.auction.service.AuctionServiceImpl;
import com.auction.util.AuctionManager;
import com.auction.util.ItemManager;
import com.auction.util.UserManager;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        // 1. CHUẨN BỊ DỮ LIỆU BẰNG CÁC MANAGER (Giả lập Database)
        System.out.println("--- KHỞI TẠO HỆ THỐNG ---");

        // Tạo và lưu Seller
        Seller seller = new Seller(1, "NguyenVanBan", "ban@gmail.com", "123");
        UserManager.getInstance().addUser(seller);

        // Tạo và lưu Item (Điện thoại)
        Electronics iphone = new Electronics(101, "iPhone 15 Pro", "Mới 100%", 1000.0, "Apple", "15 Pro", 12);
        ItemManager.getInstance().addItem(iphone);

        // Tạo phiên đấu giá, bắt đầu ngay bây giờ và kết thúc sau 1 phút nữa
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(1, seller.getId(), iphone.getId(), now, now.plusMinutes(1), iphone.getStartingPrice());
        auction.setStatus("RUNNING"); // Mở cửa phiên đấu giá

        // Đưa phiên đấu giá vào Manager quản lý
        AuctionManager.getInstance().addAuction(auction);

        AuctionServiceImpl service = new AuctionServiceImpl();

        // 2. KỊCH BẢN TEST: 10 NGƯỜI CÙNG ĐẶT GIÁ
        System.out.println("\n--- BẮT ĐẦU MỞ CỬA CHO 10 NGƯỜI CÙNG TRANH NHAU ---");

        for (int i = 1; i <= 10; i++) {
            final int bidderId = i;

            // Khởi tạo 10 Bidder và lưu vào UserManager
            Bidder bidder = new Bidder(bidderId, "Bidder" + i, "bidder" + i + "@gmail.com", "pass123");
            UserManager.getInstance().addUser(bidder);

            // Tạo 10 luồng (Thread) chạy song song cùng lúc để mô phỏng đặt giá đồng thời
            new Thread(() -> {
                try {
                    // Cố gắng đặt giá 1200$
                    service.placeManualBid(auction.getId(), bidderId, 1200.0);
                } catch (Exception e) {
                    // Nếu chậm tay hơn người khác hoặc có lỗi, nó sẽ bắt được Exception ở đây
                    System.out.println("❌ Bidder " + bidderId + " thất bại: " + e.getMessage());
                }
            }).start();
        }

        // Đợi 2 giây cho các luồng chạy xong rồi in kết quả tổng kết
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n--- KẾT QUẢ CHUNG CUỘC ---");
        System.out.println("Giá chốt cuối cùng: " + auction.getCurrentHighestBid() + "$");
        System.out.println("Người chiến thắng (ID): " + auction.getCurrentWinnerId());
        System.out.println("Tổng số giao dịch thành công được ghi nhận: " + auction.getBidHistory().size());
    }
}