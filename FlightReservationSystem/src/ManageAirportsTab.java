import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageAirportsTab {

    public static JPanel build(JFrame parentFrame) {
        String[] airportHeaders = {"Airport ID", "Name", "City", "Country"};
        DefaultTableModel airportTableModel = new DefaultTableModel(airportHeaders, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable airportsTable = new JTable(airportTableModel);
        airportsTable.setRowHeight(22);
        airportsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");
        loadAllAirports(airportTableModel, statusLabel);
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField idField = new JTextField(4);
                JTextField nameField = new JTextField(20);
                JTextField cityField = new JTextField(15);
                JTextField countryField = new JTextField(15);
                Object[] addFields = {"Airport ID (3-letter):", idField, "Name:", nameField, "City:", cityField, "Country:", countryField};
                int userResponse = JOptionPane.showConfirmDialog(parentFrame, addFields, "Add Airport", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (userResponse != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("INSERT INTO Airport (airport_id,name,city,country) VALUES ('" + idField.getText().trim().toUpperCase() + "','" + nameField.getText().trim() + "','" + cityField.getText().trim() + "','" + countryField.getText().trim() + "')");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Airport added.");
                    loadAllAirports(airportTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = airportsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select an airport.");
                    return;
                }
                String airportId = (String) airportTableModel.getValueAt(row, 0);
                JTextField nameField = new JTextField((String) airportTableModel.getValueAt(row, 1), 20);
                JTextField cityField = new JTextField((String) airportTableModel.getValueAt(row, 2), 15);
                JTextField countryField = new JTextField((String) airportTableModel.getValueAt(row, 3), 15);
                Object[] editFields = {"Name:", nameField, "City:", cityField, "Country:", countryField};
                int userResponse = JOptionPane.showConfirmDialog(parentFrame, editFields, "Edit Airport " + airportId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (userResponse != JOptionPane.OK_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("UPDATE Airport SET name='" + nameField.getText().trim() + "', city='" + cityField.getText().trim() + "', country='" + countryField.getText().trim() + "' WHERE airport_id='" + airportId + "'");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Airport updated.");
                    loadAllAirports(airportTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = airportsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select an airport.");
                    return;
                }
                String airportId = (String) airportTableModel.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "Delete airport " + airportId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    DBConnection.getStatement().executeUpdate("DELETE FROM Airport WHERE airport_id='" + airportId + "'");
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Airport deleted.");
                    loadAllAirports(airportTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(airportsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadAllAirports(DefaultTableModel model, JLabel status) {
        model.setRowCount(0);
        try {
            ResultSet airportResults = DBConnection.getStatement().executeQuery("SELECT airport_id, name, city, country FROM Airport ORDER BY airport_id");
            while (airportResults.next()) {
                model.addRow(new Object[]{airportResults.getString("airport_id"), airportResults.getString("name"), airportResults.getString("city"), airportResults.getString("country")});
            }
            status.setText(model.getRowCount() + " airport(s).");
        }
        catch (SQLException ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }
}