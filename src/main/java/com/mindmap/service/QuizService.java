package com.mindmap.service;

import com.mindmap.ai.dto.SuggestedQuizQuestion;
import com.mindmap.dao.QuizDAO;
import com.mindmap.dao.StudySessionDAO;
import com.mindmap.dao.impl.QuizDAOImpl;
import com.mindmap.dao.impl.StudySessionDAOImpl;
import com.mindmap.model.Origin;
import com.mindmap.model.QuizAttempt;
import com.mindmap.model.QuizQuestion;
import com.mindmap.model.QuizQuestionType;
import com.mindmap.model.StudySession;
import com.mindmap.model.User;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Quiz orchestration: question CRUD, session build, scoring, weak-topic detection.
 */
public class QuizService {

    private final QuizDAO         quizDAO;
    private final StudySessionDAO sessionDAO;

    public QuizService() {
        this.quizDAO    = new QuizDAOImpl();
        this.sessionDAO = new StudySessionDAOImpl();
    }

    public QuizService(QuizDAO quizDAO, StudySessionDAO sessionDAO) {
        this.quizDAO = quizDAO;
        this.sessionDAO = sessionDAO;
    }

    // ------------------------------------------------------------- reads

    public List<QuizQuestion> getAllQuestions() throws ServiceException {
        try { return quizDAO.findQuestionsByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not load questions.", e); }
    }

    public List<QuizQuestion> getQuestionsForItem(int knowledgeItemId) throws ServiceException {
        try { return quizDAO.findQuestionsByKnowledgeItem(knowledgeItemId); }
        catch (SQLException e) { throw new ServiceException("Could not load questions.", e); }
    }

    public int countQuestions() throws ServiceException {
        try { return quizDAO.countQuestionsByUser(uid()); }
        catch (SQLException e) { throw new ServiceException("Could not count questions.", e); }
    }

    public List<QuizAttempt> getRecentAttempts(int limit) throws ServiceException {
        try { return quizDAO.findAttemptsByUser(uid(), limit); }
        catch (SQLException e) { throw new ServiceException("Could not load attempts.", e); }
    }

    /** Weak questions: at least 2 attempts, accuracy <= 50%. */
    public List<QuizQuestion> getWeakQuestions() throws ServiceException {
        try {
            List<Integer> ids = quizDAO.findWeakQuestionIds(uid(), 2, 0.5);
            List<QuizQuestion> out = new ArrayList<>();
            for (Integer id : ids) {
                Optional<QuizQuestion> q = quizDAO.findQuestionById(id);
                q.ifPresent(out::add);
            }
            return out;
        } catch (SQLException e) {
            throw new ServiceException("Could not compute weak topics.", e);
        }
    }

    // ------------------------------------------------------------- writes

    public QuizQuestion createQuestion(QuizQuestion q) throws ServiceException {
        validate(q);
        q.setUserId(uid());
        if (q.getOrigin() == null) q.setOrigin(Origin.USER_CREATED);
        try { return quizDAO.insertQuestion(q); }
        catch (SQLException e) { throw new ServiceException("Could not save question.", e); }
    }

    public QuizQuestion updateQuestion(QuizQuestion q) throws ServiceException {
        validate(q);
        try {
            quizDAO.updateQuestion(q);
            return quizDAO.findQuestionById(q.getId()).orElse(q);
        } catch (SQLException e) {
            throw new ServiceException("Could not update question.", e);
        }
    }

    public void deleteQuestion(int id) throws ServiceException {
        try { quizDAO.deleteQuestion(id); }
        catch (SQLException e) { throw new ServiceException("Could not delete question.", e); }
    }

    /**
     * Bulk-create from AI suggestions for a knowledge item.
     */
    public List<QuizQuestion> createFromSuggestions(List<SuggestedQuizQuestion> accepted,
                                                    Integer knowledgeItemId)
            throws ServiceException {
        List<QuizQuestion> created = new ArrayList<>();
        if (accepted == null) return created;

        for (SuggestedQuizQuestion s : accepted) {
            if (!s.isAccepted()) continue;
            if (s.getQuestion() == null || s.getQuestion().isBlank()) continue;
            if (s.getCorrect() == null || s.getCorrect().isBlank()) continue;

            QuizQuestion q = new QuizQuestion();
            q.setKnowledgeItemId(knowledgeItemId);
            q.setQuestionType(QuizQuestionType.fromName(s.getType()));
            q.setQuestionText(s.getQuestion());
            q.setOptions(s.getOptions());
            q.setCorrectAnswer(s.getCorrect());
            q.setExplanation(s.getExplanation());
            q.setOrigin(Origin.USER_CONFIRMED);

            try {
                created.add(createQuestion(q));
            } catch (ServiceException e) {
                // Skip malformed, keep going
            }
        }
        return created;
    }

    // ------------------------------------------------------------- quiz session

    /**
     * Builds a random quiz from the user's question pool.
     * @param count     desired number of questions (clamped to available)
     * @param weakOnly  if true, prioritize weak questions
     */
    public List<QuizQuestion> buildQuiz(int count, boolean weakOnly) throws ServiceException {
        List<QuizQuestion> pool;
        if (weakOnly) {
            pool = new ArrayList<>(getWeakQuestions());
            // Fall back to full pool if we don't have enough weak items
            if (pool.size() < count) {
                List<QuizQuestion> all = getAllQuestions();
                for (QuizQuestion q : all) {
                    if (!pool.contains(q)) pool.add(q);
                }
            }
        } else {
            pool = new ArrayList<>(getAllQuestions());
        }

        Collections.shuffle(pool);
        if (pool.size() > count) return pool.subList(0, count);
        return pool;
    }

    // ------------------------------------------------------------- scoring

    /**
     * Checks the user's answer against the correct answer for this question type.
     * Returns true if correct.
     */
    public boolean isCorrect(QuizQuestion q, String userAnswer) {
        if (userAnswer == null) return false;

        String user = normalize(userAnswer);

        switch (q.getQuestionType()) {
            case MCQ: {
                return normalize(q.getCorrectAnswer()).equals(user);
            }
            case TRUE_FALSE: {
                return normalize(q.getCorrectAnswer()).equals(user);
            }
            case SHORT: {
                // Fuzzy: normalize both, then check for containment either way
                String expected = normalize(q.getCorrectAnswer());
                if (expected.isEmpty()) return false;
                return expected.equals(user)
                        || user.contains(expected)
                        || expected.contains(user);
            }
            default:
                return false;
        }
    }

    /** Records an attempt and returns whether it was correct. */
    public boolean submitAnswer(QuizQuestion q, String userAnswer) throws ServiceException {
        boolean correct = isCorrect(q, userAnswer);
        QuizAttempt a = new QuizAttempt(q.getId(), userAnswer, correct);
        a.setUserId(uid());
        try { quizDAO.insertAttempt(a); }
        catch (SQLException e) { throw new ServiceException("Could not save attempt.", e); }
        return correct;
    }

    // ------------------------------------------------------------- sessions

    public StudySession startSession() throws ServiceException {
        try { return sessionDAO.insert(new StudySession(uid(), StudySession.Type.QUIZ)); }
        catch (SQLException e) { throw new ServiceException("Could not start session.", e); }
    }

    public void finishSession(int sessionId, int total, int correct) throws ServiceException {
        try { sessionDAO.finish(sessionId, total, correct); }
        catch (SQLException e) { throw new ServiceException("Could not finish session.", e); }
    }

    // ------------------------------------------------------------- helpers

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ");
    }

    private void validate(QuizQuestion q) throws ServiceException {
        if (q == null) throw new ServiceException("No question provided.");
        if (q.getQuestionText() == null || q.getQuestionText().isBlank())
            throw new ServiceException("Question text is required.");
        if (q.getCorrectAnswer() == null || q.getCorrectAnswer().isBlank())
            throw new ServiceException("Correct answer is required.");
        if (q.getQuestionType() == QuizQuestionType.MCQ) {
            if (q.getOptions() == null || q.getOptions().size() < 2)
                throw new ServiceException("Multiple choice needs at least 2 options.");
            if (!q.getOptions().contains(q.getCorrectAnswer()))
                throw new ServiceException("Correct answer must be one of the options.");
        }
        if (q.getQuestionType() == QuizQuestionType.TRUE_FALSE) {
            String c = q.getCorrectAnswer().toLowerCase().trim();
            if (!c.equals("true") && !c.equals("false"))
                throw new ServiceException("True/False answer must be 'true' or 'false'.");
        }
    }

    private int uid() throws ServiceException {
        User u = AppContext.getInstance().getCurrentUser();
        if (u == null) throw new ServiceException("You are not signed in.");
        return u.getId();
    }
}