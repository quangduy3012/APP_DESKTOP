package com.calendar.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.calendar.model.Schedule;

import javafx.application.Platform;
import javafx.scene.control.Alert;
@SuppressWarnings("unused")
public class NotificationService {
    private final ScheduleService scheduleService;
    private Timer timer;
    private int currentUserId;

    public NotificationService(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Bắt đầu dịch vụ nhắc nhở
     */
    public void startNotificationService(int userId) {
        this.currentUserId = userId;
        
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer(true); // daemon thread
        
        // Kiểm tra mỗi 30 giây
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndNotify();
            }
        }, 0, 30000); // 0ms delay, 30s period

        System.out.println("Dịch vụ nhắc nhở đã được khởi động cho user ID: " + userId);
    }

    /**
     * Dừng dịch vụ nhắc nhở
     */
    public void stopNotificationService() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            System.out.println("Dịch vụ nhắc nhở đã dừng");
        }
    }

    /**
     * Kiểm tra và gửi thông báo
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private void checkAndNotify() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Schedule> upcomingSchedules = scheduleService.getUpcomingReminders(currentUserId);

            for (Schedule schedule : upcomingSchedules) {
                LocalDateTime reminderTime = schedule.getStartTime()
                    .minusMinutes(schedule.getReminderMinutes());

                // Kiểm tra nếu thời gian nhắc nhở đã đến
                Duration duration = Duration.between(now, reminderTime);
                long minutesUntilReminder = duration.toMinutes();

                // Nếu còn 0-1 phút nữa là đến thời gian nhắc nhở
                if (minutesUntilReminder >= 0 && minutesUntilReminder <= 1) {
                    showNotification(schedule);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị thông báo
     */
    private void showNotification(Schedule schedule) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Nhắc nhở lịch");
            alert.setHeaderText(schedule.getTitle());
            
            StringBuilder content = new StringBuilder();
            content.append("Thời gian: ")
                   .append(formatDateTime(schedule.getStartTime()))
                   .append("\n\n");
            
            if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
                content.append("Mô tả: ")
                       .append(schedule.getDescription())
                       .append("\n");
            }
            
            if (schedule.getNote() != null && !schedule.getNote().isEmpty()) {
                content.append("\nGhi chú: ")
                       .append(schedule.getNote());
            }

            alert.setContentText(content.toString());
            
            // Phát âm thanh (nếu muốn)
            playSound();
            
            alert.showAndWait();
        });
    }

    /**
     * Phát âm thanh thông báo
     */
    private void playSound() {
        try {
            // Phát âm thanh mặc định của hệ thống
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // Bỏ qua nếu không phát được âm thanh
        }
    }

    /**
     * Format DateTime để hiển thị
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return String.format("%02d/%02d/%d %02d:%02d",
            dateTime.getDayOfMonth(),
            dateTime.getMonthValue(),
            dateTime.getYear(),
            dateTime.getHour(),
            dateTime.getMinute()
        );
    }

    /**
     * Hiển thị thông báo thủ công cho một lịch cụ thể
     */
    public void showManualNotification(Schedule schedule) {
        showNotification(schedule);
    }

    /**
     * Kiểm tra xem có lịch nào cần nhắc nhở trong khoảng thời gian sắp tới không
     */
    public int getUpcomingRemindersCount(int minutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkUntil = now.plusMinutes(minutes);
        
        List<Schedule> schedules = scheduleService.getUpcomingReminders(currentUserId);
        
        int count = 0;
        for (Schedule schedule : schedules) {
            LocalDateTime reminderTime = schedule.getStartTime()
                .minusMinutes(schedule.getReminderMinutes());
            
            if (reminderTime.isAfter(now) && reminderTime.isBefore(checkUntil)) {
                count++;
            }
        }
        
        return count;
    }
}