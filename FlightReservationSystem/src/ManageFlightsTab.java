import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageFlightsTab {

    public static JPanel build(JFrame parentFrame) {
        String[] columns = {"Flight No", "Airline", "Aircraft", "From", "To", "Depart", "Arrive", "Intl", "Economy $", "Business $", "First $", "Stops"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable flightsTable = new JTable(tableModel);
        flightsTable.setRowHeight(22);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load All Flights");
        JButton addButton = new JButton("Add Flight");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setForeground(Color.RED);

        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadAllFlights(tableModel, messageLabel);
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showFlightDialog(parentFrame, null, null, tableModel, messageLabel);
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = flightsTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a flight to edit.");
                    return;
                }
                String flightNo = (String) tableModel.getValueAt(selectedRow, 0);
                String airlineId = (String) tableModel.getValueAt(selectedRow, 1);
                showFlightDialog(parentFrame, flightNo, airlineId, tableModel, messageLabel);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = flightsTable.getSelectedRow();
                if (selectedRow < 0) {
                    messageLabel.setText("Select a flight to delete.");
                    return;
                }
                String flightNo  = (String) tableModel.getValueAt(selectedRow, 0);
                String airlineId = (String) tableModel.getValueAt(selectedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete flight " + airlineId + flightNo + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("DELETE FROM Flight_Day WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                    DBConnection.getStatement().executeUpdate("DELETE FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Flight deleted.");
                    loadAllFlights(tableModel, messageLabel);
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        loadAllFlights(tableModel, messageLabel);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(loadButton); topPanel.add(addButton);
        topPanel.add(editButton); topPanel.add(deleteButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    static void loadAllFlights(DefaultTableModel tableModel, JLabel messageLabel) {
        tableModel.setRowCount(0);
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery("SELECT flight_no, airline_id, aircraft_id, dep_airport_id, arr_airport_id, " + "dep_time, arr_time, is_international, " + "base_economy_fare, base_business_fare, base_first_fare, stops FROM Flight ORDER BY airline_id, flight_no");
            while (rs.next()) {
                String isIntl;
                if (rs.getBoolean("is_international")) {
                    isIntl = "Yes";
                }
                else {
                    isIntl = "No";
                }
                tableModel.addRow(new Object[]{rs.getString("flight_no"), rs.getString("airline_id"), rs.getInt("aircraft_id"), rs.getString("dep_airport_id"), rs.getString("arr_airport_id"), rs.getString("dep_time"), rs.getString("arr_time"), isIntl, "$" + rs.getString("base_economy_fare"), "$" + rs.getString("base_business_fare"), "$" + rs.getString("base_first_fare"), rs.getInt("stops")});
            }
            messageLabel.setText(tableModel.getRowCount() + " flight(s) loaded.");
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }

    static void showFlightDialog(JFrame parentFrame, String flightNo, String airlineId, DefaultTableModel tableModel, JLabel messageLabel) {
        boolean isEdit = (flightNo != null);

        String flightNoValue;
        if (flightNo != null) {
            flightNoValue = flightNo;
        }
        else {
            flightNoValue = "";
        }
        String airlineValue;
        if (airlineId != null) {
            airlineValue = airlineId;
        }
        else {
            airlineValue = "";
        }

        JTextField flightNoField = new JTextField(flightNoValue, 8);
        JTextField airlineField = new JTextField(airlineValue, 4);
        JTextField aircraftField = new JTextField(8);
        JTextField fromField = new JTextField(4);
        JTextField toField = new JTextField(4);
        JTextField depTimeField = new JTextField(8);
        JTextField arrTimeField = new JTextField(8);
        JCheckBox intlCheckBox = new JCheckBox("International");
        JTextField ecoFareField = new JTextField(8);
        JTextField bizFareField = new JTextField(8);
        JTextField fstFareField = new JTextField(8);
        JTextField stopsField = new JTextField("0", 4);
        JTextField daysField = new JTextField("Mon,Tue,Wed,Thu,Fri", 20);

        if (isEdit) {
            flightNoField.setEditable(false);
            airlineField.setEditable(false);
            try {
                ResultSet rs = DBConnection.getStatement().executeQuery("SELECT * FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (rs.next()) {
                    aircraftField.setText(String.valueOf(rs.getInt("aircraft_id")));
                    fromField.setText(rs.getString("dep_airport_id"));
                    toField.setText(rs.getString("arr_airport_id"));
                    depTimeField.setText(rs.getString("dep_time"));
                    arrTimeField.setText(rs.getString("arr_time"));
                    intlCheckBox.setSelected(rs.getBoolean("is_international"));
                    ecoFareField.setText(rs.getString("base_economy_fare"));
                    bizFareField.setText(rs.getString("base_business_fare"));
                    fstFareField.setText(rs.getString("base_first_fare"));
                    stopsField.setText(String.valueOf(rs.getInt("stops")));
                }
                ResultSet daysResult = DBConnection.getStatement().executeQuery("SELECT GROUP_CONCAT(day_of_week) AS days FROM Flight_Day " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (daysResult.next()) {
                    daysField.setText(daysResult.getString("days"));
                }
            }
            catch (SQLException ex) {
                messageLabel.setText("Error loading flight: " + ex.getMessage());
                return;
            }
        }

        Object[] fields = {"Flight No:", flightNoField, "Airline ID:", airlineField, "Aircraft ID:", aircraftField, "From Airport:", fromField, "To Airport:", toField, "Dep Time (HH:MM):", depTimeField, "Arr Time (HH:MM):", arrTimeField, intlCheckBox, "Economy Fare:", ecoFareField, "Business Fare:", bizFareField, "First Fare:", fstFareField, "Stops:", stopsField, "Days (comma-sep, e.g. Mon,Wed,Fri):", daysField
        };

        String dialogTitle;
        if (isEdit) {
            dialogTitle = "Edit Flight";
        }
        else {
            dialogTitle = "Add Flight";
        }

        int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, dialogTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResult != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String fn = flightNoField.getText().trim();
            String al = airlineField.getText().trim().toUpperCase();
            String ac = aircraftField.getText().trim();
            String from = fromField.getText().trim().toUpperCase();
            String to = toField.getText().trim().toUpperCase();
            String dep = depTimeField.getText().trim();
            String arr = arrTimeField.getText().trim();
            boolean intl = intlCheckBox.isSelected();
            String eco = ecoFareField.getText().trim();
            String biz = bizFareField.getText().trim();
            String fst = fstFareField.getText().trim();
            String stops = stopsField.getText().trim();
            String[] days = daysField.getText().trim().split(",");

            if (isEdit) {
                DBConnection.getStatement().executeUpdate("UPDATE Flight SET aircraft_id=" + ac + ", dep_airport_id='" + from + "', arr_airport_id='" + to + "', dep_time='" + dep + "', arr_time='" + arr + "', is_international=" + intl + ", base_economy_fare=" + eco + ", base_business_fare=" + biz + ", base_first_fare=" + fst + ", stops=" + stops + " WHERE flight_no='" + fn + "' AND airline_id='" + al + "'");
                DBConnection.getStatement().executeUpdate("DELETE FROM Flight_Day WHERE flight_no='" + fn + "' AND airline_id='" + al + "'");
            }
            else {
                DBConnection.getStatement().executeUpdate("INSERT INTO Flight (flight_no,airline_id,aircraft_id,dep_airport_id,arr_airport_id,dep_time,arr_time,is_international,base_economy_fare,base_business_fare,base_first_fare,stops) " + "VALUES ('" + fn + "','" + al + "'," + ac + ",'" + from + "','" + to + "','" + dep + "','" + arr + "'," + intl + "," + eco + "," + biz + "," + fst + "," + stops + ")");
            }
            for (String day : days) {
                String d = day.trim();
                if (!d.isEmpty()) {
                    DBConnection.getStatement().executeUpdate("INSERT INTO Flight_Day (flight_no,airline_id,day_of_week) VALUES ('" + fn + "','" + al + "','" + d + "')");
                }
            }
            messageLabel.setForeground(new Color(0, 120, 0));
            if (isEdit) {
                messageLabel.setText("Flight updated.");
            }
            else {
                messageLabel.setText("Flight added.");
            }
            loadAllFlights(tableModel, messageLabel);
        }
        catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }
}