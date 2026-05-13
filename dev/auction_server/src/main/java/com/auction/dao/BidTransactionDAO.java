package com.auction.dao;

import com.auction.model.BidTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BidTransactionDAO {
    private static final Logger LOGGER = Logger.getLogger(BidTransactionDAO.class.getName());

    // 1. Lưu giao dịch đặt giá mới
    public void insertBidTransaction(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, bid.getAuctionId());
            stmt.setInt(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4, Timestamp.valueOf(bid.getBidTime()));

            stmt.executeUpdate();

            // Lấy ID tự sinh từ MySQL
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    bid.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu Lịch sử đặt giá", e);
        }
    }

    // 2. Lấy giá cao nhất thực tế từ bid_transactions cho 1 phiên (dùng để reconcile)
    public double getMaxBidByAuctionId(int auctionId) {
        String sql = "SELECT COALESCE(MAX(amount), -1) AS max_bid FROM bid_transactions WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("max_bid");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy giá cao nhất", e);
        }
        return -1;
    }

    // 3. Lấy bidderId của người bid cao nhất trong 1 phiên
    public int getWinnerBidderIdByAuctionId(int auctionId) {
        String sql = "SELECT bidder_id FROM bid_transactions WHERE auction_id = ? ORDER BY amount DESC, bid_time ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("bidder_id");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy winner", e);
        }
        return -1;
    }

    // 4. Lấy toàn bộ lịch sử đặt giá của 1 phiên đấu giá (Load lúc khởi động)
    public List<BidTransaction> getBidsByAuctionId(int auctionId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT id, auction_id, bidder_id, amount, bid_time FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction(
                            rs.getInt("id"),
                            rs.getInt("auction_id"),
                            rs.getInt("bidder_id"),
                            rs.getDouble("amount"),
                            rs.getTimestamp("bid_time").toLocalDateTime()
                    );
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi tải danh sách Bid", e);
        }
        return bids;
    }
}