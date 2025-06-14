package org.timetable.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * An enhanced GUI for viewing timetables with filtering options.
 */
public class EnhancedTimetableViewer extends JFrame {
    private JTabbedPane tabbedPane;
    private JList<String> studentList;
    private JList<String> teacherList;
    private JTextField studentFilterField;
    private JTextField teacherFilterField;
    private JEditorPane htmlViewer;
    private DefaultListModel<String> studentListModel;
    private DefaultListModel<String> teacherListModel;
    private List<String> allStudentFiles;
    private List<String> allTeacherFiles;
    
    public EnhancedTimetableViewer() {
        super("Enhanced Timetable Viewer");
        initComponents();
        setupLayout();
        loadTimetables();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        
        // Student list
        studentListModel = new DefaultListModel<>();
        studentList = new JList<>(studentListModel);
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selected = studentList.getSelectedValue();
                    if (selected != null) {
                        loadHTMLFile(new File("output/student_timetables/" + selected));
                    }
                }
            }
        });
        
        // Teacher list
        teacherListModel = new DefaultListModel<>();
        teacherList = new JList<>(teacherListModel);
        teacherList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teacherList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selected = teacherList.getSelectedValue();
                    if (selected != null) {
                        loadHTMLFile(new File("output/teacher_timetables/" + selected));
                    }
                }
            }
        });
        
        // Filter fields
        studentFilterField = new JTextField();
        studentFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterStudentList();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterStudentList();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterStudentList();
            }
        });
        
        teacherFilterField = new JTextField();
        teacherFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTeacherList();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTeacherList();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTeacherList();
            }
        });
        
        // HTML viewer
        htmlViewer = new JEditorPane();
        htmlViewer.setEditable(false);
        htmlViewer.setContentType("text/html");
        
        // Initialize lists
        allStudentFiles = new ArrayList<>();
        allTeacherFiles = new ArrayList<>();
    }
    
    private void setupLayout() {
        // Student panel
        JPanel studentPanel = new JPanel(new BorderLayout());
        JPanel studentFilterPanel = new JPanel(new BorderLayout());
        studentFilterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        studentFilterPanel.add(studentFilterField, BorderLayout.CENTER);
        studentPanel.add(studentFilterPanel, BorderLayout.NORTH);
        studentPanel.add(new JScrollPane(studentList), BorderLayout.CENTER);
        
        // Teacher panel
        JPanel teacherPanel = new JPanel(new BorderLayout());
        JPanel teacherFilterPanel = new JPanel(new BorderLayout());
        teacherFilterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        teacherFilterPanel.add(teacherFilterField, BorderLayout.CENTER);
        teacherPanel.add(teacherFilterPanel, BorderLayout.NORTH);
        teacherPanel.add(new JScrollPane(teacherList), BorderLayout.CENTER);
        
        // Add tabs
        tabbedPane.addTab("Student Timetables", studentPanel);
        tabbedPane.addTab("Teacher Timetables", teacherPanel);
        
        // Main layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                             tabbedPane, new JScrollPane(htmlViewer));
        splitPane.setDividerLocation(300);
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTimetables();
            }
        });
        
        getContentPane().add(refreshButton, BorderLayout.SOUTH);
    }
    
    private void loadTimetables() {
        // Load student timetables
        File studentDir = new File("output/student_timetables");
        if (studentDir.exists() && studentDir.isDirectory()) {
            loadFilesFromDirectory(studentDir, allStudentFiles, studentListModel);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Student timetables directory not found. Please run the timetable generator first.",
                "Warning", JOptionPane.WARNING_MESSAGE);
        }
        
        // Load teacher timetables
        File teacherDir = new File("output/teacher_timetables");
        if (teacherDir.exists() && teacherDir.isDirectory()) {
            loadFilesFromDirectory(teacherDir, allTeacherFiles, teacherListModel);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Teacher timetables directory not found. Please run the timetable generator first.",
                "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void loadFilesFromDirectory(File dir, List<String> allFiles, DefaultListModel<String> model) {
        allFiles.clear();
        model.clear();
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".html")) {
                allFiles.add(file.getName());
                model.addElement(file.getName());
            }
        }
    }
    
    private void filterStudentList() {
        String filter = studentFilterField.getText().toLowerCase();
        studentListModel.clear();
        
        for (String file : allStudentFiles) {
            if (file.toLowerCase().contains(filter)) {
                studentListModel.addElement(file);
            }
        }
    }
    
    private void filterTeacherList() {
        String filter = teacherFilterField.getText().toLowerCase();
        teacherListModel.clear();
        
        for (String file : allTeacherFiles) {
            if (file.toLowerCase().contains(filter)) {
                teacherListModel.addElement(file);
            }
        }
    }
    
    private void loadHTMLFile(File file) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            htmlViewer.setText(content);
            htmlViewer.setCaretPosition(0);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EnhancedTimetableViewer().setVisible(true);
            }
        });
    }
} 