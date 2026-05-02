import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ActiveFlightsTab {

    public static JPanel build() {
        JTextField limitField = new JTextField("10", 5);

        String[] flightTableHeaders = {"Rank", "Flight", "Airline", "From", "To", "Tickets Sold"};
        DefaultTableModel flightTableModel = new DefaultTableModel(flightTableHeaders, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable activeFlightsTable = new JTable(flightTableModel);
        activeFlightsTable.setRowHeight(22);
        JLabel statusLabel = new JLabel(" ");
        JButton fetchFlightsButton = new JButton("Load");
        fetchFlightsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flightTableModel.setRowCount(0);
                String limit = limitField.getText().trim();
                try {
                    Integer.parseInt(limit);
                }
                catch (NumberFormatException ex) {
                    limit = "10";
                }
                String query = "SELECT tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, "
                        + "COUNT(DISTINCT t.ticket_no) AS tickets_sold "
                        + "FROM Ticket_Flight tf "
                        + "JOIN Ticket t ON tf.ticket_no=t.ticket_no "
                        + "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id "
                        + "WHERE t.status != 'cancelled' "
                        + "GROUP BY tf.flight_no, tf.airline_id "
                        + "ORDER BY tickets_sold DESC "
                        + "LIMIT " + limit;
                try {
                    ResultSet flightsData = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (flightsData.next()) {
                        flightTableModel.addRow(new Object[]{
                                rank++,
                                flightsData.getString("flight_no"),
                                flightsData.getString("airline_id"),
                                flightsData.getString("dep_airport_id"),
                                flightsData.getString("arr_airport_id"),
                                flightsData.getInt("tickets_sold")
                        });
                    }
                    statusLabel.setText("Top " + (rank - 1) + " most active flight(s).");
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel flightOptionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        flightOptionsPanel.add(new JLabel("Show top:"));
        flightOptionsPanel.add(limitField);
        flightOptionsPanel.add(fetchFlightsButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(flightOptionsPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(activeFlightsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }
}
