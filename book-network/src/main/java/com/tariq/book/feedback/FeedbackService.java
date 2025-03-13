package com.tariq.book.feedback;

import com.tariq.book.book.Book;
import com.tariq.book.book.BookRepository;
import com.tariq.book.common.PageResponse;
import com.tariq.book.exception.OperationNotPermittedException;
import com.tariq.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final BookRepository bookRepository;
    private final FeedBackRepository feedBackRepository;
    private final FeedbackMapper feedbackMapper;

    public Integer save(FeedbackRequest request, Authentication connectedUser) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID:" + request.bookId()));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("This book is not available for feedback");
        }

        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getCreatedBy(), connectedUser.getName())) {
            throw new OperationNotPermittedException("You cannot give feedback on your own book");
        }

        Feedback feedback = feedbackMapper.toFeedback(request);
        return feedBackRepository.save(feedback).getId();
    }

    public PageResponse<FeedBackResponse> findAllFeedbackByBook(Integer bookId, int page, int size, Authentication connectedUser) {
        Pageable pageable = PageRequest.of(page, size);
        User user = ((User) connectedUser.getPrincipal());

        Page<Feedback> feedbacks = feedBackRepository.findAllByBookId(bookId, pageable);

        List<FeedBackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> feedbackMapper.toFeedbackResponse(f, user.getId()))
                .toList();
        return new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );
    }
}
