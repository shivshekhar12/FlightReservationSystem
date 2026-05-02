import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageUsersTab {

    public static JPanel build(JFrame parentFrame) {
        JComboBox<String> userTypeCombo = new JComboBox<>(new String[]{"Customers", "Representatives"});
        String[] customerColumns = {"ID", "Name", "Email", "Phone", "Username"};
        String[] repColumns = {"ID", "Name", "Role", "Username"};
        DefaultTableModel customerTableModel = new DefaultTableModel(customerColumns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable usersTable = new JTable(customerTableModel);
        usersTable.setRowHeight(22);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customerTableModel.setRowCount(0);
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                if (isCustomer) {
                    updateColumns(customerTableModel, customerColumns);
                    try {
                        ResultSet customerResults = DBConnection.getStatement().executeQuery(
                            "SELECT cust_id, name, email, phone, username FROM Customer ORDER BY name");
                        while (customerResults.next()) {
                            customerTableModel.addRow(new Object[]{customerResults.getInt("cust_id"), customerResults.getString("name"), customerResults.getString("email"), customerResults.getString("phone"), customerResults.getString("username")});
                        }
                    }
                    catch (SQLException ex) {
                        messageLabel.setText("Error: " + ex.getMessage());
                    }
                }
                else {
                    updateColumns(customerTableModel, repColumns);
                    try {
                        ResultSet employeeData = DBConnection.getStatement().executeQuery("SELECT emp_id, name, role, username FROM Employee ORDER BY name");
                        while (employeeData.next()) {
                            customerTableModel.addRow(new Object[]{employeeData.getInt("emp_id"), employeeData.getString("name"), employeeData.getString("role"), employeeData.getString("username")});
                        }
                    }
                    catch (SQLException ex) {
                        messageLabel.setText("Error: " + ex.getMessage());
                    }
                }
                messageLabel.setText(customerTableModel.getRowCount() + " record(s) loaded.");
            }
        });

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                if (isCustomer) {
                    showAddCustomerDialog(parentFrame, messageLabel, customerTableModel);
                }
                else {
                    showAddRepDialog(parentFrame, messageLabel, customerTableModel);
                }
            }
        });

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int clickedRow = usersTable.getSelectedRow();
                if (clickedRow < 0) {
                    messageLabel.setText("Select a row to edit.");
                    return;
                }
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                int targetId = (int) customerTableModel.getValueAt(clickedRow, 0);
                if (isCustomer) {
                    showEditCustomerDialog(parentFrame, targetId, messageLabel, customerTableModel);
                }
                else {
                    showEditRepDialog(parentFrame, targetId, messageLabel, customerTableModel);
                }
            }
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int clickedRow = usersTable.getSelectedRow();
                if (clickedRow < 0) {
                    messageLabel.setText("Select a row to delete.");
                    return;
                }
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                int userId = (int) customerTableModel.getValueAt(clickedRow, 0);
                String userName = (String) customerTableModel.getValueAt(clickedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete " + userName + "? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    String deleteQuery;
                    if (isCustomer) {
                        deleteQuery = "DELETE FROM Customer WHERE cust_id=" + userId;
                    }
                    else {
                        deleteQuery = "DELETE FROM Employee WHERE emp_id=" + userId;
                    }
                    DBConnection.getStatement().executeUpdate(deleteQuery);
                    customerTableModel.removeRow(clickedRow);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText(userName + " deleted.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Delete error: " + ex.getMessage());
                }
            }
        });

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        controlsPanel.add(new JLabel("Viewing:"));
        controlsPanel.add(userTypeCombo);
        controlsPanel.add(loadButton);
        controlsPanel.add(addButton);
        controlsPanel.add(editButton);
        controlsPanel.add(deleteButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(controlsPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void updateColumns(DefaultTableModel dataModel, String[] columns) {
        dataModel.setColumnCount(0);
        for (String column : columns) {
            dataModel.addColumn(column);
        }
    }

    private static void showAddCustomerDialog(JFrame parentFrame, JLabel messageLabel, DefaultTableModel customerTableModel) {
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(15);
        JTextField usernameField = new JTextField(15);
        JTextField passwordField = new JPasswordField(15);
        Object[] fields = {"Name:", nameField, "Email:", emailField, "Phone:", phoneField, "Username:", usernameField, "Password:", passwordField};
        int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Add Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResult != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            DBConnection.getStatement().executeUpdate("INSERT INTO Customer (name,email,phone,username,password_hash) VALUES ('" + nameField.getText().trim() + "','" + emailField.getText().trim()
                    + "','" + phoneField.getText().trim() + "','" + usernameField.getText().trim() + "','" + passwordField.getText().trim() + "')");
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Customer added.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void showAddRepDialog(JFrame parentFrame, JLabel messageLabel, DefaultTableModel tableModel) {
        JTextField nameField = new JTextField(20);
        JTextField usernameField = new JTextField(15);
        JTextField passwordField = new JPasswordField(15);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer_rep", "admin"});
        Object[] fields = {"Name:", nameField, "Username:", usernameField, "Password:", passwordField, "Role:", roleCombo};
        int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Add Employee",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResult != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            DBConnection.getStatement().executeUpdate("INSERT INTO Employee (name,role,username,password_hash) VALUES ('" + nameField.getText().trim() + "','" + roleCombo.getSelectedItem()
                    + "','" + usernameField.getText().trim() + "','" + passwordField.getText().trim() + "')");
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Employee added.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void showEditCustomerDialog(JFrame parentFrame, int custId, JLabel messageLabel, DefaultTableModel tableModel) {
        try {
            ResultSet customerData = DBConnection.getStatement().executeQuery("SELECT name,email,phone FROM Customer WHERE cust_id=" + custId);
            if (!customerData.next()) {
                return;
            }
            JTextField nameField = new JTextField(customerData.getString("name"), 20);
            JTextField emailField = new JTextField(customerData.getString("email"), 20);
            JTextField phoneField = new JTextField(customerData.getString("phone"), 15);
            Object[] fields = {"Name:", nameField, "Email:", emailField, "Phone:", phoneField};
            int editCustomer = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (editCustomer != JOptionPane.OK_OPTION) {
                return;
            }
            DBConnection.getStatement().executeUpdate("UPDATE Customer SET name='" + nameField.getText().trim() + "', email='" + emailField.getText().trim() + "', phone='" + phoneField.getText().trim() + "' WHERE cust_id=" + custId);
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Customer updated.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void showEditRepDialog(JFrame parentFrame, int empId, JLabel messageLabel, DefaultTableModel tableModel) {
        try {
            ResultSet employeeData = DBConnection.getStatement().executeQuery("SELECT name, role FROM Employee WHERE emp_id=" + empId);
            if (!employeeData.next()) {
                return;
            }
            JTextField nameField = new JTextField(employeeData.getString("name"), 20);
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer_rep", "admin"});
            roleCombo.setSelectedItem(employeeData.getString("role"));
            Object[] fields = {"Name:", nameField, "Role:", roleCombo};
            int userResponse = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Employee",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (userResponse != JOptionPane.OK_OPTION) {
                return;
            }
            DBConnection.getStatement().executeUpdate("UPDATE Employee SET name='" + nameField.getText().trim() + "', role='" + roleCombo.getSelectedItem() + "' WHERE emp_id=" + empId);
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Employee updated.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }
}
