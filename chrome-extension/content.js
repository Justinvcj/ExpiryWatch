// content.js runs on every page

chrome.runtime.sendMessage(
    { action: 'check_domain', domain: window.location.hostname },
    (response) => {
        if (response && response.documents && response.documents.length > 0) {
            showExpiryAlert(response.documents);
        }
    }
);

function showExpiryAlert(documents) {
    const banner = document.createElement('div');
    banner.style.position = 'fixed';
    banner.style.top = '0';
    banner.style.left = '0';
    banner.style.width = '100%';
    banner.style.backgroundColor = '#fee2e2';
    banner.style.color = '#991b1b';
    banner.style.padding = '15px';
    banner.style.textAlign = 'center';
    banner.style.zIndex = '999999';
    banner.style.fontFamily = 'sans-serif';
    banner.style.fontWeight = 'bold';
    banner.style.borderBottom = '2px solid #ef4444';
    banner.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';

    let docList = documents.map(d => `${d.title} (Expires: ${d.extractedExpiryDate})`).join(', ');

    banner.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; max-width: 800px; margin: 0 auto;">
            <span>🚨 ExpiryWatch Alert: You are on a site related to your tracked documents: ${docList}. Make sure to renew them!</span>
            <button id="ew-close-btn" style="background: transparent; border: none; font-size: 20px; cursor: pointer; color: #991b1b;">&times;</button>
        </div>
    `;

    document.body.prepend(banner);

    document.getElementById('ew-close-btn').addEventListener('click', () => {
        banner.remove();
    });
}
