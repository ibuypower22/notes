module com.example.notes {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.sql;

    opens com.example.notes to javafx.fxml;
    exports com.example.notes;
}