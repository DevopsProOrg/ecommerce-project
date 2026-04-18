const API_BASE = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "http://localhost:8888/api";
const adminUser = JSON.parse(localStorage.getItem("user") || "null");
const DEFAULT_IMAGE = "assets/images/products/og-marketplace-style-1.svg";

const form = document.getElementById("product-form");
const statusElement = document.getElementById("status");
const productsContainer = document.getElementById("products");
const formMode = document.getElementById("form-mode");
const submitButton = document.getElementById("submit-btn");
const adminMeta = document.getElementById("admin-meta");
const settingsForm = document.getElementById("settings-form");
const settingsStatusElement = document.getElementById("settings-status");
const settingsSubmitButton = document.getElementById("settings-submit-btn");
const supportRequestsContainer = document.getElementById("support-requests");

function setStatus(message, type = "") {
    statusElement.textContent = message;
    statusElement.className = `status ${type}`.trim();
}

function setSettingsStatus(message, type = "") {
    settingsStatusElement.textContent = message;
    settingsStatusElement.className = `status ${type}`.trim();
}

function ensureAdmin() {
    if (!adminUser || adminUser.role !== "ADMIN") {
        window.location.href = "login.html";
        return false;
    }

    adminMeta.textContent = `${adminUser.name} (${adminUser.email}) - managing the live OG Fashions marketplace catalog.`;
    return true;
}

function resetForm() {
    form.reset();
    document.getElementById("product-id").value = "";
    document.getElementById("brand").value = "OG Fashions";
    document.getElementById("image").value = DEFAULT_IMAGE;
    document.getElementById("delivery-info").value = "Ships across India in 3-5 business days.";
    document.getElementById("rating").value = "4.5";
    document.getElementById("review-count").value = "0";
    document.getElementById("featured").checked = false;
    formMode.textContent = "Create";
    submitButton.textContent = "Save product";
    setStatus("");
}

function toNumber(value) {
    return value === "" ? null : Number(value);
}

function getPayload() {
    return {
        adminUserId: adminUser.id,
        name: document.getElementById("name").value.trim(),
        category: document.getElementById("category").value,
        subcategory: document.getElementById("subcategory").value.trim(),
        brand: document.getElementById("brand").value.trim(),
        sku: document.getElementById("sku").value.trim(),
        description: document.getElementById("description").value.trim(),
        highlights: document.getElementById("highlights").value.trim(),
        badge: document.getElementById("badge").value.trim(),
        price: Number(document.getElementById("price").value),
        originalPrice: toNumber(document.getElementById("original-price").value),
        stock: Number(document.getElementById("stock").value),
        image: document.getElementById("image").value.trim(),
        color: document.getElementById("color").value.trim(),
        material: document.getElementById("material").value.trim(),
        sizeInfo: document.getElementById("size-info").value.trim(),
        deliveryInfo: document.getElementById("delivery-info").value.trim(),
        rating: toNumber(document.getElementById("rating").value),
        reviewCount: toNumber(document.getElementById("review-count").value),
        featured: document.getElementById("featured").checked
    };
}

