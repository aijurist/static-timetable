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

/**
 * A simple GUI for viewing timetables.
 */
public class TimetableViewer extends JFrame {
    private JTree fileTree;
    private JEditorPane htmlViewer;
    private JSplitPane splitPane;
    
    public TimetableViewer() {
        super("Timetable Viewer");
        initComponents();
        setupLayout();
        loadTimetables();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        fileTree = new JTree();
        fileTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
                    fileTree.getLastSelectedPathComponent();
                
                if (node == null || node.isRoot() || !node.isLeaf()) return;
                
                FileNode fileNode = (FileNode) node.getUserObject();
                if (fileNode.getFile().isFile() && 
                    fileNode.getFile().getName().endsWith(".html")) {
                    loadHTMLFile(fileNode.getFile());
                }
            }
        });
        
        htmlViewer = new JEditorPane();
        htmlViewer.setEditable(false);
        htmlViewer.setContentType("text/html");
    }
    
    private void setupLayout() {
        JScrollPane treeScrollPane = new JScrollPane(fileTree);
        treeScrollPane.setPreferredSize(new Dimension(300, 600));
        
        JScrollPane htmlScrollPane = new JScrollPane(htmlViewer);
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                  treeScrollPane, htmlScrollPane);
        splitPane.setDividerLocation(300);
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
    }
    
    private void loadTimetables() {
        File outputDir = new File("output");
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, 
                "Output directory not found. Please run the timetable generator first.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(outputDir));
        createNodes(root, outputDir);
        
        DefaultTreeModel model = new DefaultTreeModel(root);
        fileTree.setModel(model);
    }
    
    private void createNodes(DefaultMutableTreeNode parent, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileNode(file));
            parent.add(node);
            
            if (file.isDirectory()) {
                createNodes(node, file);
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
     * A node in the file tree.
     */
    private static class FileNode {
        private File file;
        
        public FileNode(File file) {
            this.file = file;
        }
        
        public File getFile() {
            return file;
        }
        
        @Override
        public String toString() {
            return file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
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
                new TimetableViewer().setVisible(true);
            }
        });
    }
} 