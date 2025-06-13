package com.todolist;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Tugas {
    private final IntegerProperty id;
    private final StringProperty deskripsi;
    private final ObjectProperty<Status> status;

    public Tugas(int id, String deskripsi, Status status) {
        this.id = new SimpleIntegerProperty(id);
        this.deskripsi = new SimpleStringProperty(deskripsi);
        this.status = new SimpleObjectProperty<>(status);
    }

    // --- Getters ---
    public int getId() { return id.get(); }
    public String getDeskripsi() { return deskripsi.get(); }
    public Status getStatus() { return status.get(); }

    // --- Setters ---
    public void setDeskripsi(String value) { deskripsi.set(value); }
    public void setStatus(Status value) { status.set(value); }
    
    // --- Property Getters (Penting untuk JavaFX TableView) ---
    public IntegerProperty idProperty() { return id; }
    public StringProperty deskripsiProperty() { return deskripsi; }
    public ObjectProperty<Status> statusProperty() { return status; }
}