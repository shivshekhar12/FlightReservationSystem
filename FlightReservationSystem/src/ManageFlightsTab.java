import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageFlightsTab {

    public static JPanel build(JFrame parentFrame) {
        String[] flightColumnNames = {"Flight No", "Airline", "Aircraft", "From", "To", "Depart", "Arrive", "Intl", "Economy $", "Business $", "First $", "Stops"};
        DefaultTableModel flightTableModel = new DefaultTableModel(flightColumnNames, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable flightsTable = new JTable(flightTableModel);
        flightsTable.setRowHeight(22);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load All Flights");
        JButton addButton = new JButton("Add Flight");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setForeground(Color.RED);

        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadAllFlights(flightTableModel, statusLabel);
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showFlightDialog(parentFrame, null, null, flightTableModel, statusLabel);
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = flightsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a flight to edit.");
                    return;
                }
                String flightNo = (String) flightTableModel.getValueAt(row, 0);
                String airlineId = (String) flightTableModel.getValueAt(row, 1);
                showFlightDialog(parentFrame, flightNo, airlineId, flightTableModel, statusLabel);
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = flightsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a flight to delete.");
                    return;
                }
                String flightNo  = (String) flightTableModel.getValueAt(row, 0);
                String airlineId = (String) flightTableModel.getValueAt(row, 1);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete flight " + airlineId + flightNo + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("DELETE FROM Flight_Day WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                    DBConnection.getStatement().executeUpdate("DELETE FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Flight deleted.");
                    loadAllFlights(flightTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        loadAllFlights(flightTableModel, statusLabel);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        actionButtonPanel.add(loadButton); actionButtonPanel.add(addButton);
        actionButtonPanel.add(editButton); actionButtonPanel.add(deleteButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(actionButtonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    static void loadAllFlights(DefaultTableModel model, JLabel status) {
        model.setRowCount(0);
        try {
            ResultSet flightData = DBConnection.getStatement().executeQuery("SELECT flight_no, airline_id, aircraft_id, dep_airport_id, arr_airport_id, " + "dep_time, arr_time, is_international, " + "base_economy_fare, base_business_fare, base_first_fare, stops FROM Flight ORDER BY airline_id, flight_no");
            while (flightData.next()) {
                String isIntl;
                if (flightData.getBoolean("is_international")) {
                    isIntl = "Yes";
                }
                else {
                    isIntl = "No";
                }
                model.addRow(new Object[]{flightData.getString("flight_no"),
                        flightData.getString("airline_id"),
                        flightData.getInt("aircraft_id"),
                        flightData.getString("dep_airport_id"),
                        flightData.getString("arr_airport_id"),
                        flightData.getString("dep_time"),
                        flightData.getString("arr_time"), isIntl, "$" + flightData.getString("base_economy_fare"), "$" + flightData.getString("base_business_fare"), "$" + flightData.getString("base_first_fare"),
                        flightData.getInt("stops")});
            }
            status.setText(model.getRowCount() + " flight(s) loaded.");
        }
        catch (SQLException ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }

    static void showFlightDialog(JFrame parentFrame, String flightNo, String airlineId, DefaultTableModel model, JLabel status) {
        boolean isEdit = (flightNo != null);

        String initFlightNo;
        if (flightNo != null) {
            initFlightNo = flightNo;
        }
        else {
            initFlightNo = "";
        }
        String initAirline;
        if (airlineId != null) {
            initAirline = airlineId;
        }
        else {
            initAirline = "";
        }

        JTextField flightNoField = new JTextField(initFlightNo, 8);
        JTextField airlineField = new JTextField(initAirline, 4);
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
                ResultSet flightData = DBConnection.getStatement().executeQuery("SELECT * FROM Flight WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (flightData.next()) {
                    aircraftField.setText(String.valueOf(flightData.getInt("aircraft_id")));
                    fromField.setText(flightData.getString("dep_airport_id"));
                    toField.setText(flightData.getString("arr_airport_id"));
                    depTimeField.setText(flightData.getString("dep_time"));
                    arrTimeField.setText(flightData.getString("arr_time"));
                    intlCheckBox.setSelected(flightData.getBoolean("is_international"));
                    ecoFareField.setText(flightData.getString("base_economy_fare"));
                    bizFareField.setText(flightData.getString("base_business_fare"));
                    fstFareField.setText(flightData.getString("base_first_fare"));
                    stopsField.setText(String.valueOf(flightData.getInt("stops")));
                }
                ResultSet flightDaysResult = DBConnection.getStatement().executeQuery("SELECT GROUP_CONCAT(day_of_week) AS days FROM Flight_Day " + "WHERE flight_no='" + flightNo + "' AND airline_id='" + airlineId + "'");
                if (flightDaysResult.next()) {
                    daysField.setText(flightDaysResult.getString("days"));
                }
            }
            catch (SQLException ex) {
                status.setText("Error loading flight: " + ex.getMessage());
                return;
            }
        }

        Object[] flightDialogComponents = {"Flight No:", flightNoField,
                "Airline ID:", airlineField,
                "Aircraft ID:", aircraftField,
                "From Airport:", fromField,
                "To Airport:", toField,
                "Dep Time (HH:MM):", depTimeField,
                "Arr Time (HH:MM):", arrTimeField, intlCheckBox,
                "Economy Fare:", ecoFareField,
                "Business Fare:", bizFareField,
                "First Fare:", fstFareField,
                "Stops:", stopsField,
                "Days (comma-sep, e.g. Mon,Wed,Fri):", daysField
        };

        String title;
        if (isEdit) {
            title = "Edit Flight";
        }
        else {
            title = "Add Flight";
        }

        int dialogResponse = JOptionPane.showConfirmDialog(parentFrame, flightDialogComponents, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (dialogResponse != JOptionPane.OK_OPTION) {
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
                DBConnection.getStatement().executeUpdate("UPDATE Flight SET aircraft_id=" + ac + ", dep_airport_id='" + from + "', arr_airport_id='" + to + "', dep_time='" + dep + "', arr_time='"
                        + arr + "', is_international=" + intl + ", base_economy_fare=" + eco + ", base_business_fare=" + biz + ", base_first_fare=" + fst + ", stops=" + stops + " WHERE flight_no='" + fn + "' AND airline_id='" + al + "'");
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
            status.setForeground(new Color(0, 120, 0));
            if (isEdit) {
                status.setText("Flight updated.");
            }
            else {
                status.setText("Flight added.");
            }
            loadAllFlights(model, status);
        }
        catch (SQLException ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }
}