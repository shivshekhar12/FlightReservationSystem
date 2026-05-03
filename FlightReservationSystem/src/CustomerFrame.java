import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

@SuppressWarnings({"DataFlowIssue", "SqlSourceToSinkFlow"})
public class CustomerFrame extends JFrame {

    private final int customerId;
    private final String customerName;

    public CustomerFrame(int customerId, String customerName) {
        this.customerId = customerId;
        this.customerName = customerName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search Flights", buildSearchTab());
        tabs.addTab("Upcoming Trips", buildUpcomingTab());
        tabs.addTab("Past Trips", buildPastTab());
        tabs.addTab("Ask a Question", buildQuestionsTab());
        this.add(tabs);
        this.setTitle("Flight Reservation System: " + customerName);
        this.setSize(1200, 600);    
        this.setMinimumSize(new Dimension(700, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private JPanel buildSearchTab() {
        final JTextField fromField = new JTextField(6);
        final JTextField toField = new JTextField(6);
        final JTextField dateField = new JTextField(10);
        final JComboBox<String> tripTypeCombo = new JComboBox<>(new String[]{"One-Way", "Round-Trip"});
        final JTextField returnDateField = new JTextField(10);
        final JCheckBox flexibleCheckBox = new JCheckBox("Flexible +-3 days");
        final JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Sort: Price Up", "Sort: Price Down", "Sort: Departure Up", "Sort: Departure Down", "Sort: Arrival Up", "Sort: Arrival Down", "Sort: Duration Up", "Sort: Duration Down"});
        final JTextField maxPriceField = new JTextField(6);
        final JComboBox<String> stopsCombo = new JComboBox<>(new String[]{"Any Stops", "Direct Only", "1+ Stop"});
        final JTextField airlineFilterField = new JTextField(5);
        final JTextField depAfterField = new JTextField(6);
        final JTextField arrBeforeField = new JTextField(6);
        final String[] searchCriteria = {"Flight", "Airline", "From", "To", "Depart", "Arrive", "Duration", "Economy $", "Business $", "First $", "Stops"};
        final DefaultTableModel searchResultsModel = new DefaultTableModel(searchCriteria, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        final JTable resultsTable = new JTable(searchResultsModel);
        resultsTable.setRowHeight(22);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        final JLabel statusLabel = new JLabel(" ");
        JButton bookButton = new JButton("Book Selected Flight");
        bookButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = resultsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a flight first.");
                    return;
                }
                String flightNo  = (String) searchResultsModel.getValueAt(row, 0);
                String airlineId = (String) searchResultsModel.getValueAt(row, 1);
                String depDate   = dateField.getText().trim();
                bookFlight(flightNo, airlineId, depDate, tripTypeCombo.getSelectedItem().toString(), returnDateField.getText().trim(), statusLabel);
            }
        });

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchResultsModel.setRowCount(0);
                statusLabel.setText(" ");
                String from = fromField.getText().trim().toUpperCase();
                String to = toField.getText().trim().toUpperCase();
                String date = dateField.getText().trim();
                boolean flexible = flexibleCheckBox.isSelected();

                if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
                    statusLabel.setText("From, To and Date are required.");
                    return;
                }
                String orderBy;
                if (sortCombo.getSelectedIndex() == 0){
                    orderBy = "f.base_economy_fare ASC";
                }
                else if (sortCombo.getSelectedIndex() == 1){
                    orderBy = "f.base_economy_fare DESC";
                }
                else if (sortCombo.getSelectedIndex() == 2){
                    orderBy = "f.no dep_time ASC";
                }
                else if (sortCombo.getSelectedIndex() == 3){
                    orderBy = "f.dep_time DESC";
                }
                else if (sortCombo.getSelectedIndex() == 4){
                    orderBy = "f.arr_time ASC";
                }
                else if (sortCombo.getSelectedIndex() == 5) {
                    orderBy = "f.arr_time DESC";
                }
                else if (sortCombo.getSelectedIndex() == 6) {
                    orderBy = "TIMEDIFF(f.arr_time, f.dep_time) ASC";
                }
                else {
                    orderBy = "TIMEDIFF(f.arr_time, f.dep_time) DESC";
                }

