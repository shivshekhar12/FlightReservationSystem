import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class WaitlistTab {

    public static JPanel build() {
        JTextField flightNoField = new JTextField(10);
        JTextField airlineField = new JTextField(4);
        JTextField dateField = new JTextField(12);
        String[] columns = {"Position", "Customer ID", "Customer Name", "Email", "Requested", "Notified"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable waitlistTable = new JTable(tableModel);
        waitlistTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton loadButton = new JButton("Load Waitlist");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String flightNo = flightNoField.getText().trim();
                String airlineId = airlineField.getText().trim().toUpperCase();
                String date = dateField.getText().trim();
                if (flightNo.isEmpty() || airlineId.isEmpty() || date.isEmpty()) {
                    messageLabel.setText("Enter flight no, airline and date.");
                    return;
                }
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery("SELECT w.position, w.cust_id, c.name, c.email, w.join_date, w.notified " + "FROM Waitlist w JOIN Customer c ON w.cust_id=c.cust_id " + "WHERE w.flight_no='" + flightNo + "' AND w.airline_id='" + airlineId + "' AND w.dep_date='" + date + "' ORDER BY w.position");
                    int count = 0;
                    while (rs.next()) {
                        String notified;
                        if (rs.getBoolean("notified")) {
                            notified = "Yes";
                        }
                        else {
                            notified = "No";
                        }
                        tableModel.addRow(new Object[]{rs.getInt("position"), rs.getInt("cust_id"), rs.getString("name"), rs.getString("email"), rs.getString("join_date"), notified});
                        count++;
                    }
                    messageLabel.setText(count + " customer(s) on waitlist.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Flight No:"));
        topPanel.add(flightNoField);
        topPanel.add(new JLabel("Airline ID:"));
        topPanel.add(airlineField);
        topPanel.add(new JLabel("Date:"));
        topPanel.add(dateField);
        topPanel.add(loadButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}