package com.example.notes;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class HelloController {
    private List<User> users;
    private final ObjectMapper objectMapper;
    @FXML
    private TextArea noteArea;
    @FXML
    private TextField usernameField;
    private String currentUsername;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ListView<String> notesListView;
    private ObservableList<String> notesList;

    public HelloController() {
        this.users = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
    }

    public void initializeNotesList(String username) {
        this.currentUsername = username;
        this.notesList = FXCollections.observableArrayList();
        this.notesListView.setItems(notesList);

        if (currentUsername != null) {
            // Завантажуємо нотатки з файла при ініціалізації
            loadNotesFromFile();
        } else {
            showAlert("Current Username is null");
        }
    }

    public void openRegistrationWindow() throws IOException {
        Stage register = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Registration.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        register.setTitle("Registration");
        register.setScene(scene);
        register.show();
    }

    public void openNotesWindow(ActionEvent event) throws IOException {
        Stage notes = new Stage();
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Notes.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        notes.setTitle("Notes");
        notes.setScene(scene);

        // Передаємо юзернейм поточного користувача у вікно з нотатками
        HelloController controller = fxmlLoader.getController();
        controller.currentUsername = usernameField.getText();
        controller.initializeNotesList(controller.currentUsername);

        currentStage.close();
        notes.show();
    }

    @FXML
    private void addNote() {
        String noteText = noteArea.getText().trim();
        if (!noteText.isEmpty()) {
            notesList.add(noteText);
            saveNotesToFile();
            noteArea.clear();
        }
    }

    private void loadNotesFromFile() {
        try {
            File file = new File(getNotesFileName(currentUsername));
            if (file.exists() && file.length() > 0) {
                // Якщо файл є і не порожній, завантажуємо нотатки з нього
                List<String> loadedNotes = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                notesList.clear();
                notesList.addAll(loadedNotes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error loading notes from file: " + e.getMessage());
        }
    }
    private String getNotesFileName(String currentUsername) {
        return currentUsername + ".json";
    }

    @FXML
    private void saveNotesToFile() {
        if (currentUsername != null) {
            try {
                File file = new File(getNotesFileName(currentUsername));
                objectMapper.writeValue(file, notesList);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error saving notes to file: " + e.getMessage());
            }
        } else {
            showAlert("Current username is null. Unable to save notes.");
        }
    }

    @FXML
    private void deleteNote() {
        int selectedIndex = notesListView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            notesList.remove(selectedIndex);
        } else {
            showAlert("No note selected for deletion");
        }
    }

    @FXML
    private void cellEdit() {
        notesListView.setEditable(true);
        notesListView.setCellFactory(TextFieldListCell.forListView());
        notesListView.setOnEditCommit(event -> {
            notesList.set(event.getIndex(), event.getNewValue());
            saveNotesToFile();
        });
    }

    @FXML
    private void editNote(MouseEvent event) {
        if (event.getClickCount() == 2) {
            cellEdit();
        }
    }

    @FXML
    public void registerUser() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Перевірка на порожні поля
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("All fields must be filled");
            return;
        }

        // Перевірка на довжину логіна та пароля
        if (username.length() > 24 || password.length() > 24) {
            showAlert("Username and password must be at most 24 characters long");
            return;
        }
        // Перевірка на довжину пароля в мінімум 8 символів
        if (password.length() < 8) {
            showAlert("Password must be at least 8 characters long");
            return;
        }

        // Перевірка символів на англійські букви
        if (!isValidString(username) || !isValidString(password)) {
            showAlert("Username and password must contain only English letters, digits, and special characters");
            return;
        }

        // Перевірка на відповідність паролів
        if (!password.equals(confirmPassword)) {
            showAlert("Passwords do not match");
            return;
        }

        // Якщо користувач з такім логіном вже є видати помилку
        if (isUsernameTaken(username)) {
            showAlert("User with this name already exists");
            return;
        }
        try {
            registerUser(username, password);
            showAlert("Registration successful");
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.close();
        } catch (IOException e) {
            showAlert("Error registering user");
        }
    }

    private boolean isValidString(String str) {
        // Перевірка, чи рядок містить тільки англійські літери, цифри та спеціальні символи
        return str.matches("^[a-zA-Z0-9!@#$%&*?+-]+$");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void registerUser(String username, String password) throws IOException {
        String hashedPassword = hashPassword(password);
        User newUser = new User(username, hashedPassword);
        users.add(newUser);
        saveUsersToJsonFile();
    }

    private boolean isUsernameTaken(String username) {
        return loadUsersFromJsonFile().stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    private void saveUsersToJsonFile() {
        try {
            File file = new File("users.json");
            List<User> existingUsers = loadUsersFromJsonFile();

            // Додаємо нових користувачів якщо їх ще немає в файлі з усіма користувачами
            for (User newUser : users) {
                if (!existingUsers.contains(newUser)) {
                    existingUsers.add(newUser);
                }
            }

            objectMapper.writeValue(file, existingUsers);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error saving users to file: " + e.getMessage());
        }
    }

    private List<User> loadUsersFromJsonFile() {
        try {
            File file = new File("users.json");

            if (file.exists()) {
                return objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error loading users from file: ");
        }
        return new ArrayList<>();
    }

    private User authenticateUser(String username, String password) {
        this.users = loadUsersFromJsonFile();

        return users.stream()
                .filter(user -> user.getUsername().equals(username) && checkPassword(password, user.getPassword()))
                .findFirst()
                .orElse(null);
    }

    public void loginValidate(ActionEvent event) throws IOException {
        this.users = loadUsersFromJsonFile();
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Перевірка на порожні поля
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("All fields must be filled");
            return;
        }

        // Перевірка символів на англійські букви
        if (!isValidString(username) || !isValidString(password)) {
            showAlert("Username and password must contain only English letters, digits, and special characters");
            return;
        }

        // Перевірка, чи існує користувач + чи дані введені правильно
        User authenticatedUser = authenticateUser(username, password);
        if (authenticatedUser == null) {
            showAlert("Invalid username or password");
            return;
        }

        currentUsername = authenticatedUser.getUsername();

        // Створюємо файл з нотатками при першому вході користувача
        File notesFile = new File(getNotesFileName(currentUsername));
        if (!notesFile.exists()) {
            notesFile.createNewFile();
        }
        openNotesWindow(event);
    }

    // Хешування
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Перетворення масиву байтів в рядок
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            showAlert("Error in hashing");
            return null;
        }
    }

    private boolean checkPassword(String inputPassword, String hashedPassword) {
        String hashedInputPassword = hashPassword(inputPassword);
        return hashedInputPassword.equals(hashedPassword);
    }
}