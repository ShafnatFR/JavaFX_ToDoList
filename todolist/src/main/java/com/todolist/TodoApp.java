package com.todolist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TodoApp extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/todolist";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private final TableView<Tugas> tableView = new TableView<>();
    private final ObservableList<Tugas> daftarTugasObservable = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Aplikasi To-Do List");

        // --- Konfigurasi Tabel ---
        TableColumn<Tugas, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Tugas, String> descCol = new TableColumn<>("Deskripsi Tugas");
        descCol.setCellValueFactory(new PropertyValueFactory<>("deskripsi"));
        descCol.setPrefWidth(250);
        TableColumn<Tugas, Status> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableView.getColumns().addAll(idCol, descCol, statusCol);

        // --- Form Tambah Tugas ---
        HBox formTambah = new HBox(10);
        TextField inputDeskripsi = new TextField();
        inputDeskripsi.setPromptText("Masukkan deskripsi tugas baru");
        inputDeskripsi.setPrefWidth(300);
        Button btnTambah = new Button("Tambah");

        btnTambah.setOnAction(e -> {
            String deskripsi = inputDeskripsi.getText();
            if (!deskripsi.trim().isEmpty()) {
                tambahTugasKeDb(deskripsi);
                refreshTabelTugas();
                inputDeskripsi.clear();
            }
        });
        formTambah.getChildren().addAll(inputDeskripsi, btnTambah);

        // --- Tombol Aksi (Selesai, Edit, Hapus) ---
        HBox areaAksi = new HBox(10);
        Button btnSelesai = new Button("Tandai Selesai");
        Button btnEdit = new Button("Edit Tugas");
        Button btnHapus = new Button("Hapus Tugas");

        // Aksi untuk menandai tugas sebagai selesai
        btnSelesai.setOnAction(e -> {
            Tugas tugasTerpilih = tableView.getSelectionModel().getSelectedItem();
            if (tugasTerpilih != null) {
                updateStatusDiDb(tugasTerpilih.getId(), Status.SELESAI);
                refreshTabelTugas();
            } else {
                 showAlertError("Tidak Ada Tugas Terpilih", "Silakan pilih tugas yang ingin ditandai selesai.");
            }
        });

        // ### LOGIKA EDIT BARU DITAMBAHKAN DI SINI ###
        btnEdit.setOnAction(e -> {
            Tugas tugasTerpilih = tableView.getSelectionModel().getSelectedItem();
            if (tugasTerpilih != null) {
                // 1. Tampilkan dialog untuk input teks baru
                TextInputDialog dialog = new TextInputDialog(tugasTerpilih.getDeskripsi());
                dialog.setTitle("Edit Tugas");
                dialog.setHeaderText("Mengedit tugas: " + tugasTerpilih.getDeskripsi());
                dialog.setContentText("Masukkan deskripsi baru:");

                // 2. Dapatkan hasil dari dialog
                Optional<String> result = dialog.showAndWait();

                // 3. Jika pengguna menekan OK dan deskripsi tidak kosong
                result.ifPresent(deskripsiBaru -> {
                    if (!deskripsiBaru.trim().isEmpty()) {
                        // 4. Update ke database
                        editTugasDiDb(tugasTerpilih.getId(), deskripsiBaru);
                        // 5. Refresh tabel
                        refreshTabelTugas();
                    }
                });
            } else {
                 showAlertError("Tidak Ada Tugas Terpilih", "Silakan pilih tugas yang ingin diedit.");
            }
        });


        // Aksi untuk menghapus tugas
        btnHapus.setOnAction(e -> {
            Tugas tugasTerpilih = tableView.getSelectionModel().getSelectedItem();
            if (tugasTerpilih != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Konfirmasi Hapus");
                alert.setHeaderText("Hapus tugas: " + tugasTerpilih.getDeskripsi());
                alert.setContentText("Apakah Anda yakin?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    hapusTugasDariDb(tugasTerpilih.getId());
                    refreshTabelTugas();
                }
            } else {
                 showAlertError("Tidak Ada Tugas Terpilih", "Silakan pilih tugas yang ingin dihapus.");
            }
        });

        // ### MENAMBAHKAN btnEdit KE LAYOUT ###
        areaAksi.getChildren().addAll(btnSelesai, btnEdit, btnHapus);

        // --- Layout Utama ---
        VBox layoutUtama = new VBox(15);
        layoutUtama.setPadding(new Insets(15));
        layoutUtama.getChildren().addAll(new Label("Daftar Tugas Anda"), tableView, formTambah, areaAksi);

        Scene scene = new Scene(layoutUtama, 500, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Muat data saat aplikasi pertama kali berjalan
        refreshTabelTugas();
    }

    // --- METODE DATABASE ---

    private void refreshTabelTugas() {
        try {
            List<Tugas> daftarTugas = ambilSemuaTugasDariDb();
            daftarTugasObservable.setAll(daftarTugas);
            tableView.setItems(daftarTugasObservable);
        } catch (SQLException e) {
            showAlertError("Gagal Memuat Data", "Tidak dapat mengambil data dari database: " + e.getMessage());
        }
    }

    private List<Tugas> ambilSemuaTugasDariDb() throws SQLException {
        String sql = "SELECT id_tugas, deskripsi_tugas, status_tugas FROM tb_tugas ORDER BY id_tugas";
        List<Tugas> daftarTugas = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_tugas");
                String deskripsi = rs.getString("deskripsi_tugas");
                Status status = rs.getInt("status_tugas") == 1 ? Status.SELESAI : Status.BELUM_SELESAI;
                daftarTugas.add(new Tugas(id, deskripsi, status));
            }
        }
        return daftarTugas;
    }

    private void tambahTugasKeDb(String deskripsi) {
        String sql = "INSERT INTO tb_tugas(deskripsi_tugas, status_tugas) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deskripsi);
            pstmt.setInt(2, 0); // 0 untuk BELUM_SELESAI
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlertError("Gagal Menambah Tugas", "Error: " + e.getMessage());
        }
    }
    
    // ### METODE EDIT YANG SUDAH DIPERBAIKI ###
    private void editTugasDiDb(int id, String deskripsiBaru) {
        // Query SQL yang benar untuk UPDATE
        String sql = "UPDATE tb_tugas SET deskripsi_tugas = ? WHERE id_tugas = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set parameter: deskripsi baru, lalu id tugas yang akan diupdate
            pstmt.setString(1, deskripsiBaru);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlertError("Gagal Mengupdate Tugas", "Error: " + e.getMessage());
        }
    }


    private void updateStatusDiDb(int id, Status newStatus) {
        String sql = "UPDATE tb_tugas SET status_tugas = ? WHERE id_tugas = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStatus == Status.SELESAI ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlertError("Gagal Update Status", "Error: " + e.getMessage());
        }
    }

    private void hapusTugasDariDb(int id) {
        String sql = "DELETE FROM tb_tugas WHERE id_tugas = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlertError("Gagal Menghapus Tugas", "Error: " + e.getMessage());
        }
    }

    private void showAlertError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
    
    