                String dateFilter;
                if (flexible) {
                    dateFilter = "fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 0 DAY)) OR " +
                            "fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 1 DAY)) " +
                            "OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 1 DAY)) " +
                            "OR fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 2 DAY)) " +
                            "OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 2 DAY)) " +
                            "OR fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 3 DAY)) " +
                            "OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 3 DAY))";
                }
                else {
                    dateFilter = "fd.day_of_week = DAYNAME('" + date + "')";
                }

                String priceFilter = "";
                String maxPrice = maxPriceField.getText().trim();
                if (!maxPrice.isEmpty()){
                    priceFilter = " AND f.base_economy_fare <= " + maxPrice;
                }

                String depTimeFilter = "";
                String depAfter = depAfterField.getText().trim();
                if (!depAfter.isEmpty()) {
                    depTimeFilter = " AND f.dep_time >= '" + depAfter + "'";
                }

                String arrTimeFilter = "";
                String arrBefore = arrBeforeField.getText().trim();
                if (!arrBefore.isEmpty()) {
                    arrTimeFilter = " AND f.arr_time <= '" + arrBefore + "'";
                }
                String airlineFilter = "";
                String airlineInput = airlineFilterField.getText().trim().toUpperCase();
                if (!airlineInput.isEmpty()){
                    airlineFilter = " AND f.airline_id = '" + airlineInput + "'";
                }

