package com.calendar;

import java.io.IOException;

import com.calendar.database.DatabaseManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CalendarApp extends Application {
    
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Quản lý Lịch Làm Việc");

        
        // Khởi tạo database
        DatabaseManager.getInstance();
        
        // Hiển thị màn hình đăng nhập
        showLoginScreen();
        
        primaryStage.setOnCloseRequest(event -> {
            DatabaseManager.getInstance().closeConnection();
            System.exit(0);
        });
        
        primaryStage.show();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void showLoginScreen() {
        try {
            Parent root = FXMLLoader.load(CalendarApp.class.getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(CalendarApp.class.getResource("/css/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình đăng nhập");
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void showRegisterScreen() {
        try {
            Parent root = FXMLLoader.load(CalendarApp.class.getResource("/fxml/register.fxml"));
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(CalendarApp.class.getResource("/css/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình đăng ký");
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void showMainScreen(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(CalendarApp.class.getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            // Truyền userId vào controller
            com.calendar.controller.MainController controller = loader.getController();
            controller.setUserId(userId);
            controller.loadSchedules();
            
            Scene scene = new Scene(root, 1200, 700);
            scene.getStylesheets().add(CalendarApp.class.getResource("/css/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setWidth(1200);
            primaryStage.setHeight(700);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình chính");
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}