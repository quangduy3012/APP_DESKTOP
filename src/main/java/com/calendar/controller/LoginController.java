package com.calendar.controller;

import com.calendar.CalendarApp;
import com.calendar.model.User;
import com.calendar.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();
        
        // Enter để đăng nhập
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", "error");
            return;
        }

        // Disable button khi đang xử lý
        loginButton.setDisable(true);
        messageLabel.setText("Đang đăng nhập...");

        // Thực hiện đăng nhập
        User user = userService.login(username, password);

        if (user != null) {
            showMessage("Đăng nhập thành công!", "success");
            
            // Chuyển sang màn hình chính sau 500ms
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    javafx.application.Platform.runLater(() -> {
                        CalendarApp.showMainScreen(user.getId());
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } else {
            showMessage("Sai tên đăng nhập hoặc mật khẩu!", "error");
            loginButton.setDisable(false);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handleRegister() {
        CalendarApp.showRegisterScreen();
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