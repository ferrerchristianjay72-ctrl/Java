import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.util.ArrayList;
import java.util.List;

public class StudentRegistrationApp extends JFrame {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/student_registration"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "student_app";
    // Keep this in sync with the password set for student_app in MySQL.
    private static final String DB_PASSWORD = "12345678910";

    private JTextField idField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField ageField;
    private JComboBox<String> courseBox;
    private DefaultTableModel recordsModel;
    private JButton registerButton;
    private JButton viewButton;
    private JButton clearButton;

    // Components for the separate, history-only window.
    private JDialog historyDialog;
    private JTextField historyWindowSearchField;
    private JLabel historyWindowStatus;
    private JButton historyWindowSearchButton;
    private JButton historyWindowAllButton;

    public StudentRegistrationApp() {
        setTitle("Student Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(680, 850));
        setSize(760, 850);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        GradientBackground background = new GradientBackground();
        background.setLayout(new BorderLayout(0, 20));
        background.setBorder(new EmptyBorder(28, 34, 30, 34));
        setContentPane(background);

        recordsModel = new DefaultTableModel(
                new Object[]{"Student ID", "First Name", "Last Name", "Age", "Course", "Registered"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("INTERWORLD COLLEGES FOUNDATION INC.");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setForeground(new Color(204, 224, 255, 205));
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Student Registration");
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(eyebrow);
        heading.add(Box.createVerticalStrut(8));
        heading.add(title);
        background.add(heading, BorderLayout.NORTH);


        GlassCard formCard = new GlassCard();
        formCard.setLayout(new BorderLayout(0, 12));
        formCard.setBorder(new EmptyBorder(22, 24, 28, 24));

        JLabel formTitle = new JLabel("Your details");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        formTitle.setForeground(Color.WHITE);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(5, 2, 14, 8));
        form.setOpaque(false);

        idField = createTextField("e.g. 2026-001");
        firstNameField = createTextField("First name");
        lastNameField = createTextField("Last name");
        ageField = createTextField("Age");
        courseBox = new JComboBox<>(new String[]{"BSCS", "BSTM", "BSA", "BSED", "BSBA", "BPA"});
        styleComboBox(courseBox);

        addFormRow(form, "STUDENT ID", idField);
        addFormRow(form, "FIRST NAME", firstNameField);
        addFormRow(form, "LAST NAME", lastNameField);
        addFormRow(form, "AGE", ageField);
        addFormRow(form, "COURSE", courseBox);
        formCard.add(form, BorderLayout.CENTER);

        registerButton = new RoundedButton("Register", true);
        clearButton = new RoundedButton("Clear", false);
        viewButton = new RoundedButton("View Saved Records", false);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 14, 0));
        buttons.setOpaque(false);
        buttons.add(registerButton);
        buttons.add(clearButton);
        buttons.add(viewButton);

        formCard.add(buttons, BorderLayout.SOUTH);

        formCard.setPreferredSize(new Dimension(1000, 408));

        JPanel formHost = new JPanel(new GridBagLayout());
        formHost.setOpaque(false);
        formHost.add(formCard, new GridBagConstraints());
        background.add(formHost, BorderLayout.CENTER);

        registerButton.addActionListener(e -> registerStudent());
        clearButton.addActionListener(e -> clearForm());
        viewButton.addActionListener(e -> openHistoryWindow());
    }


