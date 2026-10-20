import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedWriter;
import java.io.File;
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
    private JTextField contactField;
    private JComboBox<String> yearLevelBox;
    private JComboBox<String> semesterBox;
    private JComboBox<String> courseBox;
    private DefaultTableModel recordsModel;
    private JButton registerButton;
    private JButton viewButton;
    private JButton clearButton;

    // Two views inside the same application window.
    private CardLayout screenLayout;
    private JPanel screenCards;
    private JPanel registrationHeading;
    private boolean historyVisible;
    private JTextField historyWindowSearchField;
    private JLabel historyWindowStatus;
    private JButton historyWindowSearchButton;
    private JButton historyWindowAllButton;
    private JButton historyEditButton;
    private JButton historyDeleteButton;
    private JButton historyExportButton;
    private JTable historyTable;

    public StudentRegistrationApp() {
        setTitle("Student Registration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(680, 600));
        setSize(900, 680);
        setLocationRelativeTo(null);


        GradientBackground background = new GradientBackground();
        background.setLayout(new BorderLayout(0, 20));
        background.setBorder(new EmptyBorder(22, 26, 22, 26));
        setContentPane(background);

        recordsModel = new DefaultTableModel(
                new Object[]{"Student ID", "First Name", "Last Name", "Age", "Course", "Contact Number", "Year Level", "Semester", "Registered"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        registrationHeading = new JPanel();
        registrationHeading.setOpaque(false);
        registrationHeading.setLayout(new BoxLayout(registrationHeading, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("INTERWORLD COLLEGES FOUNDATION INC.");
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
        eyebrow.setForeground(new Color(222, 242, 232, 225));
        eyebrow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Student Registration");
        title.setFont(new Font("SansSerif", Font.BOLD, 29));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        registrationHeading.add(eyebrow);
        registrationHeading.add(Box.createVerticalStrut(8));
        registrationHeading.add(title);
        background.add(registrationHeading, BorderLayout.NORTH);


        GlassCard formCard = new GlassCard();
        formCard.setLayout(new BorderLayout(0, 12));
        formCard.setBorder(new EmptyBorder(18, 20, 20, 20));

        JLabel formTitle = new JLabel("Your details");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        formTitle.setForeground(new Color(25, 80, 72));
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 4, 12, 6));
        form.setOpaque(false);

        idField = createTextField("e.g. 2026-001");
        firstNameField = createTextField("First name");
        lastNameField = createTextField("Last name");
        ageField = createTextField("Age");
        contactField = createTextField("09XXXXXXXXX");
        yearLevelBox = new JComboBox<>(new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year"});
        semesterBox = new JComboBox<>(new String[]{"1st Semester", "2nd Semester"});
        courseBox = new JComboBox<>(new String[]{"BSCS", "BSTM", "BSA", "BSED", "BSBA", "BPA"});
        styleComboBox(yearLevelBox);
        styleComboBox(semesterBox);
        styleComboBox(courseBox);

        addFormRow(form, "STUDENT ID", idField);
        addFormRow(form, "AGE", ageField);
        addFormRow(form, "FIRST NAME", firstNameField);
        addFormRow(form, "LAST NAME", lastNameField);
        addFormRow(form, "CONTACT NUMBER", contactField);
        addFormRow(form, "YEAR LEVEL", yearLevelBox);
        addFormRow(form, "COURSE", courseBox);
        addFormRow(form, "SEMESTER", semesterBox);
        formCard.add(form, BorderLayout.CENTER);

        registerButton = new RoundedButton("Register", true);
        clearButton = new RoundedButton("Clear", false, true);
        viewButton = new RoundedButton("View Saved Records", false, true);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 14, 0));
        buttons.setOpaque(false);
        buttons.add(registerButton);
        buttons.add(clearButton);
        buttons.add(viewButton);

        formCard.add(buttons, BorderLayout.SOUTH);

        formCard.setPreferredSize(new Dimension(780, 370));

        JPanel formHost = new JPanel(new GridBagLayout());
        formHost.setOpaque(false);
        formHost.add(formCard, new GridBagConstraints());
        screenCards = new JPanel(new CardLayout());
        screenCards.setOpaque(false);
        screenLayout = (CardLayout) screenCards.getLayout();
        screenCards.add(formHost, "registration");
        screenCards.add(createHistoryView(), "history");
        background.add(screenCards, BorderLayout.CENTER);

        registerButton.addActionListener(e -> registerStudent());
        clearButton.addActionListener(e -> clearForm());
        viewButton.addActionListener(e -> openHistoryWindow());
    }


    private void openHistoryWindow() {
        historyWindowSearchField.setText("");
        historyVisible = true;
        registrationHeading.setVisible(false);
        screenLayout.show(screenCards, "history");
        getContentPane().revalidate();
        getContentPane().repaint();
        viewSavedRecords("");
    }

    private void showRegistrationView() {
        historyVisible = false;
        registrationHeading.setVisible(true);
        screenLayout.show(screenCards, "registration");
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private JPanel createHistoryView() {
        JPanel historyView = new JPanel(new BorderLayout(0, 16));
        historyView.setOpaque(false);
        historyView.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel navigation = new JPanel(new BorderLayout());
        navigation.setOpaque(false);
        JButton backButton = new RoundedButton("← Back", false);
        backButton.addActionListener(e -> showRegistrationView());
        navigation.add(backButton, BorderLayout.WEST);
        topPanel.add(navigation);
        topPanel.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("Registration history");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("All saved student records");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(new Color(220, 232, 255, 225));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
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
        historyView.add(topPanel, BorderLayout.NORTH);

        historyTable = new JTable(recordsModel);
        styleRecordsTable(historyTable);
        historyTable.setRowHeight(36);
        historyTable.setFont(new Font("SansSerif", Font.PLAIN, 15));
        historyTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        JScrollPane historyScrollPane = new JScrollPane(historyTable);
        historyScrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(255, 255, 255, 100)));
        historyScrollPane.getViewport().setBackground(new Color(249, 252, 250));
        historyScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        historyView.add(historyScrollPane, BorderLayout.CENTER);

        historyWindowSearchButton.addActionListener(e ->
                viewSavedRecords(historyWindowSearchField.getText().trim()));
        historyWindowSearchField.addActionListener(e ->
                viewSavedRecords(historyWindowSearchField.getText().trim()));
        historyWindowAllButton.addActionListener(e -> {
            historyWindowSearchField.setText("");
            viewSavedRecords("");
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        historyEditButton = new RoundedButton("Edit Selected", false);
        historyDeleteButton = new RoundedButton("Delete Selected", false);
        historyExportButton = new RoundedButton("Export CSV", false);
        actions.add(historyEditButton);
        actions.add(historyDeleteButton);
        actions.add(historyExportButton);
        historyView.add(actions, BorderLayout.SOUTH);
        historyEditButton.addActionListener(e -> editSelectedRecord());
        historyDeleteButton.addActionListener(e -> deleteSelectedRecord());
        historyExportButton.addActionListener(e -> exportHistoryCsv());

        return historyView;
    }

    private void styleRecordsTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(new Color(249, 252, 250));
        table.setForeground(new Color(33, 57, 53));
        table.setGridColor(new Color(205, 223, 215));
        table.setSelectionBackground(new Color(40, 145, 119));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(25, 80, 72));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBackground(new Color(25, 80, 72));
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setOpaque(true);
        header.setDefaultRenderer(headerRenderer);
        header.setReorderingAllowed(false);
    }

    private void addFormRow(JPanel panel, String labelText, JComponent input) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(43, 79, 70));
        panel.add(label);
        panel.add(input);
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(new Color(33, 57, 53));
        field.setCaretColor(new Color(33, 57, 53));
        field.setOpaque(true);
        field.setBackground(new Color(249, 252, 250));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(170, 198, 187)),
                new EmptyBorder(7, 9, 7, 9)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setForeground(new Color(33, 57, 53));
        combo.setBackground(new Color(249, 252, 250));
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(170, 198, 187)),
                new EmptyBorder(5, 7, 5, 7)
        ));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(7, 9, 7, 9));
                label.setForeground(isSelected ? Color.WHITE : new Color(33, 57, 53));
                label.setBackground(isSelected
                        ? new Color(35, 123, 101)
                        : new Color(249, 252, 250));
                return label;
            }
        });
    }

    private void registerStudent() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String ageText = ageField.getText().trim();
        String contact = contactField.getText().trim();
        String yearLevel = (String) yearLevelBox.getSelectedItem();
        String semester = (String) semesterBox.getSelectedItem();
        String course = (String) courseBox.getSelectedItem();

        if (id.isEmpty()) {
            showMessage("Please enter a Student ID.", "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String validationError = validateStudentDetails(
                firstName, lastName, ageText, contact);
        if (validationError != null) {
            showMessage(validationError, "Check Student Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final int age = Integer.parseInt(ageText);

        setButtonsEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            private boolean saved;

            @Override
            protected String doInBackground() {
                String sql = "INSERT INTO students "
                        + "(student_id, first_name, last_name, age, course, contact_number, year_level, semester) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (Connection connection = DriverManager.getConnection(
                            DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement statement =
                            connection.prepareStatement(sql)) {
                    statement.setString(1, id);
                    statement.setString(2, firstName);
                    statement.setString(3, lastName);
                    statement.setInt(4, age);
                    statement.setString(5, course);
                    statement.setString(6, contact);
                    statement.setString(7, yearLevel);
                    statement.setString(8, semester);
                    statement.executeUpdate();
                    saved = true;
                    return "NEW REGISTRATION\n"
                            + "================================\n"
                            + "Student ID : " + id + "\n"
                            + "First Name : " + firstName + "\n"
                            + "Last Name  : " + lastName + "\n"
                            + "Age        : " + age + "\n"
                            + "Contact    : " + contact + "\n"
                            + "Year Level : " + yearLevel + "\n"
                            + "Semester   : " + semester + "\n"
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
                        if (historyVisible) {
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
                String sql = "SELECT student_id, first_name, last_name, age, course, contact_number, "
                        + "year_level, semester, registered_at FROM students "
                        + (searching ? "WHERE student_id LIKE ? OR first_name LIKE ? OR last_name LIKE ? "
                            + "OR contact_number LIKE ? " : "")
                        + "ORDER BY registered_at DESC";
                List<Object[]> rows = new ArrayList<>();

                try (Connection connection = DriverManager.getConnection(
                            DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    if (searching) {
                        String pattern = "%" + searchText + "%";
                        for (int i = 1; i <= 4; i++) {
                            statement.setString(i, pattern);
                        }
                    }
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            rows.add(new Object[]{
                                result.getString("student_id"),
                                result.getString("first_name"),
                                result.getString("last_name"),
                                result.getInt("age"),
                                result.getString("course"),
                                result.getString("contact_number"),
                                result.getString("year_level"),
                                result.getString("semester"),
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

    private String validateStudentDetails(String firstName, String lastName, String ageText,
            String contact) {
        if (firstName.isEmpty() || lastName.isEmpty() || ageText.isEmpty()
                || contact.isEmpty()) {
            return "Please complete all student details.";
        }
        try {
            int age = Integer.parseInt(ageText);
            if (age < 1 || age > 120) return "Age must be from 1 to 120.";
        } catch (NumberFormatException ex) {
            return "Age must be a whole number.";
        }
        if (!contact.matches("^\\+?[0-9][0-9 ()-]{6,19}$")) {
            return "Please enter a valid contact number.";
        }
        return null;
    }
    private void editSelectedRecord() {
        int selected = historyTable.getSelectedRow();
        if (selected < 0) {
            showMessage("Select a record in the table first.", "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = historyTable.convertRowIndexToModel(selected);
        String id = cellValue(row, 0);
        JTextField first = new JTextField(cellValue(row, 1));
        JTextField last = new JTextField(cellValue(row, 2));
        JTextField age = new JTextField(cellValue(row, 3));
        JComboBox<String> course = new JComboBox<>(new String[]{"BSCS", "BSTM", "BSA", "BSED", "BSBA", "BPA"});
        course.setSelectedItem(cellValue(row, 4));
        JTextField contact = new JTextField(cellValue(row, 5));
        JComboBox<String> year = new JComboBox<>(new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year"});
        year.setSelectedItem(cellValue(row, 6));
        JComboBox<String> semester = new JComboBox<>(new String[]{"1st Semester", "2nd Semester", "Summer"});
        semester.setSelectedItem(cellValue(row, 7));

        JPanel editor = new JPanel(new GridLayout(0, 2, 8, 8));
        editor.add(new JLabel("First name")); editor.add(first);
        editor.add(new JLabel("Last name")); editor.add(last);
        editor.add(new JLabel("Age")); editor.add(age);
        editor.add(new JLabel("Course")); editor.add(course);
        editor.add(new JLabel("Contact number")); editor.add(contact);
        editor.add(new JLabel("Year level")); editor.add(year);
        editor.add(new JLabel("Semester")); editor.add(semester);
        int answer = JOptionPane.showConfirmDialog(this, editor, "Edit Student " + id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;

        String error = validateStudentDetails(first.getText().trim(), last.getText().trim(),
                age.getText().trim(), contact.getText().trim());
        if (error != null) {
            showMessage(error, "Check Student Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setButtonsEnabled(false);
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws SQLException {
                String sql = "UPDATE students SET first_name=?, last_name=?, age=?, course=?, "
                        + "contact_number=?, year_level=?, semester=? WHERE student_id=?";
                try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement q = c.prepareStatement(sql)) {
                    q.setString(1, first.getText().trim());
                    q.setString(2, last.getText().trim());
                    q.setInt(3, Integer.parseInt(age.getText().trim()));
                    q.setString(4, (String) course.getSelectedItem());
                    q.setString(5, contact.getText().trim());
                    q.setString(6, (String) year.getSelectedItem());
                    q.setString(7, (String) semester.getSelectedItem());
                    q.setString(8, id);
                    return q.executeUpdate();
                }
            }
            @Override protected void done() {
                setButtonsEnabled(true);
                try {
                    get();
                    viewSavedRecords(historyWindowSearchField.getText().trim());
                    showMessage("Student record updated.", "Update Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showMessage("Could not update. Run student_registration_update.sql first.\n"
                            + exceptionMessage(ex), "Update Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedRecord() {
        int selected = historyTable.getSelectedRow();
        if (selected < 0) {
            showMessage("Select a record in the table first.", "No Record Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = historyTable.convertRowIndexToModel(selected);
        String id = cellValue(row, 0);
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the registration for Student ID " + id + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        setButtonsEnabled(false);
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws SQLException {
                try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement q = c.prepareStatement("DELETE FROM students WHERE student_id=?")) {
                    q.setString(1, id);
                    return q.executeUpdate();
                }
            }
            @Override protected void done() {
                setButtonsEnabled(true);
                try {
                    get();
                    viewSavedRecords(historyWindowSearchField.getText().trim());
                    showMessage("Student record deleted.", "Delete Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    showMessage("Could not delete. Run student_registration_update.sql first.\n"
                            + exceptionMessage(ex), "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void exportHistoryCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export registration history");
        chooser.setSelectedFile(new File("registration-history.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (int col = 0; col < recordsModel.getColumnCount(); col++) {
                if (col > 0) writer.write(",");
                writer.write(csvCell(recordsModel.getColumnName(col)));
            }
            writer.newLine();
            for (int r = 0; r < recordsModel.getRowCount(); r++) {
                for (int col = 0; col < recordsModel.getColumnCount(); col++) {
                    if (col > 0) writer.write(",");
                    writer.write(csvCell(recordsModel.getValueAt(r, col)));
                }
                writer.newLine();
            }
            showMessage("History exported to:\n" + file.getAbsolutePath(), "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showMessage("Could not export CSV.\n" + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String cellValue(int row, int column) {
        Object value = recordsModel.getValueAt(row, column);
        return value == null ? "" : String.valueOf(value);
    }

    private String exceptionMessage(Exception ex) {
        Throwable cause = ex.getCause();
        return cause == null || cause.getMessage() == null ? ex.getMessage() : cause.getMessage();
    }

    private void clearForm() {
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        ageField.setText("");
        contactField.setText("");
        yearLevelBox.setSelectedIndex(0);
        semesterBox.setSelectedIndex(0);
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
            historyEditButton.setEnabled(enabled);
            historyDeleteButton.setEnabled(enabled);
            historyExportButton.setEnabled(enabled);
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
            g.setColor(new Color(8, 45, 43, 42));
            g.fill(new RoundRectangle2D.Float(2, 7,
                    getWidth() - 4, getHeight() - 9, arc, arc));
            g.setColor(new Color(248, 253, 250, 238));
            g.fill(new RoundRectangle2D.Float(1, 1,
                    getWidth() - 3, getHeight() - 10, arc, arc));
            g.setColor(new Color(255, 255, 255, 220));
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
                    new Color(19, 67, 68), getWidth(), getHeight(),
                    new Color(43, 119, 103)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 220, 125, 45));
            g.fill(new Ellipse2D.Float(getWidth() - 190, -100, 300, 300));
            g.setColor(new Color(113, 211, 171, 38));
            g.fill(new Ellipse2D.Float(-130, getHeight() - 220, 330, 330));
            g.dispose();
        }
    }

    private static class RoundedButton extends JButton {
        private final boolean primary;
        private final boolean lightSurface;

        RoundedButton(String text, boolean primary) {
            this(text, primary, false);
        }

        RoundedButton(String text, boolean primary, boolean lightSurface) {
            super(text);
            this.primary = primary;
            this.lightSurface = lightSurface;
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(lightSurface ? new Color(25, 80, 72) : Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 10, 9, 10));
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
                fill = primary ? new Color(15, 126, 101) : (lightSurface ? new Color(207, 231, 220) : new Color(255, 255, 255, 62));
            } else if (getModel().isRollover()) {
                fill = primary ? new Color(34, 164, 132) : (lightSurface ? new Color(224, 241, 233) : new Color(255, 255, 255, 48));
            } else {
                fill = primary ? new Color(19, 145, 116) : (lightSurface ? new Color(236, 246, 240) : new Color(255, 255, 255, 29));
            }
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Float(0, 0,
                    getWidth(), getHeight(), 22, 22));
            g.setColor(lightSurface ? new Color(125, 169, 151) : new Color(255, 255, 255, primary ? 75 : 68));
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










