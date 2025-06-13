module com.todolist {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.todolist to javafx.fxml;
    exports com.todolist;
}
