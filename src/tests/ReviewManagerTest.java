package tests;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectMacrosUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.LightIdeaTestCase;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestCase;
import com.sun.jna.Platform;
import com.sun.xml.internal.ws.wsdl.writer.document.Service;
import reviewresult.*;
import ui.reviewpoint.ReviewPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Alisa.Afonina
 * Date: 7/26/11
 * Time: 12:22 PM
 */
public class ReviewManagerTest extends IdeaTestCase {

    private ReviewManager reviewManager;

    public ReviewManagerTest() {
        super();
        IdeaTestCase.initPlatformPrefix();
    }

    public void setUp() throws Exception {
        super.setUp();
        reviewManager = ReviewManager.getInstance(getProject());
    }

    public void testClearAll() {
        Project project = getProject();
        VirtualFile projectFile = project.getProjectFile();
        addNewReview(projectFile, "test Review", 1, 2);
        reviewManager.clearAll();
        List<Review> reviews = reviewManager.getReviews(projectFile);
        assertNullOrEmpty(reviews);
        assertNullOrEmpty(reviewManager.getFileNames());
        assertNullOrEmpty(reviewManager.getReviewPoints().keySet());
        assertNullOrEmpty(reviewManager.getState().reviews);
    }

    public void testAddOneReview() throws Exception {
        Project project = getProject();
        VirtualFile projectFile = project.getProjectFile();
        Review review = addNewReview(projectFile, "test Review", 1, 2);
        List<Review> reviews = reviewManager.getReviews(projectFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);
        assertReviewsEquals(review, addedReview);
        //reviewManager.clearAll();
    }

