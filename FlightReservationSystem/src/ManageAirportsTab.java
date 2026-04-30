import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ManageAirportsTab {

    public static JPanel build(JFrame parentFrame) {
        final String[] columns = {"Airport ID", "Name", "City", "Country"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable airportsTable = new JTable(tableModel);
        airportsTable.setRowHeight(22);
        airportsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel messageLabel = new JLabel(" ");

        loadAllAirports(tableModel, messageLabel);

        JButton addButton    = new JButton("Add");
        JButton editButton   = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        deleteButton.setForeground(Color.RED);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField idField      = new JTextField(4);
                JTextField nameField    = new JTextField(20);
                JTextField cityField    = new JTextField(15);
                JTextField countryField = new JTextField(15);
                Object[] fields = {"Airport ID (3-letter):", idField, "Name:", nameField, "City:", cityField, "Country:", countryField};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Add Airport",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "INSERT INTO Airport (airport_id,name,city,country) VALUES ('" +
                        idField.getText().trim().toUpperCase() + "','" + nameField.getText().trim() + "','" +
                        cityField.getText().trim() + "','" + countryField.getText().trim() + "')");
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Airport added.");
                    loadAllAirports(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = airportsTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select an airport."); return; }
                String airportId = (String) tableModel.getValueAt(selectedRow, 0);
                JTextField nameField    = new JTextField((String) tableModel.getValueAt(selectedRow, 1), 20);
                JTextField cityField    = new JTextField((String) tableModel.getValueAt(selectedRow, 2), 15);
                JTextField countryField = new JTextField((String) tableModel.getValueAt(selectedRow, 3), 15);
                Object[] fields = {"Name:", nameField, "City:", cityField, "Country:", countryField};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Edit Airport " + airportId,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "UPDATE Airport SET name='" + nameField.getText().trim() +
                        "', city='" + cityField.getText().trim() +
                        "', country='" + countryField.getText().trim() +
                        "' WHERE airport_id='" + airportId + "'");
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Airport updated.");
                    loadAllAirports(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = airportsTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select an airport."); return; }
                String airportId = (String) tableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(parentFrame,
                    "Delete airport " + airportId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                try {
                    DBConnection.getStatement().executeUpdate(
                        "DELETE FROM Airport WHERE airport_id='" + airportId + "'");
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Airport deleted.");
                    loadAllAirports(tableModel, messageLabel);
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
        panel.add(new JScrollPane(airportsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadAllAirports(DefaultTableModel tableModel, JLabel messageLabel) {
        tableModel.setRowCount(0);
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                "SELECT airport_id, name, city, country FROM Airport ORDER BY airport_id");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("airport_id"), rs.getString("name"),
                    rs.getString("city"), rs.getString("country")});
            }
            messageLabel.setText(tableModel.getRowCount() + " airport(s).");
        } catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }
}
