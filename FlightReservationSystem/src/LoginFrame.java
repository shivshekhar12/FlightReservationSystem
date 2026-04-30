import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private JTextField textFieldUsername;
    private JTextField textFieldPassword;
    private JLabel labelMessage;

    public LoginFrame() {
        buildUI();
    }

    private void buildUI() {
        JLabel labelUser = new JLabel("Username:");
        textFieldUsername = new JTextField();

        JLabel labelPass = new JLabel("Password:");
        textFieldPassword = new JPasswordField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.add(labelUser);
        inputPanel.add(textFieldUsername);
        inputPanel.add(labelPass);
        inputPanel.add(textFieldPassword);

        labelMessage = new JLabel(" ");

        JPanel buttonPanel = getJPanel();

        JPanel main = new JPanel(new BorderLayout());
        main.add(inputPanel, BorderLayout.NORTH);
        main.add(labelMessage, BorderLayout.CENTER);
        main.add(buttonPanel, BorderLayout.SOUTH);

        this.add(main);
        this.setTitle("Flight Reservation System - Login");
        this.setSize(520, 220);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private JPanel getJPanel() {
        JButton buttonLogin = new JButton("Login");
        JButton buttonRegister = new JButton("Register as Customer");

        buttonLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        buttonRegister.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRegister();
            }
        });
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(buttonLogin);
        buttonPanel.add(buttonRegister);
        return buttonPanel;
    }

    private void doLogin() {
        String username = textFieldUsername.getText().trim();
        String password = textFieldPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            labelMessage.setText("Please enter both username and password.");
            return;
        }

        try {
            String query = "SELECT emp_id, role, name FROM Employee " + "WHERE username='" + username + "' AND password_hash='" + password + "'";
            ResultSet resultSet = DBConnection.getStatement().executeQuery(query);
            if (resultSet.next()) {
                int empId = resultSet.getInt("emp_id");
                String role = resultSet.getString("role");
                String name = resultSet.getString("name");
                this.dispose();
                if (role.equals("admin")) {
                    new AdminFrame(empId, name);
                } 
                else {
                    new RepFrame(empId, name);
                }
                return;
            }
        } 
        catch (SQLException ex) {
            labelMessage.setText("DB error: " + ex.getMessage());
            return;
        }

        try {
            String query = "SELECT cust_id, name FROM Customer " + "WHERE username='" + username + "' AND password_hash='" + password + "'";
            ResultSet resultSet = DBConnection.getStatement().executeQuery(query);
            if (resultSet.next()) {
                int custId = resultSet.getInt("cust_id");
                String name = resultSet.getString("name");
                this.dispose();
                new CustomerFrame(custId, name);
                return;
            }
        } 
        catch (SQLException ex) {
            labelMessage.setText("DB error: " + ex.getMessage());
            return;
        }

        labelMessage.setText("Invalid username or password.");
    }

    private void doRegister() {
        JTextField textFieldName = new JTextField();
        JTextField textFieldEmail = new JTextField();
        JTextField textFieldPhone = new JTextField();
        JTextField textFieldUser = new JTextField();
        JTextField textFieldPass = new JPasswordField();

        Object[] fields = {
            "Full Name:", textFieldName,
            "Email:", textFieldEmail,
            "Phone:", textFieldPhone,
            "Username:", textFieldUser,
            "Password:", textFieldPass
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Register New Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION){
            return;
        }

        String name = textFieldName.getText().trim();
        String email = textFieldEmail.getText().trim();
        String phone = textFieldPhone.getText().trim();
        String user = textFieldUser.getText().trim();
        String pass = textFieldPass.getText().trim();

        if (name.isEmpty() || email.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            labelMessage.setText("Name, email, username and password are required.");
            return;
        }

        try {
            DBConnection.getStatement().executeUpdate("INSERT INTO Customer (name, email, phone, username, password_hash) " + "VALUES ('" + name + "','" + email + "','" + phone + "','" + user + "','" + pass + "')");
            labelMessage.setText("Account created. login.");
        } 
        catch (SQLException ex) {
            labelMessage.setText("Error: " + ex.getMessage());
        }
    }
}