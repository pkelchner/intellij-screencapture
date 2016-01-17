package net.cortexx.sreencapture;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 *
 */
public class CaptureScreenFrame extends JFrame {
    private final JPanel selection;
    private final JPanel background;

    private BufferedImage screen;

    public static BufferedImage captureScreen() {
        try {
            GraphicsDevice mainScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            DisplayMode displayMode = mainScreen.getDisplayMode();
            Rectangle screenSpace = new Rectangle(displayMode.getWidth(), displayMode.getHeight());
            Robot robot = new Robot(mainScreen);
            return robot.createScreenCapture(screenSpace);
        } catch (AWTException e) {
            throw new IllegalStateException(e);
        }
    }

    public CaptureScreenFrame() {
        this(captureScreen());
    }

    public CaptureScreenFrame(BufferedImage screen) {
        this.screen = screen;

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        background = new BackgroundPanel();
        selection = new SelectionPanel();
        background.add(selection);
        add(background);
        pack();
        setLocationRelativeTo(null);

        MouseAdapter mouseEvents = new MouseEvents();
        addMouseListener(mouseEvents);
        addMouseMotionListener(mouseEvents);

        KeyEvents keyEvents = new KeyEvents();
        addKeyListener(keyEvents);
    }

    public void setScreenCaptureImage(BufferedImage screen) {
        this.screen = screen;
        background.repaint();
    }

    public BufferedImage getSelectionImage() {
        if (selection.getWidth() > 0 && selection.getHeight() > 0) {
            return screen.getSubimage(selection.getX(), selection.getY(), selection.getWidth(), selection.getHeight());
        }
        return null;
    }

    public Rectangle getSelectionRectangle() {
        if (selection.getWidth() > 0 && selection.getHeight() > 0) {
            return new Rectangle(selection.getX(), selection.getY(), selection.getWidth(), selection.getHeight());
        }
        return null;
    }

    private class BackgroundPanel extends JPanel {
        private final Composite compAlphaOver = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);

        public BackgroundPanel() {
            setBackground(JBColor.BLACK);
            setLayout(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(compAlphaOver);
            g2d.drawImage(screen, 0, 0, this);
            g2d.dispose();
        }
    }

    private class SelectionPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            g.drawImage(screen.getSubimage(getX(), getY(), getWidth(), getHeight()), 0, 0, this);
            g.setColor(JBColor.RED);
            g.drawLine(getWidth()/2, 1, getWidth()/2, getHeight()-2);
            g.drawLine(1, getHeight()/2, getWidth()-2, getHeight()/2);
            g.setColor(JBColor.WHITE);
            g.drawRect(0, 0, getWidth()-1, getHeight()-1);
        }
    }

    private class MouseEvents extends MouseAdapter {
        private Point anchor;

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                anchor = e.getPoint();
                selection.setLocation(anchor);
                selection.setSize(0, 0);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (anchor != null) {
                selection.setBounds(
                        min(anchor.x, e.getX()),  min(anchor.y, e.getY()),
                        abs(anchor.x - e.getX()), abs(anchor.y - e.getY())
                );
                selection.revalidate();
                background.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                anchor = null;
                closeFrame();
            }
        }
    }

    private class KeyEvents extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    selection.setSize(0, 0);
                case KeyEvent.VK_ENTER:
                    closeFrame();
            }
        }
    }

    private void closeFrame() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
