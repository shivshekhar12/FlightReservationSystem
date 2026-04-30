import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class EditReservationsTab {

    public static JPanel build(JFrame parentFrame) {
        final JTextField customerIdField = new JTextField(8);

        final String[] columns = {"Ticket #", "Flight", "Airline", "Date", "Class", "Seat", "Meal", "Status"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable reservationsTable = new JTable(tableModel);
        reservationsTable.setRowHeight(22);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel messageLabel = new JLabel(" ");

        JButton loadButton = new JButton("Load Reservations");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                String customerId = customerIdField.getText().trim();
                if (customerId.isEmpty()) { messageLabel.setText("Enter customer ID."); return; }
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT t.ticket_no, tf.flight_no, tf.airline_id, tf.dep_date, " +
                        "tf.class, tf.seat_no, tf.meal_pref, t.status " +
                        "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no " +
                        "WHERE t.cust_id=" + customerId + " ORDER BY tf.dep_date DESC");
                    int count = 0;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                            rs.getInt("ticket_no"), rs.getString("flight_no"),
                            rs.getString("airline_id"), rs.getString("dep_date"),
                            rs.getString("class"), rs.getString("seat_no"),
                            rs.getString("meal_pref"), rs.getString("status")});
                        count++;
                    }
                    messageLabel.setText(count + " reservation(s) found.");
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = reservationsTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select a reservation to edit."); return; }
                int ticketNo     = (int) tableModel.getValueAt(selectedRow, 0);
                String curSeat   = (String) tableModel.getValueAt(selectedRow, 5);
                String curMeal   = (String) tableModel.getValueAt(selectedRow, 6);
                String curDate   = (String) tableModel.getValueAt(selectedRow, 3);

                JTextField seatField = new JTextField(curSeat != null ? curSeat : "", 8);
                JTextField mealField = new JTextField(curMeal != null ? curMeal : "", 15);
                JTextField dateField = new JTextField(curDate != null ? curDate : "", 12);

                Object[] fields = {"New Seat No:", seatField, "New Meal Pref:", mealField, "New Dep Date:", dateField};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields,
                    "Edit Ticket #" + ticketNo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "UPDATE Ticket_Flight SET seat_no='" + seatField.getText().trim() +
                        "', meal_pref='" + mealField.getText().trim() +
                        "', dep_date='" + dateField.getText().trim() +
                        "' WHERE ticket_no=" + ticketNo);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Ticket #" + ticketNo + " updated.");
                    loadButton.doClick();
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Customer ID:")); topPanel.add(customerIdField);
        topPanel.add(loadButton); topPanel.add(editButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }
}
