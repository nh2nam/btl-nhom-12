package com.auction.dao;

import com.auction.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {

    // Khởi tạo Logger
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // 1. Hàm lưu User vào Database
    public void insertUser(User user) {
        String sql = "INSERT INTO users (username, account_name, display_name, password_hash, role, rating, auto_bid_enabled, max_auto_bid_amount, auto_bid_increment, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());           // account_name = email
            stmt.setString(3, user.getDisplayName());     // display_name = họ tên

            stmt.setString(4, user.getPasswordHash());

            if (user instanceof Seller) {
                stmt.setString(5, "SELLER");
                stmt.setDouble(6, ((Seller) user).getRating());
                stmt.setNull(7, Types.BOOLEAN);
                stmt.setDouble(8, 0);
                stmt.setDouble(9, 0);
            } else if (user instanceof Bidder) {
                stmt.setString(5, "BIDDER");
                stmt.setDouble(6, 0);
                stmt.setBoolean(7, ((Bidder) user).isAutoBidEnabled());
                stmt.setDouble(8, ((Bidder) user).getMaxAutoBidAmount());
                stmt.setDouble(9, ((Bidder) user).getAutoBidIncrement());
            } else {
                stmt.setString(5, "ADMIN");
                stmt.setNull(6, Types.DOUBLE);
                stmt.setNull(7, Types.BOOLEAN);
                stmt.setDouble(8, 0);
                stmt.setDouble(9, 0);
            }
            stmt.setString(10, user.getPhone() != null ? user.getPhone() : "");

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            LOGGER.info(() -> "💾 Đã lưu User '" + user.getUsername() + "' vào Database.");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu User vào Database", e);
        }
    }

    // 2. Hàm lấy tất cả User từ Database
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT id, username, account_name, display_name, password_hash, role, rating, auto_bid_enabled, max_auto_bid_amount, auto_bid_increment, phone FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int    id          = rs.getInt("id");
                String loginName   = rs.getString("username");       // tên đăng nhập
                String email       = rs.getString("account_name");   // email thật
                String displayName = rs.getString("display_name");   // họ tên hiển thị
                String pass        = rs.getString("password_hash");
                String role        = rs.getString("role");
                String phone       = rs.getString("phone");

                User u;
                if ("SELLER".equals(role)) {
                    u = new Seller(id, loginName, email, pass);
                    ((Seller) u).setRating(rs.getDouble("rating"));
                } else if ("BIDDER".equals(role)) {
                    u = new Bidder(id, loginName, email, pass);
                    ((Bidder) u).setAutoBidEnabled(rs.getBoolean("auto_bid_enabled"));
                    ((Bidder) u).setMaxAutoBidAmount(rs.getDouble("max_auto_bid_amount"));
                    ((Bidder) u).setAutoBidIncrement(rs.getDouble("auto_bid_increment"));
                } else {
                    u = new Admin(id, loginName, email, pass);
                }
                u.setDisplayName(displayName != null ? displayName : loginName);
                u.setPhone(phone);
                userList.add(u);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy danh sách User", e);
        }
        return userList;
    }

    // 3. Cập nhật số điện thoại cho User
    public boolean updatePhone(int userId, String phone) {
        String sql = "UPDATE users SET phone = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            LOGGER.info(() -> "📱 Đã cập nhật phone cho userId=" + userId);
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi cập nhật phone", e);
            return false;
        }
    }

    // 4. Cập nhật email cho User
    public boolean updateEmail(int userId, String email) {
        String sql = "UPDATE users SET account_name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            LOGGER.info(() -> "📧 Đã cập nhật email cho userId=" + userId);
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi cập nhật email", e);
            return false;
        }
    }
}