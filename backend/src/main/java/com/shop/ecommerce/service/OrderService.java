package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.CartItemResponse;
import com.shop.ecommerce.dto.OrderCheckoutRequest;
import com.shop.ecommerce.dto.OrderCheckoutResponse;
import com.shop.ecommerce.model.ShopOrder;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ShopOrderRepository;
import com.shop.ecommerce.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
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

    public OrderCheckoutResponse placeOrder(OrderCheckoutRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required to place an order");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<CartItemResponse> cartItems = cartService.getCart(user.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        StoreSettings settings = storeSettingsService.getSettings();
        ShopOrder order = new ShopOrder();
        order.setUserId(user.getId());
        order.setCustomerName(user.getName() == null || user.getName().isBlank() ? "Customer" : user.getName().trim());
        order.setCustomerEmail(user.getEmail());
        order.setStatus("PLACED");
        order.setItemCount(cartItems.stream().mapToInt(CartItemResponse::getQuantity).sum());
        order.setTotalAmount(cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum());
        order.setItemsSummary(buildItemsSummary(cartItems));
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
                    sendStoreEmail(saved, cartItems, settings);
                    storeEmailSent = true;
                }
                if (saved.getCustomerEmail() != null && !saved.getCustomerEmail().isBlank()) {
                    sendCustomerEmail(saved, cartItems, settings);
                    customerEmailSent = true;
                }
                message = customerEmailSent
                        ? "Order placed successfully. A confirmation email has been sent to the customer."
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
                saved.isStoreEmailSent(),
                saved.isCustomerEmailSent(),
                message
        );
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
        }
        return summary.toString();
    }

    private void sendStoreEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(settings.getContactEmail());
        helper.setReplyTo(order.getCustomerEmail());
        helper.setSubject(settings.getBrandName() + " new order " + order.getOrderNumber());
        helper.setText(buildStoreOrderEmail(order, items), true);
        mailSender.send(message);
    }

    private void sendCustomerEmail(ShopOrder order, List<CartItemResponse> items, StoreSettings settings) throws MessagingException {
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

    private String buildStoreOrderEmail(ShopOrder order, List<CartItemResponse> items) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#b45309;text-transform:uppercase;">New Order</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">Order %s</h2>
                    <div style="margin-bottom:16px;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Customer:</strong> %s<br>
                      <strong>Email:</strong> %s<br>
                      <strong>Items:</strong> %s<br>
                      <strong>Total:</strong> %s
                    </div>
                    <div style="white-space:pre-wrap;">%s</div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(order.getCustomerEmail()),
                order.getItemCount(),
                escapeHtml(formatCurrency(order.getTotalAmount())),
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
                      <strong>Items:</strong> %s<br>
                      <strong>Total:</strong> %s
                    </div>
                    <div style="white-space:pre-wrap;margin-bottom:16px;">%s</div>
                    <p>Support email: <a href="mailto:%s">%s</a></p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(order.getOrderNumber()),
                escapeHtml(order.getCustomerName()),
                escapeHtml(settings.getBrandName()),
                escapeHtml(order.getOrderNumber()),
                order.getItemCount(),
                escapeHtml(formatCurrency(order.getTotalAmount())),
                escapeHtml(buildItemsSummary(items)),
                escapeHtml(settings.getContactEmail()),
                escapeHtml(settings.getContactEmail())
        );
    }

    private String formatCurrency(Double amount) {
        return String.format(Locale.ENGLISH, "Rs.%.0f", amount == null ? 0 : amount);
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
