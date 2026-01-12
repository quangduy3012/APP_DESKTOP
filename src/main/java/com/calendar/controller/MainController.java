package com.calendar.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.calendar.CalendarApp;
import com.calendar.model.Schedule;
import com.calendar.service.NotificationService;
import com.calendar.service.ScheduleService;
import com.calendar.service.UserService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.converter.IntegerStringConverter;

@SuppressWarnings("unused")
public class MainController {

    @FXML private Label welcomeLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TableView<Schedule> scheduleTable;
    @FXML private TableColumn<Schedule, String> titleColumn;
    @FXML private TableColumn<Schedule, String> timeColumn;
    @FXML private TableColumn<Schedule, String> categoryColumn;
    @FXML private TableColumn<Schedule, Boolean> reminderColumn;
    @FXML private TextArea detailsArea;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button logoutButton;

    private int currentUserId;
    private ScheduleService scheduleService;
    private UserService userService;
    private NotificationService notificationService;
    private ObservableList<Schedule> scheduleList;
    @SuppressWarnings("FieldMayBeFinal")
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @FXML
    public void initialize() {
        scheduleService = new ScheduleService();
        userService = new UserService();
        notificationService = new NotificationService(scheduleService);
        scheduleList = FXCollections.observableArrayList();

        setupTable();
        setupCategoryFilter();
        setupListeners();

        datePicker.setValue(LocalDate.now());
        
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        var user = userService.getUserById(userId);
        if (user != null) {
            welcomeLabel.setText("Xin chào, " + user.getUsername() + "!");
        }

        notificationService.startNotificationService(userId);
        loadSchedules();
    }

    private void setupTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        timeColumn.setCellValueFactory(cellData -> {
            Schedule schedule = cellData.getValue();
            String timeStr = schedule.getStartTime().format(timeFormatter);
            return new javafx.beans.property.SimpleStringProperty(timeStr);
        });

        reminderColumn.setCellValueFactory(new PropertyValueFactory<>("reminder"));
        reminderColumn.setCellFactory(column -> new TableCell<Schedule, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "✓" : "");
                }
            }
        });

        scheduleTable.setItems(scheduleList);

        scheduleTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showScheduleDetails(newSelection);
                    editButton.setDisable(false);
                    deleteButton.setDisable(false);
                } else {
                    detailsArea.clear();
                    editButton.setDisable(true);
                    deleteButton.setDisable(true);
                }
            }
        );
    }

    private void setupCategoryFilter() {
        categoryFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "Công việc", "Sinh hoạt", "Học tập", "Thể thao", "Giải trí", "Khác"
        ));
        categoryFilter.setValue("Tất cả");
    }

    private void setupListeners() {
        datePicker.setOnAction(e -> filterSchedules());
        categoryFilter.setOnAction(e -> filterSchedules());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterSchedules());
    }

    public void loadSchedules() {
        List<Schedule> schedules = scheduleService.getAllSchedulesByUser(currentUserId);
        scheduleList.setAll(schedules);
    }
