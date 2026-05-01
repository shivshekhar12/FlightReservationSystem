import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class TopCustomersTab {

    public static JPanel build() {
        JTextField limitField = new JTextField("10", 5);

        String[] columns = {"Rank", "Customer Name", "Email", "Total Spent"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable topCustomersTable = new JTable(tableModel);
        topCustomersTable.setRowHeight(22);

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
                String query = "SELECT c.name, c.email, " + "SUM(t.total_fare + t.booking_fee) AS total_spent " + "FROM Customer c " + "JOIN Ticket t ON c.cust_id=t.cust_id " + "WHERE t.status != 'cancelled' " + "GROUP BY c.cust_id " + "ORDER BY total_spent DESC " + "LIMIT " + limit;
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int rank = 1;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rank++, rs.getString("name"), rs.getString("email"), "$" + String.format("%.2f", rs.getDouble("total_spent"))});
                    }
                    messageLabel.setText("Top " + (rank - 1) + " customer(s) by total revenue.");
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
        panel.add(new JScrollPane(topCustomersTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
