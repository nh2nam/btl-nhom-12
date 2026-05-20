package com.auction.client.network;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Quản lý kết nối Socket duy nhất tới Server (Singleton thread-safe).
 * - Stream (in/out) được tạo một lần duy nhất khi connect(), không tạo lại mỗi lần gửi.
 * - Tự động reconnect nếu kết nối bị mất.
 * - Có timeout 10 giây để tránh treo vô thời hạn.
 */
public class ServerConnection {

    // --- Cấu hình mạng ---
    private static final String SERVER_IP   = "localhost";
    private static final int    SERVER_PORT = 9999;
    private static final int    TIMEOUT_MS  = 10_000; // 10 giây

    // --- Singleton: volatile đảm bảo thread-safe khi đọc/ghi instance ---
    private static volatile ServerConnection instance;

    // --- Tài nguyên socket (dùng chung, không tạo lại mỗi lần gửi) ---
    private Socket       socket;
    private PrintWriter  out;
    private BufferedReader in;

    private final Gson gson = new Gson();

    private ServerConnection() {}

    /** Double-checked locking — an toàn với đa luồng. */
    public static ServerConnection getInstance() {
        if (instance == null) {
            synchronized (ServerConnection.class) {
                if (instance == null) {
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Kết nối / Ngắt kết nối
    // -------------------------------------------------------------------------

    /**
     * Mở kết nối tới Server. Nếu đã kết nối rồi thì không làm gì thêm.
     * @return true nếu kết nối thành công.
     */
    public synchronized boolean connect() {
        if (isConnected()) return true;
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            socket.setSoTimeout(TIMEOUT_MS); // Fix 5: timeout tránh treo vô thời hạn

            // Fix 1: Khởi tạo stream MỘT LẦN DUY NHẤT tại đây
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("✅ Đã kết nối tới Server tại port " + SERVER_PORT);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Lỗi kết nối Server: " + e.getMessage());
            closeResources();
            return false;
        }
    }

    /** Ngắt kết nối an toàn khi tắt app. */
    public synchronized void disconnect() {
        closeResources();
        System.out.println("🛑 Đã ngắt kết nối với Server.");
    }

    // -------------------------------------------------------------------------
    // Gửi / Nhận dữ liệu
    // -------------------------------------------------------------------------

    /**
     * Gửi một request lên Server và chờ nhận Response.
     * Tự động reconnect nếu kết nối bị mất trước khi gửi.
     *
     * @param action  Tên hành động (ví dụ: "LOGIN", "PLACE_BID")
     * @param payload Dữ liệu đính kèm (có thể là Map, Object, hoặc null)
     * @return Response từ Server; trả về Response mặc định (success=false) nếu có lỗi.
     */
    public synchronized Response send(String action, Object payload) {
        // Fix 4: Tự động reconnect nếu kết nối bị mất
        if (!isConnected()) {
            System.out.println("🔄 Mất kết nối, đang thử kết nối lại...");
            if (!connect()) {
                System.err.println("❌ Không thể kết nối lại Server.");
                return errorResponse("Không thể kết nối tới Server.");
            }
        }

        try {
            // 1. Đóng gói request thành JSON
            java.util.Map<String, Object> requestMap = new java.util.HashMap<>();
            requestMap.put("action", action);
            requestMap.put("payload", payload);
            String jsonRequest = gson.toJson(requestMap);
            System.out.println("👉 Client gửi: " + jsonRequest);

            // 2. Gửi qua stream đã mở sẵn (không tạo mới)
            out.println(jsonRequest);

            // 3. Đọc phản hồi
            String jsonResponse = in.readLine();
            System.out.println("👈 Server đáp: " + jsonResponse);

            if (jsonResponse != null) {
                return gson.fromJson(jsonResponse, Response.class);
            }

            // Server đóng kết nối (readLine trả về null)
            System.err.println("⚠️ Server đóng kết nối.");
            closeResources();
            return errorResponse("Server đã đóng kết nối.");

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("⏱️ Timeout: Server không phản hồi trong " + (TIMEOUT_MS / 1000) + "s.");
            closeResources();
            return errorResponse("Server không phản hồi (timeout).");
        } catch (IOException e) {
            System.err.println("❌ Lỗi đường truyền mạng: " + e.getMessage());
            closeResources();
            return errorResponse("Lỗi mạng: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Hàm nội bộ
    // -------------------------------------------------------------------------

    /** Kiểm tra socket có đang kết nối và stream còn dùng được không. */
    private boolean isConnected() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && out != null
                && in  != null;
    }

    /** Đóng tất cả tài nguyên mạng một cách an toàn. */
    private void closeResources() {
        try { if (in     != null) in.close();     } catch (IOException ignored) {}
        try { if (out    != null) out.close();    } catch (Exception  ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        in     = null;
        out    = null;
        socket = null;
    }

    /** Tạo một Response báo lỗi để trả về khi không thể liên lạc với Server. */
    private Response errorResponse(String message) {
        return gson.fromJson(
                "{\"success\":false,\"message\":\"" + message + "\"}",
                Response.class
        );
    }
}
