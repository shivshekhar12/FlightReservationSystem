import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ReservationsListTab {

    public static JPanel build() {
        JRadioButton byFlightRadio = new JRadioButton("By Flight No");
        JRadioButton byCustomerRadio = new JRadioButton("By Customer Name");
        byFlightRadio.setSelected(true);
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(byFlightRadio);
        radioGroup.add(byCustomerRadio);

        JTextField searchField = new JTextField(20);

        String[] tableHeaders = {"Ticket #", "Customer", "Flight", "Airline", "Date", "Class", "Seat", "Status"};
        DefaultTableModel reservationsTableModel = new DefaultTableModel(tableHeaders, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable reservationsTable = new JTable(reservationsTableModel);
        reservationsTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reservationsTableModel.setRowCount(0);
                String searchValue = searchField.getText().trim();
                if (searchValue.isEmpty()) {
                    messageLabel.setText("Enter a search value.");
                    return;
                }
                String whereClause;
                if (byFlightRadio.isSelected()) {
                    whereClause = "tf.flight_no='" + searchValue + "'";
                }
                else {
                    whereClause = "c.name LIKE '%" + searchValue + "%'";
                }
                String query = "SELECT t.ticket_no, c.name, tf.flight_no, tf.airline_id, " + "tf.dep_date, tf.class, tf.seat_no, t.status "
                        + "FROM Ticket t " + "JOIN Customer c ON t.cust_id=c.cust_id " + "JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE " + whereClause + " ORDER BY tf.dep_date DESC";
                try {
                    ResultSet ticketResults = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (ticketResults.next()) {
                        reservationsTableModel.addRow(new Object[]{ticketResults.getInt("ticket_no"), ticketResults.getString("name"), ticketResults.getString("flight_no"), ticketResults.getString("airline_id"), ticketResults.getString("dep_date"), ticketResults.getString("class"), ticketResults.getString("seat_no"), ticketResults.getString("status")});
                        count++;
                    }
                    messageLabel.setText(count + " reservation(s) found.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchPanel.add(byFlightRadio);
        searchPanel.add(byCustomerRadio);
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
