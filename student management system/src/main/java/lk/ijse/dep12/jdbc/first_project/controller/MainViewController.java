package lk.ijse.dep12.jdbc.first_project.controller;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lk.ijse.dep12.jdbc.first_project.db.SingletonConnection;
import lk.ijse.dep12.jdbc.first_project.to.Student;

import java.sql.*;


public class MainViewController {

    public Button btnDelete;

    public Button btnNewStudent;

    public Button btnSave;

    public TableView<Student> tblDisplay;

    public TextField txtAddress;

    public TextField txtContact;

    public TextField txtId;

    public TextField txtName;

    public void initialize() {

        btnDelete.setDisable(true);
        tblDisplay.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("id"));
        tblDisplay.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("name"));
        tblDisplay.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("address"));
        tblDisplay.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("contact"));

        tblDisplay.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loadData();


        tblDisplay.getSelectionModel().selectedItemProperty().addListener((ob, pre, cur) -> {
            btnDelete.setDisable(cur == null);
            btnSave.setDisable(cur != null);
        });

        tblDisplay.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super Student>) change -> {
            if (tblDisplay.getSelectionModel().getSelectedItems().size() > 1) {
                for (TextField txt : new TextField[]{txtId, txtName, txtAddress, txtContact}) {
                    txt.setText("MULTIPLE SELECTION - (%d) SELECTED".formatted(
                            tblDisplay.getSelectionModel().getSelectedItems().size()
                    ));
                }
            } else if (tblDisplay.getSelectionModel().getSelectedItems().size() == 1) {
                Student selectedStudent = tblDisplay.getSelectionModel().getSelectedItem();
                txtId.setText(selectedStudent.getId());
                txtName.setText(selectedStudent.getName());
                txtAddress.setText(selectedStudent.getAddress());
                txtContact.setText(selectedStudent.getContact());
            } else {
                for (TextField txt : new TextField[]{txtId, txtName, txtAddress, txtContact}) txt.clear();
                txtId.setText("GENERATED ID");
            }
        });

    }

    private void loadData() {

        try {
            Connection connection = SingletonConnection.getInstance().getConnection();
            Statement stm = connection.createStatement();
            ResultSet re = stm.executeQuery("TABLE student");
            ObservableList<Student> studentObservableList = tblDisplay.getItems();

            while (re.next()) {
                int id = re.getInt(1);
                String name = re.getString("name");
                String address = re.getString("address");
                String contact = re.getString("contact");
                Student student1 = new Student(formattedId(id), name, address, contact);
                studentObservableList.add(student1);

            }

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to add student detail. Try again").show();
            e.printStackTrace();
        }
    }


    private String formattedId(int id) {
        return "S-%03d".formatted(id);
    }


    public void btnAddNewStudent(ActionEvent event) {
        for (TextField textField : new TextField[]{txtId, txtName, txtAddress, txtContact}) textField.clear();
        txtName.requestFocus();
        txtId.setText("GENERATE ID");

    }

    public void btnDeleteOnaction(ActionEvent event) {
        try  {
            Connection connection = SingletonConnection.getInstance().getConnection();
            System.out.println(connection);
            PreparedStatement stm = connection.prepareStatement("DELETE FROM student WHERE id=?");

            ObservableList<Student> selectedStudents = tblDisplay.getSelectionModel().getSelectedItems();
            connection.setAutoCommit(false);
            try {
                for (Student student : selectedStudents) {
                    stm.setInt(1, Integer.parseInt(student.getId().replace("S-", "")));
                    //  stm.executeUpdate();
                    stm.addBatch();
                }
                stm.executeBatch();
                ObservableList<Student> updateStudentTable = tblDisplay.getItems();
                updateStudentTable.removeAll(selectedStudents);

            } catch (Throwable e) {
                connection.rollback();
                new Alert(Alert.AlertType.ERROR, " Failed to delete student").show();

                e.printStackTrace();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Something Went Wrong").show();
            e.printStackTrace();

        }

    }

    public void btnSaveOnaction(ActionEvent event) {

        if (!validationData()) return;

        String name = txtName.getText().strip();
        String address = txtAddress.getText().strip();
        String contact = txtContact.getText().strip();

        try {
            Connection connection = SingletonConnection.getInstance().getConnection();

            PreparedStatement stm = connection.prepareStatement("""
                    INSERT INTO student (name, address,contact) VALUES (?,?,?)""", Statement.RETURN_GENERATED_KEYS);
            stm.setString(1, name);
            stm.setString(2, address);
            stm.setString(3, contact.isEmpty() ? null : contact);

            stm.executeUpdate();
            ResultSet generatedKeys = stm.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt("id");

            ObservableList<Student> studentList = tblDisplay.getItems();
            studentList.add(new Student(formattedId(id), name, address, contact));
            btnNewStudent.fire();


        } catch (SQLException e) {

            //int errorcode=e.getErrorCode();

            //String sqlState=e.getSQLState();

            //This is not a best practice

            if (e.getSQLState().equals("23505")) {
                new Alert(Alert.AlertType.ERROR, "contact number already exist").show();
                txtContact.requestFocus();
                txtContact.selectAll();
                return;
            }


            e.printStackTrace();
        }
    }

    public void tblOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) btnDelete.fire();
    }

    public void tblOnKeyReleased(KeyEvent event) {

    }

    private boolean validationData() {
        String name = txtName.getText().strip();
        if (!name.matches("[A-Za-z]{3,}")) {
            txtName.requestFocus();
            txtName.selectAll();
            return false;
        }
        String address = txtAddress.getText().strip();
        if (address.length() < 3) {
            txtAddress.requestFocus();
            txtAddress.selectAll();
            return false;
        }

        String contact = txtContact.getText().strip();
        if (!contact.matches("0[1-9]{2}-\\d{7}")) {
            txtContact.requestFocus();
            txtContact.selectAll();
            return false;
        }
        return true;
    }
}
