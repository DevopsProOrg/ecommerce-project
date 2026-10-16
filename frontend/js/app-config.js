(() => {
    const { protocol, hostname, port, origin } = window.location;
    const isLocalPreview =
        protocol === "file:" ||
        ((hostname === "127.0.0.1" || hostname === "localhost") && port === "5500");

    window.APP_CONFIG = window.APP_CONFIG || {
        API_BASE_URL: isLocalPreview
            ? `http://${hostname === "127.0.0.1" ? "127.0.0.1" : "localhost"}:8888/api`
            : `${origin.replace(/\/$/, "")}/api`
    };

    const originalFetch = window.fetch.bind(window);
    const apiOrigin = new URL(window.APP_CONFIG.API_BASE_URL, window.location.href).origin;
    window.fetch = async (input, init = {}) => {
        const requestUrl = new URL(typeof input === "string" ? input : input.url, window.location.href);
        const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
        const token = sessionStorage.getItem("accessToken");
        const isAuthRequest = requestUrl.pathname.includes("/api/auth/");
        if (token && requestUrl.origin === apiOrigin && !isAuthRequest) {
            headers.set("Authorization", `Bearer ${token}`);
        }

        const response = await originalFetch(input, { ...init, headers });
        if (response.status === 401 && token && !isAuthRequest) {
            sessionStorage.removeItem("accessToken");
            localStorage.removeItem("user");
            if (!/(login|register)\.html$/.test(window.location.pathname)) {
                window.location.assign("login.html");
            }
        }
        return response;
    };
})();
