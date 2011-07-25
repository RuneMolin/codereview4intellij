package ui.reviewtoolwindow.nodes;


import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import reviewresult.Review;
import reviewresult.ReviewManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * User: Alisa.Afonina
 * Date: 7/14/11
 * Time: 5:50 PM
 */
public class FileNode extends SimpleNode implements Navigatable{
    private Project project;
    private VirtualFile file;
    private List<SimpleNode> children = new ArrayList<SimpleNode>();
    public FileNode(Project project, VirtualFile value/*, NodeDescriptor parentDescriptor*/) {
        super(project/*, parentDescriptor*/);
        this.project = project;
        file = value;
    }

    @Override
    public Object[] getEqualityObjects() {
        Object[] equalityObjects = new Object[1];
        equalityObjects[0] = file;
        return equalityObjects;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        if(!children.isEmpty()) children.clear();
            if(file.isDirectory()) {
                Project project = getProject();
                Set<VirtualFile> filesWithReview = ReviewManager.getInstance(project).getFiles();
                for (VirtualFile virtualFile : filesWithReview) {
                   FileNode newNode = new FileNode(project, virtualFile);
                   addNode(newNode);
                }
            } else {
                List<Review> reviews = ReviewManager.getInstance(project).getReviews(file);
                for(Review review : reviews) {
                    children.add(new ReviewNode(project, review));
                }
             }
        //}
        return children.toArray(new SimpleNode[children.size()]);
    }

        private void addNode(FileNode newNode) {
            VirtualFile parentDirectory = newNode.file.getParent();
            VirtualFile baseDir = project.getBaseDir();
            if(baseDir == null) return;
            if(!parentDirectory.equals(file)) {
                if(baseDir.equals(parentDirectory)) return;
                    FileNode parentNode = new FileNode(project, parentDirectory);
                    parentNode.addNode(newNode);
                    addNode(parentNode);
                }
                else {
                    boolean exists = false;
                    for (SimpleNode child : children) {
                        FileNode fileNode = (FileNode) child;
                        if(fileNode.file.equals(newNode.file)) {
                            fileNode.children.addAll(newNode.children);
                            exists = true;
                        }
                    }
                    if(!exists)
                        children.add(newNode);
                    }
                }


    @Override
    public boolean isAutoExpandNode() {
        return true;
    }

    @Override
    public void update(PresentationData data) {

        data.addText(file.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if(!file.isDirectory()) {
            int number =  ReviewManager.getInstance(project).getReviews(file).size();
            String text = " " + String.valueOf(number) + ((number == 1)?" review":" reviews");
            data.addText( text, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        }
        Icon icon  = IconUtil.getIcon(file, Iconable.ICON_FLAG_OPEN, project);
        data.setIcons(icon);

    }


    @Override
    public void navigate(boolean requestFocus) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if(document == null) return;
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) return;
        psiFile.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    public VirtualFile getFile() {
        return file;
    }


    //todo : do real update, not rebuilding all children
    public void updateContent() {
    }
}