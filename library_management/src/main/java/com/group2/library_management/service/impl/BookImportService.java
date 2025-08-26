package com.group2.library_management.service.impl;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import com.group2.library_management.util.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import com.group2.library_management.dto.mapper.*;
import com.group2.library_management.dto.request.bookinstance.*;
import com.group2.library_management.entity.*;
import com.group2.library_management.entity.enums.*;
import com.group2.library_management.exception.DuplicateBarcodeException;
import com.group2.library_management.exception.EmptyImportDataException;
import com.group2.library_management.exception.InvalidColumnFormatException;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.*;
import com.group2.library_management.service.*;
import org.springframework.context.MessageSource;

@Service
public class BookImportService extends ExcelImportService<BookInstance>{ 
    private final Validator validator;
    private final EditionRepository editionRepository;
    private final BookInstanceRepository bookInstanceRepository;
    private final BookInstanceMapper bookInstanceMapper;
    @Autowired
    public BookImportService(EditionRepository editionRepository, BookInstanceRepository bookInstanceRepository, BookInstanceMapper bookInstanceMapper, Validator validator, MessageSource messageSource) {
        super(messageSource);
        this.editionRepository = editionRepository;
        this.bookInstanceRepository = bookInstanceRepository;
        this.bookInstanceMapper = bookInstanceMapper;
        this.validator = validator;
    }

    //  Lớp con phải định nghĩa cách ánh xạ một dòng Excel thành một đối tượng.
    //  Ném Exception nếu có lỗi validation.
    //  @param row Dòng hiện tại
    //  @param rowNumber Số thứ tự dòng (để báo lỗi)
    // @param processedBarcodes Set chứa các barcode đã xử lý để kiểm tra trùng lặp
    //  @return Đối tượng T đã được tạo và validate
    @Override
    protected BookInstance mapRowToEntity(Row row, int rowNumber, Set<String> processedBarcodes) throws  
            ResourceNotFoundException, DuplicateBarcodeException, EmptyImportDataException, InvalidColumnFormatException {
        CreateBookInstanceDto createBookInstanceDto = null;
        // mapping từ dòng Excel sang CreateBookInstanceDto
        createBookInstanceDto = mapRowToCreateDto(row, rowNumber, processedBarcodes);
        
        // Phần còn lại của phương thức giữ nguyên
        List<String> dtoErrors = new ArrayList<>();
        Set<ConstraintViolation<CreateBookInstanceDto>> violations = validator.validate(createBookInstanceDto);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<CreateBookInstanceDto> violation : violations) {
                dtoErrors.add(violation.getMessage());
            }
        }
        if (!dtoErrors.isEmpty()) {
            throw new InvalidColumnFormatException(String.join(", ", dtoErrors));
        }
        BookInstance bookInstance = bookInstanceMapper.toEntity(createBookInstanceDto);
        return bookInstance;
    }


    //  Lớp con phải định nghĩa cách lưu danh sách các đối tượng.
    //  @param entities Danh sách các đối tượng cần lưu
    @Override
    protected void saveAll(List<BookInstance> entities) {
        bookInstanceRepository.saveAll(entities);
    }

    private CreateBookInstanceDto mapRowToCreateDto(Row row, int rowNumber, Set<String> processedBarcodes) throws 
            ResourceNotFoundException, DuplicateBarcodeException, EmptyImportDataException, InvalidColumnFormatException {        
        // Cột isbn
        String isbn = ExcelUtil.getCellValueAsString(row.getCell(0), "ISBN");
        Edition edition = isValidIsbn(isbn);

        // Cột barcode
        String barcode = ExcelUtil.getCellValueAsString(row.getCell(1), "Barcode");
        //Kiểm tra xem barcode đã gặp trong file này chưa
        isValidBarcode(barcode, processedBarcodes);
        // Nếu ok thêm barcode này vào Set để kiểm tra các dòng sau
        processedBarcodes.add(barcode);

        //Cột callNumber
        String callNumber = ExcelUtil.getCellValueAsString(row.getCell(2), "Call Number");

        // Cột acquiredDate
        LocalDate acquiredDate = ExcelUtil.getCellValueAsLocalDate(row.getCell(3), "Ngày Nhập");

        // Cột acquiredPrice
        BigDecimal acquiredPrice = ExcelUtil.getCellValueAsBigDecimal(row.getCell(4), "Giá Nhập");
        
        // Cột Note
        String note = ExcelUtil.getCellValueAsString(row.getCell(5), "Ghi Chú");

        // Mặc định status là AVAILABLE
        BookStatus Status = BookStatus.AVAILABLE;

        CreateBookInstanceDto createBookInstanceDto = CreateBookInstanceDto.builder()
                .edition(edition)
                .barcode(barcode)
                .callNumber(callNumber)
                .acquiredDate(acquiredDate)
                .acquiredPrice(acquiredPrice)
                .status(Status)
                .note(note)
                .build();
        return createBookInstanceDto;
    }

    // Xử lý điều kiện thỏa mãn của ISBN
    private Edition isValidIsbn(String isbn) throws ResourceNotFoundException, EmptyImportDataException {
        if (isbn == null) {
            throw new EmptyImportDataException("ISBN");
        }
        Boolean existsIsbn = editionRepository.existsByIsbn(isbn);
        if (!existsIsbn) {
            throw new ResourceNotFoundException(isbn, "ISBN");
        }
        Edition edition = editionRepository.findByIsbn(isbn).orElseThrow(() -> new ResourceNotFoundException(isbn, "ISBN"));
        return edition;
    }

    // Xử lý điều kiện barcode
    private void isValidBarcode(String barcode, Set<String> processedBarcodes) throws DuplicateBarcodeException {
        //Kiểm tra xem barcode đã gặp trong file này chưa
        if (processedBarcodes.contains(barcode)) {
            throw new DuplicateBarcodeException(barcode);
        }
        // Nếu chưa, kiểm tra trong database
        Boolean existsBarcodeInDb = bookInstanceRepository.existsByBarcode(barcode);
        if (existsBarcodeInDb) {
            throw new DuplicateBarcodeException(barcode, "Barcode");
        }
    }
}
