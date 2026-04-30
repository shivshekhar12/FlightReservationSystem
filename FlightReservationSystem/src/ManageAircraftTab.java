import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageAircraftTab {

    public static JPanel build(JFrame parentFrame) {
        final String[] columns = {"Aircraft ID", "Airline", "Model", "Economy Seats", "Business Seats", "First Seats"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable aircraftTable = new JTable(tableModel);
        aircraftTable.setRowHeight(22);
        aircraftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel messageLabel = new JLabel(" ");

        loadAllAircraft(tableModel, messageLabel);

        JButton addButton    = new JButton("Add");
        JButton editButton   = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField idField       = new JTextField(6);
                JTextField airlineField  = new JTextField(4);
                JTextField modelField    = new JTextField(20);
                JTextField ecoSeatsField = new JTextField(6);
                JTextField bizSeatsField = new JTextField(6);
                JTextField fstSeatsField = new JTextField(6);
                Object[] fields = {
                    "Aircraft ID:", idField, "Airline ID:", airlineField, "Model:", modelField,
                    "Economy Seats:", ecoSeatsField, "Business Seats:", bizSeatsField, "First Seats:", fstSeatsField};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Add Aircraft",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Aircraft (aircraft_id,airline_id,model,economy_seats,business_seats,first_seats) VALUES (" +
                        idField.getText().trim() + ",'" + airlineField.getText().trim().toUpperCase() + "','" +
                        modelField.getText().trim() + "'," + ecoSeatsField.getText().trim() + "," +
                        bizSeatsField.getText().trim() + "," + fstSeatsField.getText().trim() + ")");
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Aircraft added.");
                    loadAllAircraft(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = aircraftTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select an aircraft."); return; }
                int aircraftId = (int) tableModel.getValueAt(selectedRow, 0);
                JTextField modelField    = new JTextField((String) tableModel.getValueAt(selectedRow, 2), 20);
                JTextField ecoSeatsField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 3)), 6);
                JTextField bizSeatsField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 4)), 6);
                JTextField fstSeatsField = new JTextField(String.valueOf(tableModel.getValueAt(selectedRow, 5)), 6);
                Object[] fields = {
                    "Model:", modelField, "Economy Seats:", ecoSeatsField,
                    "Business Seats:", bizSeatsField, "First Seats:", fstSeatsField};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Aircraft " + aircraftId,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "UPDATE Aircraft SET model='" + modelField.getText().trim() +
                        "', economy_seats=" + ecoSeatsField.getText().trim() +
                        ", business_seats=" + bizSeatsField.getText().trim() +
                        ", first_seats=" + fstSeatsField.getText().trim() +
                        " WHERE aircraft_id=" + aircraftId);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Aircraft updated.");
                    loadAllAircraft(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = aircraftTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select an aircraft."); return; }
                int aircraftId = (int) tableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(parentFrame,
                    "Delete aircraft " + aircraftId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate("DELETE FROM Aircraft WHERE aircraft_id=" + aircraftId);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Aircraft deleted.");
                    loadAllAircraft(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(addButton); topPanel.add(editButton); topPanel.add(deleteButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(aircraftTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadAllAircraft(DefaultTableModel tableModel, JLabel messageLabel) {
        tableModel.setRowCount(0);
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                "SELECT aircraft_id, airline_id, model, economy_seats, business_seats, first_seats FROM Aircraft ORDER BY aircraft_id");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("aircraft_id"), rs.getString("airline_id"),
                    rs.getString("model"), rs.getInt("economy_seats"),
                    rs.getInt("business_seats"), rs.getInt("first_seats")});
            }
            messageLabel.setText(tableModel.getRowCount() + " aircraft loaded.");
        } catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }
}