                String stopsFilter = "";
                if (stopsCombo.getSelectedIndex() == 1){
                    stopsFilter = " AND f.stops = 0";
                }
                else if (stopsCombo.getSelectedIndex() == 2){
                    stopsFilter = " AND f.stops >= 1";
                }
                String query = "SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, "
                        + "f.dep_time, f.arr_time, " + "TIMEDIFF(f.arr_time, f.dep_time) AS duration, "
                        + "f.base_economy_fare, f.base_business_fare, f.base_first_fare, " + " f.stops AS stops "
                        + "FROM Flight f " + "JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id "
                        + " WHERE f.dep_airport_id='" + from + "' AND f.arr_airport_id='" + to + "' " + "  AND (" + dateFilter + ") "
                        + priceFilter + airlineFilter + stopsFilter + depTimeFilter + arrTimeFilter + " ORDER BY " + orderBy;
                try {
                    ResultSet flightData = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (flightData.next()) {
                        searchResultsModel.addRow(new Object[]{
                                flightData.getString("flight_no"),
                                flightData.getString("airline_id"),
                                flightData.getString("dep_airport_id"),
                                flightData.getString("arr_airport_id"),
                                flightData.getString("dep_time"),
                                flightData.getString("arr_time"),
                                flightData.getString("duration"), "$" +
                                flightData.getString("base_economy_fare"), "$" +
                                flightData.getString("base_business_fare"), "$" +
                                flightData.getString("base_first_fare"),
                                flightData.getString("stops")});
                        count++;
                    }
                    if (count == 0) {
                        statusLabel.setText("No flights found.");
                    }
                    else {
                        statusLabel.setText(count + " flight(s) found.");
                    }
                } catch (SQLException ex) {
                    statusLabel.setText("Query error: " + ex.getMessage());
                }
            }
        });

        JPanel searchControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchControlsPanel.add(new JLabel("From:"));
        searchControlsPanel.add(fromField);
        searchControlsPanel.add(new JLabel("To:"));
        searchControlsPanel.add(toField);
        searchControlsPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        searchControlsPanel.add(dateField);
        searchControlsPanel.add(tripTypeCombo);
        searchControlsPanel.add(new JLabel("Return:"));
        searchControlsPanel.add(returnDateField);
        searchControlsPanel.add(flexibleCheckBox);
        searchControlsPanel.add(searchButton);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterPanel.add(sortCombo);
        filterPanel.add(new JLabel("Max Price $:"));
        filterPanel.add(maxPriceField);
        filterPanel.add(stopsCombo);
        filterPanel.add(new JLabel("Airline:"));
        filterPanel.add(airlineFilterField);
        filterPanel.add(new JLabel("Dep after:"));
        filterPanel.add(depAfterField);
        filterPanel.add(new JLabel("Arr before:"));
        filterPanel.add(arrBeforeField);
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(bookButton, BorderLayout.EAST);
        JPanel searchPanel = new JPanel(new GridLayout(2, 1));
        searchPanel.add(searchControlsPanel);
        searchPanel.add(filterPanel);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void bookFlight(String flightNo, String airlineId, String depDate, String tripType, String returnDate, JLabel statusLabel) {
        JComboBox<String> classCombo = new JComboBox<>(new String[]{"economy", "business", "first"});
        JTextField seatField = new JTextField(6);
        JTextField mealField = new JTextField(10);

        Object[] bookingInputs = {"Class:", classCombo, "Seat No:", seatField, "Meal Pref:", mealField};
        int confirmDialog = JOptionPane.showConfirmDialog(this, bookingInputs, "Book Flight " + airlineId + flightNo + " on " + depDate, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirmDialog != JOptionPane.OK_OPTION) {
            return;
        }
        String selectedClass = classCombo.getSelectedItem().toString();
        String seatNumber = seatField.getText().trim();
        String mealPref = mealField.getText().trim();
        String ticketType;
        if (tripType.equals("Round-Trip")) {
            ticketType = "round_trip";
        }
        else {
            ticketType = "one_way";
        }
        try {
            String availabilityQuery = "SELECT ac." + selectedClass + "_seats - COUNT(tf.ticket_no) AS available "
                    + "FROM Flight f " + "JOIN Aircraft ac ON f.aircraft_id = ac.aircraft_id "
                    + "LEFT JOIN Ticket_Flight tf ON tf.flight_no=f.flight_no " + "  AND tf.airline_id=f.airline_id AND tf.dep_date='"
                    + depDate + "' AND tf.class='" + selectedClass + "' " + "WHERE f.flight_no='" + flightNo + "' AND f.airline_id='"
                    + airlineId + "' " + "GROUP BY ac." + selectedClass + "_seats";
            ResultSet availabilityResult = DBConnection.getStatement().executeQuery(availabilityQuery);
            if (availabilityResult.next() && availabilityResult.getInt("available") <= 0) {
                int joinWaitlist = JOptionPane.showConfirmDialog(this, "This flight is full. Join the waiting list?", "Flight Full", JOptionPane.YES_NO_OPTION);
                if (joinWaitlist == JOptionPane.YES_OPTION) {
                    joinWaitlist(flightNo, airlineId, depDate, selectedClass, statusLabel);
                    return;
                }
            }
            String fareColumn = "base_" + selectedClass + "_fare";
            ResultSet fareResult = DBConnection.getStatement().executeQuery("SELECT " + fareColumn + ", booking_fee FROM Flight " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
            double fare = 0, bookingFee = 25;
            if (fareResult.next()) {
                fare = fareResult.getDouble(fareColumn);
                bookingFee = fareResult.getDouble("booking_fee");
            }
            DBConnection.getStatement().executeUpdate("INSERT INTO Ticket (cust_id, total_fare, booking_fee, trip_type, is_flexible, purchase_date, status) " + "VALUES (" + customerId + "," + fare + "," + bookingFee + ",'" + ticketType + "',0,NOW(),'active')");
            ResultSet keyResult = DBConnection.getStatement().executeQuery("SELECT LAST_INSERT_ID() AS id");
            int ticketNo = 0;
            if (keyResult.next()){
                ticketNo = keyResult.getInt("id");
            }
            DBConnection.getStatement().executeUpdate("INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) "
                    + "VALUES (" + ticketNo + ",1,'" + flightNo + "','" + airlineId + "','" + depDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')");

            if (ticketType.equals("round_trip") && !returnDate.isEmpty()) {
                DBConnection.getStatement().executeUpdate("INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) "
                        + "VALUES (" + ticketNo + ",2,'" + flightNo + "','" + airlineId + "','" + returnDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')");
            }
            statusLabel.setForeground(new Color(0, 120, 0));
            statusLabel.setText("Booked! Ticket #" + ticketNo + "  |  " + selectedClass + "  |  $" + (fare + bookingFee));

        }
        catch (SQLException ex) {
            statusLabel.setText("Booking error: " + ex.getMessage());
        }
    }

    private void joinWaitlist(String flightNo, String airlineId, String depDate, String selectedClass, JLabel statusLabel) {
        try {
            ResultSet queryResult = DBConnection.getStatement().executeQuery("SELECT COALESCE(MAX(position),0)+1 AS pos FROM Waitlist " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "' AND dep_date='" + depDate + "'");
            int nextPosition = 1;
            if (queryResult.next()){
                nextPosition = queryResult.getInt("pos");
            }

            DBConnection.getStatement().executeUpdate("INSERT INTO Waitlist (cust_id, flight_no, airline_id, dep_date, position, join_date, notified) " + "VALUES (" + customerId + ",'" + flightNo + "','" + airlineId + "','" + depDate + "'," + nextPosition + ",NOW(),0)");
            statusLabel.setForeground(new Color(0, 100, 180));
            statusLabel.setText("Added to waitlist at position " + nextPosition + ".");
        }
        catch (SQLException ex) {
            statusLabel.setText("Waitlist error: " + ex.getMessage());
        }
    }

    private JPanel buildUpcomingTab() {
        final String[] tableColumns = {"Ticket #", "Flight", "Airline", "From", "To", "Date", "Class", "Seat", "Status"};
        final DefaultTableModel tripTableModel = new DefaultTableModel(tableColumns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable tripTable = new JTable(tripTableModel);
        tripTable.setRowHeight(22);
        tripTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel statusLabel = new JLabel(" ");

        JButton refreshButton = new JButton("Refresh");
        JButton cancelButton  = new JButton("Cancel Reservation");

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTrips(tripTableModel, statusLabel, true);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = tripTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a reservation to cancel.");
                    return;
                }
                int ticketNo = (int) tripTableModel.getValueAt(row, 0);
                String selectedClass = (String) tripTableModel.getValueAt(row, 6);
                doCancelReservation(ticketNo, selectedClass, tripTableModel, statusLabel);
            }
        });

        loadTrips(tripTableModel, statusLabel, true);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        actionPanel.add(cancelButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statusLabel, BorderLayout.WEST);
        southPanel.add(actionPanel, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(tripTable), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPastTab() {
        final String[] tableHeaders = {"Ticket #", "Flight", "Airline", "From", "To", "Date", "Class", "Seat", "Total Fare", "Status"};
        final DefaultTableModel tripsTableModel = new DefaultTableModel(tableHeaders, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        final JTable tripsTable = new JTable(tripsTableModel);
        tripsTable.setRowHeight(22);

        final JLabel statusLabel = new JLabel(" ");

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTrips(tripsTableModel, statusLabel, false);
            }
        });

        loadTrips(tripsTableModel, statusLabel, false);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statusLabel, BorderLayout.WEST);
        southPanel.add(refreshButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(tripsTable), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadTrips(DefaultTableModel tripsTableModel, JLabel statusLabel, boolean upcoming) {
        tripsTableModel.setRowCount(0);
        String dateOperator;
        if (upcoming) {
            dateOperator = ">= CURDATE()";
        }
        else {
            dateOperator = "< CURDATE()";
        }
        String query = "SELECT t.ticket_no, tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, "
                + " tf.dep_date, tf.class, tf.seat_no, t.total_fare, t.status " + "FROM Ticket t " + "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no "
                + "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " + "WHERE t.cust_id=" + customerId + "  AND t.status != 'cancelled' "
                + "  AND tf.dep_date " + dateOperator + " ORDER BY tf.dep_date ASC, tf.leg_order ASC";
        try {
            ResultSet tripResults = DBConnection.getStatement().executeQuery(query);
            int count = 0;
            while (tripResults.next()) {
                tripsTableModel.addRow(new Object[]{tripResults.getInt("ticket_no"), tripResults.getString("flight_no"), tripResults.getString("airline_id"), tripResults.getString("dep_airport_id"), tripResults.getString("arr_airport_id"), tripResults.getString("dep_date"), tripResults.getString("class"), tripResults.getString("seat_no"), "$" + tripResults.getString("total_fare"), tripResults.getString("status")});
                count++;
            }
            statusLabel.setText(count + " reservation(s) found.");
        }
        catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void doCancelReservation(int ticketNo, String selectedClass, DefaultTableModel tripsTableModel, JLabel statusLabel) {
        if (selectedClass.equals("economy")) {
            int feeConfirm = JOptionPane.showConfirmDialog(this, "Economy tickets require a cancellation fee of $50. Proceed?", "Cancellation Fee", JOptionPane.YES_NO_OPTION);
            if (feeConfirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Cancel ticket #" + ticketNo, "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION){
            return;
        }
        try {
            DBConnection.getStatement().executeUpdate("UPDATE Ticket SET status='cancelled' WHERE ticket_no=" + ticketNo);

            ResultSet waitlistResult = DBConnection.getStatement().executeQuery("SELECT w.cust_id, c.name FROM Waitlist w "
                    + "JOIN Ticket_Flight tf ON tf.ticket_no=" + ticketNo + " JOIN Customer c ON w.cust_id=c.cust_id "
                    + "WHERE w.flight_no=tf.flight_no AND w.airline_id=tf.airline_id " + "  AND w.dep_date=tf.dep_date AND w.notified=0 " + "ORDER BY w.position ASC LIMIT 1");
            if (waitlistResult.next()) {
                int waitlistCustomerId = waitlistResult.getInt("cust_id");
                String waitlistName = waitlistResult.getString("name");
                DBConnection.getStatement().executeUpdate("UPDATE Waitlist SET notified=1 WHERE cust_id=" + waitlistCustomerId + " AND flight_no=(SELECT flight_no FROM Ticket_Flight WHERE ticket_no=" + ticketNo + " LIMIT 1)");
                JOptionPane.showMessageDialog(this, "Seat opened. Waitlist customer notified: " + waitlistName, "Waitlist Alert", JOptionPane.INFORMATION_MESSAGE);
            }
            statusLabel.setForeground(new Color(0, 120, 0));
            statusLabel.setText("Ticket #" + ticketNo + " cancelled.");
            loadTrips(tripsTableModel, statusLabel, true);
        }
        catch (SQLException ex) {
            statusLabel.setText("Cancel error: " + ex.getMessage());
        }
    }

    private JPanel buildQuestionsTab() {
        final JTextArea  questionArea = new JTextArea(4, 40);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

        final JLabel statusLabel = new JLabel(" ");
        JButton postButton = createPostButton(questionArea, statusLabel);
        JPanel questionsPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        questionsPanel.add(new JLabel("Question:"));
        questionsPanel.add(new JScrollPane(questionArea));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(statusLabel);
        buttonPanel.add(postButton);

        JPanel postPanel = new JPanel(new BorderLayout(5, 5));
        postPanel.setBorder(BorderFactory.createTitledBorder("Post a New Question"));
        postPanel.add(questionsPanel, BorderLayout.CENTER);
        postPanel.add(buttonPanel, BorderLayout.SOUTH);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(postPanel, BorderLayout.NORTH);
        return panel;
    }

    private JButton createPostButton(JTextArea questionArea, JLabel statusLabel) {
        JButton postButton = new JButton("Post Question");

        postButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String questionText = questionArea.getText().trim();
                if (questionText.isEmpty()) {
                    statusLabel.setText("Please enter a question.");
                    return; }
                try {
                    DBConnection.getStatement().executeUpdate("INSERT INTO Customer_Question (cust_id, question_text, asked_at) " + "VALUES (" + customerId + ",'" + questionText.replace("'", "''") + "',NOW())");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Question posted!");
                    questionArea.setText("");
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
        return postButton;
    }
}