function fillForm(product) {
    document.getElementById("product-id").value = product.id;
    document.getElementById("name").value = product.name || "";
    document.getElementById("category").value = product.category || "Fashion";
    document.getElementById("subcategory").value = product.subcategory || "";
    document.getElementById("brand").value = product.brand || "OG Fashions";
    document.getElementById("sku").value = product.sku || "";
    document.getElementById("description").value = product.description || "";
    document.getElementById("highlights").value = product.highlights || "";
    document.getElementById("badge").value = product.badge || "";
    document.getElementById("price").value = product.price ?? "";
    document.getElementById("original-price").value = product.originalPrice ?? "";
    document.getElementById("stock").value = product.stock ?? 0;
    document.getElementById("image").value = product.image || DEFAULT_IMAGE;
    document.getElementById("color").value = product.color || "";
    document.getElementById("material").value = product.material || "";
    document.getElementById("size-info").value = product.sizeInfo || "";
    document.getElementById("delivery-info").value = product.deliveryInfo || "";
    document.getElementById("rating").value = product.rating ?? "";
    document.getElementById("review-count").value = product.reviewCount ?? 0;
    document.getElementById("featured").checked = Boolean(product.featured);
    formMode.textContent = "Edit";
    submitButton.textContent = "Update product";
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function formatPrice(value) {
    return `Rs.${Number(value || 0).toFixed(0)}`;
}

function updateStats(products) {
    document.getElementById("stat-products").textContent = products.length;
    document.getElementById("stat-featured").textContent = products.filter((product) => product.featured).length;
    document.getElementById("stat-low-stock").textContent = products.filter((product) => Number(product.stock || 0) <= 10).length;
    document.getElementById("stat-categories").textContent = new Set(products.map((product) => product.category).filter(Boolean)).size;
}

function formatDateTime(value) {
    if (!value) return "Just now";
    return new Date(value).toLocaleString();
}

function updateSupportStats(requests) {
    document.getElementById("stat-support").textContent = requests.length;
}

function renderSupportRequests(requests) {
    updateSupportStats(requests);

    if (!requests.length) {
        supportRequestsContainer.innerHTML = '<div class="meta-box">No support requests submitted yet.</div>';
        return;
    }

    supportRequestsContainer.innerHTML = requests.map((request) => `
        <article class="support-item">
            <div class="support-head">
                <div>
                    <strong style="font-size:1rem;">${request.name}</strong>
                    <div class="compact">${request.email}${request.phone ? ` • ${request.phone}` : ""}</div>
                </div>
                <div style="text-align:right;">
                    <div class="pill">${request.status || "RECEIVED"}</div>
                    <div class="compact" style="margin-top:6px;">${formatDateTime(request.createdAt)}</div>
                </div>
            </div>
            <div class="compact" style="margin-bottom:10px;">Order ID: ${request.orderId || "-"}</div>
            <div class="subtle" style="margin-bottom:10px;">${request.issue || "No issue details provided."}</div>
            <div class="meta-box"><strong style="color:#fff;">Mail delivery</strong><br>${request.emailSent ? "Email sent successfully." : "Saved in admin inbox. SMTP not configured or delivery pending."}</div>
        </article>
    `).join("");
}

function renderProducts(products) {
    updateStats(products);

    if (!products.length) {
        productsContainer.innerHTML = '<div class="meta-box">No products found in the catalog yet.</div>';
        return;
    }

    const sorted = [...products].sort((a, b) => Number(Boolean(b.featured)) - Number(Boolean(a.featured)));

    productsContainer.innerHTML = sorted.map((product) => {
        const discount = product.originalPrice && product.originalPrice > product.price
            ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
            : 0;

        return `
            <article class="product-card">
                <img src="${product.image || DEFAULT_IMAGE}" alt="${product.name}">
                <div class="product-body">
                    <div class="row">
                        <div>
                            <div class="pill">${product.category || "Catalog"}</div>
                            <h3 style="margin:10px 0 6px; font-size:1.15rem;">${product.name}</h3>
                            <div class="compact">${product.brand || "OG Fashions"}${product.subcategory ? ` • ${product.subcategory}` : ""}</div>
                        </div>
                        <div style="text-align:right;">
                            <div style="font-size:1.1rem; font-weight:800;">${formatPrice(product.price)}</div>
                            ${discount ? `<div class="compact"><s>${formatPrice(product.originalPrice)}</s> • ${discount}% off</div>` : ""}
                        </div>
                    </div>
                    <p class="subtle" style="margin:0;">${product.description || "No description added yet."}</p>
                    <div class="meta-grid">
                        <div class="meta-box"><strong style="color:#fff;">SKU</strong><br>${product.sku || "-"}</div>
                        <div class="meta-box"><strong style="color:#fff;">Stock</strong><br>${product.stock ?? 0} units</div>
                        <div class="meta-box"><strong style="color:#fff;">Color</strong><br>${product.color || "-"}</div>
                        <div class="meta-box"><strong style="color:#fff;">Material</strong><br>${product.material || "-"}</div>
                    </div>
                    <div class="compact">${product.badge ? `${product.badge} • ` : ""}Rating ${Number(product.rating || 0).toFixed(1)} (${product.reviewCount || 0} reviews)</div>
                    <div class="product-actions">
                        <button class="btn btn-secondary" type="button" onclick="editProduct(${product.id})">Edit</button>
                        <button class="btn btn-primary" type="button" onclick="deleteProduct(${product.id})">Delete</button>
                    </div>
                </div>
            </article>
        `;
    }).join("");

    window.catalogProducts = sorted;
}

function fillSettingsForm(settings) {
    document.getElementById("settings-brand-name").value = settings.brandName || "";
    document.getElementById("settings-tagline").value = settings.tagline || "";
    document.getElementById("settings-announcement-bar").value = settings.announcementBar || "";
    document.getElementById("settings-hero-title").value = settings.heroTitle || "";
    document.getElementById("settings-hero-subtitle").value = settings.heroSubtitle || "";
    document.getElementById("settings-featured-message").value = settings.featuredMessage || "";
    document.getElementById("settings-address").value = settings.storeAddress || "";
    document.getElementById("settings-hours").value = settings.openingHours || "";
    document.getElementById("settings-phone").value = settings.contactPhone || "";
    document.getElementById("settings-email").value = settings.contactEmail || "";
    document.getElementById("settings-instagram").value = settings.instagramUrl || "";
}

async function loadProducts() {
    const response = await fetch(`${API_BASE}/products`);
    const products = await response.json();
    renderProducts(products);
}

async function loadStoreSettings() {
    const response = await fetch(`${API_BASE}/store-settings`);
    const settings = await response.json();
    fillSettingsForm(settings);
}

async function loadSupportRequests() {
    const response = await fetch(`${API_BASE}/contact-requests/admin/${adminUser.id}`);
    if (!response.ok) {
        throw new Error("Unable to load support requests");
    }
    const requests = await response.json();
    renderSupportRequests(requests);
}

function editProduct(id) {
    const product = (window.catalogProducts || []).find((item) => item.id === id);
    if (product) {
        fillForm(product);
    }
}

async function deleteProduct(id) {
    if (!confirm("Delete this product from the catalog?")) {
        return;
    }

    const response = await fetch(`${API_BASE}/products/${id}?adminUserId=${adminUser.id}`, {
        method: "DELETE"
    });

    if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        setStatus(data.message || "Unable to delete product", "error");
        return;
    }

    setStatus("Product deleted successfully.", "success");
    await loadProducts();
    resetForm();
}

