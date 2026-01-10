package com.calendar.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@SuppressWarnings("unused")
public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:schedule_manager.db";
    private static DatabaseManager instance;
    private Connection connection;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @SuppressWarnings("CallToPrintStackTrace")
    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể kết nối đến database: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void createTables() {
        try {
            Statement stmt = connection.createStatement();

            // Bảng Users
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    created_at TEXT NOT NULL
                )
            """;

            // Bảng Schedules
            String createSchedulesTable = """
                CREATE TABLE IF NOT EXISTS schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    note TEXT,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    is_reminder INTEGER DEFAULT 0,
                    reminder_minutes INTEGER DEFAULT 15,
                    category TEXT DEFAULT 'Khác',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            stmt.execute(createUsersTable);
            stmt.execute(createSchedulesTable);

            System.out.println("Đã tạo các bảng trong database thành công!");

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo bảng: " + e.getMessage());
        }
    }

    // Helper methods để chuyển đổi LocalDateTime
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        return LocalDateTime.parse(dateTimeString, FORMATTER);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Đã đóng kết nối database");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}