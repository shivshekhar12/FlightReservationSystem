import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class FlightsByAirportTab {

    public static JPanel build() {
        JTextField airportField = new JTextField(6);
        JComboBox<String> directionCombo = new JComboBox<>(new String[]{"Departing & Arriving", "Departing Only", "Arriving Only"});
        String[] flightTableColumns = {"Direction", "Flight", "Airline", "From", "To", "Depart", "Arrive", "Days"};
        DefaultTableModel flightTableModel = new DefaultTableModel(flightTableColumns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable flightsTable = new JTable(flightTableModel);
        flightsTable.setRowHeight(22);
        JLabel statusLabel = new JLabel(" ");
        JButton loadButton = new JButton("Load Flights");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flightTableModel.setRowCount(0);
                String airportId = airportField.getText().trim().toUpperCase();
                int direction = directionCombo.getSelectedIndex();
                if (airportId.isEmpty()) {
                    statusLabel.setText("Enter an airport ID.");
                    return;
                }
                try {
                    if (direction == 0 || direction == 1) {
                        ResultSet departingResults = DBConnection.getStatement().executeQuery("SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, "
                                + "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " + "FROM Flight f " + "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id "
                                + "WHERE f.dep_airport_id='" + airportId + "' GROUP BY f.flight_no, f.airline_id");
                        while (departingResults.next()) {
                            flightTableModel.addRow(new Object[]{"Departing", departingResults.getString("flight_no"),
                                    departingResults.getString("airline_id"),
                                    departingResults.getString("dep_airport_id"),
                                    departingResults.getString("arr_airport_id"),
                                    departingResults.getString("dep_time"),
                                    departingResults.getString("arr_time"),
                                    departingResults.getString("days")});
                        }
                    }
                    if (direction == 0 || direction == 2) {
                        ResultSet arrivingResults = DBConnection.getStatement().executeQuery("SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, "
                                + "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " + "FROM Flight f " + "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id "
                                + "WHERE f.arr_airport_id='" + airportId + "' GROUP BY f.flight_no, f.airline_id");
                        while (arrivingResults.next()) {
                            flightTableModel.addRow(new Object[]{"Arriving", arrivingResults.getString("flight_no"),
                                    arrivingResults.getString("airline_id"),
                                    arrivingResults.getString("dep_airport_id"),
                                    arrivingResults.getString("arr_airport_id"),
                                    arrivingResults.getString("dep_time"), arrivingResults.getString("arr_time"),
                                    arrivingResults.getString("days")});
                        }
                    }
                    statusLabel.setText(flightTableModel.getRowCount() + " flight(s) found for " + airportId + ".");
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel airportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        airportPanel.add(new JLabel("Airport ID:"));
        airportPanel.add(airportField);
        airportPanel.add(directionCombo);
        airportPanel.add(loadButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(airportPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }
}