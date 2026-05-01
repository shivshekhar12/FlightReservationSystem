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
        this.setSize(900, 600);
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
        final JComboBox<String> sortCombo = new JComboBox<>(new String[]{"Sort: Price Up", "Sort: Price Down", "Sort: Departure Up", "Sort: Departure Down", "Sort: Arrival Up", "Sort: Arrival Down"});
        final JTextField maxPriceField = new JTextField(6);
        final JComboBox<String> stopsCombo = new JComboBox<>(new String[]{"Any Stops", "Direct Only", "1+ Stop"});
        final JTextField airlineFilterField = new JTextField(5);
        final String[] columns = {"Flight", "Airline", "From", "To", "Depart", "Arrive", "Duration", "Economy $", "Business $", "First $", "Stops"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        final JTable resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(22);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        final JLabel messageLabel = new JLabel(" ");
        JButton bookButton = new JButton("Book Selected Flight");
        bookButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = resultsTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a flight first.");
                    return;
                }
                String flightNo  = (String) tableModel.getValueAt(selectedRow, 0);
                String airlineId = (String) tableModel.getValueAt(selectedRow, 1);
                String depDate   = dateField.getText().trim();
                doBooking(flightNo, airlineId, depDate, tripTypeCombo.getSelectedItem().toString(), returnDateField.getText().trim(), messageLabel);
            }
        });

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                messageLabel.setText(" ");
                String from = fromField.getText().trim().toUpperCase();
                String to = toField.getText().trim().toUpperCase();
                String date = dateField.getText().trim();
                boolean flexible = flexibleCheckBox.isSelected();

                if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
                    messageLabel.setText("From, To and Date are required.");
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
                    orderBy = "f.dep_time ASC";
                }
                else if (sortCombo.getSelectedIndex() == 3){
                    orderBy = "f.dep_time DESC";
                }
                else if (sortCombo.getSelectedIndex() == 4){
                    orderBy = "f.arr_time ASC";
                }
                else {
                    orderBy = "f.arr_time DESC";
                }

                String dateFilter;
                if (flexible) {
                    dateFilter = "fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 0 DAY)) OR fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 1 DAY)) OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 1 DAY)) OR fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 2 DAY)) OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 2 DAY)) OR fd.day_of_week = DAYNAME(DATE_SUB('" + date + "', INTERVAL 3 DAY)) OR fd.day_of_week = DAYNAME(DATE_ADD('" + date + "', INTERVAL 3 DAY))";
                }
                else {
                    dateFilter = "fd.day_of_week = DAYNAME('" + date + "')";
                }

                String priceFilter = "";
                String maxPrice = maxPriceField.getText().trim();
                if (!maxPrice.isEmpty()){
                    priceFilter = " AND f.base_economy_fare <= " + maxPrice;
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
                String query = "SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, " + "f.dep_time, f.arr_time, " + "TIMEDIFF(f.arr_time, f.dep_time) AS duration, " + "f.base_economy_fare, f.base_business_fare, f.base_first_fare, " + " f.stops AS stops " + "FROM Flight f " + "JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " + " WHERE f.dep_airport_id='" + from + "' AND f.arr_airport_id='" + to + "' " + "  AND (" + dateFilter + ") " + priceFilter + airlineFilter + stopsFilter + "ORDER BY " + orderBy;
                try {
                    ResultSet resultSet = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (resultSet.next()) {
                        tableModel.addRow(new Object[]{resultSet.getString("flight_no"), resultSet.getString("airline_id"), resultSet.getString("dep_airport_id"), resultSet.getString("arr_airport_id"), resultSet.getString("dep_time"), resultSet.getString("arr_time"), resultSet.getString("duration"), "$" + resultSet.getString("base_economy_fare"), "$" + resultSet.getString("base_business_fare"), "$" + resultSet.getString("base_first_fare"), resultSet.getString("stops")});                        count++;
                    }
                    if (count == 0) {
                        messageLabel.setText("No flights found.");
                    }
                    else {
                        messageLabel.setText(count + " flight(s) found.");
                    }
                } catch (SQLException ex) {
                    messageLabel.setText("Query error: " + ex.getMessage());
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
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(messageLabel, BorderLayout.WEST);
        bottomPanel.add(bookButton, BorderLayout.EAST);
        JPanel northWrapper = new JPanel(new GridLayout(2, 1));
        northWrapper.add(searchControlsPanel);
        northWrapper.add(filterPanel);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(northWrapper, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void doBooking(String flightNo, String airlineId, String depDate, String tripType, String returnDate, JLabel messageLabel) {
        JComboBox<String> classCombo = new JComboBox<>(new String[]{"economy", "business", "first"});
        JTextField seatField = new JTextField(6);
        JTextField mealField = new JTextField(10);

        Object[] fields = {"Class:", classCombo, "Seat No:", seatField, "Meal Pref:", mealField};
        int dialogResult = JOptionPane.showConfirmDialog(this, fields, "Book Flight " + airlineId + flightNo + " on " + depDate, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResult != JOptionPane.OK_OPTION) {
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
            String availabilityQuery = "SELECT ac." + selectedClass + "_seats - COUNT(tf.ticket_no) AS available " + "FROM Flight f " + "JOIN Aircraft ac ON f.aircraft_id = ac.aircraft_id " + "LEFT JOIN Ticket_Flight tf ON tf.flight_no=f.flight_no " + "  AND tf.airline_id=f.airline_id AND tf.dep_date='" + depDate + "' AND tf.class='" + selectedClass + "' " + "WHERE f.flight_no='" + flightNo + "' AND f.airline_id='" + airlineId + "' " + "GROUP BY ac." + selectedClass + "_seats";
            ResultSet availabilityResult = DBConnection.getStatement().executeQuery(availabilityQuery);
            if (availabilityResult.next() && availabilityResult.getInt("available") <= 0) {
                int joinWaitlist = JOptionPane.showConfirmDialog(this, "This flight is full. Join the waiting list?", "Flight Full", JOptionPane.YES_NO_OPTION);
                if (joinWaitlist == JOptionPane.YES_OPTION) {
                    joinWaitlist(flightNo, airlineId, depDate, selectedClass, messageLabel);
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

            String insertTicketSQL = "INSERT INTO Ticket (cust_id, total_fare, booking_fee, trip_type, is_flexible, purchase_date, status) " + "VALUES (" + customerId + "," + fare + "," + bookingFee + ",'" + ticketType + "',0,NOW(),'active')";
            DBConnection.getStatement().executeUpdate(insertTicketSQL);
            ResultSet keyResult = DBConnection.getStatement().executeQuery("SELECT LAST_INSERT_ID() AS id");
            int ticketNo = 0;
            if (keyResult.next()){
                ticketNo = keyResult.getInt("id");
            }

            String insertLegSQL = "INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) " + "VALUES (" + ticketNo + ",1,'" + flightNo + "','" + airlineId + "','" + depDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')";
            DBConnection.getStatement().executeUpdate(insertLegSQL);

            if (ticketType.equals("round_trip") && !returnDate.isEmpty()) {
                String insertReturnLegSQL = "INSERT INTO Ticket_Flight (ticket_no, leg_order, flight_no, airline_id, dep_date, seat_no, class, meal_pref) " + "VALUES (" + ticketNo + ",2,'" + flightNo + "','" + airlineId + "','" + returnDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')";
                DBConnection.getStatement().executeUpdate(insertReturnLegSQL);
            }
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Booked! Ticket #" + ticketNo + "  |  " + selectedClass + "  |  $" + (fare + bookingFee));

        }
        catch (SQLException ex) {
            messageLabel.setText("Booking error: " + ex.getMessage());
        }
    }

    private void joinWaitlist(String flightNo, String airlineId, String depDate,
                              String selectedClass, JLabel messageLabel) {
        try {
            ResultSet positionResult = DBConnection.getStatement().executeQuery("SELECT COALESCE(MAX(position),0)+1 AS pos FROM Waitlist " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "' AND dep_date='" + depDate + "'");
            int nextPosition = 1;
            if (positionResult.next()){
                nextPosition = positionResult.getInt("pos");
            }

            DBConnection.getStatement().executeUpdate("INSERT INTO Waitlist (cust_id, flight_no, airline_id, dep_date, position, join_date, notified) " + "VALUES (" + customerId + ",'" + flightNo + "','" + airlineId + "','" + depDate + "'," + nextPosition + ",NOW(),0)");
            messageLabel.setForeground(new Color(0, 100, 180));
            messageLabel.setText("Added to waitlist at position " + nextPosition + ".");
        }
        catch (SQLException ex) {
            messageLabel.setText("Waitlist error: " + ex.getMessage());
        }
    }

    private JPanel buildUpcomingTab() {
        final String[] columns = {"Ticket #", "Flight", "Airline", "From", "To", "Date", "Class", "Seat", "Status"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable tripsTable = new JTable(tableModel);
        tripsTable.setRowHeight(22);
        tripsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel messageLabel = new JLabel(" ");

        JButton refreshButton = new JButton("Refresh");
        JButton cancelButton  = new JButton("Cancel Reservation");

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTrips(tableModel, messageLabel, true);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = tripsTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a reservation to cancel.");
                    return;
                }
                int ticketNo = (int) tableModel.getValueAt(selectedRow, 0);
                String selectedClass = (String) tableModel.getValueAt(selectedRow, 6);
                doCancelReservation(ticketNo, selectedClass, tableModel, messageLabel);
            }
        });

        loadTrips(tableModel, messageLabel, true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(cancelButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(messageLabel, BorderLayout.WEST);
        southPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(tripsTable), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPastTab() {
        final String[] columns = {"Ticket #", "Flight", "Airline", "From", "To", "Date", "Class", "Seat", "Total Fare", "Status"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        final JTable tripsTable = new JTable(tableModel);
        tripsTable.setRowHeight(22);

        final JLabel messageLabel = new JLabel(" ");

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTrips(tableModel, messageLabel, false);
            }
        });

        loadTrips(tableModel, messageLabel, false);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(messageLabel, BorderLayout.WEST);
        southPanel.add(refreshButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(tripsTable), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void loadTrips(DefaultTableModel tableModel, JLabel messageLabel, boolean upcoming) {
        tableModel.setRowCount(0);
        String dateOperator;
        if (upcoming) {
            dateOperator = ">= CURDATE()";
        }
        else {
            dateOperator = "< CURDATE()";
        }
        String query = "SELECT t.ticket_no, tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, " + "       tf.dep_date, tf.class, tf.seat_no, t.total_fare, t.status " + "FROM Ticket t " + "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no " + "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " + "WHERE t.cust_id=" + customerId + "  AND t.status != 'cancelled' " + "  AND tf.dep_date " + dateOperator + " ORDER BY tf.dep_date ASC, tf.leg_order ASC";
        try {
            ResultSet resultSet = DBConnection.getStatement().executeQuery(query);
            int count = 0;
            while (resultSet.next()) {
                tableModel.addRow(new Object[]{resultSet.getInt("ticket_no"), resultSet.getString("flight_no"), resultSet.getString("airline_id"), resultSet.getString("dep_airport_id"), resultSet.getString("arr_airport_id"), resultSet.getString("dep_date"), resultSet.getString("class"), resultSet.getString("seat_no"), "$" + resultSet.getString("total_fare"), resultSet.getString("status")});
                count++;
            }
            messageLabel.setText(count + " reservation(s) found.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void doCancelReservation(int ticketNo, String selectedClass, DefaultTableModel tableModel, JLabel messageLabel) {
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

            ResultSet waitlistResult = DBConnection.getStatement().executeQuery("SELECT w.cust_id, c.name FROM Waitlist w " + "JOIN Ticket_Flight tf ON tf.ticket_no=" + ticketNo + " JOIN Customer c ON w.cust_id=c.cust_id " + "WHERE w.flight_no=tf.flight_no AND w.airline_id=tf.airline_id " + "  AND w.dep_date=tf.dep_date AND w.notified=0 " + "ORDER BY w.position ASC LIMIT 1");
            if (waitlistResult.next()) {
                int waitlistCustomerId = waitlistResult.getInt("cust_id");
                String waitlistName = waitlistResult.getString("name");
                DBConnection.getStatement().executeUpdate("UPDATE Waitlist SET notified=1 WHERE cust_id=" + waitlistCustomerId + " AND flight_no=(SELECT flight_no FROM Ticket_Flight WHERE ticket_no=" + ticketNo + " LIMIT 1)");
                JOptionPane.showMessageDialog(this, "Seat opened. Waitlist customer notified: " + waitlistName, "Waitlist Alert", JOptionPane.INFORMATION_MESSAGE);
            }
            messageLabel.setForeground(new Color(0, 120, 0));
            messageLabel.setText("Ticket #" + ticketNo + " cancelled.");
            loadTrips(tableModel, messageLabel, true);
        }
        catch (SQLException ex) {
            messageLabel.setText("Cancel error: " + ex.getMessage());
        }
    }

    private JPanel buildQuestionsTab() {
        final JTextArea  questionArea = new JTextArea(4, 40);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

        final JLabel postMessageLabel = new JLabel(" ");
        JButton postButton = getJButton(questionArea, postMessageLabel);
        JPanel inputFields = new JPanel(new GridLayout(1, 2, 5, 5));
        inputFields.add(new JLabel("Question:"));
        inputFields.add(new JScrollPane(questionArea));

        JPanel postBottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        postBottomPanel.add(postMessageLabel);
        postBottomPanel.add(postButton);

        JPanel postPanel = new JPanel(new BorderLayout(5, 5));
        postPanel.setBorder(BorderFactory.createTitledBorder("Post a New Question"));
        postPanel.add(inputFields, BorderLayout.CENTER);
        postPanel.add(postBottomPanel, BorderLayout.SOUTH);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(postPanel, BorderLayout.NORTH);
        return panel;
    }

    private JButton getJButton(JTextArea questionArea, JLabel postMessageLabel) {
        JButton postButton = new JButton("Post Question");

        postButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String questionText = questionArea.getText().trim();
                if (questionText.isEmpty()) {
                    postMessageLabel.setText("Please enter a question.");
                    return; }
                try {
                    DBConnection.getStatement().executeUpdate("INSERT INTO Customer_Question (cust_id, question_text, asked_at) " + "VALUES (" + customerId + ",'" + questionText.replace("'", "''") + "',NOW())");
                    postMessageLabel.setForeground(new Color(0, 120, 0));
                    postMessageLabel.setText("Question posted!");
                    questionArea.setText("");
                }
                catch (SQLException ex) {
                    postMessageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
        return postButton;
    }
}