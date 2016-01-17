package net.cortexx.sreencapture;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.java.JavaResourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SourcesRootUtils {
    public static VirtualFile getSourcesRoot(
            AnActionEvent e,
            Predicate<SourceFolder> filterFolders,
            Comparator<SourceFolder> compareFolders) {

        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if (file == null) {
            Editor editor = e.getData(PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE);
            if (editor != null) {
                file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            }
        }

        Module module = e.getData(LangDataKeys.MODULE);
        if (module == null && file != null) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            if (project != null) {
                module = DirectoryIndex.getInstance(project).getInfoForFile(file).getModule();
            }
        }

        VirtualFile result = null;

        if (module != null) {
            result = Stream.of(ModuleRootManager.getInstance(module).getContentEntries())
                    .flatMap(ce -> Stream.of(ce.getSourceFolders()))
                    .filter(filterFolders)
                    .min(compareFolders)
                    .map(SourceFolder::getFile)
                    .orElse(null);
        }

        if (result == null && file != null) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            if (project != null) {
                result = DirectoryIndex.getInstance(project).getInfoForFile(file).getSourceRoot();
            }
        }

        // TODO maybe use c.i.ide.util.DirectoryChooser

        return result;
    }

    public static boolean isGeneratedOnly(SourceFolder sf) {
        JpsElement properties = sf.getJpsElement().getProperties();
        if (properties instanceof JavaResourceRootProperties)
            return ((JavaResourceRootProperties)properties).isForGeneratedSources();
        if (properties instanceof JavaSourceRootProperties)
            return ((JavaSourceRootProperties)properties).isForGeneratedSources();
        return false;
    }

    public static boolean noTestOrGenerated(SourceFolder sf) {
        return !sf.isTestSource() && !isGeneratedOnly(sf);
    }

    public static int preferResourceFolders(SourceFolder sf) {
        int rank = 0;
        if (sf.getRootType() instanceof JavaSourceRootType) rank += 2;
        if (sf.getUrl().toLowerCase().contains("main")) rank -= 1;
        return rank;
    }
}
