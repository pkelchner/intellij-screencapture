package net.cortexx.sreencapture;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.lang.String.format;

public class CaptureScreenRegionAction extends AnAction {
    private final Logger log = Logger.getInstance(CaptureScreenRegionAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        Caret caret = e.getData(PlatformDataKeys.CARET);
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);

        WindowManager wm = WindowManager.getInstance();
        JFrame frame = wm.getFrame(project);
        frame.setState(JFrame.ICONIFIED);

        CaptureScreenFrame capture = new CaptureScreenFrame();

        capture.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        capture.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                try {
                   WriteCommandAction.runWriteCommandAction(project, () -> {
                       Rectangle selection = capture.getSelectionRectangle();
                       if (selection == null) return;

                       String textRegion = format("%d, %d, %d, %d",
                               selection.x, selection.y, selection.width, selection.height);

                       if (editor != null && caret != null) {
                           int start = caret.getSelectionStart();
                           int end = caret.getSelectionEnd();

                           if (file != null) {
                               PsiElement psi = file.findElementAt(caret.getSelectionStart());
                               for (int i=0;i<10 && psi != null && !(psi instanceof PsiExpressionList); ++i) {
                                   psi = psi.getParent();
                               }
                               if (psi instanceof PsiExpressionList) {
                                   start = psi.getTextOffset();
                                   end = start + psi.getTextLength();
                                   textRegion = "("+textRegion+")";
                               }
                           }
                           editor.getDocument().replaceString(start, end, textRegion);
                       } else {
                           Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                           clipboard.setContents(new StringSelection(textRegion), null);
                           log.info(format("Region copied to clipboard: (%d,%d) %dx%d",
                                   selection.x, selection.y, selection.width, selection.height));
                       }
                   });
                } finally {
                    EventQueue.invokeLater(() -> frame.setState(JFrame.NORMAL));
                }
            }
        });

        EventQueue.invokeLater(() -> capture.setVisible(true));
    }
}
