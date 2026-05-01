import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class SalesReportTab {

    public static JPanel build() {
        JTextField yearField = new JTextField(6);
        JComboBox<String> monthCombo = new JComboBox<>(new String[]{"01 - January", "02 - February", "03 - March", "04 - April", "05 - May", "06 - June", "07 - July", "08 - August", "09 - September", "10 - October", "11 - November", "12 - December"});
        yearField.setText(String.valueOf(java.time.LocalDate.now().getYear()));
        String[] columns = {"Ticket #", "Customer", "Flight", "Date", "Class", "Total"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable salesTable = new JTable(tableModel);
        salesTable.setRowHeight(22);

        JLabel summaryLabel = new JLabel(" ");

        JButton generateButton = new JButton("Generate Report");
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String year = yearField.getText().trim();
                String month = monthCombo.getSelectedItem().toString().substring(0, 2);
                String query = "SELECT t.ticket_no, c.name AS cust_name, tf.flight_no, tf.dep_date, " + "tf.class, " + "(t.total_fare + t.booking_fee) AS grand_total " + "FROM Ticket t " + "JOIN Customer c ON t.cust_id = c.cust_id " + "JOIN Ticket_Flight tf ON t.ticket_no = tf.ticket_no AND tf.leg_order=1 " + "WHERE t.status != 'cancelled' " + "AND YEAR(t.purchase_date)=" + year + " AND MONTH(t.purchase_date)=" + month + " ORDER BY t.purchase_date";
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(query);
                    double totalRevenue = 0;
                    int count = 0;
                    while (rs.next()) {
                        double grandTotal = rs.getDouble("grand_total");
                        totalRevenue += grandTotal;
                        count++;
                        tableModel.addRow(new Object[]{rs.getInt("ticket_no"), rs.getString("cust_name"), rs.getString("flight_no"), rs.getString("dep_date"), rs.getString("class"), "$" + String.format("%.2f", grandTotal)});
                    }
                    summaryLabel.setText(String.format("%d ticket(s)  |  Total Revenue: $%.2f", count, totalRevenue));
                }
                catch (SQLException ex) {
                    summaryLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Year:"));
        topPanel.add(yearField);
        topPanel.add(new JLabel("Month:"));
        topPanel.add(monthCombo);
        topPanel.add(generateButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(salesTable), BorderLayout.CENTER);
        panel.add(summaryLabel, BorderLayout.SOUTH);
        return panel;
    }
}
