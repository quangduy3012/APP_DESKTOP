package com.calendar.controller;

import com.calendar.CalendarApp;
import com.calendar.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
@SuppressWarnings("unused")
public class RegisterController {
    
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
    }

    @FXML
    @SuppressWarnings("CallToPrintStackTrace")
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", "error");
            return;
        }

        if (username.length() < 3) {
            showMessage("Tên đăng nhập phải có ít nhất 3 ký tự!", "error");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showMessage("Email không hợp lệ!", "error");
            return;
        }

        if (password.length() < 6) {
            showMessage("Mật khẩu phải có ít nhất 6 ký tự!", "error");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu xác nhận không khớp!", "error");
            return;
        }

        // Disable button khi đang xử lý
        registerButton.setDisable(true);
        messageLabel.setText("Đang đăng ký...");

        // Thực hiện đăng ký
        boolean success = userService.register(username, password, email);

        if (success) {
            showMessage("Đăng ký thành công! Đang chuyển về trang đăng nhập...", "success");
            
            // Chuyển về màn hình đăng nhập sau 1.5s
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> {
                        CalendarApp.showLoginScreen();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } else {
            showMessage("Đăng ký thất bại! Tên đăng nhập hoặc email đã tồn tại.", "error");
            registerButton.setDisable(false);
        }
    }

    @FXML
    private void handleBackToLogin() {
        CalendarApp.showLoginScreen();
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success-message", "error-message");
        
        if (type.equals("success")) {
            messageLabel.getStyleClass().add("success-message");
        } else if (type.equals("error")) {
            messageLabel.getStyleClass().add("error-message");
        }
        
        messageLabel.setVisible(true);
    }
}