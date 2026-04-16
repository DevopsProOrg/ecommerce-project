let user = JSON.parse(localStorage.getItem("user"));

if (!user && !window.location.href.includes("login.html") && !window.location.href.includes("register.html")) {
    window.location.href = "login.html";
}

document.addEventListener("DOMContentLoaded", () => {

    user = JSON.parse(localStorage.getItem("user"));

    const userNameElement = document.getElementById("user-name");
    const loginBtn = document.getElementById("login-btn");
    const logoutBtn = document.getElementById("logout-btn");

    if (user) {
        userNameElement.innerText = "Welcome " + user.name;
        userNameElement.classList.remove("hidden");

        if (logoutBtn) logoutBtn.classList.remove("hidden");
        if (loginBtn) loginBtn.classList.add("hidden");

        loadCart();
    } else {
        if (userNameElement) userNameElement.classList.add("hidden");
        if (logoutBtn) logoutBtn.classList.add("hidden");
        if (loginBtn) loginBtn.classList.remove("hidden");
    }

    loadProducts(); // ✅ IMPORTANT (not renderProducts directly)
});