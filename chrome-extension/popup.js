document.addEventListener('DOMContentLoaded', () => {
    chrome.storage.sync.get(['email', 'backendUrl'], (result) => {
        if (result.email) document.getElementById('email').value = result.email;
        if (result.backendUrl) document.getElementById('backendUrl').value = result.backendUrl;
    });

    document.getElementById('saveBtn').addEventListener('click', () => {
        const email = document.getElementById('email').value;
        const backendUrl = document.getElementById('backendUrl').value;

        chrome.storage.sync.set({ email, backendUrl }, () => {
            const status = document.getElementById('status');
            status.style.display = 'block';
            setTimeout(() => { status.style.display = 'none'; }, 2000);
        });
    });
});
