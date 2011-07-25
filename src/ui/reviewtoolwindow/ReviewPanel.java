package ui.reviewtoolwindow;

import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.OccurenceNavigatorSupport;
import com.intellij.ide.actions.NextOccurenceToolbarAction;
import com.intellij.ide.actions.PreviousOccurenceToolbarAction;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SmartExpander;
import com.intellij.ui.treeStructure.PatchedDefaultMutableTreeNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import org.apache.log4j.chainsaw.Main;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reviewresult.Review;
import reviewresult.ReviewManager;
import sun.java2d.pipe.SpanShapeRenderer;
import ui.actions.PreviewAction;
import ui.forms.EditReviewForm;
import ui.reviewtoolwindow.nodes.FileNode;
import ui.reviewtoolwindow.nodes.ReviewNode;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * User: Alisa.Afonina
 * Date: 7/13/11
 * Time: 2:39 PM
 */
public class ReviewPanel extends  SimpleToolWindowPanel implements DataProvider, OccurenceNavigator {
    public static final String ACTION_GROUP = "TreeReviewItemActions";
    private SimpleTree reviewTree;
    private ReviewTreeStructure reviewTreeStructure;
    @Nullable
    private OccurenceNavigatorSupport reviewNavigatorSupport;
    private AbstractTreeBuilder reviewTreeBuilder;
    private JPanel previewPanel = new JPanel();
    private boolean isShowPreview;
    private JPanel mainPanel;
    private JScrollPane previewScrollPane;

    public ReviewPanel(Project project) {
        super(false);
        reviewTreeStructure = createTreeStructure(project);
        final DefaultTreeModel model = new DefaultTreeModel(new PatchedDefaultMutableTreeNode());
        reviewTree = new SimpleTree(model);
        SmartExpander.installOn(reviewTree);
        reviewTreeBuilder = new SimpleTreeBuilder(reviewTree, model, reviewTreeStructure, null);
        reviewTree.revalidate();
        EditSourceOnDoubleClickHandler.install(reviewTree);
        reviewTree.setRootVisible(false);
        reviewTree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    SimpleNode node = reviewTree.getSelectedNode();

                    if (node instanceof FileNode) {
                        ((FileNode) node).navigate(false);
                    } else {
                        ((ReviewNode) node).navigate(true);
                    }
                }
            }
        });
        reviewTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                NodeDescriptor userObject = (NodeDescriptor)node.getUserObject();
                if (userObject == null) return;
                Object element = userObject.getElement();
                Review review = null;
                if (element instanceof FileNode) {

                    Object o = node.getFirstLeaf().getUserObject();
                    if(!(o instanceof  NodeDescriptor)) return;
                    NodeDescriptor firstReviewNode = (NodeDescriptor) o;
                    review = ((ReviewNode) firstReviewNode.getElement()).getReview();
                }
                if (element instanceof ReviewNode) {
                    review = ((ReviewNode) element).getReview();
                }
                if(review == null) return;
                showPreview(review);
                }
        });
        reviewNavigatorSupport = new OccurenceNavigatorSupport(reviewTree) {
        protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
          if (node.getChildCount() > 0)  {return null;}
          return getNavigatableForNode(node);
        }

          @Override
        public String getNextOccurenceActionName() {
            return IdeActions.ACTION_NEXT_OCCURENCE ;
        }

        @Override
        public String getPreviousOccurenceActionName() {
            return IdeActions.ACTION_PREVIOUS_OCCURENCE;

        }
      };
        PopupHandler.installPopupHandler(reviewTree, ACTION_GROUP, ActionPlaces.TODO_VIEW_POPUP);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(reviewTree);
        previewScrollPane = ScrollPaneFactory.createScrollPane(previewPanel);
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createLeftMenu(), BorderLayout.WEST);
        previewScrollPane.setVisible(false);
        mainPanel.add(scrollPane);
        mainPanel.add(previewScrollPane, BorderLayout.EAST);
        setContent(mainPanel);
    }

    private JPanel createLeftMenu() {
        JPanel toolBar = new JPanel(new GridLayout());

        DefaultActionGroup leftGroup = new DefaultActionGroup();
        leftGroup.add(new PreviousOccurenceToolbarAction(this));
        leftGroup.add(new NextOccurenceToolbarAction(this));
        leftGroup.add(new PreviewAction());
        toolBar.add(
            ActionManager.getInstance().createActionToolbar(ActionPlaces.TODO_VIEW_TOOLBAR, leftGroup, false).getComponent());

    return toolBar;
    }

    private ReviewTreeStructure createTreeStructure(final Project project) {

        SimpleNode rootNode = new SimpleNode() {
            @Override
            public SimpleNode[] getChildren() {
                if(ReviewManager.getInstance(project).getFiles().isEmpty()) {
                    return new SimpleNode[] {};
                }
                else {
                    VirtualFile virtualFile = project.getBaseDir();
                    return new FileNode[]{new FileNode(project, virtualFile)};
                }
            }
        };
        return new ReviewTreeStructure(project, rootNode);
    }


    @Override
    public Object getData(@NonNls String dataId) {
        if (PlatformDataKeys.NAVIGATABLE.is(dataId)) {
            TreePath path = reviewTree.getSelectionPath();
            if (path == null) {
                return null;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            NodeDescriptor userObject = (NodeDescriptor)node.getUserObject();
            if (userObject == null) {
            return null;
            }
            Object element = userObject.getElement();
            if (!(element instanceof ReviewNode)) {
                return element;
            }
            Review review = ((ReviewNode) element).getReview();
            return review.getElement();
        }
     return null;
    }

    @Nullable
    private static Navigatable getNavigatableForNode(@NotNull DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof Navigatable) {
            final Navigatable navigatable = (Navigatable)userObject;
            return navigatable.canNavigate() ? navigatable : null;
        }
        return null;

    }

    public boolean hasNextOccurence() {
        return reviewNavigatorSupport != null && reviewNavigatorSupport.hasNextOccurence();
    }

    public boolean hasPreviousOccurence() {
      return reviewNavigatorSupport != null && reviewNavigatorSupport.hasPreviousOccurence();
    }

    public OccurenceInfo goNextOccurence() {
      return reviewNavigatorSupport != null ? reviewNavigatorSupport.goNextOccurence() : null;
    }

    public OccurenceInfo goPreviousOccurence() {
      return reviewNavigatorSupport != null ? reviewNavigatorSupport.goPreviousOccurence() : null;
    }

    public String getNextOccurenceActionName() {
      return reviewNavigatorSupport != null ? reviewNavigatorSupport.getNextOccurenceActionName() : "";
    }

    public String getPreviousOccurenceActionName() {
      return reviewNavigatorSupport != null ? reviewNavigatorSupport.getPreviousOccurenceActionName() : "";
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if(reviewTreeBuilder == null) return;
        reviewTreeBuilder.getUi().doUpdateFromRoot();
    }

    public void showPreview(Review review) {
        previewPanel.setVisible(isShowPreview);
        previewPanel.removeAll();
        previewPanel.add(new EditReviewForm(review).getItemsContent());
        previewPanel.updateUI();
    }

    private final class PreviewAction extends ToggleAction {

        public PreviewAction() {
            super("Preview reviews", null, IconLoader.getIcon("/actions/preview.png"));
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return isShowPreview;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            isShowPreview = state;
            if(reviewTree.isSelectionEmpty()) return;
            //todo add preview of first selection
            previewScrollPane.setVisible(state);
            mainPanel.updateUI();
        }


    }
}