import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class AnswerQuestionsTab {

    public static JPanel build(JFrame parentFrame, int empId) {
        final String[] columns = {"#", "Customer", "Subject", "Question", "Answer", "Asked", "Answered"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        final JTable questionsTable = new JTable(tableModel);
        questionsTable.setRowHeight(22);
        questionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JLabel messageLabel = new JLabel(" ");

        JButton loadUnansweredButton = new JButton("Load Unanswered");
        JButton loadAllButton        = new JButton("Load All");
        JButton replyButton          = new JButton("Reply to Selected");

        loadUnansweredButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadUnanswered(tableModel, messageLabel);
            }
        });

        loadAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
                try {
                    ResultSet rs = DBConnection.getStatement().executeQuery(
                        "SELECT q.question_id, c.name, q.subject, q.question_text, " +
                        "q.answer_text, q.asked_at, q.answered_at " +
                        "FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id " +
                        "ORDER BY q.asked_at DESC");
                    int count = 0;
                    while (rs.next()) {
                        String answerText = rs.getString("answer_text");
                        String answeredAt = rs.getString("answered_at");
                        String displayAnswer;
                        if (answerText != null) {
                            displayAnswer = answerText;
                        } else {
                            displayAnswer = "(unanswered)";
                        }
                        String displayAnsweredAt;
                        if (answeredAt != null) {
                            displayAnsweredAt = answeredAt;
                        } else {
                            displayAnsweredAt = "-";
                        }
                        tableModel.addRow(new Object[]{
                            rs.getInt("question_id"), rs.getString("name"),
                            rs.getString("subject"), rs.getString("question_text"),
                            displayAnswer, rs.getString("asked_at"), displayAnsweredAt});
                        count++;
                    }
                    messageLabel.setText(count + " question(s) total.");
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        replyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = questionsTable.getSelectedRow();
                if (selectedRow < 0) { messageLabel.setText("Select a question to reply to."); return; }
                int questionId   = (int) tableModel.getValueAt(selectedRow, 0);
                String question  = (String) tableModel.getValueAt(selectedRow, 3);

                JTextArea answerArea = new JTextArea(5, 40);
                answerArea.setLineWrap(true);
                answerArea.setWrapStyleWord(true);

                Object[] fields = {"Question: " + question, "Your Answer:", new JScrollPane(answerArea)};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields,
                    "Reply to Question #" + questionId,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) return;

                String answer = answerArea.getText().trim();
                if (answer.isEmpty()) { messageLabel.setText("Answer cannot be empty."); return; }

                try {
                    DBConnection.getStatement().executeUpdate(
                        "UPDATE Customer_Question SET " +
                        "answer_text='" + answer.replace("'", "''") + "', " +
                        "rep_id=" + empId + ", " +
                        "answered_at=NOW() " +
                        "WHERE question_id=" + questionId);
                    messageLabel.setForeground(new Color(0, 120, 0));
                    messageLabel.setText("Reply posted for question #" + questionId + ".");
                    loadUnanswered(tableModel, messageLabel);
                } catch (SQLException ex) {
                    messageLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        loadUnanswered(tableModel, messageLabel);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(loadUnansweredButton); topPanel.add(loadAllButton); topPanel.add(replyButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(questionsTable), BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadUnanswered(DefaultTableModel tableModel, JLabel messageLabel) {
        tableModel.setRowCount(0);
        try {
            ResultSet rs = DBConnection.getStatement().executeQuery(
                "SELECT q.question_id, c.name, q.subject, q.question_text, " +
                "q.asked_at FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id " +
                "WHERE q.answer_text IS NULL ORDER BY q.asked_at ASC");
            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("question_id"), rs.getString("name"),
                    rs.getString("subject"), rs.getString("question_text"),
                    "(unanswered)", rs.getString("asked_at"), "-"});
                count++;
            }
            messageLabel.setText(count + " unanswered question(s).");
        } catch (SQLException ex) {
            messageLabel.setText("Error: " + ex.getMessage());
        }
    }
}
