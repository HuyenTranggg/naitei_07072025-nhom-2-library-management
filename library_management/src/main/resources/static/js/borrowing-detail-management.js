$(document).ready(function () {
    console.log("jQuery document ready! Bắt đầu gắn sự kiện.");
    
    let changes = {};

    checkAndUpdateOverdueStatus();

    function checkAndUpdateOverdueStatus() {
        const receiptId = window.location.pathname.split('/').pop();
        
        fetch(`/admin/borrow-requests/${receiptId}/check-overdue?_method=PUT`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.ok) {
                return response.text();
            }
            throw new Error('Failed to check overdue status');
        })
        .then(result => {
            if (result === 'updated') {
                console.log("Receipt status updated to OVERDUE, reloading page...");
                window.location.reload();
            }
        })
        .catch(error => {
            console.error('Error checking overdue status:', error);
        });
    }

    $(document).on('change', '.status-dropdown', function() {
        const detailId = $(this).data('detail-id');
        const selectedValue = $(this).val();
        const $row = $(this).closest('tr');
        
        console.log(`Status changed for detail ${detailId}: ${selectedValue}`);

        hideAllInputs($row);
        
        if (!selectedValue) {
            removeChange(detailId);
            updateDropdownState(detailId, '');
            clearSuccessMessage($row);
            return;
        }

        switch(selectedValue) {
            case 'RETURN':
                handleReturnSelection(detailId, $row);
                break;
            case 'LOST':
                handleLostSelection(detailId, $row);
                break;
            case 'DAMAGED':
                handleDamagedSelection(detailId, $row);
                break;
            case 'BORROWED':
                handleBorrowedSelection(detailId, $row);
                break;
            case 'NOT_BORROWED':
                handleNotBorrowedSelection(detailId, $row);
                break;
        }
    });

    function handleReturnSelection(detailId, $row) {
        showReturnDateInput(detailId, $row);
        addChange(detailId, {
            action: 'RETURN',
            actualReturnDate: getCurrentDateTimeString()
        });
        updateDropdownState(detailId, 'success');
        showSmallSuccessMessage($row, 'Đã thêm trạng thái "Đã trả"');
    }

    function handleLostSelection(detailId, $row) {
        addChange(detailId, {
            action: 'LOST',
            actualReturnDate: getCurrentDateTimeString()
        });
        updateDropdownState(detailId, 'danger');
        showSmallSuccessMessage($row, 'Đã thêm trạng thái "Báo mất"');
    }

    function handleDamagedSelection(detailId, $row) {
        addChange(detailId, {
            action: 'DAMAGED',
            actualReturnDate: getCurrentDateTimeString()
        });
        updateDropdownState(detailId, 'warning');
        showSmallSuccessMessage($row, 'Đã thêm trạng thái "Báo hỏng"');
    }

    function handleBorrowedSelection(detailId, $row) {
        addChange(detailId, {
            action: 'BORROWED'
        });
        updateDropdownState(detailId, 'primary');
        showSmallSuccessMessage($row, 'Đã thêm trạng thái "Đã lấy sách"');
    }

    function handleNotBorrowedSelection(detailId, $row) {
        addChange(detailId, {
            action: 'NOT_BORROWED',
            actualReturnDate: getCurrentDateTimeString()
        });
        updateDropdownState(detailId, 'warning');
        showSmallSuccessMessage($row, 'Đã thêm trạng thái "Không lấy sách"');
    }

    $(document).on('change', '.return-date-input', function() {
        const detailId = $(this).data('detail-id');
        const returnDate = $(this).val();
        
        const selectedDate = new Date(returnDate);
        const today = new Date();
        today.setHours(23, 59, 59, 999);
        
        if (selectedDate > today) {
            alert('Ngày trả thực tế không được ở tương lai!');
            $(this).val(getCurrentDate());
            return;
        }
        
        if (changes[detailId] && changes[detailId].action === 'RETURN') {
            addChange(detailId, {
                action: 'RETURN',
                actualReturnDate: returnDate + 'T' + getCurrentTime()
            });
        }
    });

    $(document).on('click', '.extend-btn', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        const detailId = $(this).closest('tr').data('detail-id');
        if ($(this).is(':disabled')) return;
        
        const extendModal = $('#extendModal');
        if (extendModal.length === 0) {
            alert("Lỗi: Không thể hiển thị modal gia hạn!");
            return;
        }

        extendModal.removeData('detailId').data('detailId', detailId);
        $('#extendError').hide();
        $('#extendDays').val(7);
        extendModal.modal('show');
    });

    $(document).on('click', '#confirmExtendBtn', function() {
        const extendModal = $('#extendModal');
        const detailId = extendModal.data('detailId');
        const extendDays = parseInt($('#extendDays').val());
        
        if (!extendDays || extendDays <= 0) {
            $('#extendError').text('Vui lòng nhập số ngày hợp lệ (tối thiểu 1 ngày)').show();
            return;
        }

        addChange(detailId, {
            action: 'EXTEND',
            extendDays: extendDays
        });

        updateExtendButtonState(detailId, extendDays);
        extendModal.modal('hide');
        showSuccessMessage(`Đã thêm gia hạn ${extendDays} ngày vào danh sách cập nhật`);
    });

    $(document).on('click', '#updateReceiptBtn', function(e) {
        e.preventDefault();
        
        const updates = Object.keys(changes).map(detailId => ({
            borrowingDetailId: parseInt(detailId),
            ...changes[detailId]
        }));

        if (updates.length === 0) {
            if (typeof Swal !== 'undefined') {
                Swal.fire('Chưa có thay đổi', 'Vui lòng thực hiện thay đổi trước khi cập nhật.', 'warning');
            } else {
                alert('Chưa có thay đổi. Vui lòng thực hiện thay đổi trước khi cập nhật.');
            }
            return;
        }

        console.log('Updates to be sent:', JSON.stringify(updates, null, 2));

        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Xác nhận cập nhật',
                text: `Bạn có chắc chắn muốn lưu ${updates.length} thay đổi này không?`,
                icon: 'question',
                showCancelButton: true,
                confirmButtonText: 'Đồng ý, cập nhật!',
                cancelButtonText: 'Hủy'
            }).then((result) => {
                if (result.isConfirmed) {
                    performUpdate(updates, $(this));
                }
            });
        } else {
            if (confirm(`Bạn có chắc chắn muốn lưu ${updates.length} thay đổi này không?`)) {
                performUpdate(updates, $(this));
            }
        }
    });

    function performUpdate(updates, updateBtn) {
        updateBtn.prop('disabled', true).html('<i class="mdi mdi-loading mdi-spin mr-2"></i>Đang xử lý...');
        
        const receiptId = window.location.pathname.split('/').pop();
        const updateUrl = `/admin/borrow-requests/${receiptId}/update-details`;
        
        console.log('Sending request to:', updateUrl);
        console.log('Request body:', JSON.stringify(updates, null, 2));
        
        fetch(updateUrl, {
            method: 'PUT',
            headers: { 
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            },
            body: JSON.stringify(updates)
        })
        .then(async response => {
            console.log('Response status:', response.status);
            console.log('Response headers:', [...response.headers]);
            
            const responseText = await response.text();
            console.log('Response text:', responseText);
            
            if (response.ok) {
                let data;
                try {
                    data = responseText ? JSON.parse(responseText) : 'success';
                } catch (e) {
                    data = responseText || 'success';
                }
                return data;
            } else {
                let errorMessage;
                try {
                    const errorData = JSON.parse(responseText);
                    errorMessage = errorData.message || errorData.error || `HTTP ${response.status}`;
                } catch (e) {
                    errorMessage = responseText || `HTTP ${response.status}: Lỗi không xác định`;
                }
                throw new Error(errorMessage);
            }
        })
        .then(data => {
            console.log('Success response:', data);
            
            changes = {};
            
            sessionStorage.setItem('updateSuccess', 'Cập nhật phiếu mượn thành công!');
            window.location.reload();
        })
        .catch(error => {
            console.error('Error updating receipt:', error);
            console.error('Error stack:', error.stack);
            
            let errorMessage = 'Có lỗi xảy ra khi cập nhật.';
            if (error.message) {
                errorMessage = error.message;
            }
            
            if (typeof Swal !== 'undefined') {
                Swal.fire('Có lỗi xảy ra!', errorMessage, 'error');
            } else {
                alert('Có lỗi xảy ra: ' + errorMessage);
            }
        })
        .finally(() => {
            updateBtn.prop('disabled', false).html('<i class="mdi mdi-content-save mr-2"></i><span>Cập nhật phiếu mượn</span><i class="mdi mdi-arrow-right ml-2"></i>');
        });
    }

    function addChange(detailId, data) {
        changes[detailId] = { ...(changes[detailId] || {}), ...data };
        console.log(`Added change for detail ${detailId}:`, changes[detailId]);
        
        console.log('Total changes:', Object.keys(changes).length);
    }

    function removeChange(detailId) {
        delete changes[detailId];
        console.log(`Removed change for detail ${detailId}`);
        console.log('Remaining changes:', Object.keys(changes).length);
    }

    function showReturnDateInput(detailId, $row) {
        const $returnDateGroup = $row.find(`.return-date-group[data-detail-id="${detailId}"]`);
        $returnDateGroup.show();
        const $input = $returnDateGroup.find('.return-date-input');
        $input.val(getCurrentDate());
    }

    function hideAllInputs($row) {
        $row.find('.return-date-group').hide();
    }

    function clearSuccessMessage($row) {
        const $statusAction = $row.find('.col-status-action > div');
        $statusAction.find('.temp-success-message').remove();
    }

    function showSmallSuccessMessage($row, message) {
        const $statusAction = $row.find('.col-status-action > div');
        
        $statusAction.find('.temp-success-message').remove();
        
        const messageHtml = `
            <div class="temp-success-message mt-1">
                <div class="small text-success" style="font-size: 9px;">
                    <i class="mdi mdi-check-circle-outline mr-1"></i>${message}
                </div>
            </div>
        `;
        $statusAction.append(messageHtml);
        
        setTimeout(() => {
            $statusAction.find('.temp-success-message').fadeOut();
        }, 3000);
    }

    function updateExtendButtonState(detailId, extendDays) {
        const $extendBtn = $(`tr[data-detail-id="${detailId}"] .extend-btn`);
        $extendBtn.removeClass('btn-outline-info').addClass('btn-success');
        $extendBtn.html(`<i class="mdi mdi-check mr-1"></i><span class="font-weight-medium">+${extendDays} ngày</span>`);
        $extendBtn.prop('disabled', true);
    }

    function updateDropdownState(detailId, state) {
        const $dropdown = $(`.status-dropdown[data-detail-id="${detailId}"]`);
        $dropdown.removeClass('border-success border-warning border-danger border-primary');
        
        switch(state) {
            case 'success': $dropdown.addClass('border-success'); break;
            case 'warning': $dropdown.addClass('border-warning'); break;
            case 'danger': $dropdown.addClass('border-danger'); break;
            case 'primary': $dropdown.addClass('border-primary'); break;
        }
    }

    function showSuccessMessage(message) {
        const targetCardBody = $('#updateBorrowingForm').closest('.card-body');
        targetCardBody.find('.alert-info').remove();
        
        const alertHtml = `<div class="alert alert-info alert-dismissible fade show" role="alert">
            <i class="mdi mdi-information-outline mr-2"></i> ${message}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>`;
        
        targetCardBody.prepend(alertHtml);
        setTimeout(() => targetCardBody.find('.alert-info').fadeOut(), 3000);
    }

    function getCurrentDate() {
        return new Date().toISOString().split('T')[0];
    }

    function getCurrentTime() {
        return new Date().toTimeString().split(' ')[0];
    }

    function getCurrentDateTimeString() {
        const date = new Date();
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0'); 
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
    }

    const successMessage = sessionStorage.getItem('updateSuccess');
    if (successMessage) {
        const mainContainer = $('.content-wrapper').length > 0 ? $('.content-wrapper') : $('.main-panel');
        const targetContainer = mainContainer.first();
        
        targetContainer.find('.alert-success').remove();
        
        const alertHtml = `<div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="mdi mdi-check-circle-outline mr-2"></i> ${successMessage}
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>`;
        
        targetContainer.prepend(alertHtml);
        sessionStorage.removeItem('updateSuccess');
        
        setTimeout(() => targetContainer.find('.alert-success').fadeOut(), 5000);
    }

    console.log("=== INITIALIZATION COMPLETE ===");
});
