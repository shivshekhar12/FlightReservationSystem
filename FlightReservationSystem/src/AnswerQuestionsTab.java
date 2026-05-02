import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class AnswerQuestionsTab {

    public static JPanel build(JFrame parentFrame, int empId) {
        String[] tableHeaders = {"#", "Customer", "Question", "Answer", "Asked", "Answered"};
        DefaultTableModel questionsTableModel = new DefaultTableModel(tableHeaders, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable questionsTable = new JTable(questionsTableModel);
        questionsTable.setRowHeight(22);
        questionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel statusLabel = new JLabel(" ");
        JButton loadUnansweredButton = new JButton("Load Unanswered");
        JButton loadAllButton = new JButton("Load All");
        JButton replyButton = new JButton("Reply to Selected");

        loadUnansweredButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadUnanswered(questionsTableModel, statusLabel);
            }
        });

        loadAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                questionsTableModel.setRowCount(0);
                try {
                    ResultSet queryResults = DBConnection.getStatement().executeQuery("SELECT q.question_id, c.name, q.question_text, "
                            + "q.answer_text, q.asked_at, q.answered_at "
                            + "FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id "
                            + "ORDER BY q.asked_at DESC");
                    int count = 0;
                    while (queryResults.next()) {
                        String answerText = queryResults.getString("answer_text");
                        String answeredAt = queryResults.getString("answered_at");
                        String displayAnswer;
                        if (answerText != null) {
                            displayAnswer = answerText;
                        }
                        else {
                            displayAnswer = "(unanswered)";
                        }
                        String displayAnsweredAt;
                        if (answeredAt != null) {
                            displayAnsweredAt = answeredAt;
                        }
                        else {
                            displayAnsweredAt = "-";
                        }
                        questionsTableModel.addRow(new Object[]{
                                queryResults.getInt("question_id"),
                                queryResults.getString("name"),
                                queryResults.getString("question_text"), displayAnswer,
                                queryResults.getString("asked_at"), displayAnsweredAt});
                        count++;
                    }
                    statusLabel.setText(count + " question(s) total.");
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        replyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = questionsTable.getSelectedRow();
                if (row < 0) {
                    statusLabel.setText("Select a question to reply to.");
                    return;
                }
                int questionId = (int) questionsTableModel.getValueAt(row, 0);
                String question = (String) questionsTableModel.getValueAt(row, 2);

                JTextArea answerArea = new JTextArea(5, 40);
                answerArea.setLineWrap(true);
                answerArea.setWrapStyleWord(true);

                Object[] fields = {"Question: " + question, "Your Answer:", new JScrollPane(answerArea)};
                int dialogResult = JOptionPane.showConfirmDialog(parentFrame, fields, "Reply to Question #" + questionId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (dialogResult != JOptionPane.OK_OPTION) {
                    return;
                }

                String answer = answerArea.getText().trim();
                if (answer.isEmpty()) {
                    statusLabel.setText("Answer cannot be empty.");
                    return;
                }

                try {
                    DBConnection.getStatement().executeUpdate("UPDATE Customer_Question SET "
                            + "answer_text='"
                            + answer.replace("'", "''")
                            + "', " + "rep_id=" + empId + ", " + "answered_at=NOW() "
                            + "WHERE question_id=" + questionId);
                    statusLabel.setForeground(new Color(0, 120, 0));
                    statusLabel.setText("Reply posted for question #" + questionId + ".");
                    loadUnanswered(questionsTableModel, statusLabel);
                }
                catch (SQLException ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });

        loadUnanswered(questionsTableModel, statusLabel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        actionPanel.add(loadUnansweredButton);
        actionPanel.add(loadAllButton);
        actionPanel.add(replyButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(actionPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(questionsTable), BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static void loadUnanswered(DefaultTableModel model, JLabel status) {
        model.setRowCount(0);
        try {
            ResultSet questionsResult = DBConnection.getStatement().executeQuery("SELECT q.question_id, c.name, q.question_text, "
                    + "q.asked_at FROM Customer_Question q JOIN Customer c ON q.cust_id=c.cust_id "
                    + "WHERE q.answer_text IS NULL ORDER BY q.asked_at ASC");
            int count = 0;
            while (questionsResult.next()) {
                model.addRow(new Object[]{
                        questionsResult.getInt("question_id"),
                        questionsResult.getString("name"),
                        questionsResult.getString("question_text"), "(unanswered)",
                        questionsResult.getString("asked_at"), "-"});
                count++;
            }
            status.setText(count + " unanswered question(s).");
        }
        catch (SQLException ex) {
            status.setText("Error: " + ex.getMessage());
        }
    }
}