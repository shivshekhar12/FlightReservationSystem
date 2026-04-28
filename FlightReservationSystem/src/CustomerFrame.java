import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class CustomerFrame extends JFrame {

    private final Font mainFont  = new Font("Lucida Sans", Font.PLAIN, 14);
    private final Font boldFont  = new Font("Lucida Sans", Font.BOLD,  15);

    private final int    custId;
    private final String custName;

    public CustomerFrame(int custId, String custName) {
        this.custId   = custId;
        this.custName = custName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(boldFont);

        tabs.addTab("Search Flights",    buildSearchTab());
        tabs.addTab("My Upcoming Trips", buildUpcomingTab());
        tabs.addTab("My Past Trips",     buildPastTab());
        tabs.addTab("Ask a Question",    buildQuestionsTab());

        this.add(tabs);
        this.setTitle("Flight Reservation System  —  Welcome, " + custName);
        this.setSize(900, 600);
        this.setMinimumSize(new Dimension(700, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 1 — SEARCH FLIGHTS
    // ════════════════════════════════════════════════════════════
    private JPanel buildSearchTab() {
        // ── search controls ─────────────────────────────────────
        JTextField tfFrom     = new JTextField(6);
        JTextField tfTo       = new JTextField(6);
        JTextField tfDate     = new JTextField(10);   // YYYY-MM-DD
        JComboBox<String> cbTrip = new JComboBox<>(new String[]{"One-Way", "Round-Trip"});
        JTextField tfReturn   = new JTextField(10);
        JCheckBox  chkFlex    = new JCheckBox("Flexible ±3 days");

        tfFrom.setFont(mainFont);  tfTo.setFont(mainFont);
        tfDate.setFont(mainFont);  tfReturn.setFont(mainFont);
        cbTrip.setFont(mainFont);  chkFlex.setFont(mainFont);

        // ── sort / filter controls ───────────────────────────────
        JComboBox<String> cbSort = new JComboBox<>(new String[]{
            "Sort: Price ↑", "Sort: Price ↓",
            "Sort: Departure ↑", "Sort: Departure ↓",
            "Sort: Arrival ↑",   "Sort: Arrival ↓",
            "Sort: Duration ↑",  "Sort: Duration ↓"
        });
        JTextField tfMaxPrice  = new JTextField(6);
        JComboBox<String> cbStops = new JComboBox<>(new String[]{"Any Stops","Direct Only","1+ Stop"});
        JTextField tfAirline   = new JTextField(5);

        cbSort.setFont(mainFont);  tfMaxPrice.setFont(mainFont);
        cbStops.setFont(mainFont); tfAirline.setFont(mainFont);

        // ── results table ────────────────────────────────────────
        String[] cols = {"Flight","Airline","From","To","Depart","Arrive","Duration","Economy $","Business $","First $","Stops"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        // ── book button ──────────────────────────────────────────
        JButton btnBook = new JButton("Book Selected Flight");
        btnBook.setFont(boldFont);
        btnBook.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a flight first."); return; }
            String flightNo  = (String) model.getValueAt(row, 0);
            String airlineId = (String) model.getValueAt(row, 1);
            String depDate   = tfDate.getText().trim();
            doBooking(flightNo, airlineId, depDate,
                      cbTrip.getSelectedItem().toString(),
                      tfReturn.getText().trim(), lblMsg);
        });

        // ── search button ────────────────────────────────────────
        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(boldFont);
        btnSearch.addActionListener(e -> {
            model.setRowCount(0);
            lblMsg.setText(" ");
            String from  = tfFrom.getText().trim().toUpperCase();
            String to    = tfTo.getText().trim().toUpperCase();
            String date  = tfDate.getText().trim();
            boolean flex = chkFlex.isSelected();

            if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
                lblMsg.setText("From, To and Date are required.");
                return;
            }

            // build ORDER BY clause
            String orderBy = switch (cbSort.getSelectedIndex()) {
                case 0 -> "f.base_economy_fare ASC";
                case 1 -> "f.base_economy_fare DESC";
                case 2 -> "f.dep_time ASC";
                case 3 -> "f.dep_time DESC";
                case 4 -> "f.arr_time ASC";
                case 5 -> "f.arr_time DESC";
                case 6 -> "TIMEDIFF(f.arr_time, f.dep_time) ASC";
                default -> "TIMEDIFF(f.arr_time, f.dep_time) DESC";
            };

            // flexible date range
            String dateFilter = flex
                ? "dep_date BETWEEN DATE_SUB('" + date + "', INTERVAL 3 DAY) AND DATE_ADD('" + date + "', INTERVAL 3 DAY)"
                : "dep_date = '" + date + "'";

            // stops filter (direct only = 0 stops; we join Ticket_Flight count)
            String stopsClause = "";
            if (cbStops.getSelectedIndex() == 1) stopsClause = " AND 1=1 "; // placeholder — direct flights have 1 leg
            
            // price filter
            String priceClause = "";
            String maxP = tfMaxPrice.getText().trim();
            if (!maxP.isEmpty()) priceClause = " AND f.base_economy_fare <= " + maxP;

            // airline filter
            String airlineClause = "";
            String al = tfAirline.getText().trim().toUpperCase();
            if (!al.isEmpty()) airlineClause = " AND f.airline_id = '" + al + "'";

            String query =
                "SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, " +
                "       f.dep_time, f.arr_time, " +
                "       TIMEDIFF(f.arr_time, f.dep_time) AS duration, " +
                "       f.base_economy_fare, f.base_business_fare, f.base_first_fare, " +
                "       0 AS stops " +
                "FROM Flight f " +
                "JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " +
                "WHERE f.dep_airport_id='" + from + "' AND f.arr_airport_id='" + to + "' " +
                "  AND " + dateFilter +
                "  AND fd.day_of_week = DAYNAME('" + date + "') " +
                priceClause + airlineClause +
                " ORDER BY " + orderBy;

            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(query);
                int count = 0;
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("flight_no"),
                        rs.getString("airline_id"),
                        rs.getString("dep_airport_id"),
                        rs.getString("arr_airport_id"),
                        rs.getString("dep_time"),
                        rs.getString("arr_time"),
                        rs.getString("duration"),
                        "$" + rs.getString("base_economy_fare"),
                        "$" + rs.getString("base_business_fare"),
                        "$" + rs.getString("base_first_fare"),
                        rs.getString("stops")
                    });
                    count++;
                }
                lblMsg.setText(count == 0 ? "No flights found." : count + " flight(s) found.");
            } catch (SQLException ex) {
                lblMsg.setText("Query error: " + ex.getMessage());
            }
        });

        // ── layout ───────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("From:")); topPanel.add(tfFrom);
        topPanel.add(new JLabel("To:"));   topPanel.add(tfTo);
        topPanel.add(new JLabel("Date (YYYY-MM-DD):")); topPanel.add(tfDate);
        topPanel.add(cbTrip);
        topPanel.add(new JLabel("Return:")); topPanel.add(tfReturn);
        topPanel.add(chkFlex);
        topPanel.add(btnSearch);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterPanel.add(cbSort);
        filterPanel.add(new JLabel("Max Price $:")); filterPanel.add(tfMaxPrice);
        filterPanel.add(cbStops);
        filterPanel.add(new JLabel("Airline:")); filterPanel.add(tfAirline);

        JPanel bottomPanel = new JPanel(new BorderLayout(5,5));
        bottomPanel.add(lblMsg, BorderLayout.WEST);
        bottomPanel.add(btnBook, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel northWrapper = new JPanel(new GridLayout(2,1));
        northWrapper.add(topPanel);
        northWrapper.add(filterPanel);

        panel.add(northWrapper, BorderLayout.NORTH);
        panel.add(scroll,       BorderLayout.CENTER);
        panel.add(bottomPanel,  BorderLayout.SOUTH);
        return panel;
    }

    // ── booking dialog ───────────────────────────────────────────
    private void doBooking(String flightNo, String airlineId, String depDate,
                           String tripType, String returnDate, JLabel lblMsg) {
        String[] classes = {"economy","business","first"};
        JComboBox<String> cbClass = new JComboBox<>(classes);
        JTextField tfSeat  = new JTextField(6);
        JTextField tfMeal  = new JTextField(10);
        cbClass.setFont(mainFont); tfSeat.setFont(mainFont); tfMeal.setFont(mainFont);

        Object[] fields = {"Class:", cbClass, "Seat No:", tfSeat, "Meal Pref:", tfMeal};
        int res = JOptionPane.showConfirmDialog(this, fields,
            "Book Flight " + airlineId + flightNo + " on " + depDate,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String cls    = cbClass.getSelectedItem().toString();
        String seat   = tfSeat.getText().trim();
        String meal   = tfMeal.getText().trim();
        String type   = tripType.equals("Round-Trip") ? "round_trip" : "one_way";

        try {
            // check seat availability by counting existing bookings vs aircraft capacity
            String avCheck =
                "SELECT ac."+cls+"_seats - COUNT(tf.ticket_no) AS available " +
                "FROM Flight f " +
                "JOIN Aircraft ac ON f.aircraft_id = ac.aircraft_id " +
                "LEFT JOIN Ticket_Flight tf ON tf.flight_no=f.flight_no " +
                "  AND tf.airline_id=f.airline_id AND tf.dep_date='"+depDate+"' AND tf.class='"+cls+"' " +
                "WHERE f.flight_no='"+flightNo+"' AND f.airline_id='"+airlineId+"' " +
                "GROUP BY ac."+cls+"_seats";
            ResultSet avRs = DBConnection.getStatement().executeQuery(avCheck);

            if (avRs.next() && avRs.getInt("available") <= 0) {
                // flight is full — offer waitlist
                int wl = JOptionPane.showConfirmDialog(this,
                    "This flight is full. Join the waiting list?",
                    "Flight Full", JOptionPane.YES_NO_OPTION);
                if (wl == JOptionPane.YES_OPTION) joinWaitlist(flightNo, airlineId, depDate, cls, lblMsg);
                return;
            }

            // get fare
            String fareCol = "base_" + cls + "_fare";
            ResultSet fareRs = DBConnection.getStatement().executeQuery(
                "SELECT " + fareCol + ", booking_fee FROM Flight WHERE flight_no='"+flightNo+"' AND airline_id='"+airlineId+"'");
            double fare = 0, fee = 25;
            if (fareRs.next()) { fare = fareRs.getDouble(fareCol); fee = fareRs.getDouble("booking_fee"); }

            // insert ticket
            String insTicket =
                "INSERT INTO Ticket (cust_id, total_fare, booking_fee, trip_type, is_flexible, purchase_date, status) " +
                "VALUES (" + custId + "," + fare + "," + fee + ",'" + type + "',0,NOW(),'active')";
            DBConnection.getStatement().executeUpdate(insTicket);

            ResultSet keyRs = DBConnection.getStatement().executeQuery("SELECT LAST_INSERT_ID() AS id");
            int ticketNo = 0;
            if (keyRs.next()) ticketNo = keyRs.getInt("id");

            // insert leg 1
            String insLeg =
                "INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) " +
                "VALUES (" + ticketNo + ",1,'" + flightNo + "','" + airlineId + "','" + depDate + "','" + seat + "','" + cls + "','" + meal + "')";
            DBConnection.getStatement().executeUpdate(insLeg);

            // insert return leg if round-trip
            if (type.equals("round_trip") && !returnDate.isEmpty()) {
                String insReturn =
                    "INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) " +
                    "VALUES (" + ticketNo + ",2,'" + flightNo + "','" + airlineId + "','" + returnDate + "','" + seat + "','" + cls + "','" + meal + "')";
                DBConnection.getStatement().executeUpdate(insReturn);
            }

            lblMsg.setForeground(new Color(0, 120, 0));
            lblMsg.setText("Booked! Ticket #" + ticketNo + "  |  " + cls + "  |  $" + (fare + fee));

        } catch (SQLException ex) {
            lblMsg.setText("Booking error: " + ex.getMessage());
        }
    }

    private void joinWaitlist(String flightNo, String airlineId, String depDate, String cls, JLabel lblMsg) {
        try {
            // get next position
            ResultSet posRs = DBConnection.getStatement().executeQuery(
                "SELECT COALESCE(MAX(position),0)+1 AS pos FROM Waitlist " +
                "WHERE flight_no='"+flightNo+"' AND airline_id='"+airlineId+"' AND dep_date='"+depDate+"'");
            int pos = 1;
            if (posRs.next()) pos = posRs.getInt("pos");

            DBConnection.getStatement().executeUpdate(
                "INSERT INTO Waitlist (cust_id, flight_no, airline_id, dep_date, position, join_date, notified) " +
                "VALUES ("+custId+",'"+flightNo+"','"+airlineId+"','"+depDate+"',"+pos+",NOW(),0)");
            lblMsg.setForeground(new Color(0,100,180));
            lblMsg.setText("Added to waitlist at position " + pos + ".");
        } catch (SQLException ex) {
            lblMsg.setText("Waitlist error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 2 — UPCOMING TRIPS
    // ════════════════════════════════════════════════════════════
    private JPanel buildUpcomingTab() {
        String[] cols = {"Ticket #","Flight","Airline","From","To","Date","Class","Seat","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnRefresh = new JButton("Refresh");
        JButton btnCancel  = new JButton("Cancel Reservation");
        btnRefresh.setFont(mainFont); btnCancel.setFont(mainFont);

        btnRefresh.addActionListener(e -> loadTrips(model, lblMsg, true));
        btnCancel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { lblMsg.setText("Select a reservation to cancel."); return; }
            int ticketNo = (int) model.getValueAt(row, 0);
            String cls   = (String) model.getValueAt(row, 6);
            doCancelReservation(ticketNo, cls, model, lblMsg);
        });

        loadTrips(model, lblMsg, true);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnRefresh); btnPanel.add(btnCancel);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(lblMsg, BorderLayout.WEST);
        south.add(btnPanel, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 3 — PAST TRIPS
    // ════════════════════════════════════════════════════════════
    private JPanel buildPastTab() {
        String[] cols = {"Ticket #","Flight","Airline","From","To","Date","Class","Seat","Total Fare","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(mainFont); table.setRowHeight(22);

        JLabel lblMsg = new JLabel(" ");
        lblMsg.setFont(mainFont);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(mainFont);
        btnRefresh.addActionListener(e -> loadTrips(model, lblMsg, false));

        loadTrips(model, lblMsg, false);

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(lblMsg, BorderLayout.WEST);
        south.add(btnRefresh, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // shared loader for upcoming / past tabs
    private void loadTrips(DefaultTableModel model, JLabel lblMsg, boolean upcoming) {
        model.setRowCount(0);
        String dateOp = upcoming ? ">= CURDATE()" : "< CURDATE()";
        String query =
            "SELECT t.ticket_no, tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, " +
            "       tf.dep_date, tf.class, tf.seat_no, t.total_fare, t.status " +
            "FROM Ticket t " +
            "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no " +
            "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " +
            "WHERE t.cust_id=" + custId +
            "  AND t.status != 'cancelled' " +
            "  AND tf.dep_date " + dateOp +
            " ORDER BY tf.dep_date ASC, tf.leg_order ASC";
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(query);
            int count = 0;
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ticket_no"),
                    rs.getString("flight_no"),
                    rs.getString("airline_id"),
                    rs.getString("dep_airport_id"),
                    rs.getString("arr_airport_id"),
                    rs.getString("dep_date"),
                    rs.getString("class"),
                    rs.getString("seat_no"),
                    "$" + rs.getString("total_fare"),
                    rs.getString("status")
                });
                count++;
            }
            lblMsg.setText(count + " reservation(s) found.");
        } catch (SQLException ex) {
            lblMsg.setText("Error: " + ex.getMessage());
        }
    }

    // cancel — only allowed for business/first class
    private void doCancelReservation(int ticketNo, String cls, DefaultTableModel model, JLabel lblMsg) {
        if (cls.equals("economy")) {
            lblMsg.setForeground(Color.RED);
            lblMsg.setText("Economy tickets cannot be cancelled for free. Contact a representative.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Cancel ticket #" + ticketNo + "? This cannot be undone.",
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            DBConnection.getStatement().executeUpdate(
                "UPDATE Ticket SET status='cancelled' WHERE ticket_no=" + ticketNo);

            // check if anyone is on the waitlist for this flight and notify them
            ResultSet wlRs = DBConnection.getStatement().executeQuery(
                "SELECT w.cust_id, c.name, c.email FROM Waitlist w " +
                "JOIN Ticket_Flight tf ON tf.ticket_no=" + ticketNo +
                " JOIN Customer c ON w.cust_id=c.cust_id " +
                "WHERE w.flight_no=tf.flight_no AND w.airline_id=tf.airline_id " +
                "  AND w.dep_date=tf.dep_date AND w.notified=0 " +
                "ORDER BY w.position ASC LIMIT 1");
            if (wlRs.next()) {
                int wlCustId   = wlRs.getInt("cust_id");
                String wlName  = wlRs.getString("name");
                // mark as notified
                DBConnection.getStatement().executeUpdate(
                    "UPDATE Waitlist SET notified=1 WHERE cust_id=" + wlCustId +
                    " AND flight_no=(SELECT flight_no FROM Ticket_Flight WHERE ticket_no=" + ticketNo + " LIMIT 1)");
                JOptionPane.showMessageDialog(this,
                    "Seat opened. Waitlist customer notified: " + wlName,
                    "Waitlist Alert", JOptionPane.INFORMATION_MESSAGE);
            }

            lblMsg.setForeground(new Color(0,120,0));
            lblMsg.setText("Ticket #" + ticketNo + " cancelled.");
            loadTrips(model, lblMsg, true);
        } catch (SQLException ex) {
            lblMsg.setText("Cancel error: " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  TAB 4 — ASK A QUESTION
    // ════════════════════════════════════════════════════════════
    private JPanel buildQuestionsTab() {
        // ── post a new question ──────────────────────────────────
        JTextField tfSubject  = new JTextField(30);
        JTextArea  taQuestion = new JTextArea(4, 40);
        tfSubject.setFont(mainFont);
        taQuestion.setFont(mainFont);
        taQuestion.setLineWrap(true);
        taQuestion.setWrapStyleWord(true);

        JButton btnPost = new JButton("Post Question");
        btnPost.setFont(mainFont);
        JLabel lblPost = new JLabel(" ");
        lblPost.setFont(mainFont);

        btnPost.addActionListener(e -> {
            String subj = tfSubject.getText().trim();
            String q    = taQuestion.getText().trim();
            if (q.isEmpty()) { lblPost.setText("Please enter a question."); return; }
            try {
                DBConnection.getStatement().executeUpdate(
                    "INSERT INTO Customer_Question (cust_id, subject, question_text, asked_at) " +
                    "VALUES (" + custId + ",'" + subj.replace("'","''") + "','" + q.replace("'","''") + "',NOW())");
                lblPost.setForeground(new Color(0,120,0));
                lblPost.setText("Question posted!");
                tfSubject.setText("");
                taQuestion.setText("");
            } catch (SQLException ex) {
                lblPost.setText("Error: " + ex.getMessage());
            }
        });

        JPanel postPanel = new JPanel(new BorderLayout(5,5));
        postPanel.setBorder(BorderFactory.createTitledBorder("Post a New Question"));
        JPanel fields = new JPanel(new GridLayout(2,2,5,5));
        fields.add(new JLabel("Subject:")); fields.add(tfSubject);
        fields.add(new JLabel("Question:")); fields.add(new JScrollPane(taQuestion));
        postPanel.add(fields, BorderLayout.CENTER);
        JPanel postBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        postBottom.add(lblPost); postBottom.add(btnPost);
        postPanel.add(postBottom, BorderLayout.SOUTH);

        // ── view my questions & replies ──────────────────────────
        String[] cols = {"#","Subject","Question","Answer","Asked","Answered"};
        DefaultTableModel qModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable qTable = new JTable(qModel);
        qTable.setFont(mainFont); qTable.setRowHeight(22);

        JButton btnLoad = new JButton("Load My Questions");
        btnLoad.setFont(mainFont);
        btnLoad.addActionListener(e -> {
            qModel.setRowCount(0);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery(
                    "SELECT question_id, subject, question_text, answer_text, asked_at, answered_at " +
                    "FROM Customer_Question WHERE cust_id=" + custId + " ORDER BY asked_at DESC");
                while (rs.next()) {
                    qModel.addRow(new Object[]{
                        rs.getInt("question_id"),
                        rs.getString("subject"),
                        rs.getString("question_text"),
                        rs.getString("answer_text") != null ? rs.getString("answer_text") : "(awaiting reply)",
                        rs.getString("asked_at"),
                        rs.getString("answered_at") != null ? rs.getString("answered_at") : "—"
                    });
                }
            } catch (SQLException ex) {
                lblPost.setText("Error: " + ex.getMessage());
            }
        });

        JPanel histPanel = new JPanel(new BorderLayout(5,5));
        histPanel.setBorder(BorderFactory.createTitledBorder("My Questions & Replies"));
        histPanel.add(new JScrollPane(qTable), BorderLayout.CENTER);
        histPanel.add(btnLoad, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        panel.add(postPanel,  BorderLayout.NORTH);
        panel.add(histPanel,  BorderLayout.CENTER);
        return panel;
    }
}