    private void openHistoryWindow() {
        if (historyDialog == null) {
            historyDialog = new JDialog(this, "Registration History", false);
            historyDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            historyDialog.setMinimumSize(new Dimension(850, 520));
            historyDialog.setSize(1180, 740);
            historyDialog.setLocationRelativeTo(this);

            GradientBackground windowBackground = new GradientBackground();
            windowBackground.setLayout(new BorderLayout(0, 16));
            windowBackground.setBorder(new EmptyBorder(26, 30, 26, 30));
            historyDialog.setContentPane(windowBackground);

            JPanel topPanel = new JPanel();
            topPanel.setOpaque(false);
            topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("Registration history");
            title.setFont(new Font("SansSerif", Font.BOLD, 28));
            title.setForeground(Color.WHITE);
            JLabel subtitle = new JLabel("All saved student records");
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
            subtitle.setForeground(new Color(220, 232, 255, 225));
            topPanel.add(title);
            topPanel.add(Box.createVerticalStrut(4));
            topPanel.add(subtitle);
            topPanel.add(Box.createVerticalStrut(18));

            JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
            searchPanel.setOpaque(false);
            historyWindowSearchField = createTextField("Search by student ID or name");
            historyWindowSearchButton = new RoundedButton("Search", false);
            historyWindowAllButton = new RoundedButton("Show All", false);
            JPanel searchButtons = new JPanel(new GridLayout(1, 2, 8, 0));
            searchButtons.setOpaque(false);
            searchButtons.add(historyWindowSearchButton);
            searchButtons.add(historyWindowAllButton);
            searchPanel.add(historyWindowSearchField, BorderLayout.CENTER);
            searchPanel.add(searchButtons, BorderLayout.EAST);
            topPanel.add(searchPanel);
            topPanel.add(Box.createVerticalStrut(10));

            historyWindowStatus = new JLabel("Loading saved records...");
            historyWindowStatus.setFont(new Font("SansSerif", Font.PLAIN, 13));
            historyWindowStatus.setForeground(new Color(225, 235, 255, 225));
            topPanel.add(historyWindowStatus);
            windowBackground.add(topPanel, BorderLayout.NORTH);

            // This table shares the main table model, so both views stay in sync.
            JTable windowTable = new JTable(recordsModel);
            styleRecordsTable(windowTable);
            windowTable.setRowHeight(36);
            windowTable.setFont(new Font("SansSerif", Font.PLAIN, 15));
            windowTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
            JScrollPane windowScrollPane = new JScrollPane(windowTable);
            windowScrollPane.setBorder(BorderFactory.createLineBorder(
                    new Color(255, 255, 255, 100)));
            windowScrollPane.getViewport().setBackground(new Color(245, 248, 255));
            windowScrollPane.getVerticalScrollBar().setUnitIncrement(18);
            windowBackground.add(windowScrollPane, BorderLayout.CENTER);

            historyWindowSearchButton.addActionListener(e ->
                    viewSavedRecords(historyWindowSearchField.getText().trim()));
            historyWindowSearchField.addActionListener(e ->
                    viewSavedRecords(historyWindowSearchField.getText().trim()));
            historyWindowAllButton.addActionListener(e -> {
                historyWindowSearchField.setText("");
                viewSavedRecords("");
            });
        }

        historyWindowSearchField.setText("");
        historyDialog.setVisible(true);
        historyDialog.toFront();
        viewSavedRecords("");
    }

