package com.auction.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích hash và kiểm tra mật khẩu dùng BCrypt.
 * - hash()   : dùng khi đăng ký / đổi mật khẩu
 * - verify() : dùng khi đăng nhập
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /** Tạo hash BCrypt từ mật khẩu plain-text. */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Kiểm tra mật khẩu plain-text có khớp với hash đã lưu không.
     * An toàn với timing attack vì BCrypt so sánh constant-time.
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
