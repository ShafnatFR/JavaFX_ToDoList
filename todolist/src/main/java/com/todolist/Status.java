package com.todolist;

public enum Status {
    BELUM_SELESAI,
    SELESAI;

    // Override agar tampilannya lebih bagus di tabel
    @Override
    public String toString() {
        return this == SELESAI ? "Selesai" : "Belum Selesai";
    }
}