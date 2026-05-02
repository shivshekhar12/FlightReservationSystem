import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageAircraftTab {

    public static JPanel build(JFrame parentFrame) {
        String[] aircraftAttributes = {"Aircraft ID", "Airline", "Model", "Economy Seats", "Business Seats", "First Seats"};
        DefaultTableModel aircraftTableModel = new DefaultTableModel(aircraftAttributes, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable aircraftTable = new JTable(aircraftTableModel);
        aircraftTable.setRowHeight(22);
        aircraftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");
        loadAllAircraft(aircraftTableModel, statusLabel);
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField idField = new JTextField(6);
                JTextField airlineField = new JTextField(4);
                JTextField modelField = new JTextField(20);
                JTextField ecoSeatsField = new JTextField(6);
                JTextField bizSeatsField = new JTextField(6);
                JTextField fstSeatsField = new JTextField(6);
                Object[] addFields = {"Aircraft ID:", idField, "Airline ID:", airlineField, "Model:", modelField, "Economy Seats:", ecoSeatsField, "Business Seats:", bizSeatsField, "First Seats:", fstSeatsField};
                int confirmDialog = JOptionPane.showConfirmDialog(parentFrame, addFields, "Add Aircraft", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (confirmDialog != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("INSERT INTO Aircraft (aircraft_id,airline_id,model,economy_seats,business_seats,first_seats) VALUES (" + idField.getText().trim() + ",'" + airlineField.getText().trim().toUpperCase()
                            + "','" + modelField.getText().trim() + "'," + ecoSeatsField.getText().trim() + "," + bizSeatsField.getText().trim() + "," + fstSeatsField.getText().trim() + ")");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Aircraft added.");
                    loadAllAircraft(aircraftTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = aircraftTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select an aircraft.");
                    return;
                }
                int aircraftId = (int) aircraftTableModel.getValueAt(row, 0);
                JTextField modelField = new JTextField((String) aircraftTableModel.getValueAt(row, 2), 20);
                JTextField ecoSeatsField = new JTextField(String.valueOf(aircraftTableModel.getValueAt(row, 3)), 6);
                JTextField bizSeatsField = new JTextField(String.valueOf(aircraftTableModel.getValueAt(row, 4)), 6);
                JTextField fstSeatsField = new JTextField(String.valueOf(aircraftTableModel.getValueAt(row, 5)), 6);
                Object[] editFields = {"Model:", modelField, "Economy Seats:", ecoSeatsField, "Business Seats:", bizSeatsField, "First Seats:", fstSeatsField};
                int confirmed = JOptionPane.showConfirmDialog(parentFrame, editFields, "Edit Aircraft " + aircraftId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (confirmed != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("UPDATE Aircraft SET model='" + modelField.getText().trim() + "', economy_seats=" + ecoSeatsField.getText().trim() + ", business_seats=" + bizSeatsField.getText().trim() + ", first_seats=" + fstSeatsField.getText().trim() + " WHERE aircraft_id=" + aircraftId);
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Aircraft updated.");
                    loadAllAircraft(aircraftTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = aircraftTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select an aircraft.");
                    return;
                }
                int aircraftId = (int) aircraftTableModel.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete aircraft " + aircraftId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("DELETE FROM Aircraft WHERE aircraft_id=" + aircraftId);
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Aircraft deleted.");
                    loadAllAircraft(aircraftTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(actionPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(aircraftTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadAllAircraft(DefaultTableModel model, JLabel status) {
        model.setRowCount(0);
        try {
            ResultSet aircraftData = DBConnection.getStatement().executeQuery(
                    "SELECT aircraft_id, airline_id, model, economy_seats, business_seats, first_seats FROM Aircraft ORDER BY aircraft_id");
            while (aircraftData.next()) {
                model.addRow(new Object[]{aircraftData.getInt("aircraft_id"), aircraftData.getString("airline_id"), aircraftData.getString("model"), aircraftData.getInt("economy_seats"), aircraftData.getInt("business_seats"), aircraftData.getInt("first_seats")});
            }
            status.setText(model.getRowCount() + " aircraft loaded.");
        }
        catch (SQLException ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }
}