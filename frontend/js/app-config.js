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
})();