    public void testAddReviewAfterAnotherSameFile() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review firstReview = addNewReview(firstFile, "test Review", 1, 2);
        Review secondReview = addNewReview(firstFile, "test Review", 2, 4);
        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(2, reviews.size());
        Review addedReview = reviews.get(1);
        assertReviewsEquals(secondReview, addedReview);
    }

    public void testAddReviewAfterAnotherDifferentFilesFile() throws Exception {
        Project project = getProject();
        VirtualFile firstFile = project.getProjectFile();
        VirtualFile secondFile = this.createMainModule().getModuleFile();

        Review firstReview = addNewReview(firstFile, "test review 1", 1, 3);
        Review secondReview = addNewReview(secondFile, "test review 2", 2, 3);

        Set<String> reviewFileNames = reviewManager.getFileNames();
        assertEquals(2, reviewFileNames.size());
        assertEquals(true, reviewFileNames.contains(firstFile.getUrl()));
        assertEquals(true, reviewFileNames.contains(secondFile.getUrl()));

        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);

        assertReviewsEquals(firstReview, addedReview);

        reviews = reviewManager.getReviews(secondFile);
        assertEquals(1, reviews.size());
        addedReview = reviews.get(0);

        assertReviewsEquals(secondReview, addedReview);
    }

    public void testEditReview() throws Exception {
        Project project = getProject();
        VirtualFile projectFile = project.getProjectFile();
        Review firstReview = addNewReview(projectFile, "test Review", 1, 2);
        List<Review> reviews = reviewManager.getReviews(projectFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);
        firstReview.setName("new Test Name");

        assertReviewsEquals(firstReview, addedReview);
    }

    public void testAddReviewItem() throws Exception {
         Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review firstReview = addNewReview(firstFile, "test Review", 1, 2);
        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);
        firstReview.addReviewItem(new ReviewItem("review Text", ReviewStatus.COMMENT));
        assertEquals(1, addedReview.getReviewBean().getReviewItems().size());
        assertReviewsEquals(firstReview, addedReview);
    }

    public void testRemoveReviewItem() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review firstReview = addNewReview(firstFile, "test Review", 1, 2);
        ReviewItem reviewItem = new ReviewItem("review Text", ReviewStatus.COMMENT);
        firstReview.addReviewItem(reviewItem);
        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);
        firstReview.getReviewBean().getReviewItems().remove(reviewItem);
        assertReviewsEquals(firstReview, addedReview);
    }

    public void testAddOneReviewPoint() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review review = addNewReviewPoint(firstFile, "test Review", 1, 2);
        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(1, reviews.size());
        Review addedReview = reviews.get(0);
        assertReviewsEquals(review, addedReview);
        Map<Review, ReviewPoint> reviewPoints = reviewManager.getReviewPoints();
        assertEquals(1, reviewPoints.size());
        assertTrue(reviewPoints.containsKey(review));
        assertReviewsEquals(review, reviewPoints.get(review).getReview());
    }

    public void testAddReviewPointAfterAnother() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review firstReview = addNewReviewPoint(firstFile, "test Review", 1, 2);
        Review secondReview = addNewReviewPoint(firstFile, "test Review", 1, 2);

        List<Review> reviews = reviewManager.getReviews(firstFile);
        assertEquals(2, reviews.size());
        Map<Review, ReviewPoint> reviewPoints = reviewManager.getReviewPoints();
        assertEquals(2, reviewPoints.size());

        assertTrue(reviewPoints.containsKey(firstReview));
        assertTrue(reviewPoints.containsKey(secondReview));

    }

    public void testRemoveOneOfOneReviewPoint() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        Review firstReview = addNewReviewPoint(firstFile, "test Review", 1, 2);

        ReviewPoint pointToRemove = reviewManager.findReviewPoint(firstReview);
        reviewManager.removeReview(pointToRemove);
        Map<Review, ReviewPoint> reviewPoints = reviewManager.getReviewPoints();
        assertFalse(reviewPoints.containsKey(firstReview));
        List<Review> reviews = reviewManager.getReviews(firstFile);
        if(reviews != null) {
            assertFalse(reviews.contains(firstReview));
        }
    }


    public void testFindReviewPoint() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        assertNotNull(firstFile);
        Document document = FileDocumentManager.getInstance().getDocument(firstFile);
        assertNotNull(document);
        document.insertString(0, "1\n");

        VirtualFile secondFile = this.createMainModule().getModuleFile();
        Document secondDocument = FileDocumentManager.getInstance().getDocument(secondFile);
        assertNotNull(secondDocument);
        secondDocument.insertString(0, "1\n");

        //find existing
        Review firstReview = new Review(new ReviewBean("test review", 1, 1, secondFile.getUrl()), project);
        firstReview.setValid(true);
        reviewManager.createReviewPoint(firstReview);
        assertNotNull(reviewManager.findReviewPoint(firstReview));

        //no result for nonexistent
        Review secondReview = new Review(new ReviewBean("test review review", 11, 1, secondFile.getUrl()), project);
        assertNull(reviewManager.findReviewPoint(secondReview));
    }

    public void testRemoveOneOfManyReviewPoints() throws Exception {
        Project project = getProject();

        VirtualFile firstFile = project.getProjectFile();
        VirtualFile secondFile = this.createMainModule().getModuleFile();
        Review firstReview = addNewReviewPoint(firstFile, "test Review", 1, 2);
        Review secondReview = addNewReviewPoint(secondFile, "test Review", 1, 2);

        ReviewPoint firstPointToRemove = reviewManager.findReviewPoint(firstReview);
        reviewManager.removeReview(firstPointToRemove);

        Map<Review, ReviewPoint> reviewPoints = reviewManager.getReviewPoints();
        assertFalse(reviewPoints.containsKey(firstReview));
        assertTrue(reviewPoints.containsKey(secondReview));
        List<Review> reviews = reviewManager.getReviews(secondFile);
        if(reviews != null) {
            assertFalse(reviews.contains(firstReview));
            assertTrue(reviews.contains(secondReview));
        }
    }

    public void testAddIncorrectReview() throws Exception {
        Project project = getProject();
        VirtualFile projectFile = project.getProjectFile();
        assertNotNull(projectFile);
        Review review = new Review(project, "test review", -1, 1, projectFile);
        assertIncorrectReview(projectFile, review);

        review = new Review(project, "test review", 1, -1, projectFile);
        assertIncorrectReview(projectFile, review);

        review = new Review(project, "test review", 10, 9, projectFile);
        assertIncorrectReview(projectFile, review);

        review = new Review(new ReviewBean("test review", 9, 10, "fake url"), project);
        assertIncorrectReview(projectFile, review);
    }

    public void testReviewIfDocumentChanging() throws Exception {
        VirtualFile firstFile = myProject.getProjectFile();

        assertNotNull(firstFile);
        Document document = FileDocumentManager.getInstance().getDocument(firstFile);
        Review review = addNewReviewPoint(firstFile, "test review",
                                                    document.getLineStartOffset(0),
                                                    document.getLineEndOffset(0));

        //insert line before review into document
        int start = review.getReviewBean().getStart();
        document.insertString(0, "1\n");
        assertFalse(start == review.getReviewBean().getStart());

        //insert line in the middle of review into document
        start = review.getReviewBean().getStart();
        document.insertString(start + 1, "1\n");
        assertEquals(start, review.getReviewBean().getStart());

        //insert line at the end of review into document
        start = review.getReviewBean().getStart();
        document.insertString(document.getText().length(), "1");
        assertTrue(start == review.getReviewBean().getStart());
    }

    private void assertIncorrectReview(VirtualFile projectFile, Review review) {
        assertFalse(review.isValid());
        reviewManager.createReviewPoint(review);
        assertFalse(reviewManager.getReviewPoints().containsKey(review));
        assertNullOrEmpty(reviewManager.getReviews(projectFile));
        assertFalse(reviewManager.getState().reviews.contains(review.getReviewBean()));
    }

    private static void assertReviewBeansEquals(ReviewBean reviewBean, String name, int start, int end, List<ReviewItem> reviewItems, String url) {
        assertEquals(reviewBean.getName(), name);
        assertEquals(reviewBean.getStart(), start);
        assertEquals(reviewBean.getEnd(), end);
        assertEquals(reviewBean.getUrl(), url);
        for(ReviewItem item : reviewBean.getReviewItems()) {
            assertEquals(true, reviewItems.contains(item));
        }
    }

    private static void assertReviewsEquals(Review review, Review addedReview) {
        ReviewBean reviewBean = addedReview.getReviewBean();
        assertEquals(review.getProject(), addedReview.getProject());
        assertEquals(review.getVirtualFile().getUrl(), addedReview.getVirtualFile().getUrl());
        assertReviewBeansEquals(review.getReviewBean(),
                reviewBean.getName(), reviewBean.getStart(), reviewBean.getEnd(), reviewBean.getReviewItems(), reviewBean.getUrl());
    }

    private Review addNewReview(VirtualFile file, String name, int start, int end) {
        assertNotNull(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        document.insertString(0, "1\n");

        Review review = new Review(new ReviewBean(name, start, end, file.getUrl()), myProject);
        review.setValid(true);
        //reviewManager.createReviewPoint(review);
        reviewManager.addReview(review);

        return review;
    }

     private Review addNewReviewPoint(VirtualFile file, String name, int start, int end) {
        assertNotNull(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        document.insertString(0, "111\n");

        Review review = new Review(new ReviewBean(name, start, end, file.getUrl()), myProject);
        review.setValid(true);
        reviewManager.createReviewPoint(review);
        //reviewManager.addReview(review);

        return review;
    }
}