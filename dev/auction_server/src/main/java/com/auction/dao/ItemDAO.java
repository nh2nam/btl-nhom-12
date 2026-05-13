package com.auction.dao;

import com.auction.model.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemDAO {
    private static final Logger LOGGER = Logger.getLogger(ItemDAO.class.getName());

    public void insertItem(Item item) {
        String sql = "INSERT INTO items (name, description, starting_price, image_path,category) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());
            stmt.setString(4, item.getImagePath());
            stmt.setString(5, item.getCategory());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
            LOGGER.info(() -> "📦 Đã lưu sản phẩm '" + item.getName() + "' vào Database.");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lưu Item vào Database", e);
        }
    }

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT id, name, description, starting_price, image_path,category FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // 2. ĐÃ SỬA: Dùng Nhà máy (ItemFactory) để lắp ráp sản phẩm
                Item item = com.auction.util.ItemFactory.createItem(
                        rs.getString("category"), // Lấy category để báo cho Nhà máy biết
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        rs.getString("image_path")
                );
                itemList.add(item);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi lấy danh sách Item", e);
        }
        return itemList;
    }

    public boolean deleteItem(int itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Lỗi khi xóa Item", e);
        }
        return false;
    }
}