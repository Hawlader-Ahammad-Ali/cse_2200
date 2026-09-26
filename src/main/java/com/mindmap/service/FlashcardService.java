package com.mindmap.service;

import com.mindmap.ai.dto.SuggestedFlashcard;
import com.mindmap.dao.FlashcardDAO;
import com.mindmap.dao.impl.FlashcardDAOImpl;
import com.mindmap.model.Flashcard;
import com.mindmap.model.Origin;
import com.mindmap.model.Review;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for flashcards. All operations are user-scoped.
 */
public class FlashcardService {

    private final FlashcardDAO     flashcardDAO;
    private final ReviewService    reviewService;

    public FlashcardService() {
        this.flashcardDAO = new FlashcardDAOImpl();
        this.reviewService = new ReviewService();
    }

    /** Test constructor. */
    public FlashcardService(FlashcardDAO flashcardDAO, ReviewService reviewService) {
        this.flashcardDAO = flashcardDAO;
        this.reviewService = reviewService;
    }

    // ------------------------------------------------------------- reads

    public List<Flashcard> getAll() throws ServiceException {
        try { return flashcardDAO.findByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load flashcards.", e); }
    }

    public List<Flashcard> search(String query) throws ServiceException {
        if (query == null || query.isBlank()) return getAll();
        try { return flashcardDAO.searchByUser(uid(), query.trim()); }
        catch (SQLException e) { throw new ServiceException("Search failed.", e); }
    }

    public List<Flashcard> getDue() throws ServiceException {
        try { return flashcardDAO.findDue(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load due cards.", e); }
    }

    public List<Flashcard> getForKnowledgeItem(int knowledgeItemId) throws ServiceException {
        try { return flashcardDAO.findByKnowledgeItem(knowledgeItemId); }
        catch (SQLException e) { throw new ServiceException("Could not load cards.", e); }
    }

    public Optional<Flashcard> getById(int id) throws ServiceException {
        try { return flashcardDAO.findById(id); }
        catch (SQLException e) { throw new ServiceException("Could not load card.", e); }
    }

    public int countAll() throws ServiceException {
        try { return flashcardDAO.countByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count cards.", e); }
    }

    public int countDue() throws ServiceException {
        try { return flashcardDAO.countDue(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count due cards.", e); }
    }

    // ------------------------------------------------------------- writes

    public Flashcard create(Flashcard card) throws ServiceException {
        validate(card);
        card.setUserId(uid());
        if (card.getOrigin() == null) card.setOrigin(Origin.USER_CREATED);

        try {
            Flashcard saved = flashcardDAO.insert(card);
            // Auto-enroll for spaced repetition
            try {
                reviewService.enrollFlashcard(saved.getId());
            } catch (ServiceException e) {
                // Non-fatal — card is still created
            }
            return saved;
        } catch (SQLException e) {
            throw new ServiceException("Could not save flashcard.", e);
        }
    }

    public Flashcard update(Flashcard card) throws ServiceException {
        validate(card);
        if (card.getId() <= 0) throw new ServiceException("Invalid card.");
        try {
            flashcardDAO.update(card);
            return flashcardDAO.findById(card.getId()).orElse(card);
        } catch (SQLException e) {
            throw new ServiceException("Could not update flashcard.", e);
        }
    }

    public void delete(int id) throws ServiceException {
        try { flashcardDAO.delete(id); }
        catch (SQLException e) { throw new ServiceException("Could not delete card.", e); }
    }

    /**
     * Bulk create from AI suggestions. Each accepted suggestion becomes a
     * flashcard with {@code origin = AI_SUGGESTED} (user confirmed via the dialog).
     */
    public List<Flashcard> createFromSuggestions(List<SuggestedFlashcard> accepted,
                                                 Integer knowledgeItemId)
            throws ServiceException {
        List<Flashcard> created = new ArrayList<>();
        if (accepted == null || accepted.isEmpty()) return created;

        for (SuggestedFlashcard s : accepted) {
            if (!s.isAccepted()) continue;
            Flashcard f = new Flashcard(s.getQuestion(), s.getAnswer());
            f.setKnowledgeItemId(knowledgeItemId);
            f.setOrigin(Origin.USER_CONFIRMED);
            created.add(create(f));
        }
        return created;
    }

    // ------------------------------------------------------------- review status

    public Optional<Review> getReview(int flashcardId) throws ServiceException {
        return reviewService.getReviewForFlashcard(flashcardId);
    }

    // ------------------------------------------------------------- helpers

    private void validate(Flashcard f) throws ServiceException {
        if (f == null) throw new ServiceException("No flashcard provided.");
        if (f.getQuestion() == null || f.getQuestion().isBlank())
            throw new ServiceException("Question is required.");
        if (f.getAnswer() == null || f.getAnswer().isBlank())
            throw new ServiceException("Answer is required.");
        if (f.getQuestion().length() > 2000)
            throw new ServiceException("Question is too long (max 2000 characters).");
        if (f.getAnswer().length() > 5000)
            throw new ServiceException("Answer is too long (max 5000 characters).");
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
}