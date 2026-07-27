document.addEventListener('htmx:beforeRequest', function(event) {
    var token = getCookie('token');
    if (token) {
        event.detail.xhr.setRequestHeader('Authorization', 'Bearer ' + token);
    }
});

function setCookie(name, value, days) {
    days = days || 7;
    var expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = name + "=" + encodeURIComponent(value) +
        "; expires=" + expires.toUTCString() +
        "; path=/";
}

function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) === ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) === 0) return decodeURIComponent(c.substring(nameEQ.length, c.length));
    }
    return null;
}

function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}

function saveUserCookies(token) {
    if (token) {
        setCookie('token', token);
    }
}

function clearUserCookies() {
    deleteCookie('token');
}

function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    document.querySelector(`.tab-btn[data-tab="${tabId}"]`).classList.add('active');
    document.getElementById(`tab-${tabId}`).classList.add('active');
}
