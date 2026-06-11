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
const ticketDetailPanel = document.getElementById("ticket-detail-panel");
const ordersContainer = document.getElementById("orders");
const orderDetailPanel = document.getElementById("order-detail-panel");
const usersContainer = document.getElementById("users");
const monitoringIncidentsContainer = document.getElementById("monitoring-incidents");

let currentProducts = [];
let currentTickets = [];
let currentOrders = [];
let allOrders = [];
let currentUsers = [];
let allUsers = [];
let currentStoreSettings = { contactEmail: "contact.ogfashions@gmail.com" };
let allTickets = [];
let selectedTicketIds = new Set();
let activeTicketId = null;
let selectedOrderIds = new Set();
let activeOrderId = null;
let currentAlertIncidents = [];

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
    document.getElementById("delivery-charge-enabled").checked = false;
    document.getElementById("delivery-charge-amount").value = "0";
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
        deliveryChargeEnabled: document.getElementById("delivery-charge-enabled").checked,
        deliveryChargeAmount: toNumber(document.getElementById("delivery-charge-amount").value),
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
    document.getElementById("delivery-charge-enabled").checked = Boolean(product.deliveryChargeEnabled);
    document.getElementById("delivery-charge-amount").value = product.deliveryChargeAmount ?? 0;
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

