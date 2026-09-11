const DEFAULT_BACKEND_URL = 'http://localhost:8080';

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'check_domain') {
        
        chrome.storage.sync.get(['backendUrl', 'email'], (result) => {
            const backendUrl = result.backendUrl || DEFAULT_BACKEND_URL;
            const email = result.email || '';
            
            if (!email) {
                console.log("No email configured for ExpiryWatch extension.");
                sendResponse({ documents: [] });
                return;
            }

            fetch(`${backendUrl}/api/documents/check?domain=${encodeURIComponent(request.domain)}&email=${encodeURIComponent(email)}`, {
                method: 'GET'
            })
            .then(res => {
                if (res.ok) return res.json();
                throw new Error('API error');
            })
            .then(data => {
                sendResponse({ documents: data });
            })
            .catch(err => {
                console.error("ExpiryWatch API Error:", err);
                sendResponse({ documents: [] });
            });
        });
        
        return true; 
    }
});
