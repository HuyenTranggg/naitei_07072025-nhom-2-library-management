package com.group2.library_management.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.group2.library_management.dto.request.UpdateBorrowingDetailRequest;
import com.group2.library_management.dto.response.BorrowingReceiptResponse;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.BorrowingDetail;
import com.group2.library_management.entity.BorrowingReceipt;
import com.group2.library_management.entity.BorrowingRequestDetail;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.User;
import com.group2.library_management.entity.enums.BookStatus;
import com.group2.library_management.entity.enums.BorrowingStatus;
import com.group2.library_management.repository.BookInstanceRepository;
import com.group2.library_management.repository.BorrowingDetailRepository;
import com.group2.library_management.repository.BorrowingReceiptRepository;
import com.group2.library_management.repository.EditionRepository;
import com.group2.library_management.service.BorrowingReceiptService;
import com.group2.library_management.repository.BorrowingRequestDetailRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BorrowingReceiptServiceImpl implements BorrowingReceiptService {

    private static final String RECEIPT_NOT_FOUND_ERROR = "Không tìm thấy phiếu mượn với ID: ";
    private static final String PENDING_STATUS_REQUIRED_ERROR = "Chỉ có thể phê duyệt yêu cầu đang ở trạng thái chờ phê duyệt";
    private static final String REJECTION_STATUS_REQUIRED_ERROR = "Chỉ có thể từ chối yêu cầu đang ở trạng thái chờ phê duyệt";
    private static final String BORROWED_STATUS_REQUIRED_ERROR = "Chỉ có thể trả sách khi phiếu mượn đang ở trạng thái đã mượn";

    private final BorrowingReceiptRepository borrowingReceiptRepository;
    private final BookInstanceRepository bookInstanceRepository;
    private final EditionRepository editionRepository;
    private final BorrowingDetailRepository borrowingDetailRepository;
    private final BorrowingRequestDetailRepository borrowingRequestDetailRepository;

    private final Map<Integer, LocalDateTime> detailExtendedDueDates = new HashMap<>();

    @Override
    public Page<BorrowingReceiptResponse> getAllBorrowingRequests(String keyword,
            BorrowingStatus status, Pageable pageable) {
        Specification<BorrowingReceipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("user");
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(keyword)) {
                Join<BorrowingReceipt, User> userJoin = root.join("user");
                String pattern = "%" + keyword.toLowerCase().trim() + "%";
                predicates.add(cb.or(cb.like(cb.lower(userJoin.get("name")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BorrowingReceipt> receiptPage = borrowingReceiptRepository.findAll(spec, pageable);
        return receiptPage.map(BorrowingReceiptResponse::fromEntity);
    }

    @Override
    public BorrowingReceiptResponse getBorrowingRequestById(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Không tìm thấy phiếu mượn với ID:" + " " + id));

        checkAndUpdateOverdueStatus(receipt);

        return BorrowingReceiptResponse.fromEntity(receipt, true);
    }

    @Override
    @Transactional
    public boolean checkAndUpdateOverdueStatus(Integer receiptId) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException(RECEIPT_NOT_FOUND_ERROR + receiptId));

        return checkAndUpdateOverdueStatus(receipt);
    }

    @Transactional
    protected boolean checkAndUpdateOverdueStatus(BorrowingReceipt receipt) {
        if (receipt.getStatus() != BorrowingStatus.BORROWED) {
            return false;
        }

        LocalDateTime latestDueDate = getEffectiveDueDate(receipt);

        if (latestDueDate == null || !LocalDateTime.now().isAfter(latestDueDate)) {
            return false;
        }

        List<BorrowingDetail> details = borrowingDetailRepository.findByBorrowingReceiptId(receipt.getId());
        boolean hasActiveBorrowedBooks = details.stream()
                .anyMatch(d -> d.getBookInstance().getStatus() == BookStatus.BORROWED);

        if (hasActiveBorrowedBooks) {
            receipt.setStatus(BorrowingStatus.OVERDUE);
            borrowingReceiptRepository.save(receipt);
            log.info("Cập nhật phiếu mượn ID {} sang trạng thái OVERDUE", receipt.getId());
            return true;
        }

        return false;
    }

    private LocalDateTime getEffectiveDueDate(BorrowingReceipt receipt) {
        LocalDateTime receiptDueDate = receipt.getDueDate();

        LocalDateTime maxExtendedDate = detailExtendedDueDates.values().stream()
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (maxExtendedDate != null && (receiptDueDate == null || maxExtendedDate.isAfter(receiptDueDate))) {
            return maxExtendedDate;
        }

        return receiptDueDate;
    }

    @Override
    @Transactional
    public void approveBorrowingRequest(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RECEIPT_NOT_FOUND_ERROR + id));

        // Check current status
        if (receipt.getStatus() != BorrowingStatus.PENDING) {
            throw new IllegalStateException(PENDING_STATUS_REQUIRED_ERROR);
        }

        // Get request details from borrowing_request_details table
        List<BorrowingRequestDetail> requestDetails = borrowingRequestDetailRepository.findByBorrowingReceiptId(id);
        if (requestDetails == null || requestDetails.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy chi tiết yêu cầu mượn cho phiếu này");
        }

        // Check availability and prepare error messages
        List<String> insufficientBooks = new ArrayList<>();
        Map<Integer, List<BookInstance>> availableInstancesMap = new HashMap<>();

        // Check availability for all editions
        for (BorrowingRequestDetail requestDetail : requestDetails) {
            Integer editionId = requestDetail.getEdition().getId();
            int requestedQuantity = requestDetail.getQuantity();

            // Find available book instances for this edition
            List<BookInstance> availableInstances = bookInstanceRepository
                    .findByEditionIdAndStatusOrderByAcquiredDateAsc(editionId, BookStatus.AVAILABLE);

            if (availableInstances.size() < requestedQuantity) {
                String editionTitle = requestDetail.getEdition().getTitle();
                insufficientBooks.add(String.format("'%s' (yêu cầu: %d, có sẵn: %d)", 
                    editionTitle, requestedQuantity, availableInstances.size()));
            } else {
                // Take only the needed quantity
                availableInstancesMap.put(editionId, availableInstances.subList(0, requestedQuantity));
            }
        }

        // If any edition doesn't have enough books, throw error with details
        if (!insufficientBooks.isEmpty()) {
            String errorMessage = "Không thể phê duyệt: Không đủ sách có sẵn cho " + String.join(", ", insufficientBooks);
            throw new IllegalStateException(errorMessage);
        }

        // All books are available, proceed with approval
        List<BorrowingDetail> borrowingDetailsToSave = new ArrayList<>();

        for (BorrowingRequestDetail requestDetail : requestDetails) {
            Integer editionId = requestDetail.getEdition().getId();
            List<BookInstance> instancesToAssign = availableInstancesMap.get(editionId);
            Edition edition = requestDetail.getEdition();

            for (BookInstance bookInstance : instancesToAssign) {
                // Create borrowing detail
                BorrowingDetail borrowingDetail = BorrowingDetail.builder()
                        .borrowingReceipt(receipt)
                        .bookInstance(bookInstance)
                        .build();
                borrowingDetailsToSave.add(borrowingDetail);

                // Update book instance status: AVAILABLE → RESERVED
                bookInstance.setStatus(BookStatus.RESERVED);
                bookInstanceRepository.save(bookInstance);

                log.debug("Assigned BookInstance ID {} (barcode: {}) to receipt ID {}", 
                    bookInstance.getId(), bookInstance.getBarcode(), receipt.getId());
            }

            // Update available quantity for the edition
            Integer currentAvailable = edition.getAvailableQuantity();
            if (currentAvailable == null || currentAvailable < instancesToAssign.size()) {
                throw new IllegalStateException(
                        "Số lượng sách khả dụng không đủ để cho mượn: " + edition.getTitle());
            }
            edition.setAvailableQuantity(currentAvailable - instancesToAssign.size());
            editionRepository.save(edition);

            log.debug("Updated available quantity for Edition ID {} to {}", 
                editionId, edition.getAvailableQuantity());
        }

        // Save all borrowing details
        borrowingDetailRepository.saveAll(borrowingDetailsToSave);

        // Update receipt status
        receipt.setStatus(BorrowingStatus.APPROVED);
        borrowingReceiptRepository.save(receipt);

        log.info("Successfully approved borrowing request ID {} with {} book instances assigned", 
            receipt.getId(), borrowingDetailsToSave.size());

        // TODO: Send email notification to user
        // emailService.sendApprovalNotification(receipt.getUser().getEmail(), receipt);
    }

    @Override
    @Transactional
    public void rejectBorrowingRequest(Integer id, String rejectedReason) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RECEIPT_NOT_FOUND_ERROR + id));

        // Check current status
        if (receipt.getStatus() != BorrowingStatus.PENDING) {
            throw new IllegalStateException(REJECTION_STATUS_REQUIRED_ERROR);
        }

        // Update borrowing receipt status and rejection reason
        receipt.setStatus(BorrowingStatus.REJECTED);
        receipt.setRejectedReason(rejectedReason);

        // Save changes
        borrowingReceiptRepository.save(receipt);

        // TODO: Send email notification to user
        // emailService.sendRejectionNotification(receipt.getUser().getEmail(),
        // receipt);
    }

    @Override
    @Transactional
    public void returnBook(Integer id) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(RECEIPT_NOT_FOUND_ERROR + id));

        // Check current status - must be BORROWED to return
        if (receipt.getStatus() != BorrowingStatus.BORROWED) {
            throw new IllegalStateException(BORROWED_STATUS_REQUIRED_ERROR);
        }

        // Update status of all book instances and increase availableQuantity
        receipt.getBorrowingDetails().forEach(detail -> {
            BookInstance bookInstance = detail.getBookInstance();
            Edition edition = bookInstance.getEdition();

            // Update book instance status: BORROWED → AVAILABLE (when return book)
            bookInstance.setStatus(BookStatus.AVAILABLE);
            bookInstanceRepository.save(bookInstance);

            // Increase availableQuantity when changing from BORROWED to AVAILABLE
            int currentAvailable = edition.getAvailableQuantity() != null ? edition.getAvailableQuantity() : 0;
            edition.setAvailableQuantity(currentAvailable + 1);
            editionRepository.save(edition); // Save edition to update availableQuantity

            // Set return date
            detail.setRefundDate(LocalDateTime.now());
        });

        // Update borrowing receipt status to RETURNED
        receipt.setStatus(BorrowingStatus.RETURNED);
        borrowingReceiptRepository.save(receipt);

        // TODO: Send email notification to user
        // emailService.sendReturnConfirmationNotification(receipt.getUser().getEmail(),
        // receipt);
    }

    /**
     * Helper method to format book instance information for error messages
     */
    private String formatBookInstanceError(BookInstance bookInstance) {
        return bookInstance.getEdition().getTitle() + " (" + bookInstance.getStatus() + ")";
    }

    @Override
    @Transactional
    public void updateBorrowingDetails(Integer receiptId, List<UpdateBorrowingDetailRequest> requests) {
        BorrowingReceipt receipt = borrowingReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException(RECEIPT_NOT_FOUND_ERROR + receiptId));

        if (receipt.getStatus() == BorrowingStatus.PENDING ||
                receipt.getStatus() == BorrowingStatus.REJECTED ||
                receipt.getStatus() == BorrowingStatus.RETURNED ||
                receipt.getStatus() == BorrowingStatus.CANCELLED ||
                receipt.getStatus() == BorrowingStatus.LOST_REPORTED) {
            throw new IllegalStateException(
                    "Không thể cập nhật chi tiết ở trạng thái hiện tại: " + receipt.getStatus());
        }

        BorrowingStatus initialReceiptStatus = receipt.getStatus();
        boolean hasAnyChange = false;
        boolean hasExtendAction = false;

        LocalDateTime batchCurrentTime = LocalDateTime.now();

        for (UpdateBorrowingDetailRequest req : requests) {
            BorrowingDetail detail = borrowingDetailRepository.findById(req.getBorrowingDetailId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy chi tiết mượn ID: " + req.getBorrowingDetailId()));

            if (!detail.getBorrowingReceipt().getId().equals(receiptId)) {
                throw new IllegalArgumentException("Lỗi bảo mật: Chi tiết mượn không thuộc phiếu mượn này.");
            }

            switch (req.getAction().toUpperCase()) {
                case "BORROWED":
                    handleBorrowedAction(detail, batchCurrentTime);
                    break;
                case "NOT_BORROWED":
                    handleNotBorrowedAction(detail, batchCurrentTime);
                    break;
                case "RETURN":
                    handleReturnAction(detail, req, batchCurrentTime);
                    break;
                case "LOST":
                    handleLostAction(detail, req, batchCurrentTime);
                    break;
                case "DAMAGED":
                    handleDamagedAction(detail, req, batchCurrentTime);
                    break;
                case "EXTEND":
                    handleExtendAction(receipt, req, batchCurrentTime);
                    hasExtendAction = true;
                    break;
                default:
                    continue;
            }
            hasAnyChange = true;
        }

        if (hasAnyChange) {
            if (hasExtendAction) {
                LocalDateTime maxDueDate = getEffectiveDueDate(receipt);
                if (maxDueDate != null) {
                    receipt.setDueDate(maxDueDate);
                }
            }

            updateFinalReceiptStatus(receipt, initialReceiptStatus);
            borrowingReceiptRepository.save(receipt);
        }
    }

    private void handleBorrowedAction(BorrowingDetail detail, LocalDateTime currentTime) {
        BookInstance bookInstance = detail.getBookInstance();

        if (bookInstance.getStatus() != BookStatus.RESERVED) {
            throw new IllegalStateException("Chỉ có thể lấy sách khi đang ở trạng thái 'Đã đặt chỗ'");
        }

        bookInstance.setStatus(BookStatus.BORROWED);
        bookInstanceRepository.save(bookInstance);

        BorrowingReceipt receipt = detail.getBorrowingReceipt();
        if (receipt.getBorrowedDate() == null) {
            receipt.setBorrowedDate(currentTime);
            borrowingReceiptRepository.save(receipt);
            log.info("Receipt ID {} - Set borrowedDate = {}", receipt.getId(), currentTime);
        }

        log.info("Detail ID {} - Sách đã được lấy lúc {}", detail.getId(), currentTime);
    }

    private void handleNotBorrowedAction(BorrowingDetail detail, LocalDateTime currentTime) {
        BookInstance bookInstance = detail.getBookInstance();

        if (bookInstance.getStatus() != BookStatus.RESERVED) {
            throw new IllegalStateException("Chỉ có thể đánh dấu 'Không lấy' khi sách đang ở trạng thái 'Đã đặt chỗ'");
        }

        Edition edition = bookInstance.getEdition();

        bookInstance.setStatus(BookStatus.AVAILABLE);
        bookInstanceRepository.save(bookInstance);

        Integer currentAvailable = edition.getAvailableQuantity();
        int newAvailable = (currentAvailable != null ? currentAvailable : 0) + 1;
        edition.setAvailableQuantity(newAvailable);
        editionRepository.save(edition);

        detail.setActualReturnDate(currentTime);
        borrowingDetailRepository.save(detail);

        log.info("Detail ID {} - Đánh dấu không lấy sách lúc {}", detail.getId(), currentTime);
    }

    private void handleReturnAction(BorrowingDetail detail, UpdateBorrowingDetailRequest req, LocalDateTime currentTime) {
        BookInstance bookInstance = detail.getBookInstance();

        if (detail.getActualReturnDate() != null ||
                (bookInstance.getStatus() != BookStatus.BORROWED && bookInstance.getStatus() != BookStatus.RESERVED)) {
            throw new IllegalStateException("Không thể trả sách: Sách đã được xử lý hoặc không ở trạng thái 'Đang mượn' hoặc 'Đã đặt chỗ'.");
        }

        LocalDateTime returnDate = req.getActualReturnDate() != null ? req.getActualReturnDate() : currentTime;

        detail.setActualReturnDate(returnDate);
        bookInstance.setStatus(BookStatus.AVAILABLE);
        bookInstanceRepository.save(bookInstance);

        Edition edition = bookInstance.getEdition();
        Integer currentAvailable = edition.getAvailableQuantity();
        int newAvailable = (currentAvailable != null ? currentAvailable : 0) + 1;
        edition.setAvailableQuantity(newAvailable);
        editionRepository.save(edition);

        borrowingDetailRepository.save(detail);

        log.info("Detail ID {} - Trả sách lúc {}", detail.getId(), returnDate);
    }

    private void handleLostAction(BorrowingDetail detail, UpdateBorrowingDetailRequest req, LocalDateTime currentTime) {
        BookInstance bookInstance = detail.getBookInstance();

        if (detail.getActualReturnDate() != null ||
                (bookInstance.getStatus() != BookStatus.BORROWED && bookInstance.getStatus() != BookStatus.RESERVED)) {
            throw new IllegalStateException("Không thể báo mất sách: Sách đã được xử lý hoặc không ở trạng thái 'Đang mượn' hoặc 'Đã đặt chỗ'.");
        }

        LocalDateTime reportDate = req.getActualReturnDate() != null ? req.getActualReturnDate() : currentTime;

        detail.setActualReturnDate(reportDate);
        bookInstance.setStatus(BookStatus.LOST);
        bookInstanceRepository.save(bookInstance);

        borrowingDetailRepository.save(detail);

        log.info("Detail ID {} - Báo mất lúc {}", detail.getId(), reportDate);
    }

    private void handleDamagedAction(BorrowingDetail detail, UpdateBorrowingDetailRequest req, LocalDateTime currentTime) {
        BookInstance bookInstance = detail.getBookInstance();

        if (detail.getActualReturnDate() != null ||
                (bookInstance.getStatus() != BookStatus.BORROWED && bookInstance.getStatus() != BookStatus.RESERVED)) {
            throw new IllegalStateException("Không thể báo hỏng sách: Sách đã được xử lý hoặc không ở trạng thái 'Đang mượn' hoặc 'Đã đặt chỗ'.");
        }

        LocalDateTime reportDate = req.getActualReturnDate() != null ? req.getActualReturnDate() : currentTime;

        detail.setActualReturnDate(reportDate);
        bookInstance.setStatus(BookStatus.DAMAGED);
        bookInstanceRepository.save(bookInstance);

        borrowingDetailRepository.save(detail);

        log.info("Detail ID {} - Báo hỏng lúc {}", detail.getId(), reportDate);
    }

    private void handleExtendAction(BorrowingReceipt receipt, UpdateBorrowingDetailRequest req, LocalDateTime currentTime) {
        if (req.getExtendDays() == null || req.getExtendDays() <= 0) {
            throw new IllegalArgumentException("Số ngày gia hạn phải lớn hơn 0.");
        }

        BorrowingDetail detail = borrowingDetailRepository.findById(req.getBorrowingDetailId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy chi tiết mượn ID: " + req.getBorrowingDetailId()));

        // Ensure the book is in a valid state for extension
        if (detail.getBookInstance().getStatus() != BookStatus.BORROWED) {
            throw new IllegalStateException("Chỉ có thể gia hạn sách khi đang ở trạng thái 'Đang mượn'");
        }

        LocalDateTime currentDetailDueDate = detailExtendedDueDates.get(detail.getId());
        if (currentDetailDueDate == null) {
            currentDetailDueDate = receipt.getDueDate() != null ? receipt.getDueDate() : currentTime;
        }

        LocalDateTime newDetailDueDate = currentDetailDueDate.plusDays(req.getExtendDays());

        if (newDetailDueDate.isBefore(currentTime)) {
            throw new IllegalArgumentException("Ngày gia hạn phải sau ngày hiện tại.");
        }

        detailExtendedDueDates.put(detail.getId(), newDetailDueDate);

        log.info("Detail ID {} - Gia hạn {} ngày, hạn mới: {}", detail.getId(), req.getExtendDays(), newDetailDueDate);
    }

    private void updateFinalReceiptStatus(BorrowingReceipt receipt, BorrowingStatus initialReceiptStatus) {
        List<BorrowingDetail> details = borrowingDetailRepository.findByBorrowingReceiptId(receipt.getId());
        List<BookStatus> currentBookStatuses = details.stream()
                .map(d -> d.getBookInstance().getStatus())
                .collect(Collectors.toList());

        if (currentBookStatuses.contains(BookStatus.BORROWED)) {
            LocalDateTime effectiveDueDate = getEffectiveDueDate(receipt);
            boolean isOverdue = effectiveDueDate != null && LocalDateTime.now().isAfter(effectiveDueDate);
            receipt.setStatus(isOverdue ? BorrowingStatus.OVERDUE : BorrowingStatus.BORROWED);
            return;
        }

        if (initialReceiptStatus == BorrowingStatus.APPROVED) {
            boolean allNotTaken = details.stream()
                    .allMatch(d -> d.getBookInstance().getStatus() == BookStatus.AVAILABLE &&
                            d.getActualReturnDate() != null);

            if (allNotTaken) {
                receipt.setStatus(BorrowingStatus.CANCELLED);
                return;
            }

            if (currentBookStatuses.contains(BookStatus.RESERVED)) {
                receipt.setStatus(BorrowingStatus.APPROVED);
                return;
            }
        }

        boolean hasLostOrDamaged = currentBookStatuses.contains(BookStatus.LOST) ||
                currentBookStatuses.contains(BookStatus.DAMAGED);

        if (!currentBookStatuses.contains(BookStatus.BORROWED)) {
            if (hasLostOrDamaged) {
                receipt.setStatus(BorrowingStatus.LOST_REPORTED);
            } else {
                receipt.setStatus(BorrowingStatus.RETURNED);
            }
        }
    }
}
