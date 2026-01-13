package com.calendar.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.mindrot.jbcrypt.BCrypt;

import com.calendar.database.DatabaseManager;
import com.calendar.model.User;

public class UserService {
    private final DatabaseManager dbManager;

    public UserService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Đăng ký người dùng mới
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean register(String username, String password, String email) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.length() < 6 ||
            email == null || !email.contains("@")) {
            return false;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO users (username, password, email, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email.trim());
            pstmt.setString(4, DatabaseManager.formatDateTime(LocalDateTime.now()));

            pstmt.executeUpdate();
            System.out.println("Đăng ký thành công: " + username);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Tên đăng nhập hoặc email đã tồn tại");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Đăng nhập
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                
                // Kiểm tra mật khẩu
                if (BCrypt.checkpw(password, storedHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setCreatedAt(DatabaseManager.parseDateTime(rs.getString("created_at")));
                    
                    System.out.println("Đăng nhập thành công: " + username);
                    return user;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Đăng nhập thất bại: Sai tên đăng nhập hoặc mật khẩu");
        return null;
    }

    /**
     * Lấy thông tin người dùng theo ID
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setCreatedAt(DatabaseManager.parseDateTime(rs.getString("created_at")));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Cập nhật thông tin người dùng
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean updateUser(int userId, String email) {
        String sql = "UPDATE users SET email = ? WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đổi mật khẩu
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) {
            return false;
        }

        // Lấy mật khẩu hiện tại
        String sql = "SELECT password FROM users WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                
                // Kiểm tra mật khẩu cũ
                if (!BCrypt.checkpw(oldPassword, storedHash)) {
                    System.out.println("Mật khẩu cũ không đúng");
                    return false;
                }

                // Cập nhật mật khẩu mới
                String updateSql = "UPDATE users SET password = ? WHERE id = ?";
                try (PreparedStatement updateStmt = dbManager.getConnection().prepareStatement(updateSql)) {
                    String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                    updateStmt.setString(1, newHash);
                    updateStmt.setInt(2, userId);
                    
                    int rowsAffected = updateStmt.executeUpdate();
                    return rowsAffected > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}