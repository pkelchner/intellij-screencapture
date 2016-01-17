package net.cortexx.sreencapture;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Comparator.comparingInt;

public class CaptureScreenAction extends AnAction {
    private final Logger log = Logger.getInstance(CaptureScreenAction.class);

    public static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("uuuuMMDD_HHmmss");

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        VirtualFile targetDir = SourcesRootUtils.getSourcesRoot(e,
                SourcesRootUtils::noTestOrGenerated,
                comparingInt(SourcesRootUtils::preferResourceFolders));

        if (targetDir == null) {
            log.error("No folder is marked as a resource root or a source root.");
            return;
        }

        NameAndInsert nai = NameAndInsert.fromEvent(e);

        WindowManager wm = WindowManager.getInstance();
        JFrame frame = wm.getFrame(project);
        frame.setState(JFrame.ICONIFIED);

        CaptureScreenFrame capture = new CaptureScreenFrame();

        capture.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        capture.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                try {
                    Rectangle s = capture.getSelectionRectangle();
                    if (s != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            try {
                                VirtualFile imageFile = null;
                                for (int i=0; i<100; ++i) {
                                    if (targetDir.findChild(nai.filename +(i==0?"":i)+ ".png") == null) {
                                        imageFile = targetDir.createChildData(capture, nai.filename + (i == 0 ? "" : i) + ".png");
                                        if (i > 0)
                                            nai.filename = nai.filename + i;
                                        break;
                                    }
                                }
                                if (imageFile == null) {
                                    log.error("To many files named "+nai.filename+".png");
                                    return;
                                }
                                try (OutputStream out = imageFile.getOutputStream(capture)) {
                                    ImageIO.write(capture.getSelectionImage(), "png", out);
                                }
                            } catch (IOException ex) {
                                throw new IllegalArgumentException(ex);
                            }
                            nai.insert.run();
                        });
                    }
                } finally {
                    EventQueue.invokeLater(() -> frame.setState(JFrame.NORMAL));
                }
            }
        });

        EventQueue.invokeLater(() -> capture.setVisible(true));
    }

    private static class NameAndInsert {
        String filename;
        Runnable insert;

        public static NameAndInsert fromEvent(AnActionEvent e) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            Editor editor = e.getData(PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE);
            Caret caret = e.getData(PlatformDataKeys.CARET);

            NameAndInsert result = new NameAndInsert();
            PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);

            if (editor != null && caret != null) {
                PsiElement replaced = null;

                result.filename = FILENAME_TIMESTAMP.format(LocalDateTime.now());

                if (file != null) {
                    PsiElement psi = file.findElementAt(caret.getOffset());
                    if (psi != null) {
                        PsiUtils.MethodParameter mp = PsiUtils.fromCaretOnMethodParameter(psi);
                        if (mp != null && mp.paramDecl != null) {
                            result.filename = mp.paramDecl.getName();
                            replaced = mp.underCaret;
                        }

                        PsiUtils.FieldOrVariable a = PsiUtils.fromCaretOnAssigment(psi);
                        if (a != null) {
                            result.filename = a.left.getName();
                            replaced = a.underCaret;
                        }
                    }
                }

                PsiElement replaced0 = replaced; // effectively final for lambdas below
                result.filename = result.filename + "";

                if (project != null && replaced != null) {
                    result.insert = () -> {
                        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(project);
                        PsiElement replacement = factory.createExpressionFromText("\""+result.filename+".png\"", replaced0);
                        replaced0.replace(replacement);
                    };
                } else {
                    result.insert = () -> editor.getDocument().insertString(caret.getOffset(), "\""+result.filename+".png\"");
                }
            }

            if (result.filename == null) {
                result.filename = FILENAME_TIMESTAMP.format(LocalDateTime.now()) + ".png";
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                result.insert = () -> clipboard.setContents(new StringSelection(result.filename), null);
            }

            return result;
        }
    }

}
