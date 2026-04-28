import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class AdminFrame extends JFrame {

    private final Font mainFont = new Font("Lucida Sans", Font.PLAIN, 14);
    private final Font boldFont = new Font("Lucida Sans", Font.BOLD,  15);

    private final int    empId;
    private final String empName;

    public AdminFrame(int empId, String empName) {
        this.empId   = empId;
        this.empName = empName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(boldFont);

        tabs.addTab("Manage Users",       buildManageUsersTab());
        tabs.addTab("Sales Report",       buildSalesReportTab());
        tabs.addTab("Reservations List",  buildReservationsTab());
        tabs.addTab("Revenue Summary",    buildRevenueTab());
        tabs.addTab("Top Customers",      buildTopCustomersTab());
        tabs.addTab("Active Flights",     buildActiveFlightsTab());

        this.add(tabs);
        this.setTitle("Admin Dashboard  —  " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 1 — MANAGE USERS (customers + reps)
    // ════════════════════════════════════════════════════════════
    private JPanel buildManageUsersTab() {
        // ── user type toggle ─────────────────────────────────────
        JComboBox<String> cbType = new JComboBox<>(new String[]{"Customers", "Representatives"});
        cbType.setFont(mainFont);

        // ── table ────────────────────────────────────────────────
        String[] custCols = {"ID","Name","Email","Phone","Username"};
        String[] repCols  = {"ID","Name","Role","Username"};
        DefaultTableModel model = new DefaultTableModel(custCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        // ── load button ──────────────────────────────────────────
        JButton btnLoad = new JButton("Load");
        btnLoad.setFont(mainFont);
        btnLoad.addActionListener(e -> {
            model.setRowCount(0);
            boolean isCust = cbType.getSelectedIndex() == 0;
            if (isCust) {
                updateColumns(model, custCols);
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT cust_id, name, email, phone, username FROM Customer ORDER BY name");
                    while (rs.next())
                        model.addRow(new Object[]{
                            rs.getInt("cust_id"), rs.getString("name"),
                            rs.getString("email"), rs.getString("phone"),
                            rs.getString("username")});
                } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
            } else {
                updateColumns(model, repCols);
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT emp_id, name, role, username FROM Employee ORDER BY name");
                    while (rs.next())
                        model.addRow(new Object[]{
                            rs.getInt("emp_id"), rs.getString("name"),
                            rs.getString("role"), rs.getString("username")});
                } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
            }
            lblMsg.setText(model.getRowCount() + " record(s) loaded.");
        });

        // ── add button ───────────────────────────────────────────
        JButton btnAdd = new JButton("Add");
        btnAdd.setFont(mainFont);
        btnAdd.addActionListener(e -> {
            boolean isCust = cbType.getSelectedIndex() == 0;
            if (isCust) showAddCustomerDialog(lblMsg, model);
            else        showAddRepDialog(lblMsg, model);
        });

        // ── edit button ──────────────────────────────────────────
        JButton btnEdit = new JButton("Edit");
        btnEdit.setFont(mainFont);
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a row to edit."); return; }
            boolean isCust = cbType.getSelectedIndex() == 0;
            int id = (int) model.getValueAt(row, 0);
            if (isCust) showEditCustomerDialog(id, lblMsg, model);
            else        showEditRepDialog(id, lblMsg, model);
        });

        // ── delete button ────────────────────────────────────────
        JButton btnDelete = new JButton("Delete");
        btnDelete.setFont(mainFont);
        btnDelete.setForeground(Color.RED);
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a row to delete."); return; }
            boolean isCust = cbType.getSelectedIndex() == 0;
            int id = (int) model.getValueAt(row, 0);
            String name = (String) model.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + name + "? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                String sql = isCust
                    ? "DELETE FROM Customer WHERE cust_id=" + id
                    : "DELETE FROM Employee WHERE emp_id=" + id;
                DBConnection.getStatement().executeUpdate(sql);
                model.removeRow(row);
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText(name + " deleted.");
            } catch (SQLException ex) {
                lblMsg.setText("Delete error: " + ex.getMessage());
            }
        });

        // ── layout ───────────────────────────────────────────────
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Viewing:")); top.add(cbType);
        top.add(btnLoad); top.add(btnAdd); top.add(btnEdit); top.add(btnDelete);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    private void updateColumns(DefaultTableModel model, String[] cols) {
        model.setColumnCount(0);
        for (String c : cols) model.addColumn(c);
    }

    private void showAddCustomerDialog(JLabel lblMsg, DefaultTableModel model) {
        JTextField tfName  = new JTextField(20);
        JTextField tfEmail = new JTextField(20);
        JTextField tfPhone = new JTextField(15);
        JTextField tfUser  = new JTextField(15);
        JTextField tfPass  = new JPasswordField(15);
        for (JTextField f : new JTextField[]{tfName,tfEmail,tfPhone,tfUser,tfPass}) f.setFont(mainFont);

        Object[] fields = {"Name:", tfName, "Email:", tfEmail, "Phone:", tfPhone,
                           "Username:", tfUser, "Password:", tfPass};
        int res = JOptionPane.showConfirmDialog(this, fields, "Add Customer",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            DBConnection.getStatement().executeUpdate(
                "INSERT INTO Customer (name,email,phone,username,password_hash) VALUES ('" +
                tfName.getText().trim()  + "','" + tfEmail.getText().trim() + "','" +
                tfPhone.getText().trim() + "','" + tfUser.getText().trim()  + "','" +
                tfPass.getText().trim()  + "')");
            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Customer added.");
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    private void showAddRepDialog(JLabel lblMsg, DefaultTableModel model) {
        JTextField tfName = new JTextField(20);
        JTextField tfUser = new JTextField(15);
        JTextField tfPass = new JPasswordField(15);
        JComboBox<String> cbRole = new JComboBox<>(new String[]{"customer_rep","admin"});
        for (JTextField f : new JTextField[]{tfName,tfUser,tfPass}) f.setFont(mainFont);
        cbRole.setFont(mainFont);

        Object[] fields = {"Name:", tfName, "Username:", tfUser, "Password:", tfPass, "Role:", cbRole};
        int res = JOptionPane.showConfirmDialog(this, fields, "Add Employee",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            DBConnection.getStatement().executeUpdate(
                "INSERT INTO Employee (name,role,username,password_hash) VALUES ('" +
                tfName.getText().trim() + "','" + cbRole.getSelectedItem() + "','" +
                tfUser.getText().trim() + "','" + tfPass.getText().trim() + "')");
            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Employee added.");
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    private void showEditCustomerDialog(int custId, JLabel lblMsg, DefaultTableModel model) {
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                "SELECT name,email,phone FROM Customer WHERE cust_id=" + custId);
            if (!rs.next()) return;
            JTextField tfName  = new JTextField(rs.getString("name"),  20);
            JTextField tfEmail = new JTextField(rs.getString("email"), 20);
            JTextField tfPhone = new JTextField(rs.getString("phone"), 15);
            for (JTextField f : new JTextField[]{tfName,tfEmail,tfPhone}) f.setFont(mainFont);

            Object[] fields = {"Name:", tfName, "Email:", tfEmail, "Phone:", tfPhone};
            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            DBConnection.getStatement().executeUpdate(
                "UPDATE Customer SET name='"  + tfName.getText().trim()  +
                "', email='" + tfEmail.getText().trim() +
                "', phone='" + tfPhone.getText().trim() +
                "' WHERE cust_id=" + custId);
            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Customer updated.");
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    private void showEditRepDialog(int empId, JLabel lblMsg, DefaultTableModel model) {
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                "SELECT name, role FROM Employee WHERE emp_id=" + empId);
            if (!rs.next()) return;
            JTextField tfName = new JTextField(rs.getString("name"), 20);
            JComboBox<String> cbRole = new JComboBox<>(new String[]{"customer_rep","admin"});
            cbRole.setSelectedItem(rs.getString("role"));
            tfName.setFont(mainFont); cbRole.setFont(mainFont);

            Object[] fields = {"Name:", tfName, "Role:", cbRole};
            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Employee",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            DBConnection.getStatement().executeUpdate(
                "UPDATE Employee SET name='" + tfName.getText().trim() +
                "', role='" + cbRole.getSelectedItem() +
                "' WHERE emp_id=" + empId);
            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Employee updated.");
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 2 — SALES REPORT (by month)
    // ════════════════════════════════════════════════════════════
    private JPanel buildSalesReportTab() {
        JTextField tfYear  = new JTextField(6);
        JComboBox<String> cbMonth = new JComboBox<>(new String[]{
            "01 - January","02 - February","03 - March","04 - April",
            "05 - May","06 - June","07 - July","08 - August",
            "09 - September","10 - October","11 - November","12 - December"});
        tfYear.setFont(mainFont); cbMonth.setFont(mainFont);
        tfYear.setText(String.valueOf(java.time.LocalDate.now().getYear()));

        String[] cols = {"Ticket #","Customer","Flight","Date","Class","Fare","Booking Fee","Total"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblSummary = new JLabel(" ");
        lblSummary.setFont(boldFont);

        JButton btnRun = new JButton("Generate Report");
        btnRun.setFont(mainFont);
        btnRun.addActionListener(e -> {
            model.setRowCount(0);
            String year  = tfYear.getText().trim();
            String month = cbMonth.getSelectedItem().toString().substring(0, 2);
            String query =
                "SELECT t.ticket_no, c.name AS cust_name, tf.flight_no, tf.dep_date, " +
                "       tf.class, t.total_fare, t.booking_fee, " +
                "       (t.total_fare + t.booking_fee) AS grand_total " +
                "FROM Ticket t " +
                "JOIN Customer c ON t.cust_id = c.cust_id " +
                "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no AND tf.leg_order=1 " +
                "WHERE t.status != 'cancelled' " +
                "  AND YEAR(t.purchase_date)=" + year +
                "  AND MONTH(t.purchase_date)=" + month +
                " ORDER BY t.purchase_date";
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                double totalRev = 0;
                int    count    = 0;
                while (rs.next()) {
                    double grand = rs.getDouble("grand_total");
                    totalRev += grand;
                    count++;
                    model.addRow(new Object[]{
                        rs.getInt("ticket_no"), rs.getString("cust_name"),
                        rs.getString("flight_no"), rs.getString("dep_date"),
                        rs.getString("class"),
                        "$"+rs.getString("total_fare"),
                        "$"+rs.getString("booking_fee"),
                        "$"+String.format("%.2f",grand)});
                }
                lblSummary.setText(String.format(
                    "%d ticket(s)  |  Total Revenue: $%.2f", count, totalRev));
            } catch (SQLException ex) {
                lblSummary.setText("Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Year:")); top.add(tfYear);
        top.add(new JLabel("Month:")); top.add(cbMonth);
        top.add(btnRun);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblSummary, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 3 — RESERVATIONS LIST (by flight or customer name)
    // ════════════════════════════════════════════════════════════
    private JPanel buildReservationsTab() {
        JRadioButton rbFlight   = new JRadioButton("By Flight No");
        JRadioButton rbCustomer = new JRadioButton("By Customer Name");
        rbFlight.setSelected(true);
        rbFlight.setFont(mainFont); rbCustomer.setFont(mainFont);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbFlight); bg.add(rbCustomer);

        JTextField tfSearch = new JTextField(20);
        tfSearch.setFont(mainFont);

        String[] cols = {"Ticket #","Customer","Flight","Airline","Date","Class","Seat","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(mainFont);
        btnSearch.addActionListener(e -> {
            model.setRowCount(0);
            String val = tfSearch.getText().trim();
            if (val.isEmpty()) { lblMsg.setText("Enter a search value."); return; }

            String whereClause = rbFlight.isSelected()
                ? "tf.flight_no='" + val + "'"
                : "c.name LIKE '%" + val + "%'";

            String query =
                "SELECT t.ticket_no, c.name, tf.flight_no, tf.airline_id, " +
                "       tf.dep_date, tf.class, tf.seat_no, t.status " +
                "FROM Ticket t " +
                "JOIN Customer c ON t.cust_id=c.cust_id " +
                "JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " +
                "WHERE " + whereClause +
                " ORDER BY tf.dep_date DESC";
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("ticket_no"), rs.getString("name"),
                        rs.getString("flight_no"), rs.getString("airline_id"),
                        rs.getString("dep_date"), rs.getString("class"),
                        rs.getString("seat_no"), rs.getString("status")});
                    count++;
                }
                lblMsg.setText(count + " reservation(s) found.");
            } catch (SQLException ex) {
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(rbFlight); top.add(rbCustomer);
        top.add(new JLabel("Search:")); top.add(tfSearch);
        top.add(btnSearch);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 4 — REVENUE SUMMARY (by flight / airline / customer)
    // ════════════════════════════════════════════════════════════
    private JPanel buildRevenueTab() {
        JComboBox<String> cbGroup = new JComboBox<>(
            new String[]{"By Flight", "By Airline", "By Customer"});
        cbGroup.setFont(mainFont);

        String[] cols = {"Group","Tickets Sold","Total Fare","Total Booking Fees","Grand Total"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnRun = new JButton("Generate");
        btnRun.setFont(mainFont);
        btnRun.addActionListener(e -> {
            model.setRowCount(0);
            int idx = cbGroup.getSelectedIndex();
            String query = switch (idx) {
                case 0 -> // by flight
                    "SELECT tf.flight_no AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " +
                    "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " +
                    "SUM(t.total_fare + t.booking_fee) AS grand " +
                    "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " +
                    "WHERE t.status!='cancelled' GROUP BY tf.flight_no ORDER BY grand DESC";
                case 1 -> // by airline
                    "SELECT tf.airline_id AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " +
                    "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " +
                    "SUM(t.total_fare + t.booking_fee) AS grand " +
                    "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " +
                    "WHERE t.status!='cancelled' GROUP BY tf.airline_id ORDER BY grand DESC";
                default -> // by customer
                    "SELECT c.name AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " +
                    "SUM(t.total_fare) AS fare, SUM(t.booking_fee) AS fees, " +
                    "SUM(t.total_fare + t.booking_fee) AS grand " +
                    "FROM Ticket t JOIN Customer c ON t.cust_id=c.cust_id " +
                    "WHERE t.status!='cancelled' GROUP BY c.cust_id ORDER BY grand DESC";
            };
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("grp"), rs.getInt("cnt"),
                        "$"+String.format("%.2f", rs.getDouble("fare")),
                        "$"+String.format("%.2f", rs.getDouble("fees")),
                        "$"+String.format("%.2f", rs.getDouble("grand"))});
                    count++;
                }
                lblMsg.setText(count + " group(s).");
            } catch (SQLException ex) {
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Group:")); top.add(cbGroup); top.add(btnRun);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 5 — TOP CUSTOMERS (most total revenue)
    // ════════════════════════════════════════════════════════════
    private JPanel buildTopCustomersTab() {
        JTextField tfLimit = new JTextField("10", 5);
        tfLimit.setFont(mainFont);

        String[] cols = {"Rank","Customer Name","Email","Tickets","Total Spent"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnRun = new JButton("Load");
        btnRun.setFont(mainFont);
        btnRun.addActionListener(e -> {
            model.setRowCount(0);
            String limit = tfLimit.getText().trim();
            try { Integer.parseInt(limit); } catch (NumberFormatException ex) { limit = "10"; }
            String query =
                "SELECT c.name, c.email, " +
                "       COUNT(DISTINCT t.ticket_no) AS tickets, " +
                "       SUM(t.total_fare + t.booking_fee) AS total_spent " +
                "FROM Customer c " +
                "JOIN Ticket t ON c.cust_id=t.cust_id " +
                "WHERE t.status != 'cancelled' " +
                "GROUP BY c.cust_id " +
                "ORDER BY total_spent DESC " +
                "LIMIT " + limit;
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int rank = 1;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rank++, rs.getString("name"), rs.getString("email"),
                        rs.getInt("tickets"),
                        "$"+String.format("%.2f", rs.getDouble("total_spent"))});
                }
                lblMsg.setText("Top " + (rank-1) + " customer(s) by total revenue.");
            } catch (SQLException ex) {
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Show top:")); top.add(tfLimit); top.add(btnRun);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 6 — MOST ACTIVE FLIGHTS (most tickets sold)
    // ════════════════════════════════════════════════════════════
    private JPanel buildActiveFlightsTab() {
        JTextField tfLimit = new JTextField("10", 5);
        tfLimit.setFont(mainFont);

        String[] cols = {"Rank","Flight","Airline","From","To","Tickets Sold","Total Revenue"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnRun = new JButton("Load");
        btnRun.setFont(mainFont);
        btnRun.addActionListener(e -> {
            model.setRowCount(0);
            String limit = tfLimit.getText().trim();
            try { Integer.parseInt(limit); } catch (NumberFormatException ex) { limit = "10"; }
            String query =
                "SELECT tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, " +
                "       COUNT(DISTINCT t.ticket_no) AS tickets_sold, " +
                "       SUM(t.total_fare + t.booking_fee) AS revenue " +
                "FROM Ticket_Flight tf " +
                "JOIN Ticket t ON tf.ticket_no=t.ticket_no " +
                "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " +
                "WHERE t.status != 'cancelled' " +
                "GROUP BY tf.flight_no, tf.airline_id " +
                "ORDER BY tickets_sold DESC " +
                "LIMIT " + limit;
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int rank = 1;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rank++,
                        rs.getString("flight_no"), rs.getString("airline_id"),
                        rs.getString("dep_airport_id"), rs.getString("arr_airport_id"),
                        rs.getInt("tickets_sold"),
                        "$"+String.format("%.2f", rs.getDouble("revenue"))});
                }
                lblMsg.setText("Top " + (rank-1) + " most active flight(s).");
            } catch (SQLException ex) {
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Show top:")); top.add(tfLimit); top.add(btnRun);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }
}