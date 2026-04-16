const API_BASE = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "http://localhost:8888/api";
const adminUser = JSON.parse(localStorage.getItem("user") || "null");

const form = document.getElementById("product-form");
const statusElement = document.getElementById("status");
const productsContainer = document.getElementById("products");
const formMode = document.getElementById("form-mode");
const submitButton = document.getElementById("submit-btn");
const adminMeta = document.getElementById("admin-meta");

function setStatus(message, type = "") {
    statusElement.textContent = message;
    statusElement.className = `status ${type}`.trim();
}

function ensureAdmin() {
    if (!adminUser || adminUser.role !== "ADMIN") {
        window.location.href = "login.html";
        return false;
    }

    adminMeta.textContent = `${adminUser.name} (${adminUser.email})`;
    return true;
}

function resetForm() {
    form.reset();
    document.getElementById("product-id").value = "";
    document.getElementById("image").value = "assets/images/products/shirt1.jpg";
    formMode.textContent = "Create";
    submitButton.textContent = "Save product";
    setStatus("");
}

function getPayload() {
    return {
        adminUserId: adminUser.id,
        name: document.getElementById("name").value.trim(),
        category: document.getElementById("category").value,
        price: Number(document.getElementById("price").value),
        stock: Number(document.getElementById("stock").value),
        image: document.getElementById("image").value.trim(),
        description: document.getElementById("description").value.trim()
    };
}

function fillForm(product) {
    document.getElementById("product-id").value = product.id;
    document.getElementById("name").value = product.name || "";
    document.getElementById("category").value = product.category || "Shirts";
    document.getElementById("price").value = product.price ?? "";
    document.getElementById("stock").value = product.stock ?? 0;
    document.getElementById("image").value = product.image || "";
    document.getElementById("description").value = product.description || "";
    formMode.textContent = "Edit";
    submitButton.textContent = "Update product";
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderProducts(products) {
    if (!products.length) {
        productsContainer.innerHTML = '<div class="empty">No products found in the catalog yet.</div>';
        return;
    }

    productsContainer.innerHTML = products.map((product) => `
        <article class="product-row">
            <img src="${product.image || "assets/images/products/shirt1.jpg"}" alt="${product.name}">
            <div>
                <div style="display:flex; justify-content:space-between; gap:12px; flex-wrap:wrap; align-items:flex-start;">
                    <div>
                        <div class="pill">${product.category || "Catalog"}</div>
                        <h3 style="margin:10px 0 6px; font-family:'Playfair Display', serif;">${product.name}</h3>
                    </div>
                    <strong style="font-size:1.1rem;">Rs.${Number(product.price || 0).toFixed(0)}</strong>
                </div>
                <p class="muted" style="margin:0 0 12px; line-height:1.6;">${product.description || "No description added yet."}</p>
                <div class="muted">Stock: ${product.stock ?? 0}</div>
            </div>
            <div style="display:flex; flex-direction:column; gap:10px;">
                <button class="btn btn-secondary" type="button" onclick="editProduct(${product.id})">Edit</button>
                <button class="btn btn-primary" type="button" onclick="deleteProduct(${product.id})">Delete</button>
            </div>
        </article>
    `).join("");

    window.catalogProducts = products;
}

async function loadProducts() {
    const response = await fetch(`${API_BASE}/products`);
    const products = await response.json();
    renderProducts(products);
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

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("user");
    window.location.href = "login.html";
});

document.getElementById("refresh-btn").addEventListener("click", loadProducts);
document.getElementById("reset-btn").addEventListener("click", resetForm);
form.addEventListener("submit", saveProduct);

if (ensureAdmin()) {
    resetForm();
    loadProducts().catch(() => {
        setStatus("Unable to load products. Make sure the backend is running.", "error");
    });
}
