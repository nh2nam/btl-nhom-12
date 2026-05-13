package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.model.Bidder;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.AuctionManager;
import com.auction.exception.AuctionException;
import com.auction.exception.BidTooLowException;
import java.time.LocalDateTime;

public class AuctionServiceImpl implements IAuctionService {

    private AuctionManager auctionManager = AuctionManager.getInstance();
    private BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

    @Override
    public void placeManualBid(int auctionId, int bidderId, double bidAmount) throws AuctionException {
        // Gọi hàm xử lý lõi với cờ isAutoBid = false (đây là người thật đặt giá)
        processBid(auctionId, bidderId, bidAmount, false);
    }

    /**
     * HÀM XỬ LÝ LÕI: Tách logic để cả Người và Bot cùng sử dụng.
     * Cờ isAutoBid giúp cắt đứt vòng lặp đệ quy vô tận.
     */
    private void processBid(int auctionId, int bidderId, double bidAmount, boolean isAutoBid) throws AuctionException {
        Auction auction = auctionManager.getAuction(auctionId);

        if (auction == null) {
            throw new AuctionException("Lỗi: Không tìm thấy phiên đấu giá hợp lệ.");
        }

        synchronized (auction) {
            // 1. Kiểm tra trạng thái phiên
            if (!auction.isOpen()) {
                throw new AuctionException("Lỗi: Phiên đấu giá đã kết thúc hoặc chưa mở.");
            }

            // 2. Kiểm tra mức giá
            if (bidAmount <= auction.getCurrentHighestBid()) {
                throw new BidTooLowException("Lỗi: Giá đặt " + bidAmount + "$ quá thấp. Phải lớn hơn " + auction.getCurrentHighestBid() + "$");
            }

            // 3. Thuật toán Anti-sniping
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(auction.getEndTime().minusSeconds(30)) && now.isBefore(auction.getEndTime())) {
                auction.setEndTime(auction.getEndTime().plusSeconds(60));
                System.out.println("⚡ Anti-sniping kích hoạt: Phiên " + auctionId + " được gia hạn thêm 60s!");
            }

            // 4. Cập nhật dữ liệu trên RAM
            auction.setCurrentHighestBid(bidAmount);
            auction.setCurrentWinnerId(bidderId);
            auction.setVersion(auction.getVersion() + 1);

            // 5. Lưu lịch sử và đồng bộ Database
            BidTransaction newTransaction = new BidTransaction(0, auctionId, bidderId, bidAmount, now);
            bidTransactionDAO.insertBidTransaction(newTransaction);
            auction.getBidHistory().add(newTransaction);
            auctionManager.updateAuctionInDB(auction);

            System.out.println("✅ " + (isAutoBid ? "[BOT] " : "[USER] ") + bidderId + " đặt giá: " + bidAmount + "$");

            com.auction.network.BroadcastManager.broadcastPriceUpdate(auctionId, bidAmount);
            // 6. CHỐT CHẶN ĐỆ QUY:
            // Chỉ kích hoạt Auto-Bidding nếu người vừa đặt giá là NGƯỜI THẬT.
            // Nếu là Bot vừa đặt, ta dừng lại tại đây để tránh vòng lặp Bot A gọi Bot B gọi lại Bot A.
            if (!isAutoBid) {
                triggerAutoBidding(auction);
            }
        }
    }

    @Override
    public boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || !auction.isOpen()) {
            return false;
        }

        Bidder autoBidder = new Bidder(bidderId, "User_" + bidderId, "user@gmail.com", "hash");
        autoBidder.setAutoBidEnabled(true);
        autoBidder.setMaxAutoBidAmount(maxBidAmount);
        autoBidder.setAutoBidIncrement(increment);

        synchronized (auction) {
            auction.getAutoBidders().add(autoBidder);
            System.out.println("🤖 Bidder " + bidderId + " đã bật Auto-Bid (Max: " + maxBidAmount + ", Bước giá: " + increment + ")");
            triggerAutoBidding(auction);
        }
        return true;
    }

    private void triggerAutoBidding(Auction auction) {
        boolean autoBidOccurred;
        do {
            autoBidOccurred = false;
            for (Bidder bidder : auction.getAutoBidders()) {
                // Không tự đấu giá với chính mình
                if (bidder.getId() == auction.getCurrentWinnerId()) continue;

                double nextRequiredBid = auction.getCurrentHighestBid() + bidder.getAutoBidIncrement();

                // Nếu Bot vẫn còn đủ ngân sách
                if (nextRequiredBid <= bidder.getMaxAutoBidAmount()) {
                    try {
                        // GỌI HÀM LÕI VỚI CỜ isAutoBid = true
                        processBid(auction.getId(), bidder.getId(), nextRequiredBid, true);
                        autoBidOccurred = true;
                        break; // Thoát vòng lặp nhỏ để quét lại danh sách Bot từ đầu với giá mới
                    } catch (AuctionException e) {
                        System.out.println("⚠️ Auto-Bid cho Bidder " + bidder.getId() + " thất bại: " + e.getMessage());
                    }
                }
            }
        } while (autoBidOccurred);
    }

    @Override
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctionManager.getAllAuctions()) {
            synchronized (auction) {
                if ("RUNNING".equals(auction.getStatus()) && now.isAfter(auction.getEndTime())) {
                    auction.setStatus("FINISHED");
                    auctionManager.updateAuctionInDB(auction);
                    System.out.println("\n🔔 KẾT THÚC: Phiên " + auction.getId() + " đã đóng cửa!");
                }
            }
        }
    }
}