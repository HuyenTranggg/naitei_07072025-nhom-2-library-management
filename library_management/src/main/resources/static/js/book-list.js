document.addEventListener('DOMContentLoaded', function () {
    console.log("Script book-list.js đã được tải và đang chạy."); // Log 1
    const toggleButtons = document.querySelectorAll('.js-toggle-editions');
    console.log("Đã tìm thấy các nút:", toggleButtons); // Log 2: Xem nó có tìm thấy nút nào không

    toggleButtons.forEach(button => {
        button.addEventListener('click', function (event) {
            event.preventDefault();

            const bookId = this.dataset.bookId;
            const bookRow = document.getElementById(`book-row-${bookId}`);
            const existingEditionsRow = document.getElementById(`editions-for-book-${bookId}`);

            // Nếu đã có hàng phiên bản -> xóa đi và kết thúc
            if (existingEditionsRow) {
                existingEditionsRow.remove();
                return;
            }

            // Gọi API để lấy dữ liệu
            fetch(`/admin/books/${bookId}/editions`)
                .then(response => response.json())
                .then(editions => {
                    if (editions.length > 0) {
                        const editionsHtml = createEditionsTable(editions);
                        const newRow = document.createElement('tr');
                        newRow.id = `editions-for-book-${bookId}`;
                        newRow.innerHTML = `<td colspan="6" class="p-0">${editionsHtml}</td>`;
                        bookRow.insertAdjacentElement('afterend', newRow);
                    }
                })
                .catch(error => console.error('Error fetching editions:', error));
        });
    });

    function createEditionsTable(editions) {
        let tableRows = '';
        editions.forEach(edition => {
            tableRows += `
                <tr>
                    <td>${edition.title}</td>
                    <td>${edition.isbn}</td>
                    <td>${edition.publisherName}</td>
                    <td>${edition.publicationYear || 'N/A'}</td>
                    <td class="text-center">${edition.availableQuantity}</td>
                    <td class="text-center">
                        <a href="#" class="text-info" title="Xem chi tiết"><i class="mdi mdi-eye"></i></a>
                        <a href="#" class="text-primary ml-2" title="Chỉnh sửa"><i class="mdi mdi-pencil"></i></a>
                        <a href="#" class="text-danger ml-2" title="Xóa"><i class="mdi mdi-delete"></i></a>
                    </td>
                </tr>
            `;
        });

        return `
            <div class="editions-sub-table">
                <table class="table table-sm mb-0">
                    <thead class="thead-light">
                        <tr>
                            <th style="width: 25%;">Tiêu đề phiên bản</th>
                            <th style="width: 20%;">ISBN</th>
                            <th style="width: 20%;">Nhà xuất bản</th>
                            <th style="width: 10%;">Năm XB</th>
                            <th style="width: 10%;" class="text-center">SL khả dụng</th>
                            <th style="width: 15%;" class="text-center">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tableRows}
                    </tbody>
                </table>
            </div>
        `;
    }
});
