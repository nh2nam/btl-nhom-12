package com.auction.util;

import com.auction.model.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    // Singleton instance
    private static volatile UserManager instance;

    // Lưu trữ danh sách người dùng (ID -> User)
    private Map<Integer, User> users;

    private UserManager() {
        // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
        users = new ConcurrentHashMap<>();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    // Thêm người dùng vào hệ thống
    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    // Lấy thông tin người dùng theo ID
    public User getUser(int id) {
        return users.get(id);
    }
}