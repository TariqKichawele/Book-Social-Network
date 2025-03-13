package com.tariq.book.book;

import com.tariq.book.common.PageResponse;
import com.tariq.book.exception.OperationNotPermittedException;
import com.tariq.book.file.FileStorageService;
import com.tariq.book.histoy.BookTransactionHistory;
import com.tariq.book.histoy.BookTransactionHistoryRepository;
import com.tariq.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final BookMapper bookMapper;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return bookRepository.save(book).getId();
    };

    public BookResponse findById(Integer bookId) {
        return (BookResponse) bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id " + bookId));
    }


    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());

        List<BookResponse> booksResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                booksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getId()), pageable);

        List<BookResponse> booksResponse = books.stream()
               .map(bookMapper::toBookResponse)
               .toList();

        return new PageResponse<>(
                booksResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());

        List<BorrowedBookResponse> booksResponse = allBorrowedBooks.stream()
               .map(bookMapper::toBorrowedBookResponse)
               .toList();

        return new PageResponse<>(
                booksResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }


    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<BookTransactionHistory> allReturnedBooks = transactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());

        List<BorrowedBookResponse> booksResponse = allReturnedBooks.stream()
               .map(bookMapper::toBorrowedBookResponse)
               .toList();

        return new PageResponse<>(
                booksResponse,
                allReturnedBooks.getNumber(),
                allReturnedBooks.getSize(),
                allReturnedBooks.getTotalElements(),
                allReturnedBooks.getTotalPages(),
                allReturnedBooks.isFirst(),
                allReturnedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
               .orElseThrow(() -> new EntityNotFoundException("Book not found with id " + bookId));

        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You are not the owner of this book");
        }

        book.setShareable(!book.isShareable());
        bookRepository.save(book);

        return book.getId();
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
               .orElseThrow(() -> new EntityNotFoundException("Book not found with id " + bookId));

        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You are not the owner of this book");
        }

        book.setArchived(!book.isArchived());
        bookRepository.save(book);

        return book.getId();
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
               .orElseThrow(() -> new EntityNotFoundException("Book not found with id " + bookId));

        if (book.isArchived() ||!book.isShareable()) {
            throw new OperationNotPermittedException("This book is not available");
        }

        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        final boolean isAlreadyBorrowedByUser = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());
        if (isAlreadyBorrowedByUser) {
            throw new OperationNotPermittedException("You have already borrowed this book and is not returned");
        }

        final boolean isAlreadyBorrowedByAnotherUser = transactionHistoryRepository.isAlreadyBorrowed(bookId);
        if (isAlreadyBorrowedByAnotherUser) {
            throw new OperationNotPermittedException("Another user has already borrowed this book");
        }

        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                        .user(user)
                        .book(book)
                        .returned(false)
                        .returnApproved(false)
                        .build();

        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found for bookId: " + bookId));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("This book is not available for borrowing");
        }

        User user = ((User) connectedUser.getPrincipal());
        if (Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        bookTransactionHistory.setReturned(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found for bookId: " + bookId));

        if (book.isArchived() || !book.isShareable()) {
            throw new OperationNotPermittedException("This book is not available for borrowing");
        }

        User user = ((User) connectedUser.getPrincipal());
        if (!Objects.equals(book.getOwner().getId(), user.getId())) {
            throw new OperationNotPermittedException("You cannot approve the return of a book you do not own");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet. You cannot approve its return"));

        bookTransactionHistory.setReturnApproved(true);
        return transactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public void uploadBookCover(Integer bookId, MultipartFile file, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found for bookId: " + bookId));

        User user = ((User) connectedUser.getPrincipal());

        var bookCover = fileStorageService.saveFile(file, user.getId());

        book.setBookCover(bookCover);
        bookRepository.save(book);

    }
}
