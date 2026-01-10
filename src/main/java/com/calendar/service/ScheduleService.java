package com.calendar.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.calendar.database.DatabaseManager;
import com.calendar.model.Schedule;

public class ScheduleService {
    private final DatabaseManager dbManager;

    public ScheduleService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Thêm lịch mới
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean addSchedule(Schedule schedule) {
        String sql = """
            INSERT INTO schedules 
            (user_id, title, description, note, start_time, end_time, 
             is_reminder, reminder_minutes, category, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getUserId());
            pstmt.setString(2, schedule.getTitle());
            pstmt.setString(3, schedule.getDescription());
            pstmt.setString(4, schedule.getNote());
            pstmt.setString(5, DatabaseManager.formatDateTime(schedule.getStartTime()));
            pstmt.setString(6, DatabaseManager.formatDateTime(schedule.getEndTime()));
            pstmt.setInt(7, schedule.isReminder() ? 1 : 0);
            pstmt.setInt(8, schedule.getReminderMinutes());
            pstmt.setString(9, schedule.getCategory());
            pstmt.setString(10, DatabaseManager.formatDateTime(LocalDateTime.now()));
            pstmt.setString(11, DatabaseManager.formatDateTime(LocalDateTime.now()));

            pstmt.executeUpdate();
            System.out.println("Đã thêm lịch: " + schedule.getTitle());
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật lịch
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean updateSchedule(Schedule schedule) {
        String sql = """
            UPDATE schedules 
            SET title = ?, description = ?, note = ?, start_time = ?, end_time = ?, 
                is_reminder = ?, reminder_minutes = ?, category = ?, updated_at = ? 
            WHERE id = ? AND user_id = ?
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, schedule.getTitle());
            pstmt.setString(2, schedule.getDescription());
            pstmt.setString(3, schedule.getNote());
            pstmt.setString(4, DatabaseManager.formatDateTime(schedule.getStartTime()));
            pstmt.setString(5, DatabaseManager.formatDateTime(schedule.getEndTime()));
            pstmt.setInt(6, schedule.isReminder() ? 1 : 0);
            pstmt.setInt(7, schedule.getReminderMinutes());
            pstmt.setString(8, schedule.getCategory());
            pstmt.setString(9, DatabaseManager.formatDateTime(LocalDateTime.now()));
            pstmt.setInt(10, schedule.getId());
            pstmt.setInt(11, schedule.getUserId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Đã cập nhật lịch: " + schedule.getTitle());
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Xóa lịch
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public boolean deleteSchedule(int scheduleId, int userId) {
        String sql = "DELETE FROM schedules WHERE id = ? AND user_id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, scheduleId);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Đã xóa lịch ID: " + scheduleId);
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lấy tất cả lịch của người dùng
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Schedule> getAllSchedulesByUser(int userId) {
        List<Schedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE user_id = ? ORDER BY start_time ASC";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }

    /**
     * Lấy lịch theo ngày
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Schedule> getSchedulesByDate(int userId, LocalDate date) {
        List<Schedule> schedules = new ArrayList<>();
        String sql = """
            SELECT * FROM schedules 
            WHERE user_id = ? 
            AND date(start_time) = ? 
            ORDER BY start_time ASC
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }

    /**
     * Lấy lịch có nhắc nhở sắp tới
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Schedule> getUpcomingReminders(int userId) {
        List<Schedule> schedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkUntil = now.plusHours(24); // Kiểm tra 24h tới

        String sql = """
            SELECT * FROM schedules 
            WHERE user_id = ? 
            AND is_reminder = 1 
            AND start_time BETWEEN ? AND ? 
            ORDER BY start_time ASC
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, DatabaseManager.formatDateTime(now));
            pstmt.setString(3, DatabaseManager.formatDateTime(checkUntil));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }

    /**
     * Tìm kiếm lịch
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Schedule> searchSchedules(int userId, String keyword) {
        List<Schedule> schedules = new ArrayList<>();
        String sql = """
            SELECT * FROM schedules 
            WHERE user_id = ? 
            AND (title LIKE ? OR description LIKE ? OR note LIKE ?) 
            ORDER BY start_time DESC
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setInt(1, userId);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }

    /**
     * Helper method để map ResultSet sang Schedule object
     */
    private Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
        Schedule schedule = new Schedule();
        schedule.setId(rs.getInt("id"));
        schedule.setUserId(rs.getInt("user_id"));
        schedule.setTitle(rs.getString("title"));
        schedule.setDescription(rs.getString("description"));
        schedule.setNote(rs.getString("note"));
        schedule.setStartTime(DatabaseManager.parseDateTime(rs.getString("start_time")));
        schedule.setEndTime(DatabaseManager.parseDateTime(rs.getString("end_time")));
        schedule.setReminder(rs.getInt("is_reminder") == 1);
        schedule.setReminderMinutes(rs.getInt("reminder_minutes"));
        schedule.setCategory(rs.getString("category"));
        schedule.setCreatedAt(DatabaseManager.parseDateTime(rs.getString("created_at")));
        schedule.setUpdatedAt(DatabaseManager.parseDateTime(rs.getString("updated_at")));
        return schedule;
    }
}