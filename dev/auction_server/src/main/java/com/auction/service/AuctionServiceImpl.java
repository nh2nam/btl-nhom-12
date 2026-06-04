package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.model.Bidder;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.AuctionManager;
import com.auction.exception.AuctionException;
import com.auction.exception.BidTooLowException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServiceImpl implements IAuctionService {

    private AuctionManager auctionManager = AuctionManager.getInstance();
    private BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    private ExecutorService eventQueue = Executors.newCachedThreadPool();

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

            com.auction.network.BroadcastManager.broadcastPriceUpdate(
                    auctionId, bidAmount,
                    auction.getEndTime().toString().replace("T", " ").replaceAll("\\..*", ""));
            notifyAutoBidObservers(auction);
            // 6. CHỐT CHẶN ĐỆ QUY:
            // Chỉ kích hoạt Auto-Bidding nếu người vừa đặt giá là NGƯỜI THẬT.
            // Nếu là Bot vừa đặt, ta dừng lại tại đây để tránh vòng lặp Bot A gọi Bot B gọi lại Bot A.
            if (!isAutoBid) {
                notifyAutoBidObservers(auction);
            }
        }
    }


    @Override
    public boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || !auction.isOpen()) return false;

        Bidder autoBidder = new Bidder(bidderId, "User_" + bidderId, "user@gmail.com", "hash");
        autoBidder.setAutoBidEnabled(true);
        autoBidder.setMaxAutoBidAmount(maxBidAmount);
        autoBidder.setAutoBidIncrement(increment);

        synchronized (auction) {
            auction.getAutoBidders().add(autoBidder);
            System.out.println("🤖 Bidder " + bidderId + " đã đăng ký Lắng nghe sự kiện (Observer)");

            // Kích hoạt nhịp đầu tiên để Bot rà soát giá hiện tại
            notifyAutoBidObservers(auction);
        }
        return true;
    }

    // ĐÂY LÀ OBSERVER LISTENER: Nơi các Bot lắng nghe và phản ứng
    private void notifyAutoBidObservers(Auction auction) {
        // Tống công việc phản ứng của Bot vào hàng đợi thay vì chạy trực tiếp
        eventQueue.submit(() -> {
            try {
                // Nhịp thở 1.5 giây để Client kịp vẽ UI và người thật có kẽ hở đặt giá
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            synchronized (auction) {
                if (!auction.isOpen()) return;

                for (Bidder bot : auction.getAutoBidders()) {
                    // Không tự đấu giá với chính mình
                    if (bot.getId() == auction.getCurrentWinnerId()) continue;

                    double nextRequiredBid = auction.getCurrentHighestBid() + bot.getAutoBidIncrement();

                    // Nếu ngân sách vẫn đủ
                    if (nextRequiredBid <= bot.getMaxAutoBidAmount()) {
                        try {
                            // Khi Bot này đặt giá, nó sẽ tự động gọi processBid
                            // và processBid sẽ lại phát ra tín hiệu mới để duy trì chuỗi sự kiện!
                            processBid(auction.getId(), bot.getId(), nextRequiredBid, true);
                            break; // Dừng lại! Nhường quyền kích hoạt sự kiện tiếp theo cho processBid
                        } catch (AuctionException e) {
                            System.out.println("⚠️ Lỗi Bot " + bot.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
        });
    }
    @Override
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctionManager.getAllAuctions()) {
            synchronized (auction) {
                // 1. Phiên RUNNING hết giờ → chuyển sang PENDING_PAYMENT (nếu có winner) hoặc FINISHED (nếu không ai đặt)
                if ("RUNNING".equals(auction.getStatus()) && now.isAfter(auction.getEndTime())) {
                    if (auction.getCurrentWinnerId() != -1) {
                        // Có người thắng → chờ thanh toán 10 phút
                        auction.setStatus("PENDING_PAYMENT");
                        auction.setPaymentDeadline(now.plusMinutes(10));
                        System.out.println("\n🔔 PENDING: Phiên " + auction.getId()
                                + " chờ thanh toán từ user " + auction.getCurrentWinnerId()
                                + " đến " + auction.getPaymentDeadline());
                    } else {
                        // Không ai đặt giá → kết thúc luôn
                        auction.setStatus("FINISHED");
                        System.out.println("\n🔔 FINISHED (no bids): Phiên " + auction.getId());
                    }
                    auctionManager.updateAuctionInDB(auction);
                }

                // 2. Phiên PENDING_PAYMENT quá hạn → hủy (CANCELLED)
                if ("PENDING_PAYMENT".equals(auction.getStatus())
                        && auction.getPaymentDeadline() != null
                        && now.isAfter(auction.getPaymentDeadline())) {
                    auction.setStatus("CANCELLED");
                    auctionManager.updateAuctionInDB(auction);
                    System.out.println("\n⛔ CANCELLED: Phiên " + auction.getId() + " hết hạn thanh toán.");
                }
            }
        }
    }
}