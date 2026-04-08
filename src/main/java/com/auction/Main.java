package com.auction;

import com.auction.model.Auction;
import com.auction.model.Electronics;
import com.auction.model.Seller;
import com.auction.service.AuctionServiceImpl;
import com.auction.service.BidResult;
import com.auction.util.AuctionManager;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        // 1. Chuẩn bị dữ liệu ảo
        Seller seller = new Seller(1, "NguyenVanBan", "ban@gmail.com", "123");
        Electronics iphone = new Electronics(101, "iPhone 15 Pro", "Mới 100%", 1000.0, "Apple", "15 Pro", 12);

        // Tạo phiên đấu giá, bắt đầu ngay bây giờ và kết thúc sau 1 phút nữa
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(1, seller.getId(), iphone.getId(), now, now.plusMinutes(1), iphone.getStartingPrice());
        auction.setStatus("RUNNING"); // Mở cửa phiên đấu giá

        // Đưa vào Manager quản lý
        AuctionManager.getInstance().addAuction(auction);

        AuctionServiceImpl service = new AuctionServiceImpl();

        // 2. Kịch bản test: 10 người cùng đặt giá 1200$ vào cùng 1 thời điểm
        System.out.println("--- BẮT ĐẦU MỞ CỬA CHO 10 NGƯỜI CÙNG TRANH NHAU ---");

        for (int i = 1; i <= 10; i++) {
            final int bidderId = i;

            // Tạo 10 luồng (Thread) chạy song song cùng lúc
            new Thread(() -> {
                BidResult result = service.placeManualBid(auction.getId(), bidderId, 1200.0);
                if (result == BidResult.BID_TOO_LOW) {
                    System.out.println("❌ Bidder " + bidderId + " thất bại: Trễ nhịp, giá đã bị người khác đẩy lên!");
                }
            }).start();
        }

        // Đợi 2 giây cho các luồng chạy xong rồi in kết quả
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