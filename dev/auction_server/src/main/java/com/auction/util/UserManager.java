package com.auction.util;

import com.auction.model.User;
import com.auction.dao.UserDAO; // 1. Import lớp DAO bạn vừa tạo
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class UserManager {
    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName());

    // Singleton instance
    private static volatile UserManager instance;

    // Lưu trữ danh sách người dùng trên RAM (Cache)
    private Map<Integer, User> users;

    // 2. Khai báo đối tượng DAO để làm việc với Database
    private UserDAO userDAO;

    private UserManager() {
        users = new ConcurrentHashMap<>();
        userDAO = new UserDAO();

        // 3. ĐỒNG BỘ: Khi Server vừa bật lên, ta load hết User từ Database lên RAM
        for (User u : userDAO.getAllUsers()) {
            users.put(u.getId(), u);
        }
        LOGGER.info(() -> "✅ Đã đồng bộ " + users.size() + " người dùng từ Database lên RAM.");
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

    /**
     * Thêm người dùng mới vào hệ thống
     */
    public void addUser(User user) {
        // 4. LƯU XUỐNG DB: Gọi DAO để INSERT dữ liệu vào MySQL
        // Sau lệnh này, user sẽ có ID được DB tự động cấp phát
        userDAO.insertUser(user);

        // 5. CẬP NHẬT RAM: Đưa vào map để các luồng xử lý khác lấy ra dùng ngay
        users.put(user.getId(), user);

        LOGGER.info(() -> "👤 User '" + user.getUsername() + "' đã được lưu vào cả DB và RAM.");
    }

    // Lấy thông tin người dùng theo ID (Lấy từ RAM nên cực nhanh)
    public User getUser(int id) {
        return users.get(id);
    }

    // Lấy toàn bộ danh sách người dùng (dùng cho login)
    public java.util.Collection<User> getAllUsers() {
        return users.values();
    }

    // Cập nhật số điện thoại — lưu cả DB lẫn RAM
    public boolean updatePhone(int userId, String phone) {
        boolean ok = userDAO.updatePhone(userId, phone);
        if (ok) {
            User u = users.get(userId);
            if (u != null) u.setPhone(phone);
        }
        return ok;
    }

    // Cập nhật email — lưu cả DB lẫn RAM
    public boolean updateEmail(int userId, String email) {
        boolean ok = userDAO.updateEmail(userId, email);
        if (ok) {
            User u = users.get(userId);
            if (u != null) u.setEmail(email);
        }
        return ok;
    }
}