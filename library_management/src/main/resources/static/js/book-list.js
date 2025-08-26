document.addEventListener('DOMContentLoaded', function () {
    
    const toggleButtons = document.querySelectorAll('.js-toggle-editions');
    const editionsTemplate = document.getElementById('editions-table-template');

    // Get translated strings from template
    const i18n = buildI18nForEditions(editionsTemplate);

    toggleButtons.forEach(button => {
        button.addEventListener('click', function (event) {
            event.preventDefault();

            const bookId = this.dataset.bookId;
            const bookRow = document.getElementById(`book-row-${bookId}`);
            const existingEditionsRow = document.getElementById(`editions-for-book-${bookId}`);

            // if already exists -> remove it and return
            if (existingEditionsRow) {
                existingEditionsRow.remove();
                return;
            }

            fetch(`/admin/books/${bookId}/editions`)
                .then(response => response.json())
                .then(editions => {
                    if (editions.length > 0) {
                        const newRow = document.createElement('tr');
                        newRow.id = `editions-for-book-${bookId}`;
                        const newCell = document.createElement('td');
                        newCell.colSpan = 6; // Ensure correct column count
                        newCell.classList.add('p-0');

                        // Create sub-table from template
                        newCell.appendChild(createEditionsTable(editions, i18n));
                        newRow.appendChild(newCell);
                        
                        bookRow.insertAdjacentElement('afterend', newRow);
                    }
                })
                .catch(error => console.error('Error fetching editions:', error));
        });
    });

    // Handle edition detail view
    const mainTable = document.querySelector('.main-book-table');
    const editionModal = new bootstrap.Modal(document.getElementById('editionDetailModal'));
    const errorMessage = mainTable.dataset.errorMessage || 'An error occurred.';
    
    // Use event delegation to handle dynamic button clicks
    mainTable.addEventListener('click', function(event) {
        const viewButton = event.target.closest('.js-view-edition');
        if (!viewButton) {
            return;
        }

        event.preventDefault();
        const editionId = viewButton.dataset.editionId;

        if (!editionId) {
            console.error('Could not get a valid data-edition-id from the clicked button.');
            return;
        }

        // Call API
        fetch(`/admin/editions/${editionId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                populateModal(data);
                editionModal.show();
            })
            .catch(error => {
                console.error('Error fetching edition details:', error);
                // Show toast notification
                showToast(errorMessage, 'error');
            });
    });

    function populateModal(data) {
        const i18nQtyInitial = document.getElementById('modal-edition-initial-qty').textContent.split(':')[0];
        const i18nQtyAvailable = document.getElementById('modal-edition-available-qty').textContent.split(':')[0];

        document.getElementById('modal-edition-title').textContent = data.title || 'N/A';
        document.getElementById('modal-book-title').textContent = data.bookTitle || 'N/A';
        document.getElementById('modal-edition-authors').textContent = data.authors ? data.authors.join(', ') : 'N/A';
        document.getElementById('modal-edition-isbn').textContent = data.isbn || 'N/A';
        document.getElementById('modal-edition-publisher').textContent = `${data.publisherName || 'N/A'} (${data.publicationDate ? new Date(data.publicationDate).getFullYear() : 'N/A'})`;
        document.getElementById('modal-edition-language').textContent = data.language || 'N/A';
        document.getElementById('modal-edition-pages').textContent = data.pageNumber || 'N/A';
        document.getElementById('modal-edition-format').textContent = data.format || 'N/A';
        document.getElementById('modal-edition-description').textContent = data.description || 'N/A';
        
        document.getElementById('modal-edition-initial-qty').textContent = `${i18nQtyInitial}: ${data.initialQuantity}`;
        document.getElementById('modal-edition-available-qty').textContent = `${i18nQtyAvailable}: ${data.availableQuantity}`;
        
        const coverImage = document.getElementById('modal-edition-cover');
        const defaultCover = '/images/editions/default-cover.png';
        const imageUrl = data.coverImageUrl || '/images/default-cover.png';
        coverImage.src = `${imageUrl}?v=${new Date().getTime()}`; // Cache-busting query parameter
        coverImage.onerror = function () {
            this.onerror = null;
            this.src = defaultCover;
        };
    }


    function createEditionsTable(editions, i18n) {
        // Clone template's content
        const templateContent = editionsTemplate.content.cloneNode(true);
        const tableContainer = templateContent.querySelector('.editions-sub-table');
        const tableHeader = tableContainer.querySelector('thead tr');
        const tableBody = tableContainer.querySelector('tbody');

        // Fill in the translated column headers
        tableHeader.querySelector('.col-sub-title').textContent = i18n.header.title;
        tableHeader.querySelector('.col-sub-isbn').textContent = i18n.header.isbn;
        tableHeader.querySelector('.col-sub-publisher').textContent = i18n.header.publisher;
        tableHeader.querySelector('.col-sub-year').textContent = i18n.header.year;
        tableHeader.querySelector('.col-sub-quantity').textContent = i18n.header.quantity;
        tableHeader.querySelector('.col-sub-actions').textContent = i18n.header.actions;

        // Create data rows
        editions.forEach(edition => {
            const row = document.createElement('tr');

            let deleteButtonHtml = '';
            const status = edition.deletionStatus; // Get status from DTO

            console.log(status);

            if (status === 'CANNOT_DELETE') {
                deleteButtonHtml = `
                    <span title="${i18n.tooltip.disabled}">
                        <a href="#" class="text-danger ml-2 disabled-delete">
                            <i class="mdi mdi-delete"></i>
                        </a>
                    </span>`;
            } else {
                deleteButtonHtml = `
                    <a href="#" class="text-danger ml-2 js-delete-edition" 
                       title="${i18n.tooltip.delete}" 
                       data-edition-id="${edition.id}" 
                       data-edition-isbn="${edition.isbn}">
                        <i class="mdi mdi-delete"></i>
                    </a>`;
            }

            console.log(deleteButtonHtml);

            const editUrl = `/admin/editions/${edition.id}/edit`;
            row.innerHTML = `
                <td class="col-sub-title">${edition.title}</td>
                <td class="col-sub-isbn">${edition.isbn}</td>
                <td class="col-sub-publisher">${edition.publisherName}</td>
                <td class="col-sub-year text-center">${edition.publicationYear || 'N/A'}</td>
                <td class="col-sub-quantity text-center">${edition.availableQuantity}</td>
                <td class="col-sub-actions">
                    <!-- BỌC CÁC ICON VÀO MỘT DIV CONTAINER -->
                    <div class="action-icons">
                        <a href="#" class="text-info js-view-edition" title="${i18n.tooltip.view}" data-edition-id="${edition.id}"><i class="mdi mdi-eye"></i></a>
                        <a href="${editUrl}" class="text-primary" title="${i18n.tooltip.edit}"><i class="mdi mdi-pencil"></i></a>
                        ${deleteButtonHtml}
                    </div>
                </td>
            `;
            tableBody.appendChild(row);
        });

        return tableContainer;
    }

    const deleteEditionModal = new bootstrap.Modal(document.getElementById('deleteEditionModal'));
    const deleteConfirmMessage = document.getElementById('delete-confirm-message');
    const deleteEditionForm = document.getElementById('deleteEditionForm');

    // event delegation
    document.body.addEventListener('click', function(event) {
        const deleteButton = event.target.closest('.js-delete-edition');
        if (deleteButton && !deleteButton.disabled) {
            event.preventDefault();
            const editionIsbn = deleteButton.dataset.editionIsbn;
            const editionId = deleteButton.dataset.editionId;
            
            // get message from template
            const message = `${i18n.message.confirmDelete} ${editionIsbn}?`;
            deleteConfirmMessage.textContent = message;

            // update action of form
            // deleteEditionForm.action = `/admin/editions/${editionId}/delete`;
            deleteEditionForm.action = `/admin/editions/${editionId}`; 

            deleteEditionModal.show();
        }
    });
    // --- KHỞI TẠO MODAL ---
    const deleteBookModalElement = document.getElementById('deleteBookModal');
    if (deleteBookModalElement) {
        const deleteBookModal = new bootstrap.Modal(deleteBookModalElement);
        const deleteBookForm = document.getElementById('deleteBookForm');
        const deleteBookMessage = document.getElementById('deleteBookConfirmMessage');

        // --- SỬ DỤNG EVENT DELEGATION ---
        document.body.addEventListener('click', function(event) {
            const deleteButton = event.target.closest('.js-delete-book');
            if (deleteButton) {
                // Đọc dữ liệu trực tiếp từ các thuộc tính data-* của nút
                const bookId = deleteButton.dataset.bookId;
                const confirmMessage = deleteButton.dataset.confirmMessage;

                // Cập nhật form trong Modal
                if (deleteBookForm && deleteBookMessage) {
                    deleteBookForm.action = `/admin/books/${bookId}`;
                    deleteBookMessage.textContent = confirmMessage;
                    deleteBookModal.show();
                }
            }
        });
    }
});
