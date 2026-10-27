// Delete-confirmation modal (journal, revision, inner work, tags).
// Contract with the templates is unchanged: #confirmModal toggles the `hidden`
// class and #journalId receives the entry id.
function openModal(id) {
    const field = document.getElementById('journalId');
    const modal = document.getElementById('confirmModal');
    if (!field || !modal) return;
    field.value = id;
    modal.classList.remove('hidden');
}

function closeModal() {
    const modal = document.getElementById('confirmModal');
    if (modal) modal.classList.add('hidden');
}

// Liquid Glass extras: click on the blurred backdrop or press Esc to dismiss.
document.addEventListener('click', function (e) {
    const modal = document.getElementById('confirmModal');
    if (modal && e.target === modal) closeModal();
});
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeModal();
});