function countByStatus(tickets, statuses) {
    return tickets.filter((ticket) => statuses.includes(String(ticket.status || "").toUpperCase())).length;
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

function syncActiveTicket(tickets) {
    const hasActive = tickets.some((ticket) => ticket.id === activeTicketId);
    if (!hasActive) {
        activeTicketId = tickets.length ? tickets[0].id : null;
    }

    selectedTicketIds = new Set(
        [...selectedTicketIds].filter((id) => tickets.some((ticket) => ticket.id === id))
    );
}

function updateTicketSelectionMeta() {
    const selectionMeta = document.getElementById("ticket-selection-meta");
    if (!selectionMeta) {
        return;
    }

    const count = selectedTicketIds.size;
    selectionMeta.textContent = count
        ? `${count} ticket(s) selected`
        : "No tickets selected.";
}

function getTicketStatsBase() {
    const showArchived = document.getElementById("ticket-archive-filter").checked;
    if (showArchived) {
        return allTickets;
    }
    return allTickets.filter((ticket) => !ticket.archived);
}

function updateTicketStats(requests) {
    const baseTickets = getTicketStatsBase();
    document.getElementById("stat-support").textContent = requests.length;
    document.getElementById("stat-open-work").textContent = countByStatus(baseTickets, ["NEW", "OPEN", "PENDING", "HOLD"]);
    document.getElementById("stat-resolved").textContent = countByStatus(baseTickets, ["RESOLVED", "CLOSED"]);
    document.getElementById("stat-urgent").textContent = baseTickets.filter((ticket) => String(ticket.priority || "").toUpperCase() === "URGENT").length;

    document.getElementById("stat-ticket-new").textContent = countByStatus(baseTickets, ["NEW"]);
    document.getElementById("stat-ticket-open").textContent = countByStatus(baseTickets, ["OPEN"]);
    document.getElementById("stat-ticket-pending").textContent = countByStatus(baseTickets, ["PENDING", "HOLD"]);
    document.getElementById("stat-ticket-closed").textContent = countByStatus(baseTickets, ["RESOLVED", "CLOSED"]);
    document.getElementById("stat-ticket-archived").textContent = allTickets.filter((ticket) => ticket.archived).length;

    const meta = document.getElementById("ticket-results-meta");
    if (meta) {
        meta.textContent = `Showing ${requests.length} ticket(s) from ${baseTickets.length} queue record(s). Total archived in database: ${allTickets.filter((ticket) => ticket.archived).length}.`;
    }
}

function updateMonitoringStats(summary = {}, incidents = currentAlertIncidents) {
    const openAlerts = Number(summary.openAlerts || 0);
    const resolvedAlerts = Number(summary.resolvedAlerts || 0);
    const latest = incidents[0];

    document.getElementById("stat-alert-open").textContent = openAlerts;
    document.getElementById("stat-alert-resolved").textContent = resolvedAlerts;
    document.getElementById("stat-alert-tracked").textContent = Number(summary.trackedAlerts || incidents.length || 0);
    document.getElementById("stat-alert-critical").textContent = incidents.filter((incident) =>
        String(incident.status || "").toLowerCase() !== "resolved"
        && String(incident.severity || "").toLowerCase() === "critical"
    ).length;
    document.getElementById("stat-alert-warning").textContent = incidents.filter((incident) =>
        String(incident.status || "").toLowerCase() !== "resolved"
        && String(incident.severity || "").toLowerCase() === "warning"
    ).length;
    document.getElementById("stat-alert-last").textContent = latest ? formatDateTime(latest.lastSeenAt) : "-";
}

function incidentStatusClass(status) {
    return String(status || "").toLowerCase() === "resolved" ? "status-resolved" : "status-open";
}

function renderMonitoringIncidents(incidents) {
    if (!monitoringIncidentsContainer) {
        return;
    }

    const meta = document.getElementById("monitoring-results-meta");
    if (meta) {
        meta.textContent = incidents.length
            ? `Showing ${incidents.length} retained alert incident(s). Resolved incidents stay here for audit and handover.`
            : "No alert lifecycle events are stored yet. Trigger or resolve an Alertmanager alert to populate this table.";
    }

    if (!incidents.length) {
        monitoringIncidentsContainer.innerHTML = '<div class="empty-state">No platform alert history yet. Confirm Alertmanager webhook routing is enabled and the backend is reachable from the monitoring network.</div>';
        return;
    }

    monitoringIncidentsContainer.innerHTML = `
        <table class="ticket-table">
            <thead>
                <tr>
                    <th>State</th>
                    <th>Alert</th>
                    <th>Service</th>
                    <th>Environment</th>
                    <th>First seen</th>
                    <th>Last update</th>
                    <th>Events</th>
                    <th>Summary</th>
                </tr>
            </thead>
            <tbody>
                ${incidents.map((incident) => `
                    <tr>
                        <td>
                            <span class="status-pill ${incidentStatusClass(incident.status)}">${escapeHtml(incident.status || "unknown")}</span>
                            <div class="compact" style="margin-top:8px;">${escapeHtml(incident.severity || "unknown")}</div>
                        </td>
                        <td>
                            <button class="ticket-link-btn" type="button">${escapeHtml(incident.alertName || "unknown-alert")}</button>
                            <div class="compact">${escapeHtml(incident.fingerprint || "-")}</div>
                        </td>
                        <td>${escapeHtml(incident.serviceName || "-")}</td>
                        <td>${escapeHtml(incident.environment || "docker/local")}</td>
                        <td>${escapeHtml(formatDateTime(incident.firstSeenAt))}</td>
                        <td>
                            ${escapeHtml(formatDateTime(incident.lastSeenAt))}
                            <div class="compact">Resolved: ${escapeHtml(incident.resolvedAt ? formatDateTime(incident.resolvedAt) : "not yet")}</div>
                        </td>
                        <td>${escapeHtml(incident.eventCount || 1)}</td>
                        <td>
                            <div class="ticket-summary-main">${escapeHtml(incident.summary || "-")}</div>
                            <div class="ticket-summary-sub">${escapeHtml(incident.description || "")}</div>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;
}

async function loadMonitoringIncidents() {
    const [summaryResponse, incidentsResponse] = await Promise.all([
        fetch(`${API_BASE}/monitoring/alerts/summary`),
        fetch(`${API_BASE}/monitoring/alerts/incidents?limit=100`)
    ]);
    const summary = await parseApiResponse(summaryResponse);
    const incidents = await parseApiResponse(incidentsResponse);

    if (!summaryResponse.ok) {
        throw new Error(summary.message || "Unable to load alert summary");
    }
    if (!incidentsResponse.ok) {
        throw new Error(incidents.message || "Unable to load alert incidents");
    }

    currentAlertIncidents = Array.isArray(incidents) ? incidents : [];
    updateMonitoringStats(summary, currentAlertIncidents);
    renderMonitoringIncidents(currentAlertIncidents);
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
    document.getElementById("settings-payment-qr").value = settings.paymentQrImageUrl || "";
    document.getElementById("settings-payment-upi").value = settings.paymentUpiId || "";
    document.getElementById("settings-payment-instructions").value = settings.paymentInstructions || "";
    document.getElementById("settings-cod-enabled").checked = settings.cashOnDeliveryEnabled !== false;
}

function formatReadableDate(value) {
    if (!value) return "Not updated yet";
    return new Date(value).toLocaleString();
}

function readablePayment(method) {
    if (method === "QR_PAYMENT") return "QR payment";
    if (method === "CASH_ON_DELIVERY") return "Cash on delivery";
    return method || "Not set";
}

function renderOrdersLegacy(orders) {
    if (!orders.length) {
        ordersContainer.innerHTML = '<div class="empty-state">No orders have been placed yet. Once shoppers check out, fulfillment updates will appear here.</div>';
        return;
    }

    ordersContainer.className = "orders-grid";
    ordersContainer.innerHTML = orders.map((order) => `
        <article class="order-board-card">
            <div class="order-board-head">
                <div class="order-board-title">
                    <div class="order-board-number">${escapeHtml(order.orderNumber || `#${order.id}`)}</div>
                    <div class="order-board-name">${escapeHtml(order.customerName || "Customer order")}</div>
                    <div class="order-board-meta">${escapeHtml(order.customerEmail || "-")} • ${escapeHtml(order.customerPhone || "-")} • ${formatReadableDate(order.createdAt)}</div>
                </div>
                <div class="order-chip-row">
                    <span class="status-pill ${statusClass(order.status)}">${escapeHtml(order.status || "PLACED")}</span>
                    <span class="priority-pill ${priorityClass(order.paymentStatus || "pending")}">${escapeHtml(order.paymentStatus || "PENDING")}</span>
                </div>
                <div class="order-chip-row">
                    <span class="order-chip">${escapeHtml(readablePayment(order.paymentMethod))}</span>
                </div>
            </div>

            <div class="order-board-summary">
                <div class="order-kpi">
                    <div class="label-line">Subtotal</div>
                    <div class="value-line">${formatPrice(order.subtotalAmount || 0)}</div>
                </div>
                <div class="order-kpi">
                    <div class="label-line">Shipping</div>
                    <div class="value-line">${formatPrice(order.shippingAmount || 0)}</div>
                </div>
                <div class="order-kpi">
                    <div class="label-line">Total</div>
                    <div class="value-line">${formatPrice(order.totalAmount || 0)}</div>
                </div>
                <div class="order-kpi">
                    <div class="label-line">Last updated</div>
                    <div class="value-line">${formatReadableDate(order.updatedAt)}</div>
                </div>
            </div>

            <div class="order-board-body">
                <div class="order-stack">
                    <div class="info-box">
                        <div class="label-line">Items ordered</div>
                        <div class="value-line" style="white-space:pre-wrap;">${escapeHtml(order.itemsSummary || "No items recorded.")}</div>
                    </div>
                    <div class="mail-grid">
                        <div class="mail-box">
                            <div class="label-line">Shipping summary</div>
                            <div class="value-line" style="white-space:pre-wrap;">${escapeHtml(order.shippingAddress || "No shipping address shared.")}</div>
                        </div>
                        <div class="mail-box">
                            <div class="label-line">Payment and notes</div>
                            <div class="value-line">
                                Method: ${escapeHtml(readablePayment(order.paymentMethod))}<br>
                                Reference: ${escapeHtml(order.paymentReference || "Not shared")}<br>
                                Customer note: ${escapeHtml(order.customerNote || "No customer note")}
                            </div>
                        </div>
                    </div>
                    <div class="mail-grid">
                        <div class="mail-box">
                            <div class="label-line">Customer mailbox</div>
                            <div class="value-line">${escapeHtml(order.customerEmail || "-")}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(order.customerEmailSent)}">${order.customerEmailSent ? "Customer email sent" : "Customer email pending"}</span>
                            </div>
                        </div>
                        <div class="mail-box">
                            <div class="label-line">Store mailbox</div>
                            <div class="value-line">${escapeHtml(currentStoreSettings.contactEmail || "contact.ogfashions@gmail.com")}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(order.storeEmailSent)}">${order.storeEmailSent ? "Store email sent" : "Store email pending"}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="order-actions-panel">
                    <div class="ticket-actions">
                        <textarea id="order-note-${order.id}" placeholder="Share internal fulfillment notes, courier updates, or return remarks...">${escapeHtml(order.adminNote || "")}</textarea>
                        <div class="ticket-form-grid">
                            <label>Order status
                                <select id="order-status-${order.id}">
                                    ${["PLACED", "PAYMENT_REVIEW", "CONFIRMED", "PACKING", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "RETURN_REQUESTED", "RETURNED", "CANCELLED"].map((value) => `
                                        <option value="${value}" ${order.status === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Payment status
                                <select id="payment-status-${order.id}">
                                    ${["PENDING", "PAID_SUBMITTED", "PAID_VERIFIED", "COD_PENDING", "COD_COLLECTED", "FAILED", "REFUNDED"].map((value) => `
                                        <option value="${value}" ${order.paymentStatus === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Order number
                                <input value="${escapeHtml(order.orderNumber || "")}" disabled>
                            </label>
                        </div>
                        <div class="ticket-action-row">
                            <button class="btn btn-primary" type="button" onclick="updateOrderStatus(${order.id})">Send order update</button>
                        </div>
                    </div>
                </div>
            </div>
        </article>
    `).join("");
}

function getOrderFilters() {
    return {
        search: document.getElementById("order-search").value.trim().toLowerCase(),
        status: document.getElementById("order-status-filter").value,
        paymentStatus: document.getElementById("order-payment-status-filter").value,
        paymentMethod: document.getElementById("order-payment-method-filter").value,
        fromDate: document.getElementById("order-date-from").value,
        toDate: document.getElementById("order-date-to").value,
        archived: document.getElementById("order-archive-filter").checked
    };
}

function matchesOrderFilters(order, filters) {
    const searchable = [
        order.orderNumber,
        order.customerName,
        order.customerEmail,
        order.customerPhone,
        order.paymentReference,
        order.itemsSummary,
        order.shippingAddress
    ].filter(Boolean).join(" ").toLowerCase();

    const createdDate = order.createdAt ? new Date(order.createdAt) : null;
    const fromDate = filters.fromDate ? new Date(`${filters.fromDate}T00:00:00`) : null;
    const toDate = filters.toDate ? new Date(`${filters.toDate}T23:59:59`) : null;

    if (!filters.archived && order.archived) {
        return false;
    }
    if (filters.status !== "ALL" && String(order.status || "").toUpperCase() !== filters.status) {
        return false;
    }
    if (filters.paymentStatus !== "ALL" && String(order.paymentStatus || "").toUpperCase() !== filters.paymentStatus) {
        return false;
    }
    if (filters.paymentMethod !== "ALL" && String(order.paymentMethod || "").toUpperCase() !== filters.paymentMethod) {
        return false;
    }
    if (filters.search && !searchable.includes(filters.search)) {
        return false;
    }
    if (fromDate && createdDate && createdDate < fromDate) {
        return false;
    }
    if (toDate && createdDate && createdDate > toDate) {
        return false;
    }
    return true;
}

function syncActiveOrder(orders) {
    const hasActive = orders.some((order) => order.id === activeOrderId);
    if (!hasActive) {
        activeOrderId = orders.length ? orders[0].id : null;
    }

    selectedOrderIds = new Set(
        [...selectedOrderIds].filter((id) => orders.some((order) => order.id === id))
    );
}

function updateOrderSelectionMeta() {
    const meta = document.getElementById("order-selection-meta");
    if (!meta) return;
    meta.textContent = selectedOrderIds.size
        ? `${selectedOrderIds.size} order(s) selected.`
        : "No orders selected.";
}

function updateOrderResultsMeta(orders) {
    const baseOrders = document.getElementById("order-archive-filter").checked
        ? allOrders
        : allOrders.filter((order) => !order.archived);
    const meta = document.getElementById("order-results-meta");
    if (!meta) return;
    meta.textContent = `Showing ${orders.length} order(s) from ${baseOrders.length} visible fulfillment record(s). Archived in database: ${allOrders.filter((order) => order.archived).length}.`;
}

function getUserFilters() {
    return {
        search: document.getElementById("user-search").value.trim().toLowerCase()
    };
}

function applyUserFilters(users = allUsers) {
    const filters = getUserFilters();
    currentUsers = users.filter((user) => {
        const searchable = [
            user.name,
            user.email,
            user.mobile,
            user.role,
            user.passwordStatus
        ].filter(Boolean).join(" ").toLowerCase();
        return !filters.search || searchable.includes(filters.search);
    });
    renderUsers(currentUsers);
}

function applyOrderFilters(orders = allOrders) {
    const filtered = orders.filter((order) => matchesOrderFilters(order, getOrderFilters()));
    currentOrders = filtered;
    renderOrders(filtered);
}

function renderOrderDetailPanel(order) {
    if (!order) {
        orderDetailPanel.innerHTML = '<div class="ticket-empty-desk">Select an order in the queue to review shipping details, payment collection, and customer communication history.</div>';
        return;
    }

    orderDetailPanel.innerHTML = `
        <div class="order-detail-shell">
            <div class="ticket-workbench-head">
                <div>
                    <div class="order-board-number">${escapeHtml(order.orderNumber || `#${order.id}`)}</div>
                    <div class="order-board-name" style="margin-top:6px;">${escapeHtml(order.customerName || "Customer order")}</div>
                    <div class="order-board-meta" style="margin-top:6px;">${escapeHtml(order.customerEmail || "No customer email")} • ${escapeHtml(order.customerPhone || "No contact number")} • Placed ${formatReadableDate(order.createdAt)}</div>
                </div>
                <div class="pill-row">
                    <span class="status-pill ${statusClass(order.status)}">${escapeHtml(order.status || "PLACED")}</span>
                    <span class="priority-pill ${priorityClass(order.paymentStatus || "pending")}">${escapeHtml(order.paymentStatus || "PENDING")}</span>
                    ${order.archived ? '<span class="mail-pill mail-fail">Archived</span>' : ""}
                </div>
            </div>
            <div class="order-detail-grid">
                <div class="order-overview-grid">
                    <div class="order-brief-card">
                        <div class="label-line">Items ordered</div>
                        <div class="value-line" style="white-space:pre-wrap;">${escapeHtml(order.itemsSummary || "No items recorded.")}</div>
                    </div>
                    <div class="order-brief-card">
                        <div class="label-line">Shipping summary</div>
                        <div class="value-line" style="white-space:pre-wrap;">${escapeHtml(order.shippingAddress || "No shipping address shared.")}</div>
                    </div>
                    <div class="order-brief-card">
                        <div class="label-line">Payment and customer note</div>
                        <div class="value-line">
                            Method: ${escapeHtml(readablePayment(order.paymentMethod))}<br>
                            Reference: ${escapeHtml(order.paymentReference || "Not shared")}<br>
                            Note: ${escapeHtml(order.customerNote || "No customer note")}
                        </div>
                    </div>
                    <div class="order-brief-card">
                        <div class="label-line">Mail confirmation</div>
                        <div class="value-line">
                            Customer inbox: ${escapeHtml(order.customerEmail || "-")}<br>
                            Store inbox: ${escapeHtml(currentStoreSettings.contactEmail || "contact.ogfashions@gmail.com")}
                        </div>
                        <div class="order-mail-status">
                            <span class="mail-pill ${mailClass(order.customerEmailSent)}">${order.customerEmailSent ? "Customer mail sent" : "Customer mail pending"}</span>
                            <span class="mail-pill ${mailClass(order.storeEmailSent)}">${order.storeEmailSent ? "Store mail sent" : "Store mail pending"}</span>
                        </div>
                    </div>
                    <div class="order-brief-card">
                        <div class="label-line">Order totals</div>
                        <div class="value-line">Subtotal: ${formatPrice(order.subtotalAmount || 0)}<br>Shipping: ${formatPrice(order.shippingAmount || 0)}<br>Total: ${formatPrice(order.totalAmount || 0)}</div>
                    </div>
                    <div class="order-brief-card">
                        <div class="label-line">Timeline</div>
                        <div class="value-line">Created: ${formatReadableDate(order.createdAt)}<br>Updated: ${formatReadableDate(order.updatedAt)}</div>
                    </div>
                </div>

                <div class="order-actions-panel">
                    <div class="order-actions-head">
                        <div>
                            <div class="label-line">Fulfillment controls</div>
                            <div class="value-line" style="margin-top:4px;">Update shipment progress, payment collection, archive, or permanently delete this order.</div>
                        </div>
                        <span class="mini-pill">Order desk</span>
                    </div>
                    <div class="ticket-actions">
                        <textarea id="order-note-${order.id}" placeholder="Share internal fulfillment notes, courier updates, or return remarks...">${escapeHtml(order.adminNote || "")}</textarea>
                        <div class="ticket-form-grid">
                            <label>Order status
                                <select id="order-status-${order.id}">
                                    ${["PLACED", "PAYMENT_REVIEW", "CONFIRMED", "PACKING", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "RETURN_REQUESTED", "RETURNED", "CANCELLED"].map((value) => `
                                        <option value="${value}" ${order.status === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Payment status
                                <select id="payment-status-${order.id}">
                                    ${["PENDING", "PAID_SUBMITTED", "PAID_VERIFIED", "COD_PENDING", "COD_COLLECTED", "FAILED", "REFUNDED"].map((value) => `
                                        <option value="${value}" ${order.paymentStatus === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Payment method
                                <input value="${escapeHtml(readablePayment(order.paymentMethod))}" disabled>
                            </label>
                        </div>
                        <div class="ticket-action-row">
                            <button class="btn btn-primary" type="button" onclick="updateOrderStatus(${order.id})">Send order update</button>
                            <button class="btn btn-secondary" type="button" onclick="archiveOrder(${order.id}, ${order.archived ? "false" : "true"})">${order.archived ? "Restore order" : "Archive order"}</button>
                            <button class="btn btn-danger" type="button" onclick="deleteOrderRecord(${order.id})">Delete order</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function renderOrders(orders) {
    updateOrderSelectionMeta();
    updateOrderResultsMeta(orders);

    if (!orders.length) {
        ordersContainer.innerHTML = '<div class="empty-state">No orders match the current filters yet. Adjust the filters or wait for new checkouts.</div>';
        renderOrderDetailPanel(null);
        return;
    }

    syncActiveOrder(orders);
    const activeOrder = orders.find((order) => order.id === activeOrderId) || orders[0];

    ordersContainer.className = "order-table-wrap";
    ordersContainer.innerHTML = `
        <table class="order-table">
            <thead>
                <tr>
                    <th style="width:48px;"><input class="ticket-check" type="checkbox" ${orders.length && orders.every((order) => selectedOrderIds.has(order.id)) ? "checked" : ""} onchange="toggleAllOrders(this.checked)"></th>
                    <th style="width:17%;">Order</th>
                    <th style="width:17%;">Customer</th>
                    <th style="width:14%;">Opened</th>
                    <th style="width:13%;">Total</th>
                    <th style="width:15%;">Payment</th>
                    <th style="width:14%;">State</th>
                    <th style="width:10%;">Actions</th>
                </tr>
            </thead>
            <tbody>
                ${orders.map((order) => `
                    <tr class="${activeOrder.id === order.id ? "active" : ""}">
                        <td><input class="ticket-check" type="checkbox" ${selectedOrderIds.has(order.id) ? "checked" : ""} onchange="toggleOrderSelection(${order.id}, this.checked)"></td>
                        <td>
                            <div class="order-summary-cell">
                                <button class="ticket-link-btn" type="button" onclick="openOrderDesk(${order.id})">${escapeHtml(order.orderNumber || `#${order.id}`)}</button>
                                <div class="order-summary-sub">${escapeHtml((order.itemsSummary || "").split("\n").filter(Boolean).length || 1)} item(s) • ${order.archived ? "Archived queue" : "Active queue"}</div>
                            </div>
                        </td>
                        <td>
                            <div class="order-summary-cell">
                                <div class="order-summary-main">${escapeHtml(order.customerName || "Customer")}</div>
                                <div class="order-summary-sub">${escapeHtml(order.customerEmail || "-")}<br>${escapeHtml(order.customerPhone || "-")}</div>
                            </div>
                        </td>
                        <td>${formatReadableDate(order.createdAt)}</td>
                        <td>
                            <div class="order-summary-main">${formatPrice(order.totalAmount || 0)}</div>
                            <div class="order-summary-sub">Shipping ${formatPrice(order.shippingAmount || 0)}</div>
                        </td>
                        <td>
                            <div class="order-summary-main">${escapeHtml(readablePayment(order.paymentMethod))}</div>
                            <div class="order-summary-sub">${escapeHtml(order.paymentStatus || "PENDING")}</div>
                        </td>
                        <td>
                            <div class="pill-row" style="justify-content:flex-start;">
                                <span class="status-pill ${statusClass(order.status)}">${escapeHtml(order.status || "PLACED")}</span>
                                ${order.archived ? '<span class="mail-pill mail-fail">Archived</span>' : ""}
                            </div>
                        </td>
                        <td>
                            <div class="ticket-actions-inline">
                                <button class="btn btn-secondary" type="button" onclick="openOrderDesk(${order.id})">Open</button>
                                <button class="btn btn-secondary" type="button" onclick="archiveOrder(${order.id}, ${order.archived ? "false" : "true"})">${order.archived ? "Restore" : "Archive"}</button>
                                <button class="btn btn-danger" type="button" onclick="deleteOrderRecord(${order.id})">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;

    renderOrderDetailPanel(activeOrder);
}

function renderUsers(users) {
    const meta = document.getElementById("user-results-meta");
    if (meta) {
        meta.textContent = `Showing ${users.length} user account(s) from ${allUsers.length} registered profile(s).`;
    }

    if (!users.length) {
        usersContainer.innerHTML = '<div class="empty-state">No users match the current search.</div>';
        return;
    }

    usersContainer.className = "order-table-wrap";
    usersContainer.innerHTML = `
        <table class="order-table">
            <thead>
                <tr>
                    <th style="width:10%;">ID</th>
                    <th style="width:20%;">Name</th>
                    <th style="width:22%;">Email</th>
                    <th style="width:16%;">Mobile</th>
                    <th style="width:12%;">Role</th>
                    <th style="width:12%;">Password</th>
                    <th style="width:12%;">Created</th>
                    <th style="width:18%;">Actions</th>
                </tr>
            </thead>
            <tbody>
                ${users.map((user) => `
                    <tr>
                        <td>${escapeHtml(user.id)}</td>
                        <td>
                            <div class="order-summary-cell">
                                <div class="order-summary-main">${escapeHtml(user.name || "Unnamed user")}</div>
                                <div class="order-summary-sub">${escapeHtml(user.role || "CUSTOMER")}</div>
                            </div>
                        </td>
                        <td>${escapeHtml(user.email || "-")}</td>
                        <td>${escapeHtml(user.mobile || "Not shared")}</td>
                        <td>
                            <select id="user-role-${user.id}">
                                ${["ADMIN", "CUSTOMER"].map((role) => `<option value="${role}" ${String(user.role || "").toUpperCase() === role ? "selected" : ""}>${role}</option>`).join("")}
                            </select>
                        </td>
                        <td><span class="mail-pill mail-ok">${escapeHtml(user.passwordStatus || "Protected")}</span></td>
                        <td>${formatReadableDate(user.createdAt)}</td>
                        <td>
                            <div class="ticket-actions-inline">
                                <input id="user-mobile-${user.id}" value="${escapeHtml(user.mobile || "")}" placeholder="Mobile">
                                <button class="btn btn-secondary" type="button" onclick="updateUserRecord(${user.id})">Save</button>
                                <button class="btn btn-danger" type="button" onclick="deleteUserRecord(${user.id})">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;
}

function renderSupportRequestsCardLegacy(requests) {
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

function renderSupportRequests(requests) {
    updateTicketStats(requests);
    syncActiveTicket(requests);
    updateTicketSelectionMeta();

    if (!requests.length) {
        supportRequestsContainer.innerHTML = '<div class="empty-state">No tickets match the current filters. Try another status, priority, archived view, or search term.</div>';
        ticketDetailPanel.innerHTML = "";
        return;
    }

    const storeEmail = currentStoreSettings.contactEmail || "contact.ogfashions@gmail.com";

    supportRequestsContainer.innerHTML = `
        <table class="ticket-table">
            <thead>
                <tr>
                    <th><input id="tickets-select-all" class="ticket-check" type="checkbox" ${requests.every((ticket) => selectedTicketIds.has(ticket.id)) ? "checked" : ""}></th>
                    <th>Number</th>
                    <th>Opened</th>
                    <th>Caller</th>
                    <th>Ticket summary</th>
                    <th>Priority</th>
                    <th>State</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${requests.map((request) => `
                    <tr class="${request.id === activeTicketId ? "active" : ""}">
                        <td><input class="ticket-check" type="checkbox" ${selectedTicketIds.has(request.id) ? "checked" : ""} onchange="toggleTicketSelection(${request.id}, this.checked)"></td>
                        <td>
                            <div class="ticket-cell-meta">
                                <button class="ticket-link-btn" type="button" onclick="openTicketDesk(${request.id})">${escapeHtml(request.ticketNumber || `#${request.id}`)}</button>
                                ${request.archived ? '<span class="mini-pill">Archived</span>' : ""}
                            </div>
                        </td>
                        <td>${formatDateTime(request.createdAt)}</td>
                        <td>
                            <div class="ticket-cell-meta">
                                <strong>${escapeHtml(request.name || "Customer")}</strong>
                                <span class="subtle">${escapeHtml(request.email || "-")}</span>
                            </div>
                        </td>
                        <td>
                            <div class="ticket-summary-cell">
                                <div class="ticket-summary-main">${escapeHtml(request.subject || "Support request")}</div>
                                <div class="ticket-summary-sub">
                                    Order: ${escapeHtml(request.orderId || "Not linked")} ·
                                    Channel: ${escapeHtml(request.channel || "Website")} ·
                                    Assignee: ${escapeHtml(request.assigneeName || "Support Desk")}
                                </div>
                            </div>
                        </td>
                        <td><span class="priority-pill ${priorityClass(getTicketPriority(request))}">${escapeHtml(getTicketPriority(request))}</span></td>
                        <td><span class="status-pill ${statusClass(request.status)}">${escapeHtml(request.status || "NEW")}</span></td>
                        <td>
                            <div class="ticket-actions-inline">
                                <button class="btn btn-secondary" type="button" onclick="openTicketDesk(${request.id})">Open</button>
                                <button class="btn btn-secondary" type="button" onclick="archiveTicket(${request.id}, ${request.archived ? "false" : "true"})">${request.archived ? "Restore" : "Archive"}</button>
                                <button class="btn btn-danger" type="button" onclick="deleteTicket(${request.id})">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;

    const activeTicket = requests.find((request) => request.id === activeTicketId) || requests[0];
    activeTicketId = activeTicket.id;
    ticketDetailPanel.innerHTML = `
        <article class="ticket-card">
            <div class="ticket-workbench-head">
                <div>
                    <div class="ticket-id">${escapeHtml(activeTicket.ticketNumber || `#${activeTicket.id}`)}</div>
                    <div class="ticket-title">${escapeHtml(activeTicket.subject || "Support request")}</div>
                    <div class="ticket-sub">${escapeHtml(activeTicket.name || "Customer")} • ${escapeHtml(activeTicket.email || "No email")} • Created ${formatDateTime(activeTicket.createdAt)}</div>
                </div>
                <div class="pill-row">
                    <span class="status-pill ${statusClass(activeTicket.status)}">${escapeHtml(activeTicket.status || "NEW")}</span>
                    <span class="priority-pill ${priorityClass(getTicketPriority(activeTicket))}">${escapeHtml(getTicketPriority(activeTicket))}</span>
                </div>
            </div>

            <div class="ticket-workbench-grid">
                <div class="panel-stack">
                    <div class="info-box">
                        <div class="label-line">Customer issue</div>
                        <div class="value-line">${escapeHtml(activeTicket.issue || "No issue details provided.")}</div>
                    </div>
                    <div class="info-box">
                        <div class="label-line">Latest admin response</div>
                        <div class="value-line">${escapeHtml(activeTicket.adminReply || "No response sent yet.")}</div>
                    </div>
                    <div class="info-box">
                        <div class="label-line">Internal support note</div>
                        <div class="value-line">${escapeHtml(activeTicket.internalNote || "No internal note added yet.")}</div>
                    </div>
                    <div class="mail-grid">
                        <div class="mail-box">
                            <div class="label-line">Customer mailbox</div>
                            <div class="value-line">${escapeHtml(activeTicket.email || "-")}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(activeTicket.customerEmailSent)}">${activeTicket.customerEmailSent ? "Customer email sent" : "Customer email pending"}</span>
                            </div>
                        </div>
                        <div class="mail-box">
                            <div class="label-line">Store mailbox</div>
                            <div class="value-line">${escapeHtml(storeEmail)}</div>
                            <div class="pill-row" style="justify-content:flex-start;margin-top:10px;">
                                <span class="mail-pill ${mailClass(activeTicket.emailSent)}">${activeTicket.emailSent ? "Store email sent" : "Store email pending"}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="panel-stack">
                    <div class="info-box">
                        <div class="label-line">Incident details</div>
                        <div class="value-line">
                            <strong>Order ID:</strong> ${escapeHtml(activeTicket.orderId || "Not linked")}<br>
                            <strong>Channel:</strong> ${escapeHtml(activeTicket.channel || "WEBSITE")}<br>
                            <strong>Assignee:</strong> ${escapeHtml(activeTicket.assigneeName || "Support Desk")}<br>
                            <strong>Updated:</strong> ${formatDateTime(activeTicket.updatedAt || activeTicket.createdAt)}<br>
                            <strong>Last replied:</strong> ${formatDateTime(activeTicket.lastRespondedAt)}
                        </div>
                    </div>
                    <div class="ticket-actions">
                        <textarea id="reply-${activeTicket.id}" placeholder="Write the customer-facing update or resolution...">${escapeHtml(activeTicket.adminReply || "")}</textarea>
                        <textarea id="note-${activeTicket.id}" placeholder="Internal note for support team only...">${escapeHtml(activeTicket.internalNote || "")}</textarea>
                        <div class="ticket-form-grid">
                            <label>Status
                                <select id="status-${activeTicket.id}">
                                    ${["NEW", "OPEN", "PENDING", "HOLD", "RESOLVED", "CLOSED"].map((value) => `
                                        <option value="${value}" ${activeTicket.status === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Priority
                                <select id="priority-${activeTicket.id}">
                                    ${["LOW", "MEDIUM", "HIGH", "URGENT"].map((value) => `
                                        <option value="${value}" ${getTicketPriority(activeTicket) === value ? "selected" : ""}>${value}</option>
                                    `).join("")}
                                </select>
                            </label>
                            <label>Assignee
                                <input id="assignee-${activeTicket.id}" value="${escapeHtml(activeTicket.assigneeName || "Support Desk")}" placeholder="Support Desk">
                            </label>
                        </div>
                        <div class="ticket-action-row">
                            <button class="btn btn-primary" type="button" onclick="respondToTicket(${activeTicket.id})">Send update</button>
                            <button class="btn btn-secondary" type="button" onclick="archiveTicket(${activeTicket.id}, ${activeTicket.archived ? "false" : "true"})">${activeTicket.archived ? "Restore to active queue" : "Archive ticket"}</button>
                            <button class="btn btn-danger" type="button" onclick="deleteTicket(${activeTicket.id})">Permanent delete</button>
                        </div>
                    </div>
                </div>
            </div>
        </article>
    `;

    const selectAll = document.getElementById("tickets-select-all");
    if (selectAll) {
        selectAll.addEventListener("change", (event) => {
            if (event.target.checked) {
                requests.forEach((ticket) => selectedTicketIds.add(ticket.id));
            } else {
                requests.forEach((ticket) => selectedTicketIds.delete(ticket.id));
            }
            renderSupportRequests(currentTickets);
        });
    }
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
                    <div class="compact">${product.deliveryChargeEnabled ? `Delivery charge enabled â€¢ ${formatPrice(product.deliveryChargeAmount || 0)}` : "Delivery charge not enabled"}</div>
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
    if (currentOrders.length) {
        renderOrders(currentOrders);
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

async function loadOrders() {
    const response = await fetch(`${API_BASE}/orders?adminUserId=${adminUser.id}`);
    const data = await parseApiResponse(response);
    if (!response.ok) {
        ordersContainer.innerHTML = `<div class="empty-state">${escapeHtml(data.message || "Unable to load orders")}</div>`;
        throw new Error(data.message || "Unable to load orders");
    }
    allOrders = data;
    applyOrderFilters(data);
}

async function loadUsers() {
    const response = await fetch(`${API_BASE}/admin/users?adminUserId=${adminUser.id}`);
    const data = await parseApiResponse(response);
    if (!response.ok) {
        usersContainer.innerHTML = `<div class="empty-state">${escapeHtml(data.message || "Unable to load users")}</div>`;
        throw new Error(data.message || "Unable to load users");
    }
    allUsers = data;
    applyUserFilters(data);
}

function resetOrderFilters() {
    document.getElementById("order-search").value = "";
    document.getElementById("order-status-filter").value = "ALL";
    document.getElementById("order-payment-status-filter").value = "ALL";
    document.getElementById("order-payment-method-filter").value = "ALL";
    document.getElementById("order-date-from").value = "";
    document.getElementById("order-date-to").value = "";
    document.getElementById("order-archive-filter").checked = false;
}

function resetUserFilters() {
    document.getElementById("user-search").value = "";
}

function openOrderDesk(id) {
    activeOrderId = id;
    renderOrders(currentOrders);
}

function toggleOrderSelection(id, checked) {
    if (checked) {
        selectedOrderIds.add(id);
    } else {
        selectedOrderIds.delete(id);
    }
    updateOrderSelectionMeta();
}

function toggleAllOrders(checked) {
    currentOrders.forEach((order) => {
        if (checked) {
            selectedOrderIds.add(order.id);
        } else {
            selectedOrderIds.delete(order.id);
        }
    });
    renderOrders(currentOrders);
}

async function archiveOrder(id, archived) {
    const prompt = archived
        ? "Archive this order and remove it from the active fulfillment queue?"
        : "Restore this order back to the active fulfillment queue?";
    if (!confirm(prompt)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/${id}/archive`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                archived
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update the order archive state");
        }
        if (activeOrderId === id && archived) {
            activeOrderId = null;
        }
        await loadOrders();
        setSettingsStatus(archived ? "Order archived successfully." : "Order restored successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function archiveSelectedOrders(archived) {
    const orderIds = [...selectedOrderIds];
    if (!orderIds.length) {
        setSettingsStatus("Select at least one order first.", "error");
        return;
    }

    const prompt = archived
        ? `Archive ${orderIds.length} selected order(s)?`
        : `Restore ${orderIds.length} selected order(s) to the active queue?`;
    if (!confirm(prompt)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/bulk/archive`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                orderIds,
                archived
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update selected orders");
        }
        selectedOrderIds = new Set();
        await loadOrders();
        setSettingsStatus(archived ? `Archived ${data.updatedCount || orderIds.length} order(s).` : `Restored ${data.updatedCount || orderIds.length} order(s).`, "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function deleteOrderRecord(id) {
    if (!confirm("Permanently delete this order from the database? This cannot be undone.")) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/${id}?adminUserId=${adminUser.id}`, {
            method: "DELETE"
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to delete the order");
        }
        selectedOrderIds.delete(id);
        if (activeOrderId === id) {
            activeOrderId = null;
        }
        await loadOrders();
        setSettingsStatus("Order permanently deleted.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function deleteSelectedOrders() {
    const orderIds = [...selectedOrderIds];
    if (!orderIds.length) {
        setSettingsStatus("Select at least one order first.", "error");
        return;
    }

    if (!confirm(`Permanently delete ${orderIds.length} selected order(s)? This cannot be undone.`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/bulk`, {
            method: "DELETE",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                orderIds
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to delete the selected orders");
        }
        selectedOrderIds = new Set();
        activeOrderId = null;
        await loadOrders();
        setSettingsStatus(`Deleted ${data.deletedCount || orderIds.length} order(s) permanently.`, "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function updateUserRecord(id) {
    try {
        const selectedUser = allUsers.find((user) => user.id === id);
        const response = await fetch(`${API_BASE}/admin/users/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                name: selectedUser?.name || "",
                mobile: document.getElementById(`user-mobile-${id}`).value.trim(),
                role: document.getElementById(`user-role-${id}`).value
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update the user");
        }
        await loadUsers();
        setSettingsStatus(`User ${data.email || id} updated successfully.`, "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function deleteUserRecord(id) {
    if (!confirm("Delete this user account? This will remove their login profile permanently.")) {
        return;
    }
    try {
        const response = await fetch(`${API_BASE}/admin/users/${id}?adminUserId=${adminUser.id}`, {
            method: "DELETE"
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to delete the user");
        }
        await loadUsers();
        setSettingsStatus("User deleted successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

function exportOrders() {
    const exportRows = currentOrders;
    if (!exportRows.length) {
        setSettingsStatus("There are no filtered orders to export right now.", "error");
        return;
    }

    const filters = getOrderFilters();
    const filterSummary = [
        `Search: ${filters.search || "All"}`,
        `Order status: ${filters.status}`,
        `Payment status: ${filters.paymentStatus}`,
        `Payment method: ${filters.paymentMethod}`,
        `From: ${filters.fromDate || "Any"}`,
        `To: ${filters.toDate || "Any"}`,
        `Archived view: ${filters.archived ? "Yes" : "No"}`
    ].join(" | ");

    const tableRows = exportRows.map((order) => `
        <tr>
            <td>${escapeHtml(order.orderNumber || `#${order.id}`)}</td>
            <td>${escapeHtml(order.customerName || "")}</td>
            <td>${escapeHtml(order.customerEmail || "")}</td>
            <td>${escapeHtml(order.customerPhone || "")}</td>
            <td>${escapeHtml(formatReadableDate(order.createdAt))}</td>
            <td>${escapeHtml(order.status || "")}</td>
            <td>${escapeHtml(order.paymentStatus || "")}</td>
            <td>${escapeHtml(readablePayment(order.paymentMethod))}</td>
            <td>${escapeHtml(formatPrice(order.subtotalAmount || 0))}</td>
            <td>${escapeHtml(formatPrice(order.shippingAmount || 0))}</td>
            <td>${escapeHtml(formatPrice(order.totalAmount || 0))}</td>
            <td>${escapeHtml(order.paymentReference || "")}</td>
            <td>${escapeHtml(order.shippingAddress || "")}</td>
            <td>${escapeHtml(order.customerNote || "")}</td>
            <td>${escapeHtml(order.adminNote || "")}</td>
            <td>${order.customerEmailSent ? "Yes" : "No"}</td>
            <td>${order.storeEmailSent ? "Yes" : "No"}</td>
            <td>${order.archived ? "Yes" : "No"}</td>
        </tr>
    `).join("");

    const workbook = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
        <head><meta charset="UTF-8"></head>
        <body>
            <table border="1">
                <tr><th colspan="18">OG Fashions orders export</th></tr>
                <tr><td colspan="18">${escapeHtml(filterSummary)}</td></tr>
                <tr>
                    <th>Order number</th>
                    <th>Customer</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Created</th>
                    <th>Order status</th>
                    <th>Payment status</th>
                    <th>Payment method</th>
                    <th>Subtotal</th>
                    <th>Shipping</th>
                    <th>Total</th>
                    <th>Payment reference</th>
                    <th>Shipping address</th>
                    <th>Customer note</th>
                    <th>Admin note</th>
                    <th>Customer mail sent</th>
                    <th>Store mail sent</th>
                    <th>Archived</th>
                </tr>
                ${tableRows}
            </table>
        </body>
        </html>
    `;

    const blob = new Blob([workbook], { type: "application/vnd.ms-excel;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    const dateStamp = new Date().toISOString().slice(0, 10);
    link.href = url;
    link.download = `og-orders-${dateStamp}.xls`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    setSettingsStatus(`Exported ${exportRows.length} order(s) to Excel format.`, "success");
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

function openTicketDesk(id) {
    activeTicketId = id;
    renderSupportRequests(currentTickets);
}

function toggleTicketSelection(id, checked) {
    if (checked) {
        selectedTicketIds.add(id);
    } else {
        selectedTicketIds.delete(id);
    }
    updateTicketSelectionMeta();
}

async function deleteTicket(id) {
    if (!confirm("Permanently delete this ticket from the database? This cannot be undone.")) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/contact-requests/${id}?adminUserId=${adminUser.id}`, {
            method: "DELETE"
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to delete the ticket");
        }
        selectedTicketIds.delete(id);
        if (activeTicketId === id) {
            activeTicketId = null;
        }
        await loadSupportRequests();
        setSettingsStatus("Ticket permanently deleted.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function archiveSelectedTickets(archived) {
    const ticketIds = [...selectedTicketIds];
    if (!ticketIds.length) {
        setSettingsStatus("Select at least one ticket first.", "error");
        return;
    }

    const prompt = archived
        ? "Archive the selected tickets?"
        : "Restore the selected tickets to the active queue?";
    if (!confirm(prompt)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/contact-requests/bulk/archive`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                ticketIds,
                archived
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update the selected tickets");
        }
        selectedTicketIds = new Set();
        await loadSupportRequests();
        setSettingsStatus(archived ? "Selected tickets archived successfully." : "Selected tickets restored successfully.", "success");
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
}

async function deleteSelectedTickets() {
    const ticketIds = [...selectedTicketIds];
    if (!ticketIds.length) {
        setSettingsStatus("Select at least one ticket first.", "error");
        return;
    }

    if (!confirm(`Permanently delete ${ticketIds.length} selected ticket(s)? This cannot be undone.`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/contact-requests/bulk`, {
            method: "DELETE",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                adminUserId: adminUser.id,
                ticketIds
            })
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to delete the selected tickets");
        }
        selectedTicketIds = new Set();
        activeTicketId = null;
        await loadSupportRequests();
        setSettingsStatus(`Deleted ${data.deletedCount || ticketIds.length} ticket(s) permanently.`, "success");
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
                brandName: document.getElementById("settings-brand-name").value.trim() || currentStoreSettings.brandName || "OG Fashions",
                tagline: document.getElementById("settings-tagline").value.trim() || currentStoreSettings.tagline || "Signature styles from Hyderabad.",
                announcementBar: document.getElementById("settings-announcement-bar").value.trim() || currentStoreSettings.announcementBar || "New arrivals, flash sales, fashion essentials, watches, pouches, and gifting picks shipped across India.",
                heroTitle: document.getElementById("settings-hero-title").value.trim() || currentStoreSettings.heroTitle || "Own your everyday style with OG Fashions.",
                heroSubtitle: document.getElementById("settings-hero-subtitle").value.trim() || currentStoreSettings.heroSubtitle || "Fresh catalog drops and elevated essentials for Hyderabad shoppers.",
                featuredMessage: document.getElementById("settings-featured-message").value.trim() || currentStoreSettings.featuredMessage || "Sharper product browsing with richer filters and quick detail views.",
                storeAddress: document.getElementById("settings-address").value.trim() || currentStoreSettings.storeAddress || "OG Fashion, New green city Budhwel, Rajendhranagar, Hyderabad.\n300030, India",
                openingHours: document.getElementById("settings-hours").value.trim() || currentStoreSettings.openingHours || "Monday - Saturday: 10:00 AM - 8:30 PM\nSunday: Closed",
                contactPhone: "",
                contactEmail: document.getElementById("settings-email").value.trim(),
                instagramUrl: document.getElementById("settings-instagram").value.trim(),
                paymentQrImageUrl: document.getElementById("settings-payment-qr").value.trim(),
                paymentUpiId: document.getElementById("settings-payment-upi").value.trim(),
                paymentInstructions: document.getElementById("settings-payment-instructions").value.trim(),
                cashOnDeliveryEnabled: document.getElementById("settings-cod-enabled").checked
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
    const results = await Promise.allSettled([loadProducts(), loadStoreSettings(), loadSupportRequests(), loadOrders(), loadUsers(), loadMonitoringIncidents()]);
    const criticalFailure = results.slice(0, 3).find((result) => result.status === "rejected");
    if (criticalFailure) {
        throw criticalFailure.reason;
    }
    const orderFailure = results[3];
    if (orderFailure && orderFailure.status === "rejected") {
        setSettingsStatus(orderFailure.reason?.message || "Orders could not be loaded yet.", "error");
    }
    const userFailure = results[4];
    if (userFailure && userFailure.status === "rejected") {
        setSettingsStatus(userFailure.reason?.message || "Users could not be loaded yet.", "error");
    }
    const monitoringFailure = results[5];
    if (monitoringFailure && monitoringFailure.status === "rejected") {
        renderMonitoringIncidents([]);
        setSettingsStatus(monitoringFailure.reason?.message || "Monitoring incidents could not be loaded yet.", "error");
    }
}

async function updateOrderStatus(id) {
    const payload = {
        adminUserId: adminUser.id,
        status: document.getElementById(`order-status-${id}`).value,
        paymentStatus: document.getElementById(`payment-status-${id}`).value,
        adminNote: document.getElementById(`order-note-${id}`).value.trim()
    };

    try {
        const response = await fetch(`${API_BASE}/orders/${id}/status`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const data = await parseApiResponse(response);
        if (!response.ok) {
            throw new Error(data.message || "Unable to update the order");
        }
        await loadOrders();
        const emailType = data.customerEmailSent === false
            ? "error"
            : "success";
        const emailMessage = data.customerEmailSent === false
            ? `Order ${data.orderNumber || id} was updated, but the customer email was not sent.`
            : `Order ${data.orderNumber || id} updated successfully and the customer email was sent.`;
        setSettingsStatus(emailMessage, emailType);
    } catch (error) {
        setSettingsStatus(error.message, "error");
    }
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

document.getElementById("monitoring-refresh-btn").addEventListener("click", () => {
    loadMonitoringIncidents()
        .then(() => setSettingsStatus("Monitoring incidents refreshed.", "success"))
        .catch((error) => setSettingsStatus(error.message || "Unable to refresh monitoring incidents.", "error"));
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

document.getElementById("ticket-bulk-archive-btn").addEventListener("click", () => archiveSelectedTickets(true));
document.getElementById("ticket-bulk-restore-btn").addEventListener("click", () => archiveSelectedTickets(false));
document.getElementById("ticket-bulk-delete-btn").addEventListener("click", deleteSelectedTickets);

document.getElementById("order-apply-btn").addEventListener("click", () => {
    if (allOrders.length) {
        applyOrderFilters();
        return;
    }
    loadOrders().catch((error) => {
        ordersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load orders right now.")}</div>`;
    });
});

document.getElementById("order-reset-btn").addEventListener("click", () => {
    resetOrderFilters();
    if (allOrders.length) {
        applyOrderFilters();
        return;
    }
    loadOrders().catch((error) => {
        ordersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load orders right now.")}</div>`;
    });
});

document.getElementById("order-export-btn").addEventListener("click", exportOrders);
document.getElementById("order-bulk-archive-btn").addEventListener("click", () => archiveSelectedOrders(true));
document.getElementById("order-bulk-restore-btn").addEventListener("click", () => archiveSelectedOrders(false));
document.getElementById("order-bulk-delete-btn").addEventListener("click", deleteSelectedOrders);
document.getElementById("order-search").addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault();
        if (allOrders.length) {
            applyOrderFilters();
            return;
        }
        loadOrders().catch((error) => {
            ordersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load orders right now.")}</div>`;
        });
    }
});

document.getElementById("user-apply-btn").addEventListener("click", () => {
    if (allUsers.length) {
        applyUserFilters();
        return;
    }
    loadUsers().catch((error) => {
        usersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load users right now.")}</div>`;
    });
});

document.getElementById("user-reset-btn").addEventListener("click", () => {
    resetUserFilters();
    if (allUsers.length) {
        applyUserFilters();
        return;
    }
    loadUsers().catch((error) => {
        usersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load users right now.")}</div>`;
    });
});

document.getElementById("user-search").addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        event.preventDefault();
        if (allUsers.length) {
            applyUserFilters();
            return;
        }
        loadUsers().catch((error) => {
            usersContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load users right now.")}</div>`;
        });
    }
});

document.getElementById("reset-btn").addEventListener("click", resetForm);
form.addEventListener("submit", saveProduct);
settingsForm.addEventListener("submit", saveStoreSettings);

const settingsQrFileInput = document.getElementById("settings-payment-qr-file");
if (settingsQrFileInput) {
    settingsQrFileInput.addEventListener("change", () => {
        const [file] = settingsQrFileInput.files || [];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = () => {
            const source = String(reader.result || "");
            if (!source) {
                setSettingsStatus("Unable to read the selected QR image.", "error");
                return;
            }

            const image = new Image();
            image.onload = () => {
                const maxSize = 420;
                const ratio = Math.min(1, maxSize / Math.max(image.width || 1, image.height || 1));
                const canvas = document.createElement("canvas");
                canvas.width = Math.max(1, Math.round((image.width || 1) * ratio));
                canvas.height = Math.max(1, Math.round((image.height || 1) * ratio));
                const context = canvas.getContext("2d");
                if (!context) {
                    document.getElementById("settings-payment-qr").value = source;
                    setSettingsStatus("QR image loaded into the form. Save storefront settings to publish it at checkout.", "success");
                    return;
                }
                context.fillStyle = "#ffffff";
                context.fillRect(0, 0, canvas.width, canvas.height);
                context.drawImage(image, 0, 0, canvas.width, canvas.height);
                document.getElementById("settings-payment-qr").value = canvas.toDataURL("image/jpeg", 0.9);
                setSettingsStatus("QR image loaded into the form. Save storefront settings to publish it at checkout.", "success");
            };
            image.onerror = () => {
                document.getElementById("settings-payment-qr").value = source;
                setSettingsStatus("QR image loaded into the form. Save storefront settings to publish it at checkout.", "success");
            };
            image.src = source;
        };
        reader.onerror = () => {
            setSettingsStatus("Unable to read the selected QR image.", "error");
        };
        reader.readAsDataURL(file);
    });
}

window.editProduct = editProduct;
window.deleteProduct = deleteProduct;
window.respondToTicket = respondToTicket;
window.archiveTicket = archiveTicket;
window.deleteTicket = deleteTicket;
window.openTicketDesk = openTicketDesk;
window.toggleTicketSelection = toggleTicketSelection;
window.updateOrderStatus = updateOrderStatus;
window.openOrderDesk = openOrderDesk;
window.toggleOrderSelection = toggleOrderSelection;
window.toggleAllOrders = toggleAllOrders;
window.archiveOrder = archiveOrder;
window.deleteOrderRecord = deleteOrderRecord;
window.updateUserRecord = updateUserRecord;
window.deleteUserRecord = deleteUserRecord;

if (ensureAdmin()) {
    resetForm();
    resetTicketFilters();
    resetOrderFilters();
    resetUserFilters();
    refreshDashboard().catch((error) => {
        setStatus("Unable to load products. Make sure the backend is running.", "error");
        supportRequestsContainer.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Unable to load admin workspace right now.")}</div>`;
    });
}
