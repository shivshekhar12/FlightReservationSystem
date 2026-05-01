import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ActiveFlightsTab {

    public static JPanel build() {
        JTextField limitField = new JTextField("10", 5);

        String[] columns = {"Rank", "Flight", "Airline", "From", "To", "Tickets Sold"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable activeFlightsTable = new JTable(tableModel);
        activeFlightsTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String limit = limitField.getText().trim();
                try {
                    Integer.parseInt(limit);
                }
                catch (NumberFormatException ex) {
                    limit = "10";
                }
                String query = "SELECT tf.flight_no, tf.airline_id, f.dep_airport_id, f.arr_airport_id, " + "COUNT(DISTINCT t.ticket_no) AS tickets_sold " + "FROM Ticket_Flight tf " + "JOIN Ticket t ON tf.ticket_no=t.ticket_no " + "JOIN Flight f ON tf.flight_no=f.flight_no AND tf.airline_id=f.airline_id " + "WHERE t.status != 'cancelled' " + "GROUP BY tf.flight_no, tf.airline_id " + "ORDER BY tickets_sold DESC " + "LIMIT " + limit;
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rank++, rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_airport_id"), rs.getString("arr_airport_id"), rs.getInt("tickets_sold")});
                    }
                    messageLabel.setText("Top " + (rank - 1) + " most active flight(s).");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Show top:"));
        topPanel.add(limitField);
        topPanel.add(loadButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(activeFlightsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
