package com.auction.service;

import com.auction.model.Bidder;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.AuctionManager;
import java.time.LocalDateTime;


public class AuctionServiceImpl implements IAuctionService {

    // Lấy instance duy nhất của Manager để lấy dữ liệu
    private AuctionManager auctionManager = AuctionManager.getInstance();

    @Override
    public BidResult placeManualBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = auctionManager.getAuction(auctionId);

        // 1. Kiểm tra tồn tại
        if (auction == null) {
            return BidResult.INVALID_AUCTION;
        }

        /* KHÓA ĐỒNG THỜI (CONCURRENCY CONTROL)
         * Dùng synchronized để "khóa" phiên đấu giá này lại.
         * Ngăn chặn 2 người cùng đặt giá trúng cùng 1 mili-giây.
         */
        synchronized (auction) {
            // 2. Validate: Phiên còn mở không?
            if (!auction.isOpen()) {
                return BidResult.AUCTION_CLOSED;
            }

            // 3. Validate: Giá có hợp lệ không? (Phải lớn hơn giá hiện tại)
            if (bidAmount <= auction.getCurrentHighestBid()) {
                return BidResult.BID_TOO_LOW;
            }

            // 4. Thuật toán Anti-sniping (Gia hạn phiên)
            // Logic: Nếu có bid hợp lệ trong 30 giây cuối cùng, tự động cộng thêm 60 giây
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime timeToTriggerAntiSniping = auction.getEndTime().minusSeconds(30);

            if (now.isAfter(timeToTriggerAntiSniping) && now.isBefore(auction.getEndTime())) {
                auction.setEndTime(auction.getEndTime().plusSeconds(60));
                System.out.println("⚡ Anti-sniping kích hoạt: Phiên " + auctionId + " được gia hạn thêm 60s!");
            }

            // 5. Cập nhật người chiến thắng và giá cao nhất
            auction.setCurrentHighestBid(bidAmount);
            auction.setCurrentWinnerId(bidderId);

            // Tăng version lên 1
            auction.setVersion(auction.getVersion() + 1);

            // 6. Lưu lại lịch sử giao dịch
            int transactionId = (int) (Math.random() * 10000); // Tạo ID tạm thời
            BidTransaction newTransaction = new BidTransaction(
                    transactionId, auctionId, bidderId, bidAmount, now
            );

            // Thêm giao dịch vào danh sách lịch sử của phiên đấu giá
            auction.getBidHistory().add(newTransaction);

            System.out.println("✅ Bidder " + bidderId + " đặt giá thành công: " + bidAmount + "$");

            triggerAutoBidding(auction);

            return BidResult.SUCCESS;
        }
    }

    @Override
    public boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || !auction.isOpen()) {
            return false; // Không cho đăng ký nếu phiên lỗi hoặc đã đóng
        }

        // Tạo tạm một đối tượng Bidder để lưu thông tin cấu hình
        // Lưu ý: Trong hệ thống thật, bạn sẽ lấy User từ UserDAO trong Database ra
        Bidder autoBidder = new Bidder(bidderId, "User_" + bidderId, "user@gmail.com", "hash");
        autoBidder.setAutoBidEnabled(true);
        autoBidder.setMaxAutoBidAmount(maxBidAmount);
        autoBidder.setAutoBidIncrement(increment);

        synchronized (auction) {
            auction.getAutoBidders().add(autoBidder);
            System.out.println("🤖 Bidder " + bidderId + " đã bật Auto-Bid (Max: " + maxBidAmount + ", Bước giá: " + increment + ")");

            // KÍCH HOẠT TỨC THÌ: Vừa bật xong là tự động đặt giá luôn (nếu đủ tiền)
            triggerAutoBidding(auction);
        }
        return true;
    }

    // HÀM BỔ TRỢ: Đây là nơi diễn ra trận chiến "đấu súng" tự động
    private void triggerAutoBidding(Auction auction) {
        boolean autoBidOccurred;

        // Vòng lặp: Chạy liên tục cho đến khi không còn ai đủ tiền đè giá nữa thì thôi
        do {
            autoBidOccurred = false;
            for (Bidder bidder : auction.getAutoBidders()) {
                // Không tự đấu giá với chính mình
                if (bidder.getId() == auction.getCurrentWinnerId()) continue;

                double nextRequiredBid = auction.getCurrentHighestBid() + bidder.getAutoBidIncrement();

                // Nếu tiền trong túi vẫn chịu nổi mức giá mới
                if (nextRequiredBid <= bidder.getMaxAutoBidAmount()) {
                    // Dùng chính hàm thủ công lúc nãy để đặt giá
                    BidResult result = placeManualBid(auction.getId(), bidder.getId(), nextRequiredBid);
                    if (result == BidResult.SUCCESS) {
                        autoBidOccurred = true;
                        break; // Có giá mới, thoát vòng lặp nhỏ để chạy lại vòng lớn đánh giá lại
                    }
                }
            }
        } while (autoBidOccurred);
    }

    @Override
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả phiên đấu giá từ Manager ra kiểm tra
        for (Auction auction : auctionManager.getAllAuctions()) {
            synchronized (auction) {
                // Nếu phiên đang chạy và thời gian hiện tại đã vượt qua giờ kết thúc
                if ("RUNNING".equals(auction.getStatus()) && now.isAfter(auction.getEndTime())) {
                    auction.setStatus("FINISHED");
                    System.out.println("\n🔔 KẾT THÚC: Phiên " + auction.getId() + " đã đóng cửa!");

                    if (auction.getCurrentWinnerId() != -1) {
                        System.out.println("🏆 Người chiến thắng: Bidder " + auction.getCurrentWinnerId() + " với giá " + auction.getCurrentHighestBid() + "$");
                    } else {
                        System.out.println("😔 Không có ai đặt giá. Sản phẩm ế.");
                    }
                }
            }
        }
    }
}