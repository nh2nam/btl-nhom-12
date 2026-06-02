package com.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO quản lý lịch sử chat của từng phiên đấu giá.
 * Bảng chat_messages được tạo tự động nếu chưa tồn tại (xem DatabaseConnection.ensureSchema).
 */
public class ChatMessageDAO {

    private static final Logger LOGGER = Logger.getLogger(ChatMessageDAO.class.getName());

    /** Lưu một tin nhắn chat mới vào database */
    public void insertMessage(int auctionId, String senderName, String content) {
        String sql = "INSERT INTO chat_messages (auction_id, sender_name, content, sent_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            stmt.setString(2, senderName);
            stmt.setString(3, content);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu tin nhắn chat", e);
        }
    }

    /** Xóa toàn bộ tin nhắn chat của một phiên đấu giá */
    public void deleteByAuctionId(int auctionId) {
        String sql = "DELETE FROM chat_messages WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi xóa chat theo auctionId", e);
        }
    }

    /** Xóa toàn bộ tin nhắn chat liên quan đến một user (theo sender_name) */
    public void deleteByUsername(String username) {
        String sql = "DELETE FROM chat_messages WHERE sender_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi xóa chat theo username", e);
        }
    }

    /**
     * Lấy lịch sử chat của một phiên, sắp xếp từ cũ đến mới.
     * Trả về List<Map> để ClientHandler dễ serialize thành JSON.
     */
    public List<Map<String, Object>> getMessagesByAuctionId(int auctionId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT sender_name, content, sent_at FROM chat_messages " +
                     "WHERE auction_id = ? ORDER BY sent_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sender",  rs.getString("sender_name"));
                    row.put("content", rs.getString("content"));
                    // Format thời gian cho đẹp (bỏ phần nano-giây)
                    String sentAt = rs.getTimestamp("sent_at").toLocalDateTime().toString()
                            .replace("T", " ");
                    if (sentAt.contains(".")) sentAt = sentAt.substring(0, sentAt.indexOf("."));
                    row.put("sentAt", sentAt);
                    result.add(row);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi tải lịch sử chat", e);
        }
        return result;
    }
}
