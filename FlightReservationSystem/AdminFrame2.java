import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class AdminFrame extends JFrame {

    private int empId;
    private String empName;

    public AdminFrame(int empId, String empName) {
        this.empId = empId;
        this.empName = empName;
        buildUI();
    }
    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Users", buildManageUsersTab());
        tabs.addTab("Sales Report", buildSalesReportTab());
        tabs.addTab("Reservations List", buildReservationsTab());
        tabs.addTab("Revenue Summary", buildRevenueTab());
        tabs.addTab("Top Customers", buildTopCustomersTab());
        tabs.addTab("Active Flights", buildActiveFlightsTab());
        this.add(tabs);
        this.setTitle("Admin Dashboard  —  " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private JPanel buildManageUsersTab() {
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
                        ResultSet rs = DBConnection.getStatement().executeQuery("SELECT cust_id, name, email, phone, username FROM Customer ORDER BY name");
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
                    showAddCustomerDialog(messageLabel, tableModel);
                }
                else {
                    showAddRepDialog(messageLabel, tableModel);
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
                    showEditCustomerDialog(selectedId, messageLabel, tableModel);
                }
                else {
                    showEditRepDialog(selectedId, messageLabel, tableModel);
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
                int confirm = JOptionPane.showConfirmDialog(AdminFrame.this, "Delete " + selectedName + "? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
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

    private void updateColumns(DefaultTableModel tableModel, String[] columns) {
        tableModel.setColumnCount(0);
        for (String column : columns) {
            tableModel.addColumn(column);
        }
    }

    private void showAddCustomerDialog(JLabel messageLabel, DefaultTableModel tableModel) {
        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField phoneField = new JTextField(15);
        JTextField usernameField = new JTextField(15);
        JTextField passwordField = new JPasswordField(15);

        Object[] fields = {"Name:", nameField, "Email:", emailField, "Phone:", phoneField, "Username:", usernameField, "Password:", passwordField};
        int dialogResult = JOptionPane.showConfirmDialog(this, fields, "Add Customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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

    private void showAddRepDialog(JLabel messageLabel, DefaultTableModel tableModel) {
        JTextField nameField = new JTextField(20);
        JTextField usernameField = new JTextField(15);
        JTextField passwordField = new JPasswordField(15);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer_rep", "admin"});
        Object[] fields = {"Name:", nameField, "Username:", usernameField, "Password:", passwordField, "Role:", roleCombo};
        int dialogResult = JOptionPane.showConfirmDialog(this, fields, "Add Employee",
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

    private void showEditCustomerDialog(int custId, JLabel messageLabel, DefaultTableModel tableModel) {
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery("SELECT name,email,phone FROM Customer WHERE cust_id=" + custId);
            if (!rs.next()) {
                return;
            }
            JTextField nameField = new JTextField(rs.getString("name"), 20);
            JTextField emailField = new JTextField(rs.getString("email"), 20);
            JTextField phoneField = new JTextField(rs.getString("phone"), 15);
            Object[] fields = {"Name:", nameField, "Email:", emailField, "Phone:", phoneField};
            int dialogResult = JOptionPane.showConfirmDialog(this, fields, "Edit Customer",
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

    private void showEditRepDialog(int empId, JLabel messageLabel, DefaultTableModel tableModel) {
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery("SELECT name, role FROM Employee WHERE emp_id=" + empId);
            if (!rs.next()) {
                return;
            }
            JTextField nameField = new JTextField(rs.getString("name"), 20);
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer_rep", "admin"});
            roleCombo.setSelectedItem(rs.getString("role"));
            Object[] fields = {"Name:", nameField, "Role:", roleCombo};
            int dialogResult = JOptionPane.showConfirmDialog(this, fields, "Edit Employee",
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

    private JPanel buildSalesReportTab() {
        JTextField yearField = new JTextField(6);
        JComboBox<String> monthCombo = new JComboBox<>(new String[]{"01 - January", "02 - February", "03 - March", "04 - April", "05 - May", "06 - June", "07 - July", "08 - August", "09 - September", "10 - October", "11 - November", "12 - December"});
        yearField.setText(String.valueOf(java.time.LocalDate.now().getYear()));
        String[] columns = {"Ticket #", "Customer", "Flight", "Date", "Class", "Fare", "Booking Fee", "Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable salesTable = new JTable(tableModel);
        salesTable.setRowHeight(22);
        JLabel summaryLabel = new JLabel(" ");
        JButton generateButton = new JButton("Generate Report");
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String year = yearField.getText().trim();
                String month = monthCombo.getSelectedItem().toString().substring(0, 2);
                String query = "SELECT t.ticket_no, c.name AS cust_name, tf.flight_no, tf.dep_date, " + "tf.class, t.total_fare, t.booking_fee, " + "(t.total_fare + t.booking_fee) AS grand_total " + "FROM Ticket t " + "JOIN Customer c ON t.cust_id = c.cust_id " + "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no AND tf.leg_order=1 " + "WHERE t.status != 'cancelled' " + "AND YEAR(t.purchase_date)=" + year + " AND MONTH(t.purchase_date)=" + month + " ORDER BY t.purchase_date";
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    double totalRevenue = 0;
                    int count = 0;
                    while (rs.next()) {
                        double grandTotal = rs.getDouble("grand_total");
                        totalRevenue += grandTotal;
                        count++;
                        tableModel.addRow(new Object[]{rs.getInt("ticket_no"), rs.getString("cust_name"), rs.getString("flight_no"), rs.getString("dep_date"), rs.getString("class"), "$" + rs.getString("total_fare"), "$" + rs.getString("booking_fee"), "$" + String.format("%.2f", grandTotal)});
                    }
                    summaryLabel.setText(String.format("%d ticket(s)  |  Total Revenue: $%.2f", count, totalRevenue));
                }
                catch (SQLException ex) {
                    summaryLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Year:"));
        topPanel.add(yearField);
        topPanel.add(new JLabel("Month:"));
        topPanel.add(monthCombo);
        topPanel.add(generateButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(salesTable), BorderLayout.CENTER);
        panel.add(summaryLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildReservationsTab() {
        JRadioButton byFlightRadio = new JRadioButton("By Flight No");
        JRadioButton byCustomerRadio = new JRadioButton("By Customer Name");
        byFlightRadio.setSelected(true);
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(byFlightRadio);
        radioGroup.add(byCustomerRadio);
        JTextField searchField = new JTextField(20);
        String[] columns = {"Ticket #", "Customer", "Flight", "Airline", "Date", "Class", "Seat", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable reservationsTable = new JTable(tableModel);
        reservationsTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String searchValue = searchField.getText().trim();
                if (searchValue.isEmpty()) {
                    messageLabel.setText("Enter a search value.");
                    return;
                }
                String whereClause;
                if (byFlightRadio.isSelected()) {
                    whereClause = "tf.flight_no='" + searchValue + "'";
                }
                else {
                    whereClause = "c.name LIKE '%" + searchValue + "%'";
                }
                String query = "SELECT t.ticket_no, c.name, tf.flight_no, tf.airline_id, " + "tf.dep_date, tf.class, tf.seat_no, t.status " + "FROM Ticket t " + "JOIN Customer c ON t.cust_id=c.cust_id " + "JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE " + whereClause + " ORDER BY tf.dep_date DESC";
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rs.getInt("ticket_no"), rs.getString("name"), rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_date"), rs.getString("class"), rs.getString("seat_no"), rs.getString("status")});
                        count++;
                    }
                    messageLabel.setText(count + " reservation(s) found.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(byFlightRadio);
        topPanel.add(byCustomerRadio);
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(searchButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRevenueTab() {
        JComboBox<String> groupByCombo = new JComboBox<>(new String[]{"By Flight", "By Airline", "By Customer"});
        String[] columns = {"Group", "Tickets Sold", "Total Fare", "Total Booking Fees", "Grand Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable revenueTable = new JTable(tableModel);
        revenueTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                int selectedIndex = groupByCombo.getSelectedIndex();
                String query;
                if (selectedIndex == 0) {
                    query = "SELECT tf.flight_no AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE t.status!='cancelled' GROUP BY tf.flight_no ORDER BY grand DESC";
                }
                else if (selectedIndex == 1) {
                    query = "SELECT tf.airline_id AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE t.status!='cancelled' GROUP BY tf.airline_id ORDER BY grand DESC";
                }
                else {
                    query = "SELECT c.name AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Customer c ON t.cust_id=c.cust_id " + "WHERE t.status!='cancelled' GROUP BY c.cust_id ORDER BY grand DESC";
                }
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rs.getString("grp"), rs.getInt("cnt"), "$" + String.format("%.2f", rs.getDouble("fare")), "$" + String.format("%.2f", rs.getDouble("fees")), "$" + String.format("%.2f", rs.getDouble("grand"))});
                        count++;
                    }
                    messageLabel.setText(count + " group(s).");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Group:"));
        topPanel.add(groupByCombo);
        topPanel.add(generateButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(revenueTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTopCustomersTab() {
        JTextField limitField = new JTextField("10", 5);

        String[] columns = {"Rank", "Customer Name", "Email", "Tickets", "Total Spent"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable topCustomersTable = new JTable(tableModel);
        topCustomersTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String limit = limitField.getText().trim();
                try {
                    Integer.parseInt(limit);
                }
                catch (NumberFormatException ex) {
                    limit = "10";
                }
                String query =
                        "SELECT c.name, c.email, " + "COUNT(DISTINCT t.ticket_no) AS tickets, " + "SUM(t.total_fare + t.booking_fee) AS total_spent " + "FROM Customer c " + "JOIN Ticket t ON c.cust_id=t.cust_id " + "WHERE t.status != 'cancelled' " + "GROUP BY c.cust_id " + "ORDER BY total_spent DESC " + "LIMIT " + limit;
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                                rank++, rs.getString("name"), rs.getString("email"),
                                rs.getInt("tickets"),
                                "$" + String.format("%.2f", rs.getDouble("total_spent"))});
                    }
                    messageLabel.setText("Top " + (rank - 1) + " customer(s) by total revenue.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Show top:"));
        topPanel.add(limitField);
        topPanel.add(loadButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(topCustomersTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildActiveFlightsTab() {
        JTextField limitField = new JTextField("10", 5);

        String[] columns = {"Rank", "Flight", "Airline", "From", "To", "Tickets Sold", "Total Revenue"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable activeFlightsTable = new JTable(tableModel);
        activeFlightsTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String limit = limitField.getText().trim();
                try {
                    Integer.parseInt(limit);
                }
                catch (NumberFormatException ex) {
                    limit = "10";
                }
                String query =
                        "SELECT tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, " + "COUNT(DISTINCT t.ticket_no) AS tickets_sold, " + "SUM(t.total_fare + t.booking_fee) AS revenue " + "FROM Ticket_Flight tf " + "JOIN Ticket t ON tf.ticket_no=t.ticket_no " + "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " + "WHERE t.status != 'cancelled' " + "GROUP BY tf.flight_no, tf.airline_id " + "ORDER BY tickets_sold DESC " + "LIMIT " + limit;
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rank++, rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_airport_id"), rs.getString("arr_airport_id"), rs.getInt("tickets_sold"), "$" + String.format("%.2f", rs.getDouble("revenue"))});
                    }
                    messageLabel.setText("Top " + (rank - 1) + " most active flight(s).");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Show top:"));
        topPanel.add(limitField);
        topPanel.add(loadButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(activeFlightsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}