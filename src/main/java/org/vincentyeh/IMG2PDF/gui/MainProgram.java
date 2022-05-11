package org.vincentyeh.IMG2PDF.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import org.vincentyeh.IMG2PDF.gui.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class MainProgram {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("AA");
        frame.setContentPane(new MainFrame().getRootPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(550, 300));
        frame.setVisible(true);
    }
}
