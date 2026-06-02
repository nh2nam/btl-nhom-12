package com.auction.util;

import com.auction.model.Auction;
import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@SuppressWarnings("java:S6548")
public class AuctionManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionManager.class.getName());

    private Map<Integer, Auction> activeAuctions;
    private AuctionDAO auctionDAO;
    private BidTransactionDAO bidTransactionDAO;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        auctionDAO = new AuctionDAO();
        bidTransactionDAO = new BidTransactionDAO();

        // Khởi động Server: Bơm toàn bộ dữ liệu từ DB lên RAM
        for (Auction auction : auctionDAO.getAllAuctions()) {
            // Reconcile: tính lại giá cao nhất thực tế từ bid_transactions
            // để sửa dữ liệu sai do race condition có thể đã xảy ra trước đó
            double maxBid = bidTransactionDAO.getMaxBidByAuctionId(auction.getId());
            if (maxBid > auction.getCurrentHighestBid()) {
                LOGGER.warning(() -> "⚠️ Phiên " + auction.getId()
                        + ": current_highest_bid trong DB là " + auction.getCurrentHighestBid()
                        + " nhưng max bid thực tế là " + maxBid + " — tự động sửa.");
                auction.setCurrentHighestBid(maxBid);

                int winnerId = bidTransactionDAO.getWinnerBidderIdByAuctionId(auction.getId());
                if (winnerId != -1) auction.setCurrentWinnerId(winnerId);

                // Ghi lại giá đúng xuống DB
                auctionDAO.updateAuction(auction);
            }
            activeAuctions.put(auction.getId(), auction);
        }
        LOGGER.info(() -> "✅ Đã đồng bộ " + activeAuctions.size() + " phiên đấu giá từ Database lên RAM.");
    }

    private static class InstanceHolder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void addAuction(Auction auction) {
        // 1. Lưu xuống DB để có ID thật
        auctionDAO.insertAuction(auction);
        // 2. Lưu lên RAM
        activeAuctions.put(auction.getId(), auction);
    }

    public void refreshFromDB() {
        // 1. Lấy danh sách mới nhất từ Database
        List<Auction> listFromDB = auctionDAO.getAllAuctions();

        for (Auction dbAuction : listFromDB) {
            Auction ramAuction = activeAuctions.get(dbAuction.getId());

            if (ramAuction == null) {
                // Nếu đây là phiên đấu giá mới tinh (vừa đăng bán), thì đưa vào RAM
                activeAuctions.put(dbAuction.getId(), dbAuction);
            } else {
                synchronized (ramAuction) {
                    if (dbAuction.getVersion() > ramAuction.getVersion()) {
                        ramAuction.setCurrentHighestBid(dbAuction.getCurrentHighestBid());
                        ramAuction.setCurrentWinnerId(dbAuction.getCurrentWinnerId());
                        ramAuction.setStatus(dbAuction.getStatus());
                        ramAuction.setEndTime(dbAuction.getEndTime());
                        ramAuction.setVersion(dbAuction.getVersion());
                    }
                }
            }
        }
    }

    public Auction getAuction(int id) {
        // KHÔNG refreshFromDB() ở đây — dùng RAM để đảm bảo tính nhất quán khi bid
        // RAM luôn được cập nhật ngay trong synchronized block của processBid()
        return activeAuctions.get(id);
    }

    public Collection<Auction> getAllAuctions() {
        refreshFromDB(); // Chỉ dùng khi hiển thị danh sách, không dùng trong luồng bid
        return activeAuctions.values();
    }

    // Hàm này bạn sẽ gọi ở bên trong AuctionServiceImpl mỗi khi xử lý xong một cú đặt giá
    public void updateAuctionInDB(Auction auction) {
        auctionDAO.updateAuction(auction);
    }

    // Xóa auction khỏi RAM (DB đã được xóa riêng qua AuctionDAO)
    public void removeAuction(int auctionId) {
        activeAuctions.remove(auctionId);
        LOGGER.info(() -> "🗑️ Đã xóa auctionId=" + auctionId + " khỏi RAM.");
    }

}