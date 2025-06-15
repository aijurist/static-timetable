package org.timetable.gui;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced GUI for viewing timetables organized by department.
 */
public class DepartmentTimetableViewer extends JFrame {
    private JTree departmentTree;
    private JEditorPane htmlViewer;
    private JSplitPane splitPane;
    private JLabel statusLabel;
    
    // Patterns for parsing timetable filenames
    private static final Pattern STUDENT_PATTERN = Pattern.compile("timetable_([A-Z-]+)_Y(\\d)_([A-Z]).html");
    private static final Pattern TEACHER_PATTERN = Pattern.compile("timetable_teacher_([A-Z-]+)_Teacher_(\\d+).html");
    
    public DepartmentTimetableViewer() {
        super("Department Timetable Viewer");
        initComponents();
        setupLayout();
        loadTimetablesByDepartment();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        departmentTree = new JTree();
        departmentTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
                    departmentTree.getLastSelectedPathComponent();
                
                if (node == null || !node.isLeaf()) {
                    statusLabel.setText("Select a timetable to view");
                    return;
                }
                
                TimetableNode timetableNode = (TimetableNode) node.getUserObject();
                if (timetableNode.getFile() != null && timetableNode.getFile().exists()) {
                    loadHTMLFile(timetableNode.getFile());
                    statusLabel.setText("Viewing: " + timetableNode.getDisplayName());
                }
            }
        });
        
        htmlViewer = new JEditorPane();
        htmlViewer.setEditable(false);
        htmlViewer.setContentType("text/html");
        htmlViewer.setText("<html><body><h2>Select a timetable from the left panel to view</h2></body></html>");
        
        statusLabel = new JLabel("Ready - Select a timetable to view");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }
    
    private void setupLayout() {
        // Create tree panel with title
        JPanel treePanel = new JPanel(new BorderLayout());
        JLabel treeTitle = new JLabel("Timetables by Department", JLabel.CENTER);
        treeTitle.setFont(treeTitle.getFont().deriveFont(Font.BOLD, 14f));
        treeTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        treePanel.add(treeTitle, BorderLayout.NORTH);
        
        JScrollPane treeScrollPane = new JScrollPane(departmentTree);
        treeScrollPane.setPreferredSize(new Dimension(350, 700));
        treePanel.add(treeScrollPane, BorderLayout.CENTER);
        
        // Create HTML viewer panel
        JScrollPane htmlScrollPane = new JScrollPane(htmlViewer);
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, htmlScrollPane);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.25);
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void loadTimetablesByDepartment() {
        File studentDir = new File("output/student_timetables");
        File teacherDir = new File("output/teacher_timetables");
        
        if (!studentDir.exists() && !teacherDir.exists()) {
            JOptionPane.showMessageDialog(this, 
                "Timetable directories not found. Please run the timetable generator first.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Organize timetables by department
        Map<String, DepartmentData> departmentMap = new TreeMap<>();
        
        // Load student timetables
        if (studentDir.exists()) {
            loadStudentTimetables(studentDir, departmentMap);
        }
        
        // Load teacher timetables
        if (teacherDir.exists()) {
            loadTeacherTimetables(teacherDir, departmentMap);
        }
        
        // Create tree structure
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Departments");
        
        for (Map.Entry<String, DepartmentData> entry : departmentMap.entrySet()) {
            String deptName = entry.getKey();
            DepartmentData deptData = entry.getValue();
            
            DefaultMutableTreeNode deptNode = new DefaultMutableTreeNode(
                String.format("%s (%d students, %d teachers)", 
                    deptName, deptData.studentTimetables.size(), deptData.teacherTimetables.size()));
            
            // Add student timetables
            if (!deptData.studentTimetables.isEmpty()) {
                DefaultMutableTreeNode studentNode = new DefaultMutableTreeNode("Student Timetables");
                
                // Group by year
                Map<String, List<TimetableNode>> yearGroups = new TreeMap<>();
                for (TimetableNode node : deptData.studentTimetables) {
                    String year = "Year " + node.getYear();
                    yearGroups.computeIfAbsent(year, k -> new ArrayList<>()).add(node);
                }
                
                for (Map.Entry<String, List<TimetableNode>> yearEntry : yearGroups.entrySet()) {
                    DefaultMutableTreeNode yearNode = new DefaultMutableTreeNode(
                        yearEntry.getKey() + " (" + yearEntry.getValue().size() + " sections)");
                    
                    // Sort sections
                    yearEntry.getValue().sort(Comparator.comparing(TimetableNode::getSection));
                    
                    for (TimetableNode timetable : yearEntry.getValue()) {
                        DefaultMutableTreeNode timetableLeaf = new DefaultMutableTreeNode(timetable);
                        yearNode.add(timetableLeaf);
                    }
                    
                    studentNode.add(yearNode);
                }
                
                deptNode.add(studentNode);
            }
            
            // Add teacher timetables
            if (!deptData.teacherTimetables.isEmpty()) {
                DefaultMutableTreeNode teacherNode = new DefaultMutableTreeNode("Teacher Timetables");
                
                // Sort teachers
                deptData.teacherTimetables.sort(Comparator.comparing(TimetableNode::getTeacherNumberInt));
                
                for (TimetableNode timetable : deptData.teacherTimetables) {
                    DefaultMutableTreeNode timetableLeaf = new DefaultMutableTreeNode(timetable);
                    teacherNode.add(timetableLeaf);
                }
                
                deptNode.add(teacherNode);
            }
            
            root.add(deptNode);
        }
        
        DefaultTreeModel model = new DefaultTreeModel(root);
        departmentTree.setModel(model);
        
        // Expand all department nodes
        for (int i = 0; i < departmentTree.getRowCount(); i++) {
            departmentTree.expandRow(i);
        }
        
        statusLabel.setText(String.format("Loaded %d departments with timetables", departmentMap.size()));
    }
    
    private void loadStudentTimetables(File dir, Map<String, DepartmentData> departmentMap) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null) return;
        
        for (File file : files) {
            Matcher matcher = STUDENT_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                String dept = matcher.group(1);
                String year = matcher.group(2);
                String section = matcher.group(3);
                
                TimetableNode node = new TimetableNode(file, dept, year, section, null);
                departmentMap.computeIfAbsent(dept, k -> new DepartmentData())
                           .studentTimetables.add(node);
            }
        }
    }
    
    private void loadTeacherTimetables(File dir, Map<String, DepartmentData> departmentMap) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files == null) return;
        
        for (File file : files) {
            Matcher matcher = TEACHER_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                String dept = matcher.group(1);
                String teacherNum = matcher.group(2);
                
                TimetableNode node = new TimetableNode(file, dept, null, null, teacherNum);
                departmentMap.computeIfAbsent(dept, k -> new DepartmentData())
                           .teacherTimetables.add(node);
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
            statusLabel.setText("Error loading timetable");
        }
    }
    
    /**
     * Data structure to hold department timetables.
     */
    private static class DepartmentData {
        List<TimetableNode> studentTimetables = new ArrayList<>();
        List<TimetableNode> teacherTimetables = new ArrayList<>();
    }
    
    /**
     * Node representing a timetable file.
     */
    private static class TimetableNode {
        private File file;
        private String department;
        private String year;
        private String section;
        private String teacherNumber;
        
        public TimetableNode(File file, String department, String year, String section, String teacherNumber) {
            this.file = file;
            this.department = department;
            this.year = year;
            this.section = section;
            this.teacherNumber = teacherNumber;
        }
        
        public File getFile() { return file; }
        public String getDepartment() { return department; }
        public String getYear() { return year; }
        public String getSection() { return section; }
        public String getTeacherNumber() { return teacherNumber; }
        
        public int getTeacherNumberInt() {
            return teacherNumber != null ? Integer.parseInt(teacherNumber) : 0;
        }
        
        public String getDisplayName() {
            if (teacherNumber != null) {
                return String.format("%s Teacher %s", department, teacherNumber);
            } else {
                return String.format("%s Year %s Section %s", department, year, section);
            }
        }
        
        @Override
        public String toString() {
            if (teacherNumber != null) {
                return String.format("Teacher %s", teacherNumber);
            } else {
                return String.format("Section %s", section);
            }
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
        
        SwingUtilities.invokeLater(() -> {
            new DepartmentTimetableViewer().setVisible(true);
        });
    }
}