@FXML
private void handleResetFilter() {
    // Reset UI
    searchField.clear();
    datePicker.setValue(null);
    categoryFilter.setValue("Tất cả");

    // Load lại dữ liệu ban đầu
    loadSchedules();
}

    /**
     * TÌM KIẾM THÔNG MINH - Query trực tiếp từ database
     */
    private void filterSchedules() {
        LocalDate selectedDate = datePicker.getValue();
        String category = categoryFilter.getValue();
        String keyword = searchField.getText().trim();

        List<Schedule> filteredSchedules;

        // TH1: Có từ khóa tìm kiếm - Dùng searchSchedules() từ database
        if (!keyword.isEmpty()) {
            filteredSchedules = scheduleService.searchSchedules(currentUserId, keyword);
            
            // Sau đó filter thêm theo date và category (vì search chỉ filter keyword)
            if (selectedDate != null) {
                filteredSchedules = filteredSchedules.stream()
                    .filter(s -> s.getStartTime().toLocalDate().equals(selectedDate))
                    .toList();
            }
            
            if (category != null && !category.equals("Tất cả")) {
                filteredSchedules = filteredSchedules.stream()
                    .filter(s -> s.getCategory().equals(category))
                    .toList();
            }
        }
        // TH2: Lọc theo ngày cụ thể - Dùng getSchedulesByDate() từ database
        else if (selectedDate != null) {
            filteredSchedules = scheduleService.getSchedulesByDate(currentUserId, selectedDate);
            
            // Filter thêm theo category
            if (category != null && !category.equals("Tất cả")) {
                filteredSchedules = filteredSchedules.stream()
                    .filter(s -> s.getCategory().equals(category))
                    .toList();
            }
        }
        // TH3: Chỉ filter theo category hoặc load tất cả
        else {
            filteredSchedules = scheduleService.getAllSchedulesByUser(currentUserId);
            
            if (category != null && !category.equals("Tất cả")) {
                filteredSchedules = filteredSchedules.stream()
                    .filter(s -> s.getCategory().equals(category))
                    .toList();
            }
        }

        scheduleList.setAll(filteredSchedules);
    }

    private void showScheduleDetails(Schedule schedule) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tiêu đề: ").append(schedule.getTitle()).append("\n\n");
        sb.append("Thời gian bắt đầu: ").append(schedule.getStartTime().format(timeFormatter)).append("\n");
        sb.append("Thời gian kết thúc: ").append(schedule.getEndTime().format(timeFormatter)).append("\n\n");
        sb.append("Danh mục: ").append(schedule.getCategory()).append("\n");
        sb.append("Nhắc nhở: ").append(schedule.isReminder() ? "Có (" + schedule.getReminderMinutes() + " phút trước)" : "Không").append("\n\n");

        if (schedule.getDescription() != null && !schedule.getDescription().isEmpty()) {
            sb.append("Mô tả: ").append(schedule.getDescription()).append("\n\n");
        }

        if (schedule.getNote() != null && !schedule.getNote().isEmpty()) {
            sb.append("Ghi chú: ").append(schedule.getNote()).append("\n");
        }

        detailsArea.setText(sb.toString());
    }

    @FXML
    private void handleAddSchedule() {
        showScheduleDialog(null);
    }

    @FXML
    private void handleEditSchedule() {
        Schedule selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showScheduleDialog(selected);
        }
    }

    @FXML
    private void handleDeleteSchedule() {
        Schedule selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Bạn có chắc muốn xóa lịch này?");
        alert.setContentText(selected.getTitle());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (scheduleService.deleteSchedule(selected.getId(), currentUserId)) {
                loadSchedules();
                showAlert("Thành công", "Đã xóa lịch", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Lỗi", "Không thể xóa lịch", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleLogout() {
        notificationService.stopNotificationService();
        CalendarApp.showLoginScreen();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Helper method tạo Spinner cho giờ/phút với khả năng nhập tay và clamp giá trị
    private Spinner<Integer> createTimeSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);

        // Filter chỉ cho phép số
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("\\d*") ? change : null;
        };

        TextFormatter<Integer> formatter = new TextFormatter<>(
                new IntegerStringConverter(), initial, filter);
        spinner.getEditor().setTextFormatter(formatter);

        // Định dạng hiển thị 2 chữ số (09 thay vì 9)
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!spinner.isFocused() && !newValue.isEmpty()) {
                try {
                    int val = Integer.parseInt(newValue);
                    spinner.getEditor().setText(String.format("%02d", val));
                } catch (NumberFormatException ignored) {}
            }
        });

        // Clamp giá trị khi mất focus hoặc nhấn Enter
        spinner.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEditorText(spinner, min, max, initial);
            }
        });

        spinner.getEditor().setOnAction(e -> commitEditorText(spinner, min, max, initial));

        return spinner;
    }

    private void commitEditorText(Spinner<Integer> spinner, int min, int max, int fallback) {
        String text = spinner.getEditor().getText();
        try {
            int value = Integer.parseInt(text);
            int clamped = Math.max(min, Math.min(max, value));
            spinner.getValueFactory().setValue(clamped);
            spinner.getEditor().setText(String.format("%02d", clamped));
        } catch (NumberFormatException e) {
            spinner.getValueFactory().setValue(fallback);
        }
    }

    private void showScheduleDialog(Schedule existingSchedule) {
        Dialog<Schedule> dialog = new Dialog<>();
        dialog.setTitle(existingSchedule == null ? "Thêm lịch mới" : "Sửa lịch");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(5);  // Giảm từ 10 xuống 5
        grid.setVgap(8);   // Giảm từ 10 xuống 8
        grid.setPadding(new Insets(15, 15, 10, 10));  // Giảm padding

        TextField titleField = new TextField();
        titleField.setPromptText("Tiêu đề");
        titleField.setPrefWidth(350);  // Đặt width cố định

        TextArea descField = new TextArea();
        descField.setPromptText("Mô tả");
        descField.setPrefRowCount(2);  // Giảm từ 3 xuống 2
        descField.setPrefWidth(350);

        TextArea noteField = new TextArea();
        noteField.setPromptText("Ghi chú");
        noteField.setPrefRowCount(2);  // Giảm từ 3 xuống 2
        noteField.setPrefWidth(350);

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPrefWidth(130);  // Đặt width cố định
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPrefWidth(130);

        // Tạo các Spinner với width nhỏ gọn hơn
        Spinner<Integer> startHourSpinner   = createTimeSpinner(0, 23, 0);
        Spinner<Integer> startMinuteSpinner = createTimeSpinner(0, 59, 0);
        Spinner<Integer> endHourSpinner     = createTimeSpinner(0, 23, 0);
        Spinner<Integer> endMinuteSpinner   = createTimeSpinner(0, 59, 0);
        
        // Đặt width cho spinner
        startHourSpinner.setPrefWidth(60);
        startMinuteSpinner.setPrefWidth(60);
        endHourSpinner.setPrefWidth(60);
        endMinuteSpinner.setPrefWidth(60);

        // Tạo HBox để chứa giờ và phút (không có khoảng trống)
        javafx.scene.layout.HBox startTimeBox = new javafx.scene.layout.HBox(2);  // spacing = 2
        startTimeBox.getChildren().addAll(
            startHourSpinner,
            new Label("giờ"),
            startMinuteSpinner,
            new Label("phút")
        );
        startTimeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.HBox endTimeBox = new javafx.scene.layout.HBox(2);  // spacing = 2
        endTimeBox.getChildren().addAll(
            endHourSpinner,
            new Label("giờ"),
            endMinuteSpinner,
            new Label("phút")
        );
        endTimeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "Công việc", "Sinh hoạt", "Học tập", "Thể thao", "Giải trí", "Khác"
        ));
        categoryBox.setValue("Công việc");
        categoryBox.setPrefWidth(150);

        CheckBox reminderCheck = new CheckBox("Bật nhắc nhở");
        Spinner<Integer> reminderSpinner = new Spinner<>(5, 120, 15, 5);
        reminderSpinner.setPrefWidth(70);
        reminderSpinner.setDisable(true);

        reminderCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            reminderSpinner.setDisable(!newVal);
        });

        if (existingSchedule != null) {
            titleField.setText(existingSchedule.getTitle());
            descField.setText(existingSchedule.getDescription());
            noteField.setText(existingSchedule.getNote());
            startDatePicker.setValue(existingSchedule.getStartTime().toLocalDate());
            startHourSpinner.getValueFactory().setValue(existingSchedule.getStartTime().getHour());
            startMinuteSpinner.getValueFactory().setValue(existingSchedule.getStartTime().getMinute());
            endDatePicker.setValue(existingSchedule.getEndTime().toLocalDate());
            endHourSpinner.getValueFactory().setValue(existingSchedule.getEndTime().getHour());
            endMinuteSpinner.getValueFactory().setValue(existingSchedule.getEndTime().getMinute());
            categoryBox.setValue(existingSchedule.getCategory());
            reminderCheck.setSelected(existingSchedule.isReminder());
            reminderSpinner.getValueFactory().setValue(existingSchedule.getReminderMinutes());
        } else {
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setValue(LocalDate.now());
        }

        // Layout gọn gàng hơn
        int row = 0;
        grid.add(new Label("Tiêu đề:"), 0, row);
        grid.add(titleField, 1, row, 2, 1);

        row++;
        grid.add(new Label("Mô tả:"), 0, row);
        grid.add(descField, 1, row, 2, 1);

        row++;
        grid.add(new Label("Ghi chú:"), 0, row);
        grid.add(noteField, 1, row, 2, 1);

        row++;
        grid.add(new Label("Bắt đầu:"), 0, row);
        grid.add(startDatePicker, 1, row);
        grid.add(startTimeBox, 2, row);

        row++;
        grid.add(new Label("Kết thúc:"), 0, row);
        grid.add(endDatePicker, 1, row);
        grid.add(endTimeBox, 2, row);

        row++;
        grid.add(new Label("Danh mục:"), 0, row);
        grid.add(categoryBox, 1, row);

        row++;
        javafx.scene.layout.HBox reminderBox = new javafx.scene.layout.HBox(5);
        reminderBox.getChildren().addAll(reminderCheck, reminderSpinner, new Label("phút trước"));
        reminderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.add(reminderBox, 0, row, 3, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    showAlert("Lỗi", "Tiêu đề không được để trống", Alert.AlertType.ERROR);
                    return null;
                }

                LocalDateTime startTime = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue())
                );

                LocalDateTime endTime = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue())
                );

                if (endTime.isBefore(startTime)) {
                    showAlert("Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu", Alert.AlertType.ERROR);
                    return null;
                }

                Schedule schedule;
                if (existingSchedule != null) {
                    schedule = existingSchedule;
                } else {
                    schedule = new Schedule();
                    schedule.setUserId(currentUserId);
                }

                schedule.setTitle(title);
                schedule.setDescription(descField.getText().trim());
                schedule.setNote(noteField.getText().trim());
                schedule.setStartTime(startTime);
                schedule.setEndTime(endTime);
                schedule.setCategory(categoryBox.getValue());
                schedule.setReminder(reminderCheck.isSelected());
                schedule.setReminderMinutes(reminderSpinner.getValue());

                return schedule;
            }
            return null;
        });

        Optional<Schedule> result = dialog.showAndWait();
        result.ifPresent(schedule -> {
            boolean success;
            if (existingSchedule != null) {
                success = scheduleService.updateSchedule(schedule);
            } else {
                success = scheduleService.addSchedule(schedule);
            }

            if (success) {
                loadSchedules();
                showAlert("Thành công",
                        existingSchedule != null ? "Đã cập nhật lịch" : "Đã thêm lịch mới",
                        Alert.AlertType.INFORMATION);
            } else {
                showAlert("Lỗi", "Không thể lưu lịch", Alert.AlertType.ERROR);
            }
        });
    }
}