package com.auction.dao;

import com.auction.model.Auction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {

    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());

    // 1. Lưu phiên đấu giá mới vào Database
    public void insertAuction(Auction auction) {
        String sql = "INSERT INTO auctions (seller_id, item_id, start_time, end_time, status, current_highest_bid, current_winner_id, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, auction.getSellerId());
            stmt.setInt(2, auction.getItemId());

            // Chuyển đổi LocalDateTime của Java sang Timestamp của SQL
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));

            stmt.setString(5, auction.getStatus());
            stmt.setDouble(6, auction.getCurrentHighestBid());

            // Xử lý cạm bẫy Khóa ngoại (Tránh lỗi lưu số -1)
            if (auction.getCurrentWinnerId() == -1) {
                stmt.setNull(7, Types.INTEGER);
            } else {
                stmt.setInt(7, auction.getCurrentWinnerId());
            }

            stmt.setInt(8, auction.getVersion());

            stmt.executeUpdate();

            // Lấy ID tự động tạo và gán lại cho Object
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    auction.setId(rs.getInt(1));
                }
            }
            LOGGER.info(() -> "⚖️ Đã mở phiên đấu giá mới cho Item ID: " + auction.getItemId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu Auction vào Database", e);
        }
    }

    // 2. Load tất cả các phiên đấu giá lên RAM
    public List<Auction> getAllAuctions() {
        List<Auction> auctionList = new ArrayList<>();
        String sql = "SELECT id, seller_id, item_id, start_time, end_time, status, current_highest_bid, current_winner_id, version FROM auctions";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int sellerId = rs.getInt("seller_id");
                int itemId = rs.getInt("item_id");
                Timestamp start = rs.getTimestamp("start_time");
                Timestamp end = rs.getTimestamp("end_time");
                double highestBid = rs.getDouble("current_highest_bid");

                // Khởi tạo Object
                Auction auction = new Auction(id, sellerId, itemId, start.toLocalDateTime(), end.toLocalDateTime(), highestBid);
                auction.setStatus(rs.getString("status"));
                auction.setVersion(rs.getInt("version"));

                // Xử lý đọc dữ liệu NULL thành -1
                int winnerId = rs.getInt("current_winner_id");
                if (rs.wasNull()) {
                    auction.setCurrentWinnerId(-1);
                } else {
                    auction.setCurrentWinnerId(winnerId);
                }

                auctionList.add(auction);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy danh sách Auction", e);
        }
        return auctionList;
    }

    // 3. (Bổ sung thêm) Cập nhật trạng thái và giá khi có người đặt lệnh
    public void updateAuction(Auction auction) {
        String sql = "UPDATE auctions SET end_time = ?, status = ?, current_highest_bid = ?, current_winner_id = ?, version = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(2, auction.getStatus());
            stmt.setDouble(3, auction.getCurrentHighestBid());

            if (auction.getCurrentWinnerId() == -1) {
                stmt.setNull(4, Types.INTEGER);
            } else {
                stmt.setInt(4, auction.getCurrentWinnerId());
            }

            stmt.setInt(5, auction.getVersion());
            stmt.setInt(6, auction.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi cập nhật Auction", e);
        }
    }
}