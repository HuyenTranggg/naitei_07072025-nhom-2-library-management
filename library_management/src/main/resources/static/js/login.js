document.addEventListener('DOMContentLoaded', function () {
    const togglePasswordIcon = document.getElementById('togglePasswordIcon');
    const passwordInput = document.getElementById('password');

    if (togglePasswordIcon && passwordInput) {
        togglePasswordIcon.addEventListener('click', function () {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);

            this.classList.toggle('mdi-eye');
            this.classList.toggle('mdi-eye-off');
        });
    }
});
