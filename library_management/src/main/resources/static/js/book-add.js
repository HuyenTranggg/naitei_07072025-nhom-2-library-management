document.addEventListener('DOMContentLoaded', function () {
    $('.select2-multiple').each(function() {
        $(this).select2({
            theme: 'bootstrap',
            placeholder: $(this).data('placeholder'), 
            allowClear: true,
            tags: true, 
            createTag: function (params) {
                var term = $.trim(params.term);
                if (term === '') {
                    return null; 
                }
                return {
                    id: term,       
                    text: term,     
                    newTag: true    
                };
            }
        });
    });
    
    const titleInput = document.getElementById('title');
    const titleCounter = document.getElementById('title-char-counter');
    const maxLength = titleInput.getAttribute('maxlength');

    titleInput.addEventListener('input', function() {
        const currentLength = titleInput.value.length;
        titleCounter.textContent = `${currentLength}/${maxLength}`;
    });

    const addBookForm = document.getElementById('addBookForm');

    addBookForm.addEventListener('submit', function (event) {
        let formIsValid = true;

        // Xóa các thông báo lỗi cũ trước khi kiểm tra lại
        addBookForm.querySelectorAll('.form-group').forEach(group => {
            group.classList.remove('is-invalid');
        });

        // Validate Title
        if (titleInput.value.trim() === '') {
            titleInput.closest('.form-group').classList.add('is-invalid');
            formIsValid = false;
        }

        // Validate Authors
        const authorsSelect = $('#authors');
        if (authorsSelect.val().length === 0) {
            authorsSelect.closest('.form-group').classList.add('is-invalid');
            formIsValid = false;
        }

        // Validate Genres
        const genresSelect = $('#genres');
        if (genresSelect.val().length === 0) {
            genresSelect.closest('.form-group').classList.add('is-invalid');
            formIsValid = false;
        }

        if (!formIsValid) {
            event.preventDefault(); 
            const firstInvalidField = addBookForm.querySelector('.is-invalid');
            if (firstInvalidField) {
                firstInvalidField.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }
    });
});
