package com.example.notes;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.util.StringConverter;

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
    private ComboBox<String> fontFamilyComboBox;
    @FXML
    private ComboBox<Double> fontSizeComboBox;
    @FXML
    private CheckBox boldCheckBox;
    @FXML
    private CheckBox italicCheckBox;
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
    private TableView<Note> notesTableView;
    private ObservableList<Note> notesList;
    @FXML
    private CheckBox rememberMeCheckBox;
    @FXML
    private TextField searchField;
    @FXML
    private void searchNotes() {
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            ObservableList<Note> filteredNotes = FXCollections.observableArrayList();

            for (Note note : notesList) {
                if (note.getText().contains(searchText)) {
                    filteredNotes.add(note);
                }
            }

            notesTableView.setItems(filteredNotes);
        } else {
            notesTableView.setItems(notesList);
            notesTableView.refresh();
        }
    }
    @FXML
    private void undoSearch() {
        notesTableView.setItems(notesList);
        searchField.clear();
        notesTableView.refresh();
    }

    private void setupPinColumn() {
        TableColumn<Note, Void> pinColumn = new TableColumn<>("Pin");
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
    private void updateNoteInDatabase(Note note, int noteId) throws SQLException {
        String query = "UPDATE notes SET text = ?, date = ?, pinned = ?, font_family = ?, font_size = ?, bold = ?, italic = ?, image_path = ?, original_index = ? WHERE id = ?";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, note.getText());
            stmt.setDate(2, java.sql.Date.valueOf(note.getDate()));
            stmt.setBoolean(3, note.isPinned());
            stmt.setString(4, note.getFontFamily());
            stmt.setDouble(5, note.getFontSize());
            stmt.setBoolean(6, note.isBold());
            stmt.setBoolean(7, note.isItalic());
            stmt.setString(8, note.getImagePath());
            stmt.setInt(9, note.getOriginalIndex());
            stmt.setInt(10, noteId);
            stmt.executeUpdate();
        }
    }

    public void initialize(String username) {
        this.currentUsername = username;
        notesList = FXCollections.observableArrayList();
        notesTableView.setItems(notesList);
        notesTableView.getColumns().clear();

        fontSizeComboBox.setValue(12.0);
        fontFamilyComboBox.setValue(Font.getDefault().getFamily());

        fontFamilyComboBox.getItems().addAll(Font.getFamilies());
        fontSizeComboBox.getItems().addAll(8.0, 9.0, 10.0, 11.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0, 26.0, 28.0, 36.0, 48.0, 72.0);

        boldCheckBox.setOnAction(event -> updateNoteFont());
        italicCheckBox.setOnAction(event -> updateNoteFont());
        fontSizeComboBox.setOnAction(event -> updateNoteFont());
        fontFamilyComboBox.setOnAction(event -> updateNoteFont());

        setupPinColumn();

        TableColumn<Note, String> textColumn = new TableColumn<>("Text");
        textColumn.setCellValueFactory(new PropertyValueFactory<>("text"));
        textColumn.setPrefWidth(320);
        textColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                } else {
                    Note note = getTableView().getItems().get(getIndex());
                    FontWeight weight = note.isBold() ? FontWeight.BOLD : FontWeight.NORMAL;
                    FontPosture posture = note.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR;
                    Font font = Font.font(note.getFontFamily(), weight, posture, note.getFontSize());
                    setFont(font);
                    setText(text);

                }
            }
        });

        notesTableView.getColumns().add(textColumn);

        TableColumn<Note, LocalDate> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        notesTableView.getColumns().add(dateColumn);

        TableColumn<Note, String> imageColumn = new TableColumn<>("Image");
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        imageColumn.setCellFactory(column -> new ImageTableCell());
        notesTableView.getColumns().add(imageColumn);

        try {
            int userId = getCurrentUserId();
            loadNotesFromDatabase(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error loading notes: " + e.getMessage());
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

    public void openNotesWindow(Stage stageToClose) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Notes.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
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
    private File selectedFile;
    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            this.selectedFile = selectedFile;
        }
    }

    @FXML
    private void addNote() {
        String noteText = noteArea.getText().trim();

        Note newNote = new Note();
        newNote.setOriginalIndex(notesList.size());
        if (!noteText.isEmpty() || selectedFile != null) {
            if (selectedFile != null) {
                String imagePath = selectedFile.getAbsolutePath();
                newNote.setImagePath(imagePath);
                selectedFile = null;
            }

            if (!noteText.isEmpty()) {
                newNote.setText(noteText);
                Font currentFont = noteArea.getFont();
                String fontFamily = currentFont.getFamily();
                double fontSize = currentFont.getSize();
                boolean bold = currentFont.getStyle().contains("Bold");
                boolean italic = currentFont.getStyle().contains("Italic");
                newNote.setFontFamily(fontFamily);
                newNote.setFontSize(fontSize);
                newNote.setBold(bold);
                newNote.setItalic(italic);
            }

            newNote.setDate(LocalDate.now());
            notesList.add(newNote);
            noteArea.clear();

            try {
                int userId = getCurrentUserId();
                saveNoteToDatabase(newNote, userId);
                noteArea.clear();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error saving note: " + e.getMessage());
            }
        }
    }
    private List<Note> loadNotesFromDatabase(int userId) throws SQLException {
        String query = "SELECT id, text, date, pinned, font_family, font_size, bold, italic, image_path, original_index FROM notes WHERE user_id = ?";
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
                            rs.getString("font_family"),
                            rs.getDouble("font_size"),
                            rs.getBoolean("bold"),
                            rs.getBoolean("italic"),
                            rs.getString("image_path"),
                            rs.getInt("original_index")
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
        String query = "INSERT INTO notes (user_id, text, date, pinned, font_family, font_size, bold, italic, image_path, original_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getDatabaseConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, note.getText());
            stmt.setDate(3, java.sql.Date.valueOf(note.getDate()));
            stmt.setBoolean(4, note.isPinned());
            stmt.setString(5, note.getFontFamily());
            stmt.setDouble(6, note.getFontSize());
            stmt.setBoolean(7, note.isBold());
            stmt.setBoolean(8, note.isItalic());
            stmt.setString(9, note.getImagePath());
            stmt.setInt(10, note.getOriginalIndex());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setId(generatedKeys.getInt(1));
                }
            }
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
    private void editNote(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Note selectedNote = notesTableView.getSelectionModel().getSelectedItem();
            if (selectedNote != null) {
                cellEdit(selectedNote);
            }
        }
    }

    @FXML
    private void cellEdit(Note selectedNote) {
        notesTableView.setEditable(true);

        TableColumn<Note, String> textColumn = (TableColumn<Note, String>) notesTableView.getColumns().get(1);
        textColumn.setCellFactory(col -> new TextFieldTableCell<Note, String>(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        }) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(null);
                }

                Note note = getTableRow().getItem();
                if (note != null) {
                    setFont(Font.font(note.getFontFamily(), note.isBold() ? FontWeight.BOLD : FontWeight.NORMAL, note.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR, note.getFontSize()));
                }
            }
        });

        textColumn.setOnEditCommit(event -> {
            Note note = event.getRowValue();
            note.setText(event.getNewValue());

            try {
                updateNoteInDatabase(note, note.getId());
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error updating note in the database: " + e.getMessage());
            }
        });
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

    private void updateNoteFont() {
        String fontFamily = fontFamilyComboBox.getValue();
        Double fontSize = fontSizeComboBox.getValue();

        FontWeight fontWeight = boldCheckBox.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture fontPosture = italicCheckBox.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR;

        Font font = Font.font(fontFamily, fontWeight, fontPosture, fontSize);

        noteArea.setFont(font);
    }
    private Connection getDatabaseConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/notes";
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

class ImageTableCell extends TableCell<Note, String> {

    private final ImageView imageView = new ImageView();
    private final double imageSize = 100;

    public ImageTableCell() {
        imageView.setFitWidth(imageSize);
        imageView.setFitHeight(imageSize);
    }

    @Override
    protected void updateItem(String imagePath, boolean empty) {
        super.updateItem(imagePath, empty);

        if (empty || imagePath == null) {
            setGraphic(null);
        } else {
            Image image = new Image("file:" + imagePath);
            imageView.setImage(image);
            setGraphic(imageView);
        }
    }

}
