import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class EditReservationsTab {

    public static JPanel build(JFrame parentFrame) {
        JTextField customerIdField = new JTextField(8);
        String[] reservationFields = {"Ticket #", "Flight", "Airline", "Date", "Class", "Seat", "Meal", "Status"};
        DefaultTableModel reservationTableModel = new DefaultTableModel(reservationFields, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable reservationsTable = new JTable(reservationTableModel);
        reservationsTable.setRowHeight(22);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");

        JButton loadReservationsButton = new JButton("Load Reservations");
        loadReservationsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reservationTableModel.setRowCount(0);
                String customerId = customerIdField.getText().trim();
                if (customerId.isEmpty()) {
                    statusLabel.setText("Enter customer ID.");
                    return;
                }
                try {
                    ResultSet ticketResults = DBConnection.getStatement().executeQuery("SELECT t.ticket_no, tf.flight_no, tf.airline_id, tf.dep_date, "
                            + "tf.class, tf.seat_no, tf.meal_pref, t.status " + "FROM Ticket t JOIN Ticket_Flight tf ON t.ticket_no=tf.ticket_no "
                            + "WHERE t.cust_id=" + customerId + " ORDER BY tf.dep_date DESC");
                    int count = 0;
                    while (ticketResults.next()) {
                        reservationTableModel.addRow(new Object[]{ticketResults.getInt("ticket_no"), ticketResults.getString("flight_no"), ticketResults.getString("airline_id"), ticketResults.getString("dep_date"), ticketResults.getString("class"), ticketResults.getString("seat_no"), ticketResults.getString("meal_pref"), ticketResults.getString("status")});
                        count++;
                    }
                    statusLabel.setText(count + " reservation(s) found.");
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = reservationsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a reservation to edit.");
                    return;
                }
                int ticketNo = (int) reservationTableModel.getValueAt(row, 0);
                String seat = (String) reservationTableModel.getValueAt(row, 5);
                String meal = (String) reservationTableModel.getValueAt(row, 6);
                String date = (String) reservationTableModel.getValueAt(row, 3);
                String seatNumber;
                if (seat != null) {
                    seatNumber = seat;
                }
                else {
                    seatNumber = "";
                }
                String mealText;
                if (meal != null) {
                    mealText = meal;
                }
                else {
                    mealText = "";
                }
                String formattedDate;
                if (date != null) {
                    formattedDate = date;
                }
                else {
                    formattedDate = "";
                }
                JTextField seatField = new JTextField(seatNumber, 8);
                JTextField mealField = new JTextField(mealText, 15);
                JTextField dateField = new JTextField(formattedDate, 12);
                Object[] editFields = {"New Seat No:", seatField, "New Meal Pref:", mealField, "New Dep Date:", dateField};
                int confirmed = JOptionPane.showConfirmDialog(parentFrame, editFields, "Edit Ticket #" + ticketNo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (confirmed != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("UPDATE Ticket_Flight SET seat_no='" + seatField.getText().trim() + "', meal_pref='" + mealField.getText().trim() + "', dep_date='" + dateField.getText().trim() + "' WHERE ticket_no=" + ticketNo);
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Ticket #" + ticketNo + " updated.");
                    loadReservationsButton.doClick();
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        controlPanel.add(new JLabel("Customer ID:"));
        controlPanel.add(customerIdField);
        controlPanel.add(loadReservationsButton);
        controlPanel.add(editButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }
}