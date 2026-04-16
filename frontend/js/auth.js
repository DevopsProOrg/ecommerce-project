const AUTH_API = `${(window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "http://localhost:8888/api"}/auth`;

const form = document.getElementById("auth-form");
const statusElement = document.getElementById("status");
const submitButton = document.getElementById("submit-btn");

function setStatus(message, type = "") {
    if (!statusElement) {
        return;
    }

    statusElement.textContent = message;
    statusElement.className = `status ${type}`.trim();
}

function getMode() {
    return window.location.pathname.endsWith("register.html") ? "register" : "login";
}

async function handleSubmit(event) {
    event.preventDefault();

    const mode = getMode();
    const payload = {
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value
    };

    if (mode === "register") {
        payload.name = document.getElementById("name").value.trim();
    }

    submitButton.disabled = true;
    setStatus(mode === "register" ? "Creating account..." : "Signing you in...");

    try {
        const response = await fetch(`${AUTH_API}/${mode}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || (mode === "register" ? "Registration failed" : "Login failed"));
        }

        if (mode === "register") {
            setStatus("Account created. Redirecting to login...", "success");
            setTimeout(() => {
                window.location.href = "login.html";
            }, 1200);
            return;
        }

        localStorage.setItem("user", JSON.stringify(data));
        setStatus("Login successful. Redirecting...", "success");

        setTimeout(() => {
            window.location.href = data.role === "ADMIN" ? "admin.html" : "index.html";
        }, 800);
    } catch (error) {
        setStatus(error.message, "error");
    } finally {
        submitButton.disabled = false;
    }
}

if (form) {
    form.addEventListener("submit", handleSubmit);
}
