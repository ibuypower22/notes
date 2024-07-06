package com.example.notes;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.web.HTMLEditor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;


public class Controller {
    @FXML
    private TextField titleField;
    @FXML
    private HTMLEditor noteEditor;
    @FXML
    private TextField usernameField;
    private String currentUsername;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TableView<Note> notesTableView;
    private ObservableList<Note> notesList;
    @FXML
    private CheckBox rememberMeCheckBox;
    @FXML
    private TextField searchField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private void searchNotes() {
        String searchText = searchField.getText().trim();
        LocalDate selectedDate = datePicker.getValue();

        if (!searchText.isEmpty() || selectedDate != null) {
            ObservableList<Note> filteredNotes = FXCollections.observableArrayList();

            for (Note note : notesList) {
                String plainText = extractPlainText(note.getText());
                String title = note.getTitle();
                boolean matchesText = searchText.isEmpty() || plainText.contains(searchText) || title.contains(searchText);
                boolean matchesDate = selectedDate == null || note.getDate().equals(selectedDate);

                if (matchesText && matchesDate) {
                    filteredNotes.add(note);
                }
            }

            notesTableView.setItems(filteredNotes);
        } else {
            notesTableView.setItems(notesList);
            notesTableView.refresh();
        }
    }

    private String extractPlainText(String html) {
        return html.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    @FXML
    private void undoSearch() {
        notesTableView.setItems(notesList);
        searchField.clear();
        datePicker.setValue(null);
        notesTableView.refresh();
    }

    private void setupPinColumn() {
        TableColumn<Note, Void> pinColumn = new TableColumn<>("Pin");
        pinColumn.setPrefWidth(60);
        pinColumn.setCellFactory(col -> new TableCell<Note, Void>() {
            private final Button pinButton = new Button();

            {
                pinButton.setOnAction(event -> {
                    Note note = getTableView().getItems().get(getIndex());
                    notesList.remove(note);
                    note.setPinned(!note.isPinned());

                    if (note.isPinned()) {
                        notesList.add(0, note);
                    } else {

                        int index = note.getOriginalIndex() >= notesList.size() ? notesList.size() : note.getOriginalIndex();
                        notesList.add(index, note);
                    }

                    try {
                        updateNoteInDatabase(note, note.getId());
                    } catch (SQLException e) {
                        e.printStackTrace();
                        showAlert("Error when updating the note status: " + e.getMessage());
                    }
                    sortAndUpdateView();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Note note = getTableView().getItems().get(getIndex());
                    pinButton.setText(note.isPinned() ? "Unpin" : "Pin");
                    setGraphic(pinButton);
                }
            }
        });
        notesTableView.getColumns().add(0, pinColumn);
    }
    private void sortAndUpdateView() {
        Collections.sort(notesList, (n1, n2) -> {
            if (n1.isPinned() && !n2.isPinned()) {
                return -1;
            } else if (!n1.isPinned() && n2.isPinned()) {
                return 1;
            } else {
                return Integer.compare(n1.getOriginalIndex(), n2.getOriginalIndex());
            }
        });
        notesTableView.refresh();
    }

    public void initialize(String username) {
        this.currentUsername = username;
        notesList = FXCollections.observableArrayList();
        notesTableView.setItems(notesList);
        notesTableView.getColumns().clear();

        setupPinColumn();

        TableColumn<Note, String> textColumn = new TableColumn<>("Title");
        textColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        textColumn.setPrefWidth(745);
        textColumn.setCellFactory(col -> new TableCell<>() {
            private final WebView webView = new WebView();

            {
                webView.setPrefWidth(700);
                webView.setPrefHeight(50);

                webView.getEngine().documentProperty().addListener((obs, oldDoc, newDoc) -> {
                    if (newDoc != null) {
                        webView.getEngine().executeScript(
                                "var style = document.createElement('style');"
                                        + "style.type = 'text/css';"
                                        + "style.innerHTML = 'body { overflow: hidden; }';"
                                        + "document.head.appendChild(style);"
                        );
                    }
                });

                setGraphic(webView);
            }

            @Override
            protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                if (empty || title == null) {
                    webView.getEngine().loadContent("");
                    setGraphic(null);
                } else {
                    Note note = getTableView().getItems().get(getIndex());
                    String displayContent = "<h2>" + title + "</h2>";
                    webView.getEngine().loadContent(displayContent);
                    setGraphic(webView);

                    webView.setOnMouseClicked(event -> {
                        HTMLEditor editorForWindow = new HTMLEditor();
                        editorForWindow.setHtmlText(note.getText());

                        Button chooseImageButton = new Button("Choose Image");
                        chooseImageButton.setOnAction(e -> chooseImage(editorForWindow));

                        TextField titleField = new TextField(note.getTitle());
                        titleField.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-text-fill: #333;");

                        VBox vbox = new VBox(new Label("Title:"), titleField, editorForWindow, chooseImageButton);
                        Scene scene = new Scene(vbox, 800, 600);
                        Stage editorStage = new Stage();
                        editorStage.setScene(scene);
                        editorStage.setTitle("Edit Note");
                        editorStage.show();

                        editorStage.setOnHiding(e -> {
                            String updatedTitle = titleField.getText().trim();
                            if (updatedTitle.isEmpty()) {
                                updatedTitle = extractTitle(editorForWindow.getHtmlText());
                            }
                            note.setTitle(updatedTitle);
                            note.setText(editorForWindow.getHtmlText());

                            try {
                                updateNoteInDatabase(note, note.getId());
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                showAlert("Error updating note in the database: " + ex.getMessage());
                            }
                            notesTableView.refresh();
                        });
                    });
                }
            }
        });

        notesTableView.getColumns().add(textColumn);

        TableColumn<Note, LocalDate> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        notesTableView.getColumns().add(dateColumn);

        for (TableColumn<?, ?> column : notesTableView.getColumns()) {
            column.sortTypeProperty().addListener((observable, oldValue, newValue) -> {
                notesTableView.refresh();
            });
        }

        try {
            int userId = getCurrentUserId();
            loadNotesFromDatabase(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error loading notes: " + e.getMessage());
        }
    }

    private String extractTitle(String text) {
        String plainText = text.replaceAll("<[^>]*>", "").trim();
        String[] words = plainText.split("\\s+");
        int wordCount = Math.min(words.length, 5);
        return String.join(" ", Arrays.copyOfRange(words, 0, wordCount));
    }

    public void openRegistrationWindow() throws IOException {
        Stage register = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Registration.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        register.setTitle("Registration");
        register.setScene(scene);
        register.show();
    }

    public void openNotesWindow(Stage stageToClose) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Notes.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 960);
        Stage notesStage = new Stage();
        notesStage.setTitle("Notes");
        notesStage.setScene(scene);

        Controller controller = fxmlLoader.getController();
        controller.currentUsername = this.currentUsername;
        controller.initialize(this.currentUsername);

        notesStage.show();
        if (stageToClose != null) {
            stageToClose.close();
        }
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            String imageHtml = "<img src='" + imagePath + "' width='100' height='100' />";
            insertImageAtCursor(noteEditor, imageHtml);
        }
    }

    @FXML
    private void chooseImage(HTMLEditor editor) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            String imagePath = selectedFile.toURI().toString();
            String imageHtml = "<img src='" + imagePath + "' width='100' height='100' />";
            insertImageAtCursor(editor, imageHtml);
        }
    }

    private void insertImageAtCursor(HTMLEditor editor, String html) {
        WebView webView = (WebView) editor.lookup(".web-view");
        WebEngine webEngine = webView.getEngine();

        String escapedHtml = html.replace("'", "\\'");

        Platform.runLater(() -> {
            webEngine.executeScript(
                    "document.execCommand('insertHTML', false, '" + escapedHtml + "');"
            );
        });
    }

    private boolean isNoteEmpty(String text) {
        String textContent = text.replaceAll("<(?!img\\b)[^>]*>", "").trim();
        return textContent.isEmpty();
    }

    @FXML
    private void addNote() {
        String title = titleField.getText().trim();
        String noteText = noteEditor.getHtmlText().trim();

        if (isNoteEmpty(noteText)) {
            showAlert("Note content cannot be empty.");
            return;
        }

        if (title.isEmpty()) {
            title = extractTitle(noteText);
        }

        Note newNote = new Note();
        newNote.setOriginalIndex(notesList.size());
        newNote.setText(noteText);  // сохраняем только текст заметки
        newNote.setTitle(title);    // заголовок сохраняется отдельно
        newNote.setDate(LocalDate.now());

        notesList.add(newNote);
        titleField.setText("");
        noteEditor.setHtmlText("");

        try {
            int userId = getCurrentUserId();
            saveNoteToDatabase(newNote, userId);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error saving note: " + e.getMessage());
        }
        notesTableView.refresh();
    }


    private List<Note> loadNotesFromDatabase(int userId) throws SQLException {
        String query = "SELECT id, text, date, pinned, original_index, title FROM notes WHERE user_id = ?";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Note note = new Note(
                            rs.getInt("id"),
                            rs.getString("text"),
                            rs.getDate("date").toLocalDate(),
                            rs.getBoolean("pinned"),
                            rs.getInt("original_index"),
                            rs.getString("title")
                    );
                    notes.add(note);
                }
            }
        }

        notes.sort((n1, n2) -> {
            if (n1.isPinned() && !n2.isPinned()) return -1;
            if (!n1.isPinned() && n2.isPinned()) return 1;
            return Integer.compare(n1.getOriginalIndex(), n2.getOriginalIndex());
        });

        notesList.setAll(notes);
        return notes;
    }

    @FXML
    private void saveNoteToDatabase(Note note, int userId) throws SQLException {
        String query = "INSERT INTO notes (user_id, text, date, pinned, original_index, title) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, note.getText());
            stmt.setDate(3, java.sql.Date.valueOf(note.getDate()));
            stmt.setBoolean(4, note.isPinned());
            stmt.setInt(5, note.getOriginalIndex());
            stmt.setString(6, note.getTitle());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    private void updateNoteInDatabase(Note note, int noteId) throws SQLException {
        String query = "UPDATE notes SET text = ?, date = ?, pinned = ?, original_index = ?, title = ? WHERE id = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, note.getText());
            stmt.setDate(2, java.sql.Date.valueOf(note.getDate()));
            stmt.setBoolean(3, note.isPinned());
            stmt.setInt(4, note.getOriginalIndex());
            stmt.setString(5, note.getTitle());
            stmt.setInt(6, noteId);
            stmt.executeUpdate();
        }
    }

    @FXML
    private void deleteNote() {
        Note selectedNote = notesTableView.getSelectionModel().getSelectedItem();

        if (selectedNote != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Delete Note");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Are you sure you want to delete this note?");
            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    deleteNoteFromDatabase(selectedNote.getId());
                    notesList.remove(selectedNote);
                    notesTableView.refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error deleting note from the database: " + e.getMessage());
                }
            }
        } else {
            showAlert("No note selected for deletion");
        }
    }

    private void deleteNoteFromDatabase(int noteId) throws SQLException {
        String query = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, noteId);
            stmt.executeUpdate();
        }
    }

    @FXML
    public void registerUser() throws SQLException {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("All fields must be filled");
            return;
        }

        if (username.length() > 24 || password.length() > 24) {
            showAlert("Username and password must be at most 24 characters long");
            return;
        }

        if (password.length() < 8) {
            showAlert("Password must be at least 8 characters long");
            return;
        }

        if (!isValidString(username) || !isValidString(password)) {
            showAlert("Username and password must contain only English letters, digits, and special characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Passwords do not match");
            return;
        }

        if (isUsernameTaken(username)) {
            showAlert("User with this name already exists");
            return;
        }
        registerUser(username, password);
        showAlert("Registration successful");
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    private boolean isValidString(String str) {
        return str.matches("^[a-zA-Z0-9!@#$%&*?+-]+$");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void registerUser(String username, String password) throws SQLException {
        String hashedPassword = hashPassword(password);
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();
        }
    }

    private boolean isUsernameTaken(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private User authenticateUser(String username, String hashedPassword) throws SQLException {
        String query = "SELECT id, username FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("username"), hashedPassword);
                }
            }
        }
        return null;
    }

    @FXML
    public void loginValidate(ActionEvent event) throws IOException, SQLException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("All fields must be filled");
            return;
        }

        if (!isValidString(username) || !isValidString(password)) {
            showAlert("Username and password must contain only English letters, digits, and special characters");
            return;
        }

        String hashedPassword = hashPassword(password);
        User authenticatedUser = authenticateUser(username, hashedPassword);

        if (authenticatedUser == null) {
            showAlert("Invalid username or password");
            return;
        }

        boolean rememberMe = rememberMeCheckBox.isSelected();
        updateLoginState(username, rememberMe);

        currentUsername = authenticatedUser.getUsername();
        openNotesWindow((Stage) ((Node) event.getSource()).getScene().getWindow());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

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

    public boolean checkLoginState(Stage primaryStage) throws SQLException, IOException {
        String query = "SELECT id, username, password FROM users WHERE remember_me = true LIMIT 1";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String username = rs.getString("username");
                String hashedPassword = rs.getString("password");

                User user = authenticateUser(username, hashedPassword);
                if (user != null) {
                    currentUsername = username;
                    System.out.println("Auto-login successful for user: " + username);
                    openNotesWindow(primaryStage);
                    return true;
                } else {
                    System.out.println("Auto-login failed for user: " + username);
                }
            }
        }
        return false;
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException, SQLException {
        updateLoginState(currentUsername, false);
        Stage notesStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        notesStage.close();
        openLoginWindow();
    }

    private void updateLoginState(String username, boolean rememberMe) throws SQLException {
        String query = "UPDATE users SET remember_me = ? WHERE username = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, rememberMe);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    public void openLoginWindow() throws IOException {
        Stage loginStage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        loginStage.setTitle("Login");
        loginStage.setScene(scene);
        loginStage.show();
    }

    private Connection getDatabaseConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost/notes";
        String username = "postgres";
        String password = "Prk270830";
        return DriverManager.getConnection(url, username, password);
    }

    private int getCurrentUserId() throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentUsername);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
}
