package com.auction.service;

import com.auction.model.Bidder;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.util.AuctionManager;

import com.auction.exception.AuctionException;
import com.auction.exception.BidTooLowException;
import com.auction.dao.BidTransactionDAO; // Bổ sung import DAO

import java.time.LocalDateTime;

public class AuctionServiceImpl implements IAuctionService {

    // Lấy instance của Manager và khởi tạo DAO để gọi Database
    private AuctionManager auctionManager = AuctionManager.getInstance();
    private BidTransactionDAO bidDAO = new BidTransactionDAO();

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

            // 5. Cập nhật người chiến thắng và giá cao nhất trên RAM
            auction.setCurrentHighestBid(bidAmount);
            auction.setCurrentWinnerId(bidderId);
            auction.setVersion(auction.getVersion() + 1);

            // ==========================================
            // TÍCH HỢP DATABASE: Cập nhật thông tin thay đổi của phiên đấu giá xuống MySQL
            // ==========================================
            auctionManager.updateAuctionInDB(auction);

            // 6. Lưu lại lịch sử giao dịch
            // Đặt ID tạm là 0, sau khi insert MySQL sẽ tự sinh ID thật và gán lại vào Object
            BidTransaction newTransaction = new BidTransaction(
                    0, auctionId, bidderId, bidAmount, now
            );

            // ==========================================
            // TÍCH HỢP DATABASE: Ghi đè lịch sử giao dịch vào MySQL
            // ==========================================
            bidDAO.insertBidTransaction(newTransaction);

            // Thêm giao dịch vào danh sách lịch sử trên RAM để UI xử lý nhanh
            auction.getBidHistory().add(newTransaction);

            System.out.println("✅ Bidder " + bidderId + " đặt giá thành công: " + bidAmount + "$");

            // Kích hoạt auto-bidding sau khi có người đặt giá thủ công
            triggerAutoBidding(auction);
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

    // HÀM BỔ TRỢ: Xử lý bot Auto-Bid
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

                    // ==========================================
                    // TÍCH HỢP DATABASE: Cập nhật trạng thái "FINISHED" xuống DB
                    // ==========================================
                    auctionManager.updateAuctionInDB(auction);
                }
            }
        }
    }
}