package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.CartItemResponse;
import com.shop.ecommerce.dto.OrderBulkActionRequest;
import com.shop.ecommerce.dto.OrderCheckoutRequest;
import com.shop.ecommerce.dto.OrderCheckoutResponse;
import com.shop.ecommerce.dto.OrderStatusUpdateRequest;
import com.shop.ecommerce.model.ShopOrder;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ShopOrderRepository;
import com.shop.ecommerce.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final StoreSettingsService storeSettingsService;
    private final ShopOrderRepository shopOrderRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@ogfashions.local}")
    private String mailFrom;

    public OrderService(
            CartService cartService,
            UserRepository userRepository,
            StoreSettingsService storeSettingsService,
            ShopOrderRepository shopOrderRepository,
            JavaMailSender mailSender
    ) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.storeSettingsService = storeSettingsService;
        this.shopOrderRepository = shopOrderRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public OrderCheckoutResponse placeOrder(OrderCheckoutRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required to place an order");
        }

        User user = validateUser(request.getUserId());
        List<CartItemResponse> cartItems = cartService.getCart(user.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        StoreSettings settings = storeSettingsService.getSettings();
        String paymentMethod = normalizeValue(request.getPaymentMethod(), "CASH_ON_DELIVERY").toUpperCase(Locale.ENGLISH);
        validateCheckoutRequest(request, settings, paymentMethod);

        double subtotalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
        double shippingAmount = cartItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getDeliveryChargeEnabled()))
                .mapToDouble(item -> (item.getDeliveryChargeAmount() == null ? 0.0 : item.getDeliveryChargeAmount()) * item.getQuantity())
                .sum();
        double totalAmount = subtotalAmount + shippingAmount;

        ShopOrder order = new ShopOrder();
        order.setUserId(user.getId());
        order.setCustomerName(resolveCustomerName(request, user));
        order.setCustomerEmail(resolveCustomerEmail(request, user));
        order.setCustomerPhone(requireValue(request.getCustomerPhone(), "Contact number"));
        order.setShippingAddress(buildShippingAddress(request));
        order.setStatus("PLACED");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentMethod.equals("QR_PAYMENT") ? "PAID_SUBMITTED" : "COD_PENDING");
        order.setPaymentReference(normalizeValue(request.getPaymentReference(), ""));
        order.setItemCount(cartItems.stream().mapToInt(CartItemResponse::getQuantity).sum());
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingAmount(shippingAmount);
        order.setTotalAmount(totalAmount);
        order.setItemsSummary(buildItemsSummary(cartItems));
        order.setCustomerNote(normalizeValue(request.getCustomerNote(), ""));
        order.setAdminNote("");
        order.setOrderNumber(buildPendingOrderNumber());

        ShopOrder saved = shopOrderRepository.save(order);
        saved.setOrderNumber(formatOrderNumber(saved.getId()));
        saved = shopOrderRepository.save(saved);

        boolean storeEmailSent = false;
        boolean customerEmailSent = false;
        String message = "Order placed successfully.";

        if (mailEnabled) {
            try {
                if (settings.getContactEmail() != null && !settings.getContactEmail().isBlank()) {
                    sendStoreOrderEmail(saved, cartItems, settings);
                    storeEmailSent = true;
                }
                if (saved.getCustomerEmail() != null && !saved.getCustomerEmail().isBlank()) {
                    sendCustomerOrderEmail(saved, cartItems, settings);
                    customerEmailSent = true;
                }
                message = customerEmailSent
                        ? "Order placed successfully. Order confirmation has been sent to the customer and store."
                        : "Order placed successfully. Store notification email was sent.";
            } catch (MailException | MessagingException ex) {
                saved.setStatus("PLACED_EMAIL_FAILED");
                message = "Order placed successfully, but email delivery failed. Please review SMTP settings.";
            }
        } else {
            saved.setStatus("PLACED_SAVED_ONLY");
            message = "Order placed successfully. Email delivery is currently disabled, so the order was saved without sending mail.";
        }

        saved.setStoreEmailSent(storeEmailSent);
        saved.setCustomerEmailSent(customerEmailSent);
        shopOrderRepository.save(saved);
        cartService.clearCart(user.getId());

        return new OrderCheckoutResponse(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getStatus(),
                saved.getPaymentMethod(),
                saved.getPaymentStatus(),
                saved.getSubtotalAmount(),
                saved.getShippingAmount(),
                saved.getTotalAmount(),
                saved.isStoreEmailSent(),
                saved.isCustomerEmailSent(),
                message
        );
    }

    public List<ShopOrder> getUserOrders(Long userId) {
        validateUser(userId);
        return shopOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<ShopOrder> getAllOrders(Long adminUserId) {
        validateAdmin(adminUserId);
        return shopOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public ShopOrder archiveOrder(Long orderId, Long adminUserId, boolean archived) {
        validateAdmin(adminUserId);
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setArchived(archived);
        return shopOrderRepository.save(order);
    }

    @Transactional
    public int archiveOrders(OrderBulkActionRequest request) {
        validateAdmin(request == null ? null : request.getAdminUserId());
        List<Long> orderIds = requireOrderIds(request == null ? null : request.getOrderIds());
        List<ShopOrder> orders = shopOrderRepository.findAllById(orderIds);
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No orders were found for the selected IDs");
        }
        for (ShopOrder order : orders) {
            order.setArchived(request.isArchived());
        }
        shopOrderRepository.saveAll(orders);
        return orders.size();
    }

    @Transactional
    public void deleteOrder(Long orderId, Long adminUserId) {
        validateAdmin(adminUserId);
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        shopOrderRepository.delete(order);
    }

    @Transactional
    public int deleteOrders(OrderBulkActionRequest request) {
        validateAdmin(request == null ? null : request.getAdminUserId());
        List<Long> orderIds = requireOrderIds(request == null ? null : request.getOrderIds());
        List<ShopOrder> orders = new ArrayList<>(shopOrderRepository.findAllById(orderIds));
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No orders were found for the selected IDs");
        }
        shopOrderRepository.deleteAll(orders);
        return orders.size();
    }

    @Transactional
    public ShopOrder updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        validateAdmin(request == null ? null : request.getAdminUserId());

        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            order.setStatus(request.getStatus().trim().toUpperCase(Locale.ENGLISH));
        }
        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            order.setPaymentStatus(request.getPaymentStatus().trim().toUpperCase(Locale.ENGLISH));
        }
        order.setAdminNote(normalizeValue(request.getAdminNote(), ""));

        ShopOrder saved = shopOrderRepository.save(order);
        sendOrderUpdateEmails(saved);
        return shopOrderRepository.save(saved);
    }

    private void sendOrderUpdateEmails(ShopOrder order) {
        if (!mailEnabled) {
            order.setStoreEmailSent(false);
            order.setCustomerEmailSent(false);
            return;
        }

        StoreSettings settings = storeSettingsService.getSettings();
        boolean storeEmailSent = false;
        boolean customerEmailSent = false;
        try {
            if (settings.getContactEmail() != null && !settings.getContactEmail().isBlank()) {
                sendStoreStatusEmail(order, settings);
                storeEmailSent = true;
            }
            if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
                sendCustomerStatusEmail(order, settings);
                customerEmailSent = true;
            }
        } catch (MailException | MessagingException ignored) {
            // Keep order updates resilient even if mail delivery is temporarily unavailable.
        }
        order.setStoreEmailSent(storeEmailSent);
        order.setCustomerEmailSent(customerEmailSent);
    }

    private void validateCheckoutRequest(OrderCheckoutRequest request, StoreSettings settings, String paymentMethod) {
        requireValue(request.getCustomerName(), "Customer name");
        requireValue(request.getCustomerEmail(), "Customer email");
        requireValue(request.getCustomerPhone(), "Contact number");
        requireValue(request.getShippingAddressLine1(), "Shipping address line 1");
        requireValue(request.getCity(), "City");
        requireValue(request.getState(), "State");
        requireValue(request.getPostalCode(), "Postal code");

        if (!"QR_PAYMENT".equals(paymentMethod) && !"CASH_ON_DELIVERY".equals(paymentMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method must be QR_PAYMENT or CASH_ON_DELIVERY");
        }
        if ("QR_PAYMENT".equals(paymentMethod) && !Boolean.TRUE.equals(request.getPaymentConfirmed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please confirm the QR payment before placing the order");
        }
        if ("QR_PAYMENT".equals(paymentMethod) && (request.getPaymentReference() == null || request.getPaymentReference().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment reference is required for QR payment");
        }
        if ("CASH_ON_DELIVERY".equals(paymentMethod) && !Boolean.TRUE.equals(settings.getCashOnDeliveryEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash on delivery is currently unavailable");
        }
    }

    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user is required");
        }
        User user = validateUser(adminUserId);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can update order status");
        }
    }

    private List<Long> requireOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one order first");
        }
        return orderIds;
    }

    private String resolveCustomerName(OrderCheckoutRequest request, User user) {
        return request.getCustomerName() != null && !request.getCustomerName().isBlank()
                ? request.getCustomerName().trim()
                : normalizeValue(user.getName(), "Customer");
    }

    private String resolveCustomerEmail(OrderCheckoutRequest request, User user) {
        return request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()
                ? request.getCustomerEmail().trim()
                : requireValue(user.getEmail(), "Customer email");
    }

    private String buildShippingAddress(OrderCheckoutRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(requireValue(request.getShippingAddressLine1(), "Shipping address line 1"));
        if (request.getShippingAddressLine2() != null && !request.getShippingAddressLine2().isBlank()) {
            builder.append("\n").append(request.getShippingAddressLine2().trim());
        }
        builder.append("\n")
                .append(requireValue(request.getCity(), "City"))
                .append(", ")
                .append(requireValue(request.getState(), "State"))
                .append(" - ")
                .append(requireValue(request.getPostalCode(), "Postal code"));
        return builder.toString();
    }

    private String formatOrderNumber(Long id) {
        return String.format(Locale.ENGLISH, "%04d-O-ID", id);
    }

    private String buildPendingOrderNumber() {
        return "PENDING-" + System.currentTimeMillis();
    }

    private String buildItemsSummary(List<CartItemResponse> items) {
        StringBuilder summary = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            CartItemResponse item = items.get(index);
            if (index > 0) {
                summary.append("\n");
            }
            summary.append(index + 1)
                    .append(". ")
                    .append(item.getName())
                    .append(" - Qty ")
                    .append(item.getQuantity())
                    .append(" - ")
                    .append(formatCurrency(item.getPrice() * item.getQuantity()));
            if (Boolean.TRUE.equals(item.getDeliveryChargeEnabled()) && item.getDeliveryChargeAmount() != null && item.getDeliveryChargeAmount() > 0) {
                summary.append(" + Shipping ").append(formatCurrency(item.getDeliveryChargeAmount() * item.getQuantity()));
            }
        }
        return summary.toString();
    }

    private void sendStoreOrderEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(settings.getContactEmail());
        helper.setReplyTo(order.getCustomerEmail());
        helper.setSubject(settings.getBrandName() + " new order " + order.getOrderNumber());
        helper.setText(buildStoreOrderEmail(order, items, settings), true);
        mailSender.send(message);
    }

    private void sendCustomerOrderEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(order.getCustomerEmail());
        if (settings.getContactEmail() != null && !settings.getContactEmail().isBlank()) {
            helper.setReplyTo(settings.getContactEmail());
        }
        helper.setSubject("Your " + settings.getBrandName() + " order " + order.getOrderNumber());
        helper.setText(buildCustomerOrderEmail(order, items, settings), true);
        mailSender.send(message);
    }

    private void sendStoreStatusEmail(ShopOrder order, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(settings.getContactEmail());
        helper.setSubject(settings.getBrandName() + " order update " + order.getOrderNumber());
        helper.setText(buildStoreStatusEmail(order, settings), true);
        mailSender.send(message);
    }

    private void sendCustomerStatusEmail(ShopOrder order, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(order.getCustomerEmail());
        if (settings.getContactEmail() != null && !settings.getContactEmail().isBlank()) {
            helper.setReplyTo(settings.getContactEmail());
        }
        helper.setSubject("Order update for " + order.getOrderNumber());
        helper.setText(buildCustomerStatusEmail(order, settings), true);
        mailSender.send(message);
    }

    private String buildStoreOrderEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#b45309;text-transform:uppercase;">New Order</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">Order %s</h2>
                    <div style="margin-bottom:16px;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Customer:</strong> %s<br>
                      <strong>Email:</strong> %s<br>
                      <strong>Phone:</strong> %s<br>
                      <strong>Payment:</strong> %s (%s)<br>
                      <strong>Items:</strong> %s<br>
                      <strong>Subtotal:</strong> %s<br>
                      <strong>Shipping:</strong> %s<br>
                      <strong>Total:</strong> %s
                    </div>
                    <div style="margin-bottom:14px;"><strong>Shipping address</strong><div style="white-space:pre-wrap;">%s</div></div>
                    <div style="margin-bottom:14px;"><strong>Customer note</strong><div>%s</div></div>
                    <div style="white-space:pre-wrap;">%s</div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(order.getCustomerEmail()),
                escapeHtml(order.getCustomerPhone()),
                escapeHtml(readablePaymentMethod(order.getPaymentMethod())),
                escapeHtml(order.getPaymentReference().isBlank() ? order.getPaymentStatus() : order.getPaymentStatus() + " • " + order.getPaymentReference()),
                order.getItemCount(),
                escapeHtml(formatCurrency(order.getSubtotalAmount())),
                escapeHtml(formatCurrency(order.getShippingAmount())),
                escapeHtml(formatCurrency(order.getTotalAmount())),
                escapeHtml(order.getShippingAddress()),
                escapeHtml(normalizeValue(order.getCustomerNote(), "No customer note shared.")),
                escapeHtml(buildItemsSummary(items))
        );
    }

    private String buildCustomerOrderEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#1d4ed8;text-transform:uppercase;">Order Confirmed</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">Your order %s is confirmed</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for shopping with %s. We have received your order successfully.</p>
                    <div style="margin:16px 0;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Order ID:</strong> %s<br>
                      <strong>Payment:</strong> %s (%s)<br>
                      <strong>Items:</strong> %s<br>
                      <strong>Subtotal:</strong> %s<br>
                      <strong>Shipping:</strong> %s<br>
                      <strong>Total:</strong> %s
                    </div>
                    <div style="margin-bottom:14px;"><strong>Shipping address</strong><div style="white-space:pre-wrap;">%s</div></div>
                    <div style="white-space:pre-wrap;margin-bottom:16px;">%s</div>
                    <p>Support email: <a href="mailto:%s">%s</a></p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(settings.getBrandName()),
                escapeHtml(order.getOrderNumber()),
                escapeHtml(readablePaymentMethod(order.getPaymentMethod())),
                escapeHtml(order.getPaymentReference().isBlank() ? order.getPaymentStatus() : order.getPaymentStatus() + " • " + order.getPaymentReference()),
                order.getItemCount(),
                escapeHtml(formatCurrency(order.getSubtotalAmount())),
                escapeHtml(formatCurrency(order.getShippingAmount())),
                escapeHtml(formatCurrency(order.getTotalAmount())),
                escapeHtml(order.getShippingAddress()),
                escapeHtml(buildItemsSummary(items)),
                escapeHtml(settings.getContactEmail()),
                escapeHtml(settings.getContactEmail())
        );
    }

    private String buildStoreStatusEmail(ShopOrder order, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#b45309;text-transform:uppercase;">Order Update</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">%s</h2>
                    <p>Order status was updated to <strong>%s</strong>.</p>
                    <p>Payment status: <strong>%s</strong></p>
                    <p>Customer: <strong>%s</strong> (%s)</p>
                    <p>Admin note: %s</p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getStatus()),
                escapeHtml(order.getPaymentStatus()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(order.getCustomerEmail()),
                escapeHtml(normalizeValue(order.getAdminNote(), "No admin note shared."))
        );
    }

    private String buildCustomerStatusEmail(ShopOrder order, StoreSettings settings) {
        String headline = switch (normalizeValue(order.getStatus(), "").toUpperCase(Locale.ENGLISH)) {
            case "PAYMENT_REVIEW" -> "We are reviewing your payment update.";
            case "CONFIRMED" -> "Your order is confirmed and queued for fulfillment.";
            case "PACKING" -> "Your order is being packed right now.";
            case "SHIPPED" -> "Your order has been shipped.";
            case "OUT_FOR_DELIVERY" -> "Your order is out for delivery.";
            case "DELIVERED" -> "Your order was marked as delivered.";
            case "RETURN_REQUESTED" -> "Your return request is now being processed.";
            case "RETURNED" -> "Your order return has been completed.";
            case "CANCELLED" -> "Your order was cancelled.";
            default -> "There is a new update for your order.";
        };
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#1d4ed8;text-transform:uppercase;">Order Status Update</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">%s</h2>
                    <p>Hello %s, %s</p>
                    <p>Your order is now <strong>%s</strong>.</p>
                    <p>Payment status: <strong>%s</strong></p>
                    <p>Shipping address:</p>
                    <div style="white-space:pre-wrap;margin-bottom:14px;">%s</div>
                    <p>Admin update: %s</p>
                    <p>If you need help, reply to <a href="mailto:%s">%s</a>.</p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(headline),
                escapeHtml(order.getStatus()),
                escapeHtml(order.getPaymentStatus()),
                escapeHtml(order.getShippingAddress()),
                escapeHtml(normalizeValue(order.getAdminNote(), "No additional note shared.")),
                escapeHtml(settings.getContactEmail()),
                escapeHtml(settings.getContactEmail())
        );
    }

    private String readablePaymentMethod(String paymentMethod) {
        return "QR_PAYMENT".equalsIgnoreCase(paymentMethod) ? "QR payment" : "Cash on delivery";
    }

    private String formatCurrency(Double amount) {
        return String.format(Locale.ENGLISH, "Rs.%.0f", amount == null ? 0 : amount);
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        return value.trim();
    }

    private String normalizeValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