async function saveProduct(event) {
    event.preventDefault();
    const productId = document.getElementById("product-id").value;
    const payload = getPayload();
    const isEdit = Boolean(productId);

    submitButton.disabled = true;
    setStatus(isEdit ? "Updating product..." : "Creating product...");

    try {
        const response = await fetch(
            isEdit ? `${API_BASE}/products/${productId}` : `${API_BASE}/products`,
            {
                method: isEdit ? "PUT" : "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            }
        );

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || "Unable to save product");
        }

        setStatus(isEdit ? "Product updated successfully." : "Product created successfully.", "success");
        await loadProducts();
        resetForm();
    } catch (error) {
        setStatus(error.message, "error");
    } finally {
        submitButton.disabled = false;
    }
}

async function saveStoreSettings(event) {
    event.preventDefault();

    settingsSubmitButton.disabled = true;
    setSettingsStatus("Saving storefront settings...");

    try {
        const response = await fetch(`${API_BASE}/store-settings`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                brandName: document.getElementById("settings-brand-name").value.trim(),
                tagline: document.getElementById("settings-tagline").value.trim(),
                announcementBar: document.getElementById("settings-announcement-bar").value.trim(),
                heroTitle: document.getElementById("settings-hero-title").value.trim(),
                heroSubtitle: document.getElementById("settings-hero-subtitle").value.trim(),
                featuredMessage: document.getElementById("settings-featured-message").value.trim(),
                storeAddress: document.getElementById("settings-address").value.trim(),
                openingHours: document.getElementById("settings-hours").value.trim(),
                contactPhone: document.getElementById("settings-phone").value.trim(),
                contactEmail: document.getElementById("settings-email").value.trim(),
                instagramUrl: document.getElementById("settings-instagram").value.trim()
            })
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || "Unable to save store settings");
        }

        fillSettingsForm(data);
        setSettingsStatus("Storefront settings updated successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    } finally {
        settingsSubmitButton.disabled = false;
    }
}

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("user");
    window.location.href = "login.html";
});

document.getElementById("refresh-btn").addEventListener("click", () => {
    loadProducts();
    loadSupportRequests();
});
document.getElementById("reset-btn").addEventListener("click", resetForm);
form.addEventListener("submit", saveProduct);
settingsForm.addEventListener("submit", saveStoreSettings);

if (ensureAdmin()) {
    resetForm();
    loadProducts().catch(() => {
        setStatus("Unable to load products. Make sure the backend is running.", "error");
    });
    loadStoreSettings().catch(() => {
        setSettingsStatus("Unable to load storefront settings.", "error");
    });
    loadSupportRequests().catch(() => {
        supportRequestsContainer.innerHTML = '<div class="meta-box">Unable to load support requests right now.</div>';
    });
}
