package com.auction.util;

import com.auction.model.Item;
import com.auction.dao.ItemDAO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@SuppressWarnings("java:S6548") // Bỏ qua cảnh báo Singleton của SonarQube
public class ItemManager {
    private static final Logger LOGGER = Logger.getLogger(ItemManager.class.getName());

    private Map<Integer, Item> items;
    private ItemDAO itemDAO;

    private ItemManager() {
        items = new ConcurrentHashMap<>();
        itemDAO = new ItemDAO();

        // Load toàn bộ sản phẩm từ DB lên RAM khi Server khởi động
        for (Item item : itemDAO.getAllItems()) {
            items.put(item.getId(), item);
        }
        LOGGER.info(() -> "✅ Đã đồng bộ " + items.size() + " sản phẩm từ Database lên RAM.");
    }

    // Bill Pugh Singleton - 100% Thread-safe, không cần dùng volatile hay synchronized
    private static class InstanceHolder {
        private static final ItemManager INSTANCE = new ItemManager();
    }

    public static ItemManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    //Add item
    public void addItem(Item item) {
        // 1. Lưu vào MySQL
        itemDAO.insertItem(item);

        // 2. Cập nhật Map trên RAM
        items.put(item.getId(), item);

        LOGGER.info(() -> "🛍️ Sản phẩm '" + item.getName() + "' đã được lưu vào hệ thống.");
    }

    // Delete Item
    public boolean deleteItem(int itemId) {
        // 1. Xóa trong MySQL trước
        boolean isDeleted = itemDAO.deleteItem(itemId);

        // 2. Nếu MySQL xóa thành công, ta mới xóa trên RAM
        if (isDeleted) {
            items.remove(itemId);
            LOGGER.info(() -> "🗑️ Đã xóa sản phẩm ID " + itemId + " khỏi RAM.");
        }
        return isDeleted;
    }

    public Item getItem(int id) {
        return items.get(id);
    }

    public java.util.Collection<Item> getAllItems() {
        return items.values();
    }

    // Xóa item khỏi RAM (không xóa DB — dùng khi admin đã xóa DB riêng)
    public void removeItem(int itemId) {
        items.remove(itemId);
        LOGGER.info(() -> "🗑️ Đã xóa itemId=" + itemId + " khỏi RAM.");
    }
}