import java.awt.*;
import java.sql.*;
import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    private JTextField textFieldUsername;
    private JTextField textFieldPassword;
    private JLabel statusLabel;

    public LoginFrame() {
        buildUI();
    }

    private void buildUI() {
        JLabel userLabel = new JLabel("Username:");
        textFieldUsername = new JTextField();

        JLabel passLabel = new JLabel("Password:");
        textFieldPassword = new JPasswordField();

        JPanel loginPanel = new JPanel(new GridLayout(2, 2));
        loginPanel.add(userLabel);
        loginPanel.add(textFieldUsername);
        loginPanel.add(passLabel);
        loginPanel.add(textFieldPassword);

        statusLabel = new JLabel(" ");

        JPanel buttonPanel = createButtonPanel();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(loginPanel, BorderLayout.NORTH);
        mainPanel.add(statusLabel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainPanel);
        this.setTitle("Flight Reservation System - Login");
        this.setSize(520, 220);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private JPanel createButtonPanel() {
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
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            ResultSet employeeData = DBConnection.getStatement().executeQuery("SELECT emp_id, role, name FROM Employee " + "WHERE username='" + username + "' AND password_hash='" + password + "'");
            if (employeeData.next()) {
                int empId = employeeData.getInt("emp_id");
                String role = employeeData.getString("role");
                String name = employeeData.getString("name");
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
            statusLabel.setText("DB error: " + ex.getMessage());
            return;
        }

        try {
            ResultSet customerData = DBConnection.getStatement().executeQuery("SELECT cust_id, name FROM Customer " + "WHERE username='" + username + "' AND password_hash='" + password + "'");
            if (customerData.next()) {
                int custId = customerData.getInt("cust_id");
                String name = customerData.getString("name");
                this.dispose();
                new CustomerFrame(custId, name);
                return;
            }
        } 
        catch (SQLException ex) {
            statusLabel.setText("DB error: " + ex.getMessage());
            return;
        }

        statusLabel.setText("Invalid username or password.");
    }

    private void doRegister() {
        JTextField textFieldName = new JTextField();
        JTextField textFieldEmail = new JTextField();
        JTextField textFieldPhone = new JTextField();
        JTextField textFieldUser = new JTextField();
        JTextField textFieldPass = new JPasswordField();

        Object[] registrationFields = {"Full Name:", textFieldName, "Email:", textFieldEmail, "Phone:", textFieldPhone, "Username:", textFieldUser, "Password:", textFieldPass};

        int userResponse = JOptionPane.showConfirmDialog(this, registrationFields, "Register New Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (userResponse != JOptionPane.OK_OPTION){
            return;
        }

        String name = textFieldName.getText().trim();
        String email = textFieldEmail.getText().trim();
        String phone = textFieldPhone.getText().trim();
        String user = textFieldUser.getText().trim();
        String pass = textFieldPass.getText().trim();

        if (name.isEmpty() || email.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Name, email, username and password are required.");
            return;
        }

        try {
            DBConnection.getStatement().executeUpdate("INSERT INTO Customer (name, email, phone, username, password_hash) " + "VALUES ('" + name + "','" + email + "','" + phone + "','" + user + "','" + pass + "')");
            statusLabel.setText("Account created. login.");
        } 
        catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }
}