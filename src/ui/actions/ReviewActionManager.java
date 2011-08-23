package ui.actions;


import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.PositionTracker;
import reviewresult.Review;
import ui.forms.EditReviewForm;
import ui.gutterpoint.ReviewPoint;
import ui.gutterpoint.ReviewPointManager;

import javax.swing.*;
import java.awt.*;

/**
 * User: Alisa.Afonina
 * Date: 7/25/11
 * Time: 1:14 PM
 */
public class ReviewActionManager implements DumbAware {
    private Balloon activeBalloon = null;
    private BalloonBuilder balloonBuilder;
    private EditReviewForm editReviewForm;
    private static ReviewActionManager instance;
    private Review review;


    private ReviewActionManager() {}

    public static ReviewActionManager getInstance(Review review) {
        if(instance == null) {
            instance = new ReviewActionManager();
        }
        instance.setReview(review);
        return instance;
    }

    public void addToExistingComments(final Editor editor) {
        ReviewPoint reviewPoint = ReviewPointManager.getInstance(getReview().getProject()).findReviewPoint(getReview());
        if(reviewPoint == null) return;
        final EditorGutterComponentEx gutterComponent = ((EditorEx)editor).getGutterComponentEx();
        final Point point = gutterComponent.getPoint(reviewPoint.getGutterIconRenderer());
        if (point != null) {
            final Icon icon = reviewPoint.getGutterIconRenderer().getIcon();
            editReviewForm = new EditReviewForm(reviewPoint.getReview(), true, true);
            JComponent content = editReviewForm.getContent();
            final Point centerIconPoint = new Point(point.x + icon.getIconWidth() / 2 + gutterComponent.getIconsAreaWidth(), point.y + icon.getIconHeight() / 2);
            showBalloon(editor, centerIconPoint, content, gutterComponent, "Add Comment");
            editReviewForm.requestFocus();
        }
    }


    public void addNewComment(final Editor editor) {
        final Point point = editor.logicalPositionToXY(editor.getCaretModel().getLogicalPosition());
        editReviewForm = new EditReviewForm(getReview(), true, true);
        JComponent content = editReviewForm.getContent();
        showBalloon(editor, point, content, editor.getContentComponent(), "Add Comment");
        editReviewForm.requestFocus();
    }

    private void showBalloon(final Editor editor, final Point point, JComponent balloonContent, final JComponent contentComponent, String title) {
        if(!getReview().isValid()) return;
        hideBalloon();
        balloonBuilder = JBPopupFactory.getInstance().createDialogBalloonBuilder(balloonContent, title);
        balloonBuilder.setHideOnClickOutside(true);
        balloonBuilder.setHideOnKeyOutside(true);
        setActiveBalloon(balloonBuilder.createBalloon());
        editReviewForm.setBalloon(getActiveBalloon());
        if(getActiveBalloon() == null) return;
        getActiveBalloon().show(new ReviewPositionTracker(editor, contentComponent, point) , Balloon.Position.atRight);
    }

    private void hideBalloon() {
        if(getActiveBalloon() != null)
            if(!getActiveBalloon().isDisposed())
                getActiveBalloon().hide();
    }

    public void disposeActiveBalloon() {
        if(activeBalloon != null) {
            review.setActivated(false);
            activeBalloon.dispose();
        }
    }

     public void showExistingComments(final Editor editor) {

        ReviewPoint reviewPoint = ReviewPointManager.getInstance(getReview().getProject()).findReviewPoint(getReview());
        if(reviewPoint == null) return;
        final EditorGutterComponentEx gutterComponent = ((EditorEx)editor).getGutterComponentEx();
        final Point point = gutterComponent.getPoint(reviewPoint.getGutterIconRenderer());
        if (point != null) {
            final Icon icon = reviewPoint.getGutterIconRenderer().getIcon();
            editReviewForm = new EditReviewForm(reviewPoint.getReview(), false, true);
            JComponent content = editReviewForm.getContent();
            final Point centerIconPoint = new Point(point.x + icon.getIconWidth() / 2 + gutterComponent.getIconsAreaWidth(), point.y + icon.getIconHeight() / 2);
            showBalloon(editor, centerIconPoint, content, gutterComponent, "Edit Comment");
            editReviewForm.requestFocus();
        }
    }

    private Review getReview() {
        return review;
    }

    private void setReview(Review review) {
        this.review = review;
    }

    private Balloon getActiveBalloon() {
        return activeBalloon;
    }

    private void setActiveBalloon(Balloon activeBalloon) {
        this.activeBalloon = activeBalloon;
    }

    private class ReviewPositionTracker extends PositionTracker<Balloon>{

        private final Editor editor;
        private final JComponent component;
        private final Point point;

        public ReviewPositionTracker(Editor editor, JComponent component, Point point) {
            super(component);
            this.editor = editor;
            this.component = component;
            this.point = point;
        }

        @Override
        public RelativePoint recalculateLocation(final Balloon object) {
            if (!editor.getScrollingModel().getVisibleArea().contains(point)) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        object.hide();
                    }
                });
                 if(!object.isDisposed()) {
                    final PositionTracker<Balloon> tracker = this;
                    final VisibleAreaListener listener = new VisibleAreaListener() {
                       @Override
                       public void visibleAreaChanged(VisibleAreaEvent e) {
                            if(e.getNewRectangle().contains(point) && object.isDisposed()) {
                                hideBalloon();
                                setActiveBalloon(balloonBuilder.createBalloon());
                                editReviewForm.setBalloon(getActiveBalloon());
                                getActiveBalloon().show(tracker, Balloon.Position.atRight);
                                editor.getScrollingModel().removeVisibleAreaListener(this);
                            }
                       }
                    };
                    editor.getScrollingModel().addVisibleAreaListener(listener);
                 }
            }
            return new RelativePoint(component, point);
        }

    }


}
