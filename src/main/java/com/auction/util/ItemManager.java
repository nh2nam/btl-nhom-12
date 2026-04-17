package com.auction.util;

import com.auction.model.Item;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {
    // Singleton instance
    private static volatile ItemManager instance;

    // Lưu trữ danh sách sản phẩm (ID -> Item)
    private Map<Integer, Item> items;

    private ItemManager() {
        // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
        items = new ConcurrentHashMap<>();
    }

    public static ItemManager getInstance() {
        if (instance == null) {
            synchronized (ItemManager.class) {
                if (instance == null) {
                    instance = new ItemManager();
                }
            }
        }
        return instance;
    }

    // Thêm sản phẩm vào hệ thống
    public void addItem(Item item) {
        items.put(item.getId(), item);
    }

    // Lấy thông tin sản phẩm theo ID
    public Item getItem(int id) {
        return items.get(id);
    }
}