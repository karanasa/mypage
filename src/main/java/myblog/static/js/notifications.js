function showNotification(message, isError = false) {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.classList.remove('error');
    if (isError) {
        notification.classList.add('error');
    }
    notification.classList.add('show');
    
    notification.onclick = () => {
        notification.classList.remove('show');
    };
}

function handleUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const message = urlParams.get('message');
    if (message) {
        showNotification(decodeURIComponent(message));
        history.replaceState({}, document.title, window.location.pathname);
    }
}

function handleFormSubmission(form) {
    const endpoint = form.getAttribute('action');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const formData = new FormData(this);
        const data = new URLSearchParams();
        for (const pair of formData) {
            data.append(pair[0], pair[1]);
        }

        fetch(endpoint, {
            method: 'POST',
            body: data,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        })
        .then(response => response.json())
        .then(response => {
            if (endpoint === '/login') {
                console.log('Response data:', response);
            } else if (endpoint === '/register') {
                console.log('Registration response:', response);
            }
            
            if (response.success) {
                if (response.redirect) {
                    if(response.message) {
                        window.location.href = response.redirect + "?message=" + encodeURIComponent(response.message);
                    } else {
                        window.location.href = response.redirect;
                    }
                }
            } else {
                showNotification(response.message, true);
            }
        })
        .catch(error => {
            console.error(`${endpoint.slice(1)} error:`, error);
            showNotification('An error occurred. Please try again.', true);
        });
    });
}



window.onload = () => {
    handleUrlParams();
    const form = document.querySelector('.login-form');
    if (form) {
        handleFormSubmission(form);
    }
}; 