import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class RepFrame extends JFrame {

    private final Font mainFont = new Font("Lucida Sans", Font.PLAIN, 14);
    private final Font boldFont = new Font("Lucida Sans", Font.BOLD,  15);

    private final int    empId;
    private final String empName;

    public RepFrame(int empId, String empName) {
        this.empId   = empId;
        this.empName = empName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(boldFont);

        tabs.addTab("Book for Customer",   buildBookForCustomerTab());
        tabs.addTab("Edit Reservations",   buildEditReservationsTab());
        tabs.addTab("Manage Flights",      buildManageFlightsTab());
        tabs.addTab("Manage Airports",     buildManageAirportsTab());
        tabs.addTab("Manage Aircraft",     buildManageAircraftTab());
        tabs.addTab("Waitlist",            buildWaitlistTab());
        tabs.addTab("Flights by Airport",  buildFlightsByAirportTab());
        tabs.addTab("Answer Questions",    buildAnswerQuestionsTab());

        this.add(tabs);
        this.setTitle("Representative Dashboard  —  " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 1 — BOOK A FLIGHT ON BEHALF OF A CUSTOMER
    // ════════════════════════════════════════════════════════════
    private JPanel buildBookForCustomerTab() {
        // customer lookup
        JTextField tfCustSearch = new JTextField(20);
        tfCustSearch.setFont(mainFont);
        JComboBox<String> cbCustomer = new JComboBox<>();
        cbCustomer.setFont(mainFont);
        // store cust_id alongside display name
        java.util.Map<String,Integer> custMap = new java.util.LinkedHashMap<>();

        JButton btnFindCust = new JButton("Find Customer");
        btnFindCust.setFont(mainFont);
        btnFindCust.addActionListener(e -> {
            cbCustomer.removeAllItems();
            custMap.clear();
            String q = tfCustSearch.getText().trim();
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT cust_id, name, email FROM Customer WHERE name LIKE '%" + q + "%' ORDER BY name");
                while (rs.next()) {
                    String label = rs.getString("name") + " (" + rs.getString("email") + ")";
                    custMap.put(label, rs.getInt("cust_id"));
                    cbCustomer.addItem(label);
                }
                if (cbCustomer.getItemCount() == 0) cbCustomer.addItem("No customers found");
            } catch (SQLException ex) { cbCustomer.addItem("Error: " + ex.getMessage()); }
        });

        // flight search fields
        JTextField tfFrom   = new JTextField(6);
        JTextField tfTo     = new JTextField(6);
        JTextField tfDate   = new JTextField(12);
        JTextField tfStops = new JTextField("0", 4);
        JComboBox<String> cbTrip = new JComboBox<>(new String[]{"One-Way","Round-Trip"});
        JTextField tfReturn = new JTextField(12);
        for (JTextField f : new JTextField[]{tfFrom,tfTo,tfDate,tfReturn}) f.setFont(mainFont);
        cbTrip.setFont(mainFont);

        // results table
        String[] cols = {"Flight","Airline","From","To","Depart","Arrive","Economy $","Business $","First $"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnSearch = new JButton("Search Flights");
        btnSearch.setFont(mainFont);
        btnSearch.addActionListener(e -> {
            model.setRowCount(0);
            String from = tfFrom.getText().trim().toUpperCase();
            String to   = tfTo.getText().trim().toUpperCase();
            String date = tfDate.getText().trim();
            if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
                lblMsg.setText("From, To and Date are required."); return;
            }
            String query =
                    "SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, " +
                            "       f.dep_time, f.arr_time, " +
                            "       f.base_economy_fare, f.base_business_fare, f.base_first_fare " +
                            "FROM Flight f " +
                            "JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " +
                            "WHERE f.dep_airport_id='" + from + "' AND f.arr_airport_id='" + to + "' " +
                            "  AND fd.day_of_week = DAYNAME('" + date + "') " +
                            "ORDER BY f.dep_time";
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int cnt = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("flight_no"), rs.getString("airline_id"),
                            rs.getString("dep_airport_id"), rs.getString("arr_airport_id"),
                            rs.getString("dep_time"), rs.getString("arr_time"),
                            "$"+rs.getString("base_economy_fare"),
                            "$"+rs.getString("base_business_fare"),
                            "$"+rs.getString("base_first_fare")});
                    cnt++;
                }
                lblMsg.setText(cnt == 0 ? "No flights found." : cnt + " flight(s) found.");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JButton btnBook = new JButton("Book Selected for Customer");
        btnBook.setFont(boldFont);
        btnBook.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a flight first."); return; }
            String sel = (String) cbCustomer.getSelectedItem();
            if (sel == null || !custMap.containsKey(sel)) {
                lblMsg.setText("Select a valid customer first."); return;
            }
            int custId = custMap.get(sel);
            String flightNo  = (String) model.getValueAt(row, 0);
            String airlineId = (String) model.getValueAt(row, 1);
            String date      = tfDate.getText().trim();
            String tripType  = cbTrip.getSelectedItem().toString();
            String retDate   = tfReturn.getText().trim();
            doRepBooking(custId, flightNo, airlineId, date, tripType, retDate, lblMsg);
        });

        // layout
        JPanel custPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        custPanel.setBorder(BorderFactory.createTitledBorder("Step 1 — Select Customer"));
        custPanel.add(new JLabel("Search name:")); custPanel.add(tfCustSearch);
        custPanel.add(btnFindCust);
        custPanel.add(new JLabel("Customer:")); custPanel.add(cbCustomer);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Step 2 — Search Flights"));
        searchPanel.add(new JLabel("From:")); searchPanel.add(tfFrom);
        searchPanel.add(new JLabel("To:"));   searchPanel.add(tfTo);
        searchPanel.add(new JLabel("Date (YYYY-MM-DD):")); searchPanel.add(tfDate);
        searchPanel.add(cbTrip);
        searchPanel.add(new JLabel("Return:")); searchPanel.add(tfReturn);
        searchPanel.add(btnSearch);

        JPanel northWrap = new JPanel(new GridLayout(2,1));
        northWrap.add(custPanel); northWrap.add(searchPanel);

        JPanel south = new JPanel(new BorderLayout(5,5));
        south.add(lblMsg, BorderLayout.WEST);
        south.add(btnBook, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(northWrap, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void doRepBooking(int custId, String flightNo, String airlineId,
                              String depDate, String tripType, String returnDate, JLabel lblMsg) {
        String[] classes = {"economy","business","first"};
        JComboBox<String> cbClass = new JComboBox<>(classes);
        JTextField tfSeat = new JTextField(6);
        JTextField tfMeal = new JTextField(10);
        cbClass.setFont(mainFont); tfSeat.setFont(mainFont); tfMeal.setFont(mainFont);

        Object[] fields = {"Class:", cbClass, "Seat No:", tfSeat, "Meal Pref:", tfMeal};
        int res = JOptionPane.showConfirmDialog(this, fields,
                "Book " + airlineId + flightNo + " for customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String cls   = cbClass.getSelectedItem().toString();
        String seat  = tfSeat.getText().trim();
        String meal  = tfMeal.getText().trim();
        String type  = tripType.equals("Round-Trip") ? "round_trip" : "one_way";

        try {
            String fareCol = "base_" + cls + "_fare";
            ResultSet fareRs = DBConnection.getStatement().executeQuery(
                    "SELECT " + fareCol + ", booking_fee FROM Flight " +
                            "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
            double fare = 0, fee = 25;
            if (fareRs.next()) { fare = fareRs.getDouble(fareCol); fee = fareRs.getDouble("booking_fee"); }

            DBConnection.getStatement().executeUpdate(
                    "INSERT INTO Ticket (cust_id, handled_by, total_fare, booking_fee, trip_type, is_flexible, purchase_date, status) " +
                            "VALUES (" + custId + "," + empId + "," + fare + "," + fee + ",'" + type + "',0,NOW(),'active')");

            ResultSet keyRs = DBConnection.getStatement().executeQuery("SELECT LAST_INSERT_ID() AS id");
            int ticketNo = 0;
            if (keyRs.next()) ticketNo = keyRs.getInt("id");

            DBConnection.getStatement().executeUpdate(
                    "INSERT INTO Ticket_Flight (ticket_no,leg_order,flight_no,airline_id,dep_date,seat_no,class,meal_pref) " +
                            "VALUES (" + ticketNo + ",1,'" + flightNo + "','" + airlineId + "','" +
                            depDate + "','" + seat + "','" + cls + "','" + meal + "')");

            if (type.equals("round_trip") && !returnDate.isEmpty()) {
                DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Ticket_Flight (ticket_no,leg_order,flight_no,airline_id,dep_date,seat_no,class,meal_pref) " +
                                "VALUES (" + ticketNo + ",2,'" + flightNo + "','" + airlineId + "','" +
                                returnDate + "','" + seat + "','" + cls + "','" + meal + "')");
            }

            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Booked! Ticket #" + ticketNo + "  |  " + cls + "  |  $" + (fare + fee));
        } catch (SQLException ex) {
            lblMsg.setText("Booking error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 2 — EDIT RESERVATIONS FOR A CUSTOMER
    // ════════════════════════════════════════════════════════════
    private JPanel buildEditReservationsTab() {
        JTextField tfCustId = new JTextField(8);
        tfCustId.setFont(mainFont);

        String[] cols = {"Ticket #","Flight","Airline","Date","Class","Seat","Meal","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnLoad = new JButton("Load Reservations");
        btnLoad.setFont(mainFont);
        btnLoad.addActionListener(e -> {
            model.setRowCount(0);
            String cid = tfCustId.getText().trim();
            if (cid.isEmpty()) { lblMsg.setText("Enter customer ID."); return; }
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT t.ticket_no, tf.flight_no, tf.airline_id, tf.dep_date, " +
                                "       tf.class, tf.seat_no, tf.meal_pref, t.status " +
                                "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " +
                                "WHERE t.cust_id=" + cid + " ORDER BY tf.dep_date DESC");
                int cnt = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("ticket_no"), rs.getString("flight_no"),
                            rs.getString("airline_id"), rs.getString("dep_date"),
                            rs.getString("class"), rs.getString("seat_no"),
                            rs.getString("meal_pref"), rs.getString("status")});
                    cnt++;
                }
                lblMsg.setText(cnt + " reservation(s) found.");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JButton btnEdit = new JButton("Edit Selected");
        btnEdit.setFont(mainFont);
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a reservation to edit."); return; }
            int ticketNo = (int) model.getValueAt(row, 0);
            String curSeat = (String) model.getValueAt(row, 5);
            String curMeal = (String) model.getValueAt(row, 6);
            String curDate = (String) model.getValueAt(row, 3);

            JTextField tfSeat = new JTextField(curSeat != null ? curSeat : "", 8);
            JTextField tfMeal = new JTextField(curMeal != null ? curMeal : "", 15);
            JTextField tfDate = new JTextField(curDate != null ? curDate : "", 12);
            tfSeat.setFont(mainFont); tfMeal.setFont(mainFont); tfDate.setFont(mainFont);

            Object[] fields = {"New Seat No:", tfSeat, "New Meal Pref:", tfMeal, "New Dep Date:", tfDate};
            int res = JOptionPane.showConfirmDialog(this, fields,
                    "Edit Ticket #" + ticketNo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "UPDATE Ticket_Flight SET seat_no='" + tfSeat.getText().trim() +
                                "', meal_pref='" + tfMeal.getText().trim() +
                                "', dep_date='" + tfDate.getText().trim() +
                                "' WHERE ticket_no=" + ticketNo);
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Ticket #" + ticketNo + " updated.");
                btnLoad.doClick();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Customer ID:")); top.add(tfCustId);
        top.add(btnLoad); top.add(btnEdit);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 3 — MANAGE FLIGHTS (Add / Edit / Delete)
    // ════════════════════════════════════════════════════════════
    private JPanel buildManageFlightsTab() {
        String[] cols = {"Flight No","Airline","Aircraft","From","To","Depart","Arrive","Intl","Economy $","Business $","First $"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnLoad   = new JButton("Load All Flights");
        JButton btnAdd    = new JButton("Add Flight");
        JButton btnEdit   = new JButton("Edit Selected");
        JButton btnDelete = new JButton("Delete Selected");
        for (JButton b : new JButton[]{btnLoad,btnAdd,btnEdit,btnDelete}) b.setFont(mainFont);
        btnDelete.setForeground(Color.RED);

        btnLoad.addActionListener(e -> loadAllFlights(model, lblMsg));

        btnAdd.addActionListener(e -> showFlightDialog(null, null, model, lblMsg));

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a flight to edit."); return; }
            String flightNo  = (String) model.getValueAt(row, 0);
            String airlineId = (String) model.getValueAt(row, 1);
            showFlightDialog(flightNo, airlineId, model, lblMsg);
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a flight to delete."); return; }
            String flightNo  = (String) model.getValueAt(row, 0);
            String airlineId = (String) model.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete flight " + airlineId + flightNo + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "DELETE FROM Flight_Day WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                DBConnection.getStatement().executeUpdate(
                        "DELETE FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Flight deleted.");
                loadAllFlights(model, lblMsg);
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        loadAllFlights(model, lblMsg);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        for (JButton b : new JButton[]{btnLoad,btnAdd,btnEdit,btnDelete}) top.add(b);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    private void loadAllFlights(DefaultTableModel model, JLabel lblMsg) {
        model.setRowCount(0);
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                    "SELECT flight_no, airline_id, aircraft_id, dep_airport_id, arr_airport_id, " +
                            "dep_time, arr_time, is_international, " +
                            "base_economy_fare, base_business_fare, base_first_fare FROM Flight ORDER BY airline_id, flight_no");
            while (rs.next())
                model.addRow(new Object[]{
                        rs.getString("flight_no"), rs.getString("airline_id"),
                        rs.getInt("aircraft_id"),
                        rs.getString("dep_airport_id"), rs.getString("arr_airport_id"),
                        rs.getString("dep_time"), rs.getString("arr_time"),
                        rs.getBoolean("is_international") ? "Yes" : "No",
                        "$"+rs.getString("base_economy_fare"),
                        "$"+rs.getString("base_business_fare"),
                        "$"+rs.getString("base_first_fare")});
            lblMsg.setText(model.getRowCount() + " flight(s) loaded.");
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    private void showFlightDialog(String flightNo, String airlineId,
                                  DefaultTableModel model, JLabel lblMsg) {
        boolean isEdit = (flightNo != null);

        JTextField tfFlightNo  = new JTextField(flightNo != null ? flightNo : "", 8);
        JTextField tfAirline   = new JTextField(airlineId != null ? airlineId : "", 4);
        JTextField tfAircraft  = new JTextField(8);
        JTextField tfFrom      = new JTextField(4);
        JTextField tfTo        = new JTextField(4);
        JTextField tfDep       = new JTextField(8);
        JTextField tfArr       = new JTextField(8);
        JCheckBox  chkIntl     = new JCheckBox("International");
        JTextField tfEcoFare   = new JTextField(8);
        JTextField tfBizFare   = new JTextField(8);
        JTextField tfFstFare   = new JTextField(8);
        JTextField tfStops     = new JTextField("0", 4);
        JTextField tfDays      = new JTextField("Mon,Tue,Wed,Thu,Fri", 20);

        for (JTextField f : new JTextField[]{tfFlightNo,tfAirline,tfAircraft,tfFrom,
                tfTo,tfDep,tfArr,tfEcoFare,tfBizFare,tfFstFare,tfStops,tfDays})
            f.setFont(mainFont);
        chkIntl.setFont(mainFont);

        if (isEdit) {
            tfFlightNo.setEditable(false); tfAirline.setEditable(false);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT * FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (rs.next()) {
                    tfAircraft.setText(String.valueOf(rs.getInt("aircraft_id")));
                    tfFrom.setText(rs.getString("dep_airport_id"));
                    tfTo.setText(rs.getString("arr_airport_id"));
                    tfDep.setText(rs.getString("dep_time"));
                    tfArr.setText(rs.getString("arr_time"));
                    chkIntl.setSelected(rs.getBoolean("is_international"));
                    tfEcoFare.setText(rs.getString("base_economy_fare"));
                    tfBizFare.setText(rs.getString("base_business_fare"));
                    tfFstFare.setText(rs.getString("base_first_fare"));
                    tfStops.setText(String.valueOf(rs.getInt("stops")));
                }
                ResultSet dRs = DBConnection.getStatement().executeQuery(
                        "SELECT GROUP_CONCAT(day_of_week) AS days FROM Flight_Day " +
                                "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (dRs.next()) tfDays.setText(dRs.getString("days"));
            } catch (SQLException ex) { lblMsg.setText("Error loading flight: " + ex.getMessage()); return; }
        }

        Object[] fields = {
                "Flight No:", tfFlightNo, "Airline ID:", tfAirline,
                "Aircraft ID:", tfAircraft, "From Airport:", tfFrom,
                "To Airport:", tfTo, "Dep Time (HH:MM):", tfDep,
                "Arr Time (HH:MM):", tfArr, chkIntl,
                "Economy Fare:", tfEcoFare, "Business Fare:", tfBizFare,
                "First Fare:", tfFstFare, "Stops:", tfStops,
                "Days (comma-sep, e.g. Mon,Wed,Fri):", tfDays
        };

        int res = JOptionPane.showConfirmDialog(this, fields,
                isEdit ? "Edit Flight" : "Add Flight",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            String fn  = tfFlightNo.getText().trim();
            String al  = tfAirline.getText().trim().toUpperCase();
            String ac  = tfAircraft.getText().trim();
            String frm = tfFrom.getText().trim().toUpperCase();
            String too = tfTo.getText().trim().toUpperCase();
            String dep = tfDep.getText().trim();
            String arr = tfArr.getText().trim();
            boolean intl = chkIntl.isSelected();
            String eco = tfEcoFare.getText().trim();
            String biz = tfBizFare.getText().trim();
            String fst = tfFstFare.getText().trim();
            String stops = tfStops.getText().trim();
            String[] days = tfDays.getText().trim().split(",");

            if (isEdit) {
                DBConnection.getStatement().executeUpdate(
                        "UPDATE Flight SET aircraft_id=" + ac +
                                ", dep_airport_id='" + frm + "', arr_airport_id='" + too +
                                "', dep_time='" + dep + "', arr_time='" + arr +
                                "', is_international=" + intl +
                                ", base_economy_fare=" + eco +
                                ", base_business_fare=" + biz +
                                ", base_first_fare=" + fst +
                                ", stops=" + stops +
                                " WHERE flight_no='" + fn + "' AND airline_id='" + al + "'");
                DBConnection.getStatement().executeUpdate(
                        "DELETE FROM Flight_Day WHERE flight_no='" + fn + "' AND airline_id='" + al + "'");
            } else {
                DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Flight (flight_no,airline_id,aircraft_id,dep_airport_id,arr_airport_id," +
                                "dep_time,arr_time,is_international,base_economy_fare,base_business_fare,base_first_fare,stops) " +
                                "VALUES ('" + fn + "','" + al + "'," + ac + ",'" + frm + "','" + too + "','" +
                                dep + "','" + arr + "'," + intl + "," + eco + "," + biz + "," + fst + "," + stops + ")");
            }
            for (String day : days) {
                String d = day.trim();
                if (!d.isEmpty())
                    DBConnection.getStatement().executeUpdate(
                            "INSERT INTO Flight_Day (flight_no,airline_id,day_of_week) VALUES ('" + fn + "','" + al + "','" + d + "')");
            }
            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Flight " + (isEdit ? "updated" : "added") + ".");
            loadAllFlights(model, lblMsg);
        } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 4 — MANAGE AIRPORTS
    // ════════════════════════════════════════════════════════════
    private JPanel buildManageAirportsTab() {
        String[] cols = {"Airport ID","Name","City","Country"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        Runnable loadAll = () -> {
            model.setRowCount(0);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT airport_id, name, city, country FROM Airport ORDER BY airport_id");
                while (rs.next())
                    model.addRow(new Object[]{rs.getString("airport_id"), rs.getString("name"),
                            rs.getString("city"), rs.getString("country")});
                lblMsg.setText(model.getRowCount() + " airport(s).");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        };
        loadAll.run();

        JButton btnAdd    = new JButton("Add");
        JButton btnEdit   = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        for (JButton b : new JButton[]{btnAdd,btnEdit,btnDelete}) b.setFont(mainFont);
        btnDelete.setForeground(Color.RED);

        btnAdd.addActionListener(e -> {
            JTextField tfId      = new JTextField(4);
            JTextField tfName    = new JTextField(20);
            JTextField tfCity    = new JTextField(15);
            JTextField tfCountry = new JTextField(15);
            for (JTextField f : new JTextField[]{tfId,tfName,tfCity,tfCountry}) f.setFont(mainFont);
            Object[] fields = {"Airport ID (3-letter):", tfId, "Name:", tfName, "City:", tfCity, "Country:", tfCountry};
            int res = JOptionPane.showConfirmDialog(this, fields, "Add Airport",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Airport (airport_id,name,city,country) VALUES ('" +
                                tfId.getText().trim().toUpperCase() + "','" + tfName.getText().trim() + "','" +
                                tfCity.getText().trim() + "','" + tfCountry.getText().trim() + "')");
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Airport added."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select an airport."); return; }
            String aid = (String) model.getValueAt(row, 0);
            JTextField tfName    = new JTextField((String) model.getValueAt(row, 1), 20);
            JTextField tfCity    = new JTextField((String) model.getValueAt(row, 2), 15);
            JTextField tfCountry = new JTextField((String) model.getValueAt(row, 3), 15);
            for (JTextField f : new JTextField[]{tfName,tfCity,tfCountry}) f.setFont(mainFont);
            Object[] fields = {"Name:", tfName, "City:", tfCity, "Country:", tfCountry};
            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Airport " + aid,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "UPDATE Airport SET name='" + tfName.getText().trim() +
                                "', city='" + tfCity.getText().trim() +
                                "', country='" + tfCountry.getText().trim() +
                                "' WHERE airport_id='" + aid + "'");
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Airport updated."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select an airport."); return; }
            String aid = (String) model.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete airport " + aid + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "DELETE FROM Airport WHERE airport_id='" + aid + "'");
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Airport deleted."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(btnAdd); top.add(btnEdit); top.add(btnDelete);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 5 — MANAGE AIRCRAFT
    // ════════════════════════════════════════════════════════════
    private JPanel buildManageAircraftTab() {
        String[] cols = {"Aircraft ID","Airline","Model","Economy Seats","Business Seats","First Seats"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        Runnable loadAll = () -> {
            model.setRowCount(0);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT aircraft_id, airline_id, model, economy_seats, business_seats, first_seats FROM Aircraft ORDER BY aircraft_id");
                while (rs.next())
                    model.addRow(new Object[]{rs.getInt("aircraft_id"), rs.getString("airline_id"),
                            rs.getString("model"), rs.getInt("economy_seats"),
                            rs.getInt("business_seats"), rs.getInt("first_seats")});
                lblMsg.setText(model.getRowCount() + " aircraft loaded.");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        };
        loadAll.run();

        JButton btnAdd    = new JButton("Add");
        JButton btnEdit   = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        for (JButton b : new JButton[]{btnAdd,btnEdit,btnDelete}) b.setFont(mainFont);
        btnDelete.setForeground(Color.RED);

        btnAdd.addActionListener(e -> {
            JTextField tfId  = new JTextField(6);
            JTextField tfAl  = new JTextField(4);
            JTextField tfMod = new JTextField(20);
            JTextField tfEco = new JTextField(6);
            JTextField tfBiz = new JTextField(6);
            JTextField tfFst = new JTextField(6);
            for (JTextField f : new JTextField[]{tfId,tfAl,tfMod,tfEco,tfBiz,tfFst}) f.setFont(mainFont);
            Object[] fields = {"Aircraft ID:", tfId, "Airline ID:", tfAl, "Model:", tfMod,
                    "Economy Seats:", tfEco, "Business Seats:", tfBiz, "First Seats:", tfFst};
            int res = JOptionPane.showConfirmDialog(this, fields, "Add Aircraft",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Aircraft (aircraft_id,airline_id,model,economy_seats,business_seats,first_seats) VALUES (" +
                                tfId.getText().trim() + ",'" + tfAl.getText().trim().toUpperCase() + "','" +
                                tfMod.getText().trim() + "'," + tfEco.getText().trim() + "," +
                                tfBiz.getText().trim() + "," + tfFst.getText().trim() + ")");
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Aircraft added."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select an aircraft."); return; }
            int acid = (int) model.getValueAt(row, 0);
            JTextField tfMod = new JTextField((String) model.getValueAt(row, 2), 20);
            JTextField tfEco = new JTextField(String.valueOf(model.getValueAt(row, 3)), 6);
            JTextField tfBiz = new JTextField(String.valueOf(model.getValueAt(row, 4)), 6);
            JTextField tfFst = new JTextField(String.valueOf(model.getValueAt(row, 5)), 6);
            for (JTextField f : new JTextField[]{tfMod,tfEco,tfBiz,tfFst}) f.setFont(mainFont);
            Object[] fields = {"Model:", tfMod, "Economy Seats:", tfEco,
                    "Business Seats:", tfBiz, "First Seats:", tfFst};
            int res = JOptionPane.showConfirmDialog(this, fields, "Edit Aircraft " + acid,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate(
                        "UPDATE Aircraft SET model='" + tfMod.getText().trim() +
                                "', economy_seats=" + tfEco.getText().trim() +
                                ", business_seats=" + tfBiz.getText().trim() +
                                ", first_seats=" + tfFst.getText().trim() +
                                " WHERE aircraft_id=" + acid);
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Aircraft updated."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select an aircraft."); return; }
            int acid = (int) model.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete aircraft " + acid + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                DBConnection.getStatement().executeUpdate("DELETE FROM Aircraft WHERE aircraft_id=" + acid);
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Aircraft deleted."); loadAll.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(btnAdd); top.add(btnEdit); top.add(btnDelete);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 6 — WAITLIST FOR A FLIGHT
    // ════════════════════════════════════════════════════════════
    private JPanel buildWaitlistTab() {
        JTextField tfFlight  = new JTextField(10);
        JTextField tfAirline = new JTextField(4);
        JTextField tfDate    = new JTextField(12);
        for (JTextField f : new JTextField[]{tfFlight,tfAirline,tfDate}) f.setFont(mainFont);

        String[] cols = {"Position","Customer ID","Customer Name","Email","Requested","Notified"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnLoad = new JButton("Load Waitlist");
        btnLoad.setFont(mainFont);
        btnLoad.addActionListener(e -> {
            model.setRowCount(0);
            String fn  = tfFlight.getText().trim();
            String al  = tfAirline.getText().trim().toUpperCase();
            String dat = tfDate.getText().trim();
            if (fn.isEmpty() || al.isEmpty() || dat.isEmpty()) {
                lblMsg.setText("Enter flight no, airline and date."); return;
            }
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT w.position, w.cust_id, c.name, c.email, w.join_date, w.notified " +
                                "FROM Waitlist w JOIN Customer c ON w.cust_id=c.cust_id " +
                                "WHERE w.flight_no='" + fn + "' AND w.airline_id='" + al +
                                "' AND w.dep_date='" + dat + "' ORDER BY w.position");
                int cnt = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("position"), rs.getInt("cust_id"),
                            rs.getString("name"), rs.getString("email"),
                            rs.getString("join_date"),
                            rs.getBoolean("notified") ? "Yes" : "No"});
                    cnt++;
                }
                lblMsg.setText(cnt + " customer(s) on waitlist.");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Flight No:"));  top.add(tfFlight);
        top.add(new JLabel("Airline ID:")); top.add(tfAirline);
        top.add(new JLabel("Date:"));       top.add(tfDate);
        top.add(btnLoad);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 7 — ALL FLIGHTS FOR A GIVEN AIRPORT
    // ════════════════════════════════════════════════════════════
    private JPanel buildFlightsByAirportTab() {
        JTextField tfAirport = new JTextField(6);
        tfAirport.setFont(mainFont);
        JComboBox<String> cbDir = new JComboBox<>(new String[]{"Departing & Arriving","Departing Only","Arriving Only"});
        cbDir.setFont(mainFont);

        String[] cols = {"Direction","Flight","Airline","From","To","Depart","Arrive","Days"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnLoad = new JButton("Load Flights");
        btnLoad.setFont(mainFont);
        btnLoad.addActionListener(e -> {
            model.setRowCount(0);
            String ap  = tfAirport.getText().trim().toUpperCase();
            int    dir = cbDir.getSelectedIndex();
            if (ap.isEmpty()) { lblMsg.setText("Enter an airport ID."); return; }
            try {
                // departing
                if (dir == 0 || dir == 1) {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                            "SELECT 'Departing' AS dir, f.flight_no, f.airline_id, " +
                                    "f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, " +
                                    "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " +
                                    "FROM Flight f " +
                                    "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " +
                                    "WHERE f.dep_airport_id='" + ap + "' GROUP BY f.flight_no, f.airline_id");
                    while (rs.next())
                        model.addRow(new Object[]{"Departing",
                                rs.getString("flight_no"), rs.getString("airline_id"),
                                rs.getString("dep_airport_id"), rs.getString("arr_airport_id"),
                                rs.getString("dep_time"), rs.getString("arr_time"),
                                rs.getString("days")});
                }
                // arriving
                if (dir == 0 || dir == 2) {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                            "SELECT 'Arriving' AS dir, f.flight_no, f.airline_id, " +
                                    "f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, " +
                                    "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " +
                                    "FROM Flight f " +
                                    "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " +
                                    "WHERE f.arr_airport_id='" + ap + "' GROUP BY f.flight_no, f.airline_id");
                    while (rs.next())
                        model.addRow(new Object[]{"Arriving",
                                rs.getString("flight_no"), rs.getString("airline_id"),
                                rs.getString("dep_airport_id"), rs.getString("arr_airport_id"),
                                rs.getString("dep_time"), rs.getString("arr_time"),
                                rs.getString("days")});
                }
                lblMsg.setText(model.getRowCount() + " flight(s) found for " + ap + ".");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(new JLabel("Airport ID:")); top.add(tfAirport);
        top.add(cbDir); top.add(btnLoad);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 8 — ANSWER CUSTOMER QUESTIONS
    // ════════════════════════════════════════════════════════════
    private JPanel buildAnswerQuestionsTab() {
        String[] cols = {"#","Customer","Subject","Question","Answer","Asked","Answered"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnLoad = new JButton("Load Unanswered");
        JButton btnAll  = new JButton("Load All");
        JButton btnReply = new JButton("Reply to Selected");
        for (JButton b : new JButton[]{btnLoad,btnAll,btnReply}) b.setFont(mainFont);

        Runnable loadUnanswered = () -> {
            model.setRowCount(0);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT q.question_id, c.name, q.subject, q.question_text, " +
                                "q.answer_text, q.asked_at, q.answered_at " +
                                "FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id " +
                                "WHERE q.answer_text IS NULL ORDER BY q.asked_at ASC");
                int cnt = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("question_id"), rs.getString("name"),
                            rs.getString("subject"), rs.getString("question_text"),
                            "(unanswered)", rs.getString("asked_at"), "—"});
                    cnt++;
                }
                lblMsg.setText(cnt + " unanswered question(s).");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        };

        btnLoad.addActionListener(e -> loadUnanswered.run());

        btnAll.addActionListener(e -> {
            model.setRowCount(0);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT q.question_id, c.name, q.subject, q.question_text, " +
                                "q.answer_text, q.asked_at, q.answered_at " +
                                "FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id " +
                                "ORDER BY q.asked_at DESC");
                int cnt = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("question_id"), rs.getString("name"),
                            rs.getString("subject"), rs.getString("question_text"),
                            rs.getString("answer_text") != null ? rs.getString("answer_text") : "(unanswered)",
                            rs.getString("asked_at"),
                            rs.getString("answered_at") != null ? rs.getString("answered_at") : "—"});
                    cnt++;
                }
                lblMsg.setText(cnt + " question(s) total.");
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        btnReply.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a question to reply to."); return; }
            int qid = (int) model.getValueAt(row, 0);
            String question = (String) model.getValueAt(row, 3);

            JTextArea taAnswer = new JTextArea(5, 40);
            taAnswer.setFont(mainFont);
            taAnswer.setLineWrap(true);
            taAnswer.setWrapStyleWord(true);

            Object[] fields = {
                    "Question: " + question,
                    "Your Answer:",
                    new JScrollPane(taAnswer)
            };
            int res = JOptionPane.showConfirmDialog(this, fields,
                    "Reply to Question #" + qid,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;

            String answer = taAnswer.getText().trim();
            if (answer.isEmpty()) { lblMsg.setText("Answer cannot be empty."); return; }

            try {
                DBConnection.getStatement().executeUpdate(
                        "UPDATE Customer_Question SET " +
                                "answer_text='" + answer.replace("'","''") + "', " +
                                "rep_id=" + empId + ", " +
                                "answered_at=NOW() " +
                                "WHERE question_id=" + qid);
                lblMsg.setForeground(new Color(0,120,0));
                lblMsg.setText("Reply posted for question #" + qid + ".");
                loadUnanswered.run();
            } catch (SQLException ex) { lblMsg.setText("Error: " + ex.getMessage()); }
        });

        loadUnanswered.run();

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.add(btnLoad); top.add(btnAll); top.add(btnReply);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblMsg, BorderLayout.SOUTH);
        return panel;
    }
}