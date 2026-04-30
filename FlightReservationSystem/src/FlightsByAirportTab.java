import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class FlightsByAirportTab {

    public static JPanel build() {
        JTextField airportField = new JTextField(6);
        JComboBox<String> directionCombo = new JComboBox<>(new String[]{"Departing & Arriving", "Departing Only", "Arriving Only"});
        String[] columns = {"Direction", "Flight", "Airline", "From", "To", "Depart", "Arrive", "Days"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable flightsTable = new JTable(tableModel);
        flightsTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton loadButton = new JButton("Load Flights");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String airportId = airportField.getText().trim().toUpperCase();
                int direction = directionCombo.getSelectedIndex();
                if (airportId.isEmpty()) {
                    messageLabel.setText("Enter an airport ID.");
                    return;
                }
                try {
                    if (direction == 0 || direction == 1) {
                        ResultSet rs = DBConnection.getStatement().executeQuery("SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, " + "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " + "FROM Flight f " + "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " + "WHERE f.dep_airport_id='" + airportId + "' GROUP BY f.flight_no, f.airline_id");
                        while (rs.next()) {
                            tableModel.addRow(new Object[]{"Departing", rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_airport_id"), rs.getString("arr_airport_id"), rs.getString("dep_time"), rs.getString("arr_time"), rs.getString("days")});
                        }
                    }
                    if (direction == 0 || direction == 2) {
                        ResultSet rs = DBConnection.getStatement().executeQuery("SELECT f.flight_no, f.airline_id, f.dep_airport_id, f.arr_airport_id, f.dep_time, f.arr_time, " + "GROUP_CONCAT(fd.day_of_week ORDER BY fd.day_of_week) AS days " + "FROM Flight f " + "LEFT JOIN Flight_Day fd ON f.flight_no=fd.flight_no AND f.airline_id=fd.airline_id " + "WHERE f.arr_airport_id='" + airportId + "' GROUP BY f.flight_no, f.airline_id");
                        while (rs.next()) {
                            tableModel.addRow(new Object[]{"Arriving", rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_airport_id"), rs.getString("arr_airport_id"), rs.getString("dep_time"), rs.getString("arr_time"), rs.getString("days")});
                        }
                    }
                    messageLabel.setText(tableModel.getRowCount() + " flight(s) found for " + airportId + ".");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Airport ID:"));
        topPanel.add(airportField);
        topPanel.add(directionCombo);
        topPanel.add(loadButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(flightsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}