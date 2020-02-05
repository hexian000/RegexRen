package me.hexian000.regexren;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexRen extends JFrame {
    private JList<String> opList;
    private JTextField pathField;
    private JTextField regexField;
    private JTextField replaceField;
    private JPanel contentPane;
    private JButton runButton;

    private String dir;
    private List<String> fileList;
    private Pattern pattern;
    private String replacement;
    private List<RenameOp> operations;

    private class RenameOp {
        String from, to;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "%s => %s", from, to);
        }
    }

    private void cd(File dirFile) {
        runButton.setEnabled(false);
        dir = dirFile.getAbsolutePath();
        pathField.setText(dir);
        String[] list = dirFile.list();
        if (list != null) {
            fileList = Arrays.asList(list);
        } else {
            fileList = null;
        }
        updateList();
    }

    private List<String> filter() {
        if (fileList == null || pattern == null) {
            return null;
        }
        List<String> filtered = new ArrayList<>();
        for (String file : fileList) {
            Matcher m = pattern.matcher(file);
            if (m.find()) {
                filtered.add(file);
            }
        }
        return filtered;
    }

    private void replaceNames() {
        operations = new ArrayList<>();
        try {
            Set<String> names = new HashSet<>(fileList);
            for (String file : fileList) {
                Matcher m = pattern.matcher(file);
                if (m.find()) {
                    RenameOp op = new RenameOp();
                    op.from = file;
                    op.to = m.replaceFirst(replacement);
                    if (op.from.equals(op.to)) {
                        continue;
                    }
                    names.remove(op.from);
                    if (names.contains(op.to)) {
                        operations = null;
                        return;
                    } else {
                        names.add(op.to);
                    }
                    operations.add(op);
                }
            }
            replaceField.setForeground(Color.BLACK);
        } catch (Exception ex) {
            ex.printStackTrace();
            replaceField.setForeground(Color.RED);
            operations = null;
        }
    }

    private void updateList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String> filteredList = filter();
        if (filteredList != null && !filteredList.isEmpty()) {
            if (pattern != null && replacement != null && !replacement.isEmpty()) {
                replaceNames();
                if (operations != null && !operations.isEmpty()) {
                    for (RenameOp op : operations) {
                        listModel.addElement(op.toString());
                    }
                    opList.setModel(listModel);
                    runButton.setEnabled(true);
                    return;
                }
            }
            listModel.addAll(filteredList);
        } else {
            listModel.addAll(fileList);
        }
        opList.setModel(listModel);
        runButton.setEnabled(false);
    }

    private void onChanged(DocumentEvent e) {
        pattern = null;
        String patternText = regexField.getText();
        if (!patternText.isEmpty()) {
            try {
                pattern = Pattern.compile(patternText);
                regexField.setForeground(Color.BLACK);
            } catch (PatternSyntaxException ex) {
                regexField.setForeground(Color.RED);
            }
        }
        replacement = replaceField.getText();
        updateList();
    }

    private RegexRen() {
        setContentPane(contentPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Regex Rename");

        pathField.setDropTarget(new FileDropTarget() {
            @Override
            public void onFileDrop(List<String> files) {
                for (final String path : files) {
                    final File file = new File(path);
                    if (!file.exists()) {
                        continue;
                    }
                    if (file.isFile()) {
                        cd(file.getParentFile());
                        break;
                    }
                    if (file.isDirectory()) {
                        cd(file);
                        break;
                    }
                }
            }
        });

        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChanged(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChanged(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChanged(e);
            }
        };
        regexField.getDocument().addDocumentListener(listener);
        replaceField.getDocument().addDocumentListener(listener);

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<RenameOp> failures = new ArrayList<>();
                for (RenameOp op : operations) {
                    File f = Paths.get(dir, op.from).toFile();
                    boolean ok = f.renameTo(Paths.get(dir, op.to).toFile());
                    if (!ok) {
                        failures.add(op);
                    }
                }
                if (!failures.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (RenameOp op : failures) {
                        sb.append(op.toString()).append('\n');
                    }
                    JOptionPane.showMessageDialog(RegexRen.this, sb.toString(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                updateList();
            }
        });
    }

    public static void main(String[] args) {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException |
                ClassNotFoundException |
                InstantiationException |
                IllegalAccessException e) {
            e.printStackTrace();
        }

        final RegexRen frame = new RegexRen();
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
