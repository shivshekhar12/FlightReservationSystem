import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class TopCustomersTab {

    public static JPanel build() {
        JTextField limitField = new JTextField("10", 5);

        String[] headerTitles = {"Rank", "Customer Name", "Email", "Total Spent"};
        DefaultTableModel customerTableModel = new DefaultTableModel(headerTitles, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable topCustomersTable = new JTable(customerTableModel);
        topCustomersTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                customerTableModel.setRowCount(0);
                String limit = limitField.getText().trim();
                try {
                    Integer.parseInt(limit);
                }
                catch (NumberFormatException ex) {
                    limit = "10";
                }
                String query = "SELECT c.name, c.email, " + "SUM(t.total_fare + t.booking_fee) AS total_spent " + "FROM Customer c " + "JOIN Ticket t ON c.cust_id=t.cust_id "
                        + "WHERE t.status != 'cancelled' " + "GROUP BY c.cust_id " + "ORDER BY total_spent DESC " + "LIMIT " + limit;
                try {
                    ResultSet customerResults = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (customerResults.next()) {
                        customerTableModel.addRow(new Object[]{rank++, customerResults.getString("name"), customerResults.getString("email"), "$" + String.format("%.2f", customerResults.getDouble("total_spent"))});
                    }
                    messageLabel.setText("Top " + (rank - 1) + " customer(s) by total revenue.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topCustomersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topCustomersPanel.add(new JLabel("Show top:"));
        topCustomersPanel.add(limitField);
        topCustomersPanel.add(loadButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topCustomersPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(topCustomersTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
