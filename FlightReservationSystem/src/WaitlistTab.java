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
        String[] waitlistColumns = {"Position", "Customer ID", "Customer Name", "Email", "Requested", "Notified"};
        DefaultTableModel waitlistTableModel = new DefaultTableModel(waitlistColumns, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable waitlistTable = new JTable(waitlistTableModel);
        waitlistTable.setRowHeight(22);
        JLabel messageLabel = new JLabel(" ");
        JButton loadButton = new JButton("Load Waitlist");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                waitlistTableModel.setRowCount(0);
                String flightNo = flightNoField.getText().trim();
                String airlineId = airlineField.getText().trim().toUpperCase();
                String date = dateField.getText().trim();
                if (flightNo.isEmpty() || airlineId.isEmpty() || date.isEmpty()) {
                    messageLabel.setText("Enter flight no, airline and date.");
                    return;
                }
                try {
                    ResultSet waitlistResultSet = DBConnection.getStatement().executeQuery("SELECT w.position, w.cust_id, c.name, c.email, w.join_date, w.notified "
                            + "FROM Waitlist w JOIN Customer c ON w.cust_id=c.cust_id " + "WHERE w.flight_no='" + flightNo + "' AND w.airline_id='" + airlineId + "' AND w.dep_date='" + date + "' ORDER BY w.position");
                    int count = 0;
                    while (waitlistResultSet.next()) {
                        String notified;
                        if (waitlistResultSet.getBoolean("notified")) {
                            notified = "Yes";
                        }
                        else {
                            notified = "No";
                        }
                        waitlistTableModel.addRow(new Object[]{waitlistResultSet.getInt("position"), waitlistResultSet.getInt("cust_id"), waitlistResultSet.getString("name"), waitlistResultSet.getString("email"), waitlistResultSet.getString("join_date"), notified});
                        count++;
                    }
                    messageLabel.setText(count + " customer(s) on waitlist.");
                }
                catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel flightInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        flightInfoPanel.add(new JLabel("Flight No:"));
        flightInfoPanel.add(flightNoField);
        flightInfoPanel.add(new JLabel("Airline ID:"));
        flightInfoPanel.add(airlineField);
        flightInfoPanel.add(new JLabel("Date:"));
        flightInfoPanel.add(dateField);
        flightInfoPanel.add(loadButton);

        JPanel waitlistPanel = new JPanel(new BorderLayout(5, 5));
        waitlistPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        waitlistPanel.add(flightInfoPanel, BorderLayout.NORTH);
        waitlistPanel.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        waitlistPanel.add(messageLabel, BorderLayout.SOUTH);
        return waitlistPanel;
    }
}