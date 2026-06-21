module com.tuiperacer {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.tuiperacer to javafx.fxml;
    opens com.tuiperacer.view to javafx.fxml;
    opens com.tuiperacer.model to javafx.fxml;
    exports com.tuiperacer;
    exports com.tuiperacer.model;
    exports com.tuiperacer.interfaces;
    exports com.tuiperacer.service;
    exports com.tuiperacer.persistence;
    exports com.tuiperacer.exception;
    exports com.tuiperacer.network;
    exports com.tuiperacer.view;
}
