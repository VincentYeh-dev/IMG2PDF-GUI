package org.vincentyeh.IMG2PDF.gui.ui;


import org.vincentyeh.IMG2PDF.commandline.concrete.converter.PermissionConverter;
import org.vincentyeh.IMG2PDF.image.helper.concrete.DirectionImageHelper;
import org.vincentyeh.IMG2PDF.pdf.concrete.appender.ExecutorPageAppender;
import org.vincentyeh.IMG2PDF.pdf.concrete.calculation.strategy.StandardImagePageCalculationStrategy;
import org.vincentyeh.IMG2PDF.pdf.concrete.converter.ImageHelperPDFCreatorImpl;
import org.vincentyeh.IMG2PDF.pdf.concrete.converter.PDFBoxCreatorImpl;
import org.vincentyeh.IMG2PDF.pdf.framework.appender.PageAppender;
import org.vincentyeh.IMG2PDF.pdf.framework.converter.PDFCreator;
import org.vincentyeh.IMG2PDF.pdf.function.converter.ImagePDFCreator;
import org.vincentyeh.IMG2PDF.pdf.parameter.*;
import org.vincentyeh.IMG2PDF.task.concrete.factory.DirectoryTaskFactory;
import org.vincentyeh.IMG2PDF.task.framework.Task;
import org.vincentyeh.IMG2PDF.task.framework.factory.TaskFactory;
import org.vincentyeh.IMG2PDF.task.framework.factory.exception.TaskFactoryProcessException;
import org.vincentyeh.IMG2PDF.util.file.FileNameFormatter;
import org.vincentyeh.IMG2PDF.util.file.FileSorter;
import org.vincentyeh.IMG2PDF.util.file.GlobbingFileFilter;
import org.vincentyeh.IMG2PDF.util.file.exception.MakeDirectoryException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MainFrame {
    private File[] files;
    private JButton button_browse;
    private JPanel root;
    private JTextField field_sources;
    private JComboBox<PageSize> combo_size;
    private JComboBox<PageAlign.HorizontalAlign> combo_horizontal;
    private JComboBox<PageAlign.VerticalAlign> combo_vertical;
    private JTextField field_destination;
    private JPasswordField pwd_owner_password;
    private JPasswordField pwd_user_password;
    private JButton button_convert;
    private JTextField field_filter;
    private JComboBox<PageDirection> combo_direction;
    private JCheckBox check_auto;
    private JComboBox<FileSorter.Sortby> combo_sortby;
    private JComboBox<FileSorter.Sequence> combo_sequence;
    private JProgressBar progress;
    private JCheckBox check_overwrite;
    private JFileChooser sources_chooser;

    public MainFrame() {
        createUIComponents();
        initializeListener();
    }

    private void initializeListener() {
        button_browse.addActionListener(e -> {
            int option = sources_chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                files = sources_chooser.getSelectedFiles();
                field_sources.setText(Arrays.stream(files)
                        .map(file -> String.format("\"%s\"", file.getAbsolutePath())).collect(Collectors.joining(" ")));
            }
        });
        button_convert.addActionListener(this::startConvert);

    }


    private void createUIComponents() {
        sources_chooser = new JFileChooser();
        sources_chooser.setMultiSelectionEnabled(true);
        sources_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        sources_chooser.setCurrentDirectory(new File("").getAbsoluteFile());

        combo_sortby.setModel(new DefaultComboBoxModel<>(FileSorter.Sortby.values()));
        combo_sequence.setModel(new DefaultComboBoxModel<>(FileSorter.Sequence.values()));

        combo_direction.setModel(new DefaultComboBoxModel<>(PageDirection.values()));
        combo_vertical.setModel(new DefaultComboBoxModel<>(PageAlign.VerticalAlign.values()));
        combo_horizontal.setModel(new DefaultComboBoxModel<>(PageAlign.HorizontalAlign.values()));
        combo_size.setModel(new DefaultComboBoxModel<>(PageSize.values()));
//
//        for (PageAlign.VerticalAlign align : PageAlign.VerticalAlign.values()) {
//            combo_vertical.addItem(align);
//        }
//        for (PageAlign.HorizontalAlign align : PageAlign.HorizontalAlign.values()) {
//            combo_horizontal.addItem(align);
//        }
//        for (PageSize size : PageSize.values()) {
//            combo_size.addItem(size);
//        }
    }

    private List<Task> toTasks(File[] directories) throws TaskFactoryProcessException {

        FileSorter.Sortby sortby = combo_sortby.getItemAt(combo_sortby.getSelectedIndex());
        FileSorter.Sequence sequence = combo_sequence.getItemAt(combo_sequence.getSelectedIndex());

        TaskFactory<File> factory = new DirectoryTaskFactory(
                getDocumentArgument(), getPageArgument(), new GlobbingFileFilter(field_filter.getText()),
                new FileSorter(sortby, sequence),
                new FileNameFormatter(field_destination.getText()));

        List<Task> tasks = new LinkedList<>();
        for (File directory : directories) {
            tasks.add(factory.create(directory));
        }
        return tasks;
    }

    private PageArgument getPageArgument() {
        PageArgument argument = new PageArgument();
        PageAlign.HorizontalAlign horizontalAlign = combo_horizontal.getItemAt(combo_horizontal.getSelectedIndex());
        PageAlign.VerticalAlign verticalAlign = combo_vertical.getItemAt(combo_vertical.getSelectedIndex());

        argument.setAlign(new PageAlign(verticalAlign, horizontalAlign));
        argument.setSize(combo_size.getItemAt(combo_size.getSelectedIndex()));
        argument.setDirection(combo_direction.getItemAt(combo_direction.getSelectedIndex()));
        argument.setAutoRotate(check_auto.isSelected());
        return argument;
    }

    private DocumentArgument getDocumentArgument() {
        DocumentArgument argument = new DocumentArgument();
        argument.setInformation(null);
        String owner = String.valueOf(pwd_owner_password.getPassword());
        String user = String.valueOf(pwd_user_password.getPassword());
        if (owner.isEmpty()) {
            owner = null;
        }

        if (user.isEmpty()) {
            user = null;
        }

        argument.setOwnerPassword(owner);
        argument.setUserPassword(user);
        try {
            argument.setPermission(new PermissionConverter().convert("255"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return argument;
    }

    private void startConvert(ActionEvent e) {
        new Thread(() -> {

            try {
                boolean overwrite = check_overwrite.isSelected();
                if (overwrite) {
                    int result = JOptionPane.showConfirmDialog(null, "Overwrite when file has existed.", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result != 0)
                        return;
                }

                List<Task> tasks = toTasks(files);
                progress.setMaximum(tasks.size());
                PageAppender appender = new ExecutorPageAppender(10);

                ImagePDFCreator creator = new ImagePDFCreator(new PDFBoxCreatorImpl(new File("temp"), 1024 * 1024 * 100),
                        new ImageHelperPDFCreatorImpl(new DirectionImageHelper(null)), appender
                        , overwrite, new StandardImagePageCalculationStrategy());
                creator.setCreationListener(new PDFCreator.CreationListener() {
                    int progress_int;

                    @Override
                    public void initializing(Task task) {
                    }

                    @Override
                    public void onConversionComplete() {
                        progress.setValue(++progress_int);
                    }

                    @Override
                    public void onSaved(File file) {

                    }

                    @Override
                    public void onFinally() {

                    }
                });

                for (Task task : tasks) {
                    creator.start(task);
                }

            } catch (TaskFactoryProcessException | MakeDirectoryException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public JPanel getRootPanel() {
        return root;
    }

}
