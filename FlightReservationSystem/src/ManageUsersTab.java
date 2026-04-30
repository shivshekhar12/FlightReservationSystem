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
        DefaultTableModel tableModel = new DefaultTableModel(customerColumns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable usersTable = new JTable(tableModel);
        usersTable.setRowHeight(22);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                if (isCustomer) {
                    updateColumns(tableModel, customerColumns);
                    try {
                        ResultSet rs = DBConnection.getStatement().executeQuery(
                            "SELECT cust_id, name, email, phone, username FROM Customer ORDER BY name");
                        while (rs.next()) {
                            tableModel.addRow(new Object[]{rs.getInt("cust_id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"), rs.getString("username")});
                        }
                    }
                    catch (SQLException ex) {
                        messageLabel.setText("Error: " + ex.getMessage());
                    }
                }
                else {
                    updateColumns(tableModel, repColumns);
                    try {
                        ResultSet rs = DBConnection.getStatement().executeQuery("SELECT emp_id, name, role, username FROM Employee ORDER BY name");
                        while (rs.next()) {
                            tableModel.addRow(new Object[]{rs.getInt("emp_id"), rs.getString("name"), rs.getString("role"), rs.getString("username")});
                        }
                    }
                    catch (SQLException ex) {
                        messageLabel.setText("Error: " + ex.getMessage());
                    }
                }
                messageLabel.setText(tableModel.getRowCount() + " record(s) loaded.");
            }
        });

        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                if (isCustomer) {
                    showAddCustomerDialog(parentFrame, messageLabel, tableModel);
                }
                else {
                    showAddRepDialog(parentFrame, messageLabel, tableModel);
                }
            }
        });

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = usersTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a row to edit.");
                    return;
                }
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                int selectedId = (int) tableModel.getValueAt(selectedRow, 0);
                if (isCustomer) {
                    showEditCustomerDialog(parentFrame, selectedId, messageLabel, tableModel);
                }
                else {
                    showEditRepDialog(parentFrame, selectedId, messageLabel, tableModel);
                }
            }
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = usersTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a row to delete.");
                    return;
                }
                boolean isCustomer = userTypeCombo.getSelectedIndex() == 0;
                int selectedId = (int) tableModel.getValueAt(selectedRow, 0);
                String selectedName = (String) tableModel.getValueAt(selectedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete " + selectedName + "? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    String sql;
                    if (isCustomer) {
                        sql = "DELETE FROM Customer WHERE cust_id=" + selectedId;
                    }
                    else {
                        sql = "DELETE FROM Employee WHERE emp_id=" + selectedId;
                    }
                    DBConnection.getStatement().executeUpdate(sql);
                    tableModel.removeRow(selectedRow);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText(selectedName + " deleted.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Delete error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Viewing:"));
        topPanel.add(userTypeCombo);
        topPanel.add(loadButton);
        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void updateColumns(DefaultTableModel tableModel, String[] columns) {
        tableModel.setColumnCount(0);
        for (String column : columns) {
            tableModel.addColumn(column);
        }
    }

    private static void showAddCustomerDialog(JFrame parentFrame, JLabel messageLabel, DefaultTableModel tableModel) {
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
            DBConnection.getStatement().executeUpdate("INSERT INTO Customer (name,email,phone,username,password_hash) VALUES ('" + nameField.getText().trim() + "','" + emailField.getText().trim() + "','" + phoneField.getText().trim() + "','" + usernameField.getText().trim() + "','" + passwordField.getText().trim() + "')");
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
            DBConnection.getStatement().executeUpdate("INSERT INTO Employee (name,role,username,password_hash) VALUES ('" + nameField.getText().trim() + "','" + roleCombo.getSelectedItem() + "','" + usernameField.getText().trim() + "','" + passwordField.getText().trim() + "')");
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Employee added.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void showEditCustomerDialog(JFrame parentFrame, int custId, JLabel messageLabel, DefaultTableModel tableModel) {
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery("SELECT name,email,phone FROM Customer WHERE cust_id=" + custId);
            if (!rs.next()) {
                return;
            }
            JTextField nameField = new JTextField(rs.getString("name"), 20);
            JTextField emailField = new JTextField(rs.getString("email"), 20);
            JTextField phoneField = new JTextField(rs.getString("phone"), 15);
            Object[] fields = {"Name:", nameField, "Email:", emailField, "Phone:", phoneField};
            int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (dialogResult != JOptionPane.OK_OPTION) {
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
            ResultSet rs = DBConnection.getStatement().executeQuery("SELECT name, role FROM Employee WHERE emp_id=" + empId);
            if (!rs.next()) {
                return;
            }
            JTextField nameField = new JTextField(rs.getString("name"), 20);
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer_rep", "admin"});
            roleCombo.setSelectedItem(rs.getString("role"));
            Object[] fields = {"Name:", nameField, "Role:", roleCombo};
            int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Employee",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (dialogResult != JOptionPane.OK_OPTION) {
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
