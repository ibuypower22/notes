module com.example.notes {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires com.fasterxml.jackson.databind;

    opens com.example.notes to javafx.fxml;
    exports com.example.notes;
}