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

let currentProducts = [];
let currentTickets = [];
let currentStoreSettings = { contactEmail: "contact.ogfashions@gmail.com" };
let allTickets = [];

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

    adminMeta.textContent = `${adminUser.name} (${adminUser.email}) is managing OG Fashions support, store messaging, and live catalog operations.`;
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
    setStatus("");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

async function parseApiResponse(response) {
    const text = await response.text();
    if (!text) return {};
    try {
        return JSON.parse(text);
    } catch (error) {
        return { message: text };
    }
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
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function formatPrice(value) {
    return `Rs.${Number(value || 0).toFixed(0)}`;
}

function formatDateTime(value) {
    if (!value) return "Just now";
    return new Date(value).toLocaleString();
}

function statusClass(status) {
    return `status-${String(status || "NEW").toLowerCase()}`;
}

function priorityClass(priority) {
    return `priority-${String(priority || "UNSET").toLowerCase()}`;
}

function mailClass(sent) {
    return sent ? "mail-ok" : "mail-fail";
}

function getTicketPriority(ticket) {
    const value = String(ticket.priority || "").trim().toUpperCase();
    return value || "UNSET";
}

function updateProductStats(products) {
    document.getElementById("stat-products").textContent = products.length;
    document.getElementById("stat-featured").textContent = products.filter((product) => product.featured).length;
    document.getElementById("stat-low-stock").textContent = products.filter((product) => Number(product.stock || 0) <= 10).length;
    document.getElementById("stat-categories").textContent = new Set(products.map((product) => product.category).filter(Boolean)).size;
}

function countByStatus(statuses) {
    return currentTickets.filter((ticket) => statuses.includes(String(ticket.status || "").toUpperCase())).length;
}

function getTicketFilters() {
    return {
        status: document.getElementById("ticket-status-filter").value,
        priority: document.getElementById("ticket-priority-filter").value,
        search: document.getElementById("ticket-search").value.trim().toLowerCase(),
        archived: document.getElementById("ticket-archive-filter").checked
    };
}

function matchesTicketFilters(ticket, filters) {
    const normalizedStatus = String(ticket.status || "NEW").toUpperCase();
    const normalizedPriority = getTicketPriority(ticket);
    const haystack = [
        ticket.ticketNumber,
        ticket.orderId,
        ticket.name,
        ticket.email,
        ticket.subject,
        ticket.issue,
        ticket.assigneeName
    ].filter(Boolean).join(" ").toLowerCase();

    if (!filters.archived && ticket.archived) {
        return false;
    }
    if (filters.status !== "ALL" && normalizedStatus !== filters.status) {
        return false;
    }
    if (filters.priority !== "ALL" && normalizedPriority !== filters.priority) {
        return false;
    }
    if (filters.search && !haystack.includes(filters.search)) {
        return false;
    }
    return true;
}

function applyTicketFilters(tickets = allTickets) {
    const filtered = tickets.filter((ticket) => matchesTicketFilters(ticket, getTicketFilters()));
    currentTickets = filtered;
    renderSupportRequests(filtered);
}

function updateTicketStats(requests) {
    document.getElementById("stat-support").textContent = requests.length;
    document.getElementById("stat-open-work").textContent = countByStatus(["NEW", "OPEN", "PENDING", "HOLD"]);
    document.getElementById("stat-resolved").textContent = countByStatus(["RESOLVED", "CLOSED"]);
    document.getElementById("stat-urgent").textContent = requests.filter((ticket) => String(ticket.priority || "").toUpperCase() === "URGENT").length;

    document.getElementById("stat-ticket-new").textContent = countByStatus(["NEW"]);
    document.getElementById("stat-ticket-open").textContent = countByStatus(["OPEN"]);
    document.getElementById("stat-ticket-pending").textContent = countByStatus(["PENDING", "HOLD"]);
    document.getElementById("stat-ticket-closed").textContent = countByStatus(["RESOLVED", "CLOSED"]);
    document.getElementById("stat-ticket-archived").textContent = currentTickets.filter((ticket) => ticket.archived).length;
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
    document.getElementById("settings-email").value = settings.contactEmail || "";
    document.getElementById("settings-instagram").value = settings.instagramUrl || "";
}

function renderSupportRequests(requests) {
    updateTicketStats(requests);

    if (!requests.length) {
        supportRequestsContainer.innerHTML = '<div class="empty-state">No tickets match the current filters. Try another status, priority, archived view, or search term.</div>';
        return;
    }

    const storeEmail = currentStoreSettings.contactEmail || "contact.ogfashions@gmail.com";

    supportRequestsContainer.innerHTML = requests.map((request) => `
        <article class="ticket-card">
            <div class="ticket-topline">
                <div>
                    <div class="ticket-id">${escapeHtml(request.ticketNumber || `#${request.id}`)}</div>
                    <div class="ticket-title">${escapeHtml(request.subject || "Support request")}</div>
                    <div class="ticket-sub">${escapeHtml(request.name || "Customer")} • ${escapeHtml(request.email || "No email")} • Created ${formatDateTime(request.createdAt)}</div>
                </div>
                <div class="pill-row">
                    <span class="status-pill ${statusClass(request.status)}">${escapeHtml(request.status || "NEW")}</span>
                    <span class="priority-pill ${priorityClass(getTicketPriority(request))}">${escapeHtml(getTicketPriority(request))}</span>
                    ${request.archived ? '<span class="mini-pill">Archived</span>' : ""}
                </div>
            </div>

            <div class="ticket-panels">
                <div class="panel-stack">
                    <div class="info-box">
                        <div class="label-line">Customer issue</div>
                        <div class="value-line">${escapeHtml(request.issue || "No issue details provided.")}</div>
                    </div>
                    <div class="info-box">
                        <div class="label-line">Latest admin response</div>
                        <div class="value-line">${escapeHtml(request.adminReply || "No response sent yet.")}</div>
                    </div>
                    <div class="info-box">
                        <div class="label-line">Internal support note</div>
                        <div class="value-line">${escapeHtml(request.internalNote || "No internal note added yet.")}</div>
                    </div>
                    <div class="mail-grid">
                        <div class="mail-box">
                            <div class="label-line">Customer mailbox</div>
                            <div class="value-line">${escapeHtml(request.email || "-")}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(request.customerEmailSent)}">${request.customerEmailSent ? "Customer email sent" : "Customer email pending"}</span>
                            </div>
                        </div>
                        <div class="mail-box">
                            <div class="label-line">Store mailbox</div>
                            <div class="value-line">${escapeHtml(storeEmail)}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(request.emailSent)}">${request.emailSent ? "Store email sent" : "Store email pending"}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="panel-stack">
                    <div class="info-box">
                        <div class="label-line">Incident details</div>
                        <div class="value-line">
                            <strong>Order ID:</strong> ${escapeHtml(request.orderId || "Not linked")}<br>
                            <strong>Channel:</strong> ${escapeHtml(request.channel || "WEBSITE")}<br>
                            <strong>Assignee:</strong> ${escapeHtml(request.assigneeName || "Support Desk")}<br>
                            <strong>Updated:</strong> ${formatDateTime(request.updatedAt || request.createdAt)}<br>
                            <strong>Last replied:</strong> ${formatDateTime(request.lastRespondedAt)}
                        </div>
                    </div>
                    <div class="ticket-actions">
                        <textarea id="reply-${request.id}" placeholder="Write the customer-facing update or resolution...">${escapeHtml(request.adminReply || "")}</textarea>
                        <textarea id="note-${request.id}" placeholder="Internal note for support team only...">${escapeHtml(request.internalNote || "")}</textarea>
                        <div class="ticket-form-grid">
                            <label>Status
                                <select id="status-${request.id}">
                                    ${["NEW", "OPEN", "PENDING", "HOLD", "RESOLVED", "CLOSED"].map((value) => `
                                        <option value="${value}" ${request.status === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Priority
                                <select id="priority-${request.id}">
                                    ${["LOW", "MEDIUM", "HIGH", "URGENT"].map((value) => `
                                        <option value="${value}" ${getTicketPriority(request) === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Assignee
                                <input id="assignee-${request.id}" value="${escapeHtml(request.assigneeName || "Support Desk")}" placeholder="Support Desk">
                            </label>
                        </div>
                        <div class="ticket-action-row">
                            <button class="btn btn-primary" type="button" onclick="respondToTicket(${request.id})">Send update</button>
                            <button class="btn btn-secondary" type="button" onclick="archiveTicket(${request.id}, ${request.archived ? "false" : "true"})">${request.archived ? "Restore to active queue" : "Archive ticket"}</button>
                        </div>
                    </div>
                </div>
            </div>
        </article>
    `).join("");
}

function renderProducts(products) {
    updateProductStats(products);

    if (!products.length) {
        productsContainer.innerHTML = '<div class="empty-state">No products are available in the catalog yet.</div>';
        return;
    }

    const sorted = [...products].sort((a, b) => Number(Boolean(b.featured)) - Number(Boolean(a.featured)));

    productsContainer.innerHTML = sorted.map((product) => {
        const discount = product.originalPrice && product.originalPrice > product.price
            ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
            : 0;

        return `
            <article class="product-card">
                <img src="${product.image || DEFAULT_IMAGE}" alt="${escapeHtml(product.name)}">
                <div class="product-body">
                    <div class="row">
                        <div>
                            <span class="mini-pill">${escapeHtml(product.category || "Catalog")}</span>
                            <h3 style="margin:10px 0 6px; font-size:1.15rem;">${escapeHtml(product.name)}</h3>
                            <div class="compact">${escapeHtml(product.brand || "OG Fashions")}${product.subcategory ? ` • ${escapeHtml(product.subcategory)}` : ""}</div>
                        </div>
                        <div style="text-align:right;">
                            <div style="font-size:1.1rem; font-weight:800;">${formatPrice(product.price)}</div>
                            ${discount ? `<div class="compact"><s>${formatPrice(product.originalPrice)}</s> • ${discount}% off</div>` : ""}
                        </div>
                    </div>
                    <div class="meta-box">
                        <strong>Description</strong>
                        ${escapeHtml(product.description || "No description added yet.")}
                    </div>
                    <div class="mail-grid">
                        <div class="meta-box"><strong>SKU</strong>${escapeHtml(product.sku || "-")}</div>
                        <div class="meta-box"><strong>Stock</strong>${product.stock ?? 0} units</div>
                        <div class="meta-box"><strong>Color</strong>${escapeHtml(product.color || "-")}</div>
                        <div class="meta-box"><strong>Material</strong>${escapeHtml(product.material || "-")}</div>
                    </div>
                    <div class="compact">${product.badge ? `${escapeHtml(product.badge)} • ` : ""}Rating ${Number(product.rating || 0).toFixed(1)} (${product.reviewCount || 0} reviews)</div>
                    <div class="product-actions">
                        <button class="btn btn-secondary" type="button" onclick="editProduct(${product.id})">Edit</button>
                        <button class="btn btn-danger" type="button" onclick="deleteProduct(${product.id})">Delete</button>
                    </div>
                </div>
            </article>
        `;
    }).join("");

    window.catalogProducts = sorted;
}

async function loadProducts() {
    const response = await fetch(`${API_BASE}/products`);
    const products = await response.json();
    currentProducts = products;
    renderProducts(products);
}

async function loadStoreSettings() {
    const response = await fetch(`${API_BASE}/store-settings`);
    const settings = await response.json();
    currentStoreSettings = settings;
    fillSettingsForm(settings);
    if (currentTickets.length) {
        renderSupportRequests(currentTickets);
    }
}

async function loadSupportRequests() {
    let response = await fetch(`${API_BASE}/contact-requests?adminUserId=${adminUser.id}&archived=true`);
    if (!response.ok) {
        response = await fetch(`${API_BASE}/contact-requests?adminUserId=${adminUser.id}`);
    }
    if (!response.ok) {
        const data = await parseApiResponse(response);
        throw new Error(data.message || "Unable to load support requests");
    }
    const requests = await parseApiResponse(response);
    allTickets = requests;
    applyTicketFilters(requests);
}

async function respondToTicket(id) {
    const responseMessage = document.getElementById(`reply-${id}`).value.trim();
    const status = document.getElementById(`status-${id}`).value;
    const priority = document.getElementById(`priority-${id}`).value;
    const assigneeName = document.getElementById(`assignee-${id}`).value.trim();
    const internalNote = document.getElementById(`note-${id}`).value.trim();

    if (!responseMessage) {
        alert("Add a customer-facing reply before sending the update.");
        return;
    }

    const payload = {
        adminUserId: adminUser.id,
        status,
        priority,
        assigneeName,
        internalNote,
        responseMessage
    };

    try {
        let response = await fetch(`${API_BASE}/contact-requests/${id}/respond`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (response.status === 404) {
            response = await fetch(`${API_BASE}/contact-requests/respond/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
        }

        const data = await parseApiResponse(response);
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Ticket update route is not available on the running backend. Restart the backend server and try again.");
            }
            throw new Error(data.message || "Unable to send the ticket update");
        }

        await loadSupportRequests();
        const message = data.customerEmailSent === false
            ? `Ticket ${data.ticketNumber || id} updated, but customer email was not sent.`
            : `Ticket ${data.ticketNumber || id} updated successfully.`;
        setSettingsStatus(message, data.customerEmailSent === false ? "error" : "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function archiveTicket(id, archived) {
    const message = archived
        ? "Archive this ticket and hide it from the active queue?"
        : "Restore this ticket back into the active queue?";

    if (!confirm(message)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/contact-requests/${id}/archive`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                archived
            })
        });

        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update the ticket archive state");
        }

        await loadSupportRequests();
        setSettingsStatus(archived ? "Ticket archived successfully." : "Ticket restored successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
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
        const response = await fetch(isEdit ? `${API_BASE}/products/${productId}` : `${API_BASE}/products`, {
            method: isEdit ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const data = await parseApiResponse(response);
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
            headers: { "Content-Type": "application/json" },
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
                contactPhone: "",
                contactEmail: document.getElementById("settings-email").value.trim(),
                instagramUrl: document.getElementById("settings-instagram").value.trim()
            })
        });

        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to save store settings");
        }

        currentStoreSettings = data;
        fillSettingsForm(data);
        renderSupportRequests(currentTickets);
        setSettingsStatus("Storefront settings updated successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    } finally {
        settingsSubmitButton.disabled = false;
    }
}

async function refreshDashboard() {
    await Promise.all([loadProducts(), loadStoreSettings(), loadSupportRequests()]);
}

function resetTicketFilters() {
    document.getElementById("ticket-search").value = "";
    document.getElementById("ticket-status-filter").value = "ALL";
    document.getElementById("ticket-priority-filter").value = "ALL";
    document.getElementById("ticket-archive-filter").checked = false;
}

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("user");
    window.location.href = "login.html";
});

document.getElementById("refresh-btn").addEventListener("click", () => {
    refreshDashboard().catch((error) => {
        setSettingsStatus(error.message || "Unable to refresh the workspace.", "error");
    });
});

document.getElementById("ticket-apply-btn").addEventListener("click", () => {
    if (allTickets.length) {
        applyTicketFilters();
        return;
    }
    loadSupportRequests().catch((error) => {
        supportRequestsContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load support requests right now.")}</div>`;
    });
});

document.getElementById("ticket-reset-filters-btn").addEventListener("click", () => {
    resetTicketFilters();
    if (allTickets.length) {
        applyTicketFilters();
        return;
    }
    loadSupportRequests().catch((error) => {
        supportRequestsContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load support requests right now.")}</div>`;
    });
});

document.getElementById("ticket-search").addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault();
        if (allTickets.length) {
            applyTicketFilters();
            return;
        }
        loadSupportRequests().catch((error) => {
            supportRequestsContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load support requests right now.")}</div>`;
        });
    }
});

document.getElementById("reset-btn").addEventListener("click", resetForm);
form.addEventListener("submit", saveProduct);
settingsForm.addEventListener("submit", saveStoreSettings);

window.editProduct = editProduct;
window.deleteProduct = deleteProduct;
window.respondToTicket = respondToTicket;
window.archiveTicket = archiveTicket;

if (ensureAdmin()) {
    resetForm();
    resetTicketFilters();
    refreshDashboard().catch((error) => {
        setStatus("Unable to load products. Make sure the backend is running.", "error");
        supportRequestsContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load admin workspace right now.")}</div>`;
    });
}
