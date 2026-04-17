package com.auction.service;

import com.auction.model.Bidder;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.AuctionManager;

// Import 2 class Exception bạn vừa tạo ở Bước 1
import com.auction.exception.AuctionException;
import com.auction.exception.BidTooLowException;

import java.time.LocalDateTime;

public class AuctionServiceImpl implements IAuctionService {

    // Lấy instance duy nhất của Manager để lấy dữ liệu
    private AuctionManager auctionManager = AuctionManager.getInstance();


    @Override
    public void placeManualBid(int auctionId, int bidderId, double bidAmount) throws AuctionException {
        Auction auction = auctionManager.getAuction(auctionId);

        // 1. Kiểm tra tồn tại
        if (auction == null) {
            throw new AuctionException("Lỗi: Không tìm thấy phiên đấu giá hợp lệ.");
        }

        /* KHÓA ĐỒNG THỜI (CONCURRENCY CONTROL) */
        synchronized (auction) {
            // 2. Validate: Phiên còn mở không?
            if (!auction.isOpen()) {
                throw new AuctionException("Lỗi: Phiên đấu giá đã kết thúc hoặc chưa mở.");
            }

            // 3. Validate: Giá có hợp lệ không?
            if (bidAmount <= auction.getCurrentHighestBid()) {
                throw new BidTooLowException("Lỗi: Giá đặt " + bidAmount + "$ quá thấp. Phải lớn hơn " + auction.getCurrentHighestBid() + "$");
            }

            // 4. Thuật toán Anti-sniping (Gia hạn phiên)
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

            // Kích hoạt auto-bidding sau khi có người đặt giá thủ công
            triggerAutoBidding(auction);

            // XÓA DÒNG return BidResult.SUCCESS; (Vì đã chạy đến đây tức là không có Exception nào bị ném ra)
        }
    }

    @Override
    public boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || !auction.isOpen()) {
            return false; // Không cho đăng ký nếu phiên lỗi hoặc đã đóng
        }

        Bidder autoBidder = new Bidder(bidderId, "User_" + bidderId, "user@gmail.com", "hash");
        autoBidder.setAutoBidEnabled(true);
        autoBidder.setMaxAutoBidAmount(maxBidAmount);
        autoBidder.setAutoBidIncrement(increment);

        synchronized (auction) {
            auction.getAutoBidders().add(autoBidder);
            System.out.println("🤖 Bidder " + bidderId + " đã bật Auto-Bid (Max: " + maxBidAmount + ", Bước giá: " + increment + ")");

            // KÍCH HOẠT TỨC THÌ
            triggerAutoBidding(auction);
        }
        return true;
    }

    // HÀM BỔ TRỢ: Đã thêm try-catch để bắt lỗi Exception khi gọi placeManualBid
    private void triggerAutoBidding(Auction auction) {
        boolean autoBidOccurred;

        do {
            autoBidOccurred = false;
            for (Bidder bidder : auction.getAutoBidders()) {
                // Không tự đấu giá với chính mình
                if (bidder.getId() == auction.getCurrentWinnerId()) continue;

                double nextRequiredBid = auction.getCurrentHighestBid() + bidder.getAutoBidIncrement();

                // Nếu tiền trong túi vẫn chịu nổi mức giá mới
                if (nextRequiredBid <= bidder.getMaxAutoBidAmount()) {
                    try {
                        // Gọi hàm thủ công, nếu lỗi nó sẽ ném Exception xuống catch
                        placeManualBid(auction.getId(), bidder.getId(), nextRequiredBid);
                        autoBidOccurred = true;
                        break; // Có giá mới, thoát vòng lặp nhỏ để chạy lại vòng lớn đánh giá lại
                    } catch (AuctionException e) {
                        // In ra lỗi nếu bot auto-bid không thành công (ví dụ do nhảy giá quá nhanh)
                        // Bỏ qua và chuyển sang bidder tiếp theo
                        System.out.println("⚠️ Auto-Bid cho Bidder " + bidder.getId() + " thất bại: " + e.getMessage());
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