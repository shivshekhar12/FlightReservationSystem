import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class RevenueSummaryTab {

    public static JPanel build() {
        JComboBox<String> groupByCombo = new JComboBox<>(new String[]{"By Flight", "By Airline", "By Customer"});

        String[] columns = {"Group", "Tickets Sold", "Grand Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable revenueTable = new JTable(tableModel);
        revenueTable.setRowHeight(22);

        JLabel messageLabel = new JLabel(" ");

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                int selectedIndex = groupByCombo.getSelectedIndex();
                String query;
                if (selectedIndex == 0) {
                    query = "SELECT tf.flight_no AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE t.status!='cancelled' GROUP BY tf.flight_no ORDER BY grand DESC";
                }
                else if (selectedIndex == 1) {
                    query = "SELECT tf.airline_id AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " + "WHERE t.status!='cancelled' GROUP BY tf.airline_id ORDER BY grand DESC";
                }
                else {
                    query = "SELECT c.name AS grp, COUNT(DISTINCT t.ticket_no) AS cnt, " + "SUM(t.total_fare + t.booking_fee) AS grand " + "FROM Ticket t JOIN Customer c ON t.cust_id=c.cust_id " + "WHERE t.status!='cancelled' GROUP BY c.cust_id ORDER BY grand DESC";
                }
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    int count = 0;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{rs.getString("grp"), rs.getInt("cnt"), "$" + String.format("%.2f", rs.getDouble("grand"))});                        count++;
                    }
                    messageLabel.setText(count + " group(s).");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Group:"));
        topPanel.add(groupByCombo);
        topPanel.add(generateButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(revenueTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
