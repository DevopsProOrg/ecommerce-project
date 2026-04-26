const AUTH_API = `${(window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "http://localhost:8888/api"}/auth`;

const form = document.getElementById("auth-form");
const statusElement = document.getElementById("status");
const submitButton = document.getElementById("submit-btn");
const emailHelp = document.getElementById("email-help");
const mobileHelp = document.getElementById("mobile-help");

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

function setFieldHelp(element, message, type = "") {
    if (!element) return;
    element.textContent = message;
    element.className = `field-help ${type}`.trim();
}

async function checkAvailability() {
    if (getMode() !== "register") {
        return;
    }

    const email = document.getElementById("email").value.trim();
    const mobile = document.getElementById("mobile").value.trim();
    if (!email && !mobile) {
        setFieldHelp(emailHelp, "");
        setFieldHelp(mobileHelp, "");
        return;
    }

    try {
        const params = new URLSearchParams();
        if (email) params.set("email", email);
        if (mobile) params.set("mobile", mobile);

        const response = await fetch(`${AUTH_API}/availability?${params.toString()}`);
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            return;
        }

        if (email) {
            setFieldHelp(emailHelp, data.emailMessage || "", data.emailExists ? "error" : "success");
        }
        if (mobile) {
            setFieldHelp(mobileHelp, data.mobileMessage || "", data.mobileExists ? "error" : "success");
        }
    } catch (error) {
        // Keep registration flow usable even if availability check is temporarily unavailable.
    }
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
        payload.mobile = document.getElementById("mobile").value.trim();
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

if (emailHelp) {
    document.getElementById("email").addEventListener("blur", checkAvailability);
}

if (mobileHelp) {
    document.getElementById("mobile").addEventListener("blur", checkAvailability);
}