    private void styleRecordsTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(new Color(245, 248, 255));
        table.setForeground(new Color(25, 39, 67));
        table.setGridColor(new Color(202, 214, 235));
        table.setSelectionBackground(new Color(105, 151, 238));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(37, 58, 98));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBackground(new Color(37, 58, 98));
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setOpaque(true);
        header.setDefaultRenderer(headerRenderer);
        header.setReorderingAllowed(false);
    }

    private void addFormRow(JPanel panel, String labelText, JComponent input) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(218, 232, 255, 225));
        panel.add(label);
        panel.add(input);
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(new Color(25, 39, 67));
        field.setCaretColor(new Color(25, 39, 67));
        field.setOpaque(true);
        field.setBackground(new Color(245, 248, 255));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 65)),
                new EmptyBorder(9, 11, 9, 11)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setForeground(new Color(25, 39, 67));
        combo.setBackground(new Color(245, 248, 255));
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 65)),
                new EmptyBorder(7, 8, 7, 8)
        ));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(7, 9, 7, 9));
                label.setForeground(isSelected ? Color.WHITE : new Color(25, 39, 67));
                label.setBackground(isSelected
                        ? new Color(85, 115, 174)
                        : new Color(245, 248, 255));
                return label;
            }
        });
    }

    private void registerStudent() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String ageText = ageField.getText().trim();
        String course = (String) courseBox.getSelectedItem();

        if (id.isEmpty() || firstName.isEmpty()
                || lastName.isEmpty() || ageText.isEmpty()) {
            showMessage("Please complete all fields.", "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int age;
        try {
            age = Integer.parseInt(ageText);
            if (age < 1 || age > 120) {
                showMessage("Please enter an age from 1 to 120.",
                        "Invalid Age", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            showMessage("Age must be a whole number.",
                    "Invalid Age", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setButtonsEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            private boolean saved;

            @Override
            protected String doInBackground() {
                String sql = "INSERT INTO students "
                        + "(student_id, first_name, last_name, age, course) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (Connection connection = DriverManager.getConnection(
                            DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement statement =
                            connection.prepareStatement(sql)) {
                    statement.setString(1, id);
                    statement.setString(2, firstName);
                    statement.setString(3, lastName);
                    statement.setInt(4, age);
                    statement.setString(5, course);
                    statement.executeUpdate();
                    saved = true;
                    return "NEW REGISTRATION\n"
                            + "================================\n"
                            + "Student ID : " + id + "\n"
                            + "First Name : " + firstName + "\n"
                            + "Last Name  : " + lastName + "\n"
                            + "Age        : " + age + "\n"
                            + "Course     : " + course + "\n"
                            + "--------------------------------\n\n"
                            + "Student saved successfully.";
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == 1062) {
                        return "That Student ID is already registered.";
                    }
                    return "Could not save registration.\n" + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    String message = get();
                    if (saved) {
                        JOptionPane.showMessageDialog(
                                StudentRegistrationApp.this,
                                "Student registered successfully.",
                                "Registration Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        clearForm();
                        if (historyWindowSearchField != null) {
                            historyWindowSearchField.setText("");
                        }
                        if (historyDialog != null && historyDialog.isVisible()) {
                            viewSavedRecords("");
                        }
                    } else {
                        showMessage(message, "Registration Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    showMessage("Could not finish registration:\n"
                                    + ex.getMessage(),
                            "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void viewSavedRecords(String searchText) {
        setButtonsEnabled(false);
        String loadingText = searchText.isEmpty()
                ? "Loading saved records..."
                : "Searching records...";
        if (historyWindowStatus != null) {
            historyWindowStatus.setText(loadingText);
        }

        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws SQLException {
                boolean searching = !searchText.isEmpty();
                String sql = "SELECT student_id, first_name, last_name, age, course, registered_at "
                        + "FROM students "
                        + (searching ? "WHERE student_id LIKE ? OR first_name LIKE ? OR last_name LIKE ? " : "")
                        + "ORDER BY registered_at DESC";
                List<Object[]> rows = new ArrayList<>();

                try (Connection connection = DriverManager.getConnection(
                            DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    if (searching) {
                        String pattern = "%" + searchText + "%";
                        statement.setString(1, pattern);
                        statement.setString(2, pattern);
                        statement.setString(3, pattern);
                    }
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            rows.add(new Object[]{
                                result.getString("student_id"),
                                result.getString("first_name"),
                                result.getString("last_name"),
                                result.getInt("age"),
                                result.getString("course"),
                                result.getTimestamp("registered_at")
                            });
                        }
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    List<Object[]> rows = get();
                    recordsModel.setRowCount(0);
                    for (Object[] row : rows) {
                        recordsModel.addRow(row);
                    }
                    String statusText;
                    if (rows.isEmpty()) {
                        statusText = searchText.isEmpty()
                                ? "No saved student records yet."
                                : "No records found for: " + searchText;
                    } else {
                        statusText = "Showing " + rows.size()
                                + (rows.size() == 1 ? " record" : " records")
                                + (searchText.isEmpty() ? "" : " matching: " + searchText);
                    }
                    if (historyWindowStatus != null) {
                        historyWindowStatus.setText(statusText);
                    }
                } catch (Exception ex) {
                    String message = ex.getCause() == null
                            ? ex.getMessage() : ex.getCause().getMessage();
                    if (historyWindowStatus != null) {
                        historyWindowStatus.setText("Could not read records: " + message);
                    }
                    showMessage("Could not read student records.\n" + message,
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void clearForm() {
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        ageField.setText("");
        courseBox.setSelectedIndex(0);
        idField.requestFocusInWindow();
    }

    private void setButtonsEnabled(boolean enabled) {
        registerButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        viewButton.setEnabled(enabled);
        if (historyWindowSearchButton != null) {
            historyWindowSearchButton.setEnabled(enabled);
            historyWindowAllButton.setEnabled(enabled);
        }
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private static class GlassCard extends JPanel {
        GlassCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 28;
            g.setColor(new Color(7, 17, 43, 38));
            g.fill(new RoundRectangle2D.Float(2, 7,
                    getWidth() - 4, getHeight() - 9, arc, arc));
            g.setColor(new Color(255, 255, 255, 35));
            g.fill(new RoundRectangle2D.Float(1, 1,
                    getWidth() - 3, getHeight() - 10, arc, arc));
            g.setColor(new Color(255, 255, 255, 82));
            g.setStroke(new BasicStroke(1.1f));
            g.draw(new RoundRectangle2D.Float(1.5f, 1.5f,
                    getWidth() - 4, getHeight() - 11, arc, arc));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class GradientBackground extends JPanel {
        GradientBackground() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0,
                    new Color(18, 31, 69), getWidth(), getHeight(),
                    new Color(48, 83, 139)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(115, 165, 255, 38));
            g.fill(new Ellipse2D.Float(getWidth() - 190, -100, 300, 300));
            g.setColor(new Color(112, 105, 255, 32));
            g.fill(new Ellipse2D.Float(-130, getHeight() - 220, 330, 330));
            g.dispose();
        }
    }

    private static class RoundedButton extends JButton {
        private final boolean primary;

        RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 12, 11, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill;
            if (!isEnabled()) {
                fill = new Color(155, 171, 204, 100);
            } else if (getModel().isPressed()) {
                fill = primary ? new Color(94, 143, 244) : new Color(255, 255, 255, 62);
            } else if (getModel().isRollover()) {
                fill = primary ? new Color(121, 165, 255) : new Color(255, 255, 255, 48);
            } else {
                fill = primary ? new Color(93, 143, 246) : new Color(255, 255, 255, 29);
            }
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(0, 0,
                    getWidth(), getHeight(), 22, 22));
            g.setColor(new Color(255, 255, 255, primary ? 75 : 68));
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f,
                    getWidth() - 1, getHeight() - 1, 22, 22));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Use Swing's default appearance if this is unavailable.
            }
            new StudentRegistrationApp().setVisible(true);
        });
    } 
}











