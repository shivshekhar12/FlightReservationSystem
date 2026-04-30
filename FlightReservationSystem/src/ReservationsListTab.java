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

        String[] columns = {"Ticket #", "Customer", "Flight", "Airline", "Date", "Class", "Seat", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable reservationsTable = new JTable(tableModel);
        reservationsTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
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
                String query = "SELECT t.ticket_no, c.name, tf.flight_no, tf.airline_id, " + "tf.dep_date, tf.class, tf.seat_no, t.status " + "FROM Ticket t " + "JOIN Customer c ON t.cust_id=c.cust_id " + "JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE " + whereClause + " ORDER BY tf.dep_date DESC";
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rs.getInt("ticket_no"), rs.getString("name"), rs.getString("flight_no"), rs.getString("airline_id"), rs.getString("dep_date"), rs.getString("class"), rs.getString("seat_no"), rs.getString("status")});
                        count++;
                    }
                    messageLabel.setText(count + " reservation(s) found.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(byFlightRadio);
        topPanel.add(byCustomerRadio);
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(searchButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
