// resources/static/js/theme.js
// Light/dark switch for the Liquid Glass UI.
// Persists to localStorage under the SAME key ('theme') the <head> bootstrap script reads.
document.addEventListener("DOMContentLoaded", function () {
    const toggleBtn = document.getElementById('theme-toggle');
    const toggleText = document.getElementById('theme-toggle-text');
    const sunIcon = document.querySelector('.theme-icon-sun');
    const moonIcon = document.querySelector('.theme-icon-moon');

    // The button names the ACTION: in light mode it offers "Modo Oscuro" (moon),
    // in dark mode it offers "Modo Claro" (sun). Each element is null-guarded on its own.
    function updateToggleButton(isDark) {
        if (!toggleBtn) return;
        if (toggleText) toggleText.textContent = isDark ? "Modo Claro" : "Modo Oscuro";
        if (sunIcon) sunIcon.classList.toggle('hidden', !isDark);
        if (moonIcon) moonIcon.classList.toggle('hidden', isDark);
    }

    function applyTheme(isDark) {
        document.documentElement.classList.toggle('dark', isDark);
        updateToggleButton(isDark);
    }

    // 1. Initial sync
    applyTheme(localStorage.getItem('theme') === 'dark');

    // 2. Click handler
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const willBeDark = !document.documentElement.classList.contains('dark');
            localStorage.setItem('theme', willBeDark ? 'dark' : 'light');
            applyTheme(willBeDark);
        });
    }
});
