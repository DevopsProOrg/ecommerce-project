let productsDB = [];

async function loadProducts() {
    const apiBaseUrl = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || `${window.location.origin}/api`;
    const container = document.getElementById("product-grid");

    try {
        const response = await fetch(`${apiBaseUrl}/products`);
        if (!response.ok) {
            throw new Error(`Product request failed (${response.status})`);
        }

        const products = await response.json();
        if (!Array.isArray(products)) {
            throw new Error("Product response was not a list");
        }

        productsDB = products;
        renderProducts();
    } catch (error) {
        productsDB = [];
        if (container) {
            container.innerHTML = '<p class="muted" role="status">Products are temporarily unavailable. Please try again shortly.</p>';
        }
        console.error("Unable to load products:", error);
    }
}
