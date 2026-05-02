import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;

public class BookForCustomerTab {

    public static JPanel build(JFrame parentFrame, int empId) {
        Map<String, Integer> customerMap = new HashMap<>();
        JTextField customerSearchField = new JTextField(20);
        JComboBox<String> customerCombo = new JComboBox<>();
        JButton findCustomerButton = buildCustomerSearchButton(customerCombo, customerMap, customerSearchField);
        JTextField fromField = new JTextField(6);
        JTextField toField = new JTextField(6);
        JTextField dateField = new JTextField(12);
        JComboBox<String> tripTypeCombo = new JComboBox<>(new String[]{"One-Way", "Round-Trip"});
        JTextField returnDateField = new JTextField(12);
        String[] flightColumns = {"Flight", "Airline", "From", "To", "Depart", "Arrive", "Economy $", "Business $", "First $"};
        DefaultTableModel flightsDataModel = new DefaultTableModel(flightColumns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable flightsTable = new JTable(flightsDataModel);
        flightsTable.setRowHeight(22);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");
        JButton searchButton = new JButton("Search Flights");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flightsDataModel.setRowCount(0);
                String from = fromField.getText().trim().toUpperCase();
                String to = toField.getText().trim().toUpperCase();
                String date = dateField.getText().trim();
                if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
                    statusLabel.setText("From, To and Date are required.");
                    return;
                }
                String query = "SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, " + "f.dep_time, f.arr_time, " + "f.base_economy_fare, f.base_business_fare, f.base_first_fare " + "FROM Flight f " + "JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " + "WHERE f.dep_airport_id='" + from + "' AND f.arr_airport_id='" + to + "' " + "AND fd.day_of_week = DAYNAME('" + date + "') " + "ORDER BY f.dep_time";
                try {
                    ResultSet flightData = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (flightData.next()) {
                        flightsDataModel.addRow(new Object[]{flightData.getString("flight_no"), flightData.getString("airline_id"), flightData.getString("dep_airport_id"), flightData.getString("arr_airport_id"), flightData.getString("dep_time"), flightData.getString("arr_time"), "$" + flightData.getString("base_economy_fare"), "$" + flightData.getString("base_business_fare"), "$" + flightData.getString("base_first_fare")});
                        count++;
                    }
                    if (count == 0) {
                        statusLabel.setText("No flights found.");
                    }
                    else {
                        statusLabel.setText(count + " flight(s) found.");
                    }
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JButton bookButton = new JButton("Book Selected for Customer");
        bookButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = flightsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a flight first.");
                    return;
                }
                String selectedCustomer = (String) customerCombo.getSelectedItem();
                if (selectedCustomer == null || !customerMap.containsKey(selectedCustomer)) {
                    statusLabel.setText("Select a valid customer first.");
                    return;
                }
                int custId = customerMap.get(selectedCustomer);
                String flightNo = (String) flightsDataModel.getValueAt(row, 0);
                String airlineId = (String) flightsDataModel.getValueAt(row, 1);
                String date = dateField.getText().trim();
                String tripType = tripTypeCombo.getSelectedItem().toString();
                String returnDate = returnDateField.getText().trim();
                doRepBooking(parentFrame, empId, custId, flightNo, airlineId, date, tripType, returnDate, statusLabel);
            }
        });

        JPanel customerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        customerPanel.setBorder(BorderFactory.createTitledBorder("Step 1 - Select Customer"));
        customerPanel.add(new JLabel("Search name:"));
        customerPanel.add(customerSearchField);
        customerPanel.add(findCustomerButton);
        customerPanel.add(new JLabel("Customer:"));
        customerPanel.add(customerCombo);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Step 2 - Search Flights"));
        searchPanel.add(new JLabel("From:"));
        searchPanel.add(fromField);
        searchPanel.add(new JLabel("To:"));
        searchPanel.add(toField);
        searchPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        searchPanel.add(dateField);
        searchPanel.add(tripTypeCombo);
        searchPanel.add(new JLabel("Return:"));
        searchPanel.add(returnDateField);
        searchPanel.add(searchButton);
        JPanel bookPanel = new JPanel(new GridLayout(2, 1));
        bookPanel.add(customerPanel);
        bookPanel.add(searchPanel);
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.add(statusLabel, BorderLayout.WEST);
        southPanel.add(bookButton, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(bookPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    private static JButton buildCustomerSearchButton(JComboBox<String> customerCombo, Map<String, Integer> customerMap, JTextField customerSearchField) {
        JButton findCustomerButton = new JButton("Find Customer");
        findCustomerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customerCombo.removeAllItems();
                customerMap.clear();
                String searchText = customerSearchField.getText().trim();
                try {
                    ResultSet customerResults = DBConnection.getStatement().executeQuery("SELECT cust_id, name, email FROM Customer WHERE name LIKE '%" + searchText + "%' ORDER BY name");
                    while (customerResults.next()) {
                        String customerLabel = customerResults.getString("name") + " (" + customerResults.getString("email") + ")";
                        customerMap.put(customerLabel, customerResults.getInt("cust_id"));
                        customerCombo.addItem(customerLabel);
                    }
                    if (customerCombo.getItemCount() == 0) {
                        customerCombo.addItem("No customers found");
                    }
                }
                catch (SQLException ex) {
                    customerCombo.addItem("Error: " + ex.getMessage());
                }
            }
        });
        return findCustomerButton;
    }

    private static void doRepBooking(JFrame parentFrame, int empId, int custId, String flightNo, String airlineId, String depDate, String tripType, String returnDate, JLabel statusLabel) {
        JComboBox<String> classCombo = new JComboBox<>(new String[]{"economy", "business", "first"});
        JTextField seatField = new JTextField(6);
        JTextField mealField = new JTextField(10);

        Object[] bookingFields = {"Class:", classCombo, "Seat No:", seatField, "Meal Pref:", mealField};
        int confirmDialog = JOptionPane.showConfirmDialog(parentFrame, bookingFields, "Book " + airlineId + flightNo + " for customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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
            String fareColumn = "base_" + selectedClass + "_fare";
            ResultSet fareResult = DBConnection.getStatement().executeQuery("SELECT " + fareColumn + ", booking_fee FROM Flight " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
            double fare = 0;
            double bookingFee = 25;
            if (fareResult.next()) {
                fare = fareResult.getDouble(fareColumn);
                bookingFee = fareResult.getDouble("booking_fee");
            }

            DBConnection.getStatement().executeUpdate("INSERT INTO Ticket (cust_id, handled_by, total_fare, booking_fee, trip_type, is_flexible, purchase_date, status) " + "VALUES (" + custId + "," + empId + "," + fare + "," + bookingFee + ",'" + ticketType + "',0,NOW(),'active')");
            ResultSet keyResult = DBConnection.getStatement().executeQuery("SELECT LAST_INSERT_ID() AS id");
            int ticketNo = 0;
            if (keyResult.next()) {
                ticketNo = keyResult.getInt("id");
            }

            DBConnection.getStatement().executeUpdate("INSERT INTO Ticket_Flight (ticket_no,leg_order,flight_no,airline_id,dep_date,seat_no,class,meal_pref) "
                    + "VALUES (" + ticketNo + ",1,'" + flightNo + "','" + airlineId + "','" + depDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')");

            if (ticketType.equals("round_trip") && !returnDate.isEmpty()) {
                DBConnection.getStatement().executeUpdate("INSERT INTO Ticket_Flight (ticket_no,leg_order,flight_no,airline_id,dep_date,seat_no,class,meal_pref) "
                        + "VALUES (" + ticketNo + ",2,'" + flightNo + "','" + airlineId + "','" + returnDate + "','" + seatNumber + "','" + selectedClass + "','" + mealPref + "')");
            }
            statusLabel.setForeground(new Color(0, 120, 0));
            statusLabel.setText("Booked! Ticket #" + ticketNo + " | " + selectedClass + " | $" + (fare + bookingFee));
        }
        catch (SQLException ex) {
            statusLabel.setText("Booking error: " + ex.getMessage());
        }
    }
}