package org.vincentyeh.IMG2PDF.gui.ui;


import org.vincentyeh.IMG2PDF.image.helper.concrete.DirectionImageHelper;
import org.vincentyeh.IMG2PDF.pdf.concrete.appender.ExecutorPageAppender;
import org.vincentyeh.IMG2PDF.pdf.concrete.calculation.strategy.StandardImagePageCalculationStrategy;
import org.vincentyeh.IMG2PDF.pdf.concrete.converter.ImageHelperPDFCreatorImpl;
import org.vincentyeh.IMG2PDF.pdf.concrete.converter.PDFBoxCreatorImpl;
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
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

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

    public MainFrame() {
        createUIComponents();
        initializeListener();
    }

    private void initializeListener() {

        button_browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                files = chooser.getSelectedFiles();
            }
        });
        button_convert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<Task> tasks=toTasks(files);

                    ImagePDFCreator creator=new ImagePDFCreator(new PDFBoxCreatorImpl(new File("temp"),1024*1024*100),
                            new ImageHelperPDFCreatorImpl(new DirectionImageHelper(null)),
                            new ExecutorPageAppender(10),true,new StandardImagePageCalculationStrategy());


                    for(Task task:tasks){
                        creator.start(task);
                    }

                } catch (TaskFactoryProcessException | MakeDirectoryException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    private void createUIComponents() {
        for (PageDirection direction : PageDirection.values()) {
            combo_direction.addItem(direction);
        }
        for (PageAlign.VerticalAlign align : PageAlign.VerticalAlign.values()) {
            combo_vertical.addItem(align);
        }
        for (PageAlign.HorizontalAlign align : PageAlign.HorizontalAlign.values()) {
            combo_horizontal.addItem(align);
        }
        for (PageSize size : PageSize.values()) {
            combo_size.addItem(size);
        }
    }

    private List<Task> toTasks(File[] directories) throws TaskFactoryProcessException {
        TaskFactory<File> factory = new DirectoryTaskFactory(
                getDocumentArgument(), getPageArgument(), new GlobbingFileFilter(field_filter.getText()),
                new FileSorter(FileSorter.Sortby.NAME, FileSorter.Sequence.INCREASE),
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
        String owner, user;

        if (pwd_owner_password.getPassword().length == 0) {
            owner = new String(pwd_owner_password.getPassword());
        } else {
            owner = null;
        }

        if (pwd_user_password.getPassword().length == 0) {
            user = new String(pwd_user_password.getPassword());
        } else {
            user = null;
        }

        argument.setOwnerPassword(owner);
        argument.setUserPassword(user);
        argument.setPermission(new Permission());
        return argument;
    }

    public JPanel getRootPanel() {
        return root;
    }

}
