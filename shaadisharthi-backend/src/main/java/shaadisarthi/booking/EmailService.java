package shaadisarthi.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Properties;

import shaadisharthi.utils.ConfigUtil;

/**
 * SERVICE: EmailService
 * 
 * PURPOSE: Centralized email notification service for booking lifecycle events
 * Comprehensive email automation with:
 * - Provider notifications for new bookings
 * - Customer notifications for booking status changes
 * - Cancellation and completion confirmations
 * - Configurable SMTP settings with TLS security
 * - Environment-based credential management
 * 
 * ARCHITECTURE: Singleton-style service class
 * SECURITY: TLS-enabled SMTP with configurable credentials
 * TEMPLATES: Structured email templates for different booking events
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String APP_BASE_URL = ConfigUtil.get("app.base.url", "APP_BASE_URL");
    

    /**
     * Sends new booking notification to service provider
     * 
     * @param providerEmail Provider's email address
     * @param serviceName Name of the booked service
     * @param customerName Customer's name
     * @param phone Customer's phone number
     * @param notes Additional customer notes
     * @param bookingId Unique booking identifier
     * @param startDateTime Service start date and time
     * @param servicePrice Total service price
     */
    public void sendProviderNotification(String providerEmail, String serviceName, String customerName, String phone, String notes, int bookingId, LocalDateTime startDateTime, double servicePrice) {
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "New Booking Request on ShaadiSarthi";
        String body = String.format(
                "Dear Provider,\n\n" +
                "You have a new booking request:\n" +
                "Service: %s\n" +
                "Customer: %s (%s)\n" +
                "Booking ID: %d\n" +
                "Start Date/Time: %s\n" +
                "Price: %.2f\n" + 
                "Note:%s\n\n" +
                "Please log in to your account to accept or reject this booking.\n" +
                "Login here: %s\n\n" +
                "Best regards,\nShaadiSarthi Team",
                serviceName, customerName, phone, bookingId, startDateTime.toString(), servicePrice, notes, APP_BASE_URL + "/provider"
        );

        try {
            Message message = new MimeMessage(session);
            if (APP_BASE_URL == null) {
                logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL.");
                return;
            }
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(providerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Email sent successfully to {} for booking_id: {} at {}", providerEmail, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send email to {} for booking_id {}: {} at {}", providerEmail, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Sends booking rejection notification to customer
     * 
     * @param customerEmail Customer's email address
     * @param bookingId Unique booking identifier
     * @param reason Reason for rejection
     */
    public void sendCustomerRejection(String customerEmail, int bookingId, String reason) {
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "Booking Rejection on ShaadiSarthi";
        String body = String.format(
                "Dear %s,\n\n" +
                "Your booking request (Booking ID: %d) has been rejected.\n" +
                "Reason: %s\n\n" +
                "If you have any questions, please contact support.\n" +
                "Best regards,\nShaadiSarthi Team",
                customerEmail.split("@")[0], bookingId, reason.isEmpty() ? "No reason provided" : reason
        );

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(customerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Rejection email sent successfully to {} for booking_id: {} at {}", customerEmail, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send rejection email to {} for booking_id {}: {} at {}", customerEmail, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Sends booking acceptance confirmation to customer
     * 
     * @param customerEmail Customer's email address
     * @param serviceName Name of the accepted service
     * @param bookingId Unique booking identifier
     * @param startDateTime Service start date and time
     * @param servicePrice Total service price
     */
    public void sendCustomerAcceptance(String customerEmail, String serviceName, int bookingId, LocalDateTime startDateTime, double servicePrice) {
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "Booking Accepted on ShaadiSarthi";
        String body = String.format(
                "Dear %s,\n\n" +
                "Your booking request (Booking ID: %d) has been accepted!\n" +
                "Details:\n" +
                "Service: %s\n" +
                "Start Date/Time: %s\n" +
                "Price: %.2f\n\n" +
                "You can view your booking details here: %s\n\n" +
                "Please prepare accordingly. Contact support if you have questions.\n" +
                "Best regards,\nShaadiSarthi Team",
                customerEmail.split("@")[0], bookingId, serviceName, startDateTime.toString(), servicePrice, APP_BASE_URL + "/customer/bookings"
        );

        try {
            if (APP_BASE_URL == null) {
                logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL.");
                return;
            }
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(customerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Acceptance email sent successfully to {} for booking_id: {} at {}", customerEmail, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send acceptance email to {} for booking_id {}: {} at {}", customerEmail, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Sends cancellation notification to service provider
     * 
     * @param providerEmail Provider's email address
     * @param bookingId Unique booking identifier
     * @param reason Cancellation reason
     * @param originalStatus Booking status before cancellation
     */
    public void sendProviderCancellation(String providerEmail, int bookingId, String reason, String originalStatus) {
        if (providerEmail == null) return;

        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "Booking Cancellation on ShaadiSarthi";
        String body = String.format(
            "Dear Provider,\n\n" +
            "A booking (ID: %d) has been cancelled by the customer.\n" +
            "Original Status: %s\n" +
            "Reason: %s\n\n" +
            "Best regards,\nShaadiSarthi Team",
            bookingId, originalStatus != null ? originalStatus : "Unknown", reason.isEmpty() ? "No reason provided" : reason
        );

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(providerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Cancellation email sent successfully to {} with status for booking_id: {} at {}", providerEmail, originalStatus, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send Cancellation email to {} with status for booking_id {}: {} at {}", providerEmail, originalStatus, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Sends service completion confirmation to customer
     * 
     * @param customerEmail Customer's email address
     * @param serviceName Name of the completed service
     * @param bookingId Unique booking identifier
     * @param startDateTime Original service start date and time
     * @param servicePrice Total service price
     */
    public void sendCustomerCompletion(String customerEmail, String serviceName, int bookingId, LocalDateTime startDateTime, double servicePrice) {
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "Service Completed on ShaadiSarthi";
        String body = String.format(
                "Dear %s,\n\n" +
                "Your service has been marked as completed!\n" +
                "Booking Details:\n" +
                "Service: %s\n" +
                "Booking ID: %d\n" +
                "Service Date/Time: %s\n" +
                "Total Amount: %.2f\n\n" +
                "Thank you for using ShaadiSarthi. We hope you had a great experience!\n\n" +
                "Best regards,\nShaadiSarthi Team",
                customerEmail.split("@")[0], serviceName, bookingId, startDateTime.toString(), servicePrice
        );

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(customerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Completion email sent successfully to {} for booking_id: {} at {}", customerEmail, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send completion email to {} for booking_id {}: {} at {}", customerEmail, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }

    /**
     * Sends provider-initiated cancellation notification to customer
     * 
     * @param customerEmail Customer's email address
     * @param serviceName Name of the cancelled service
     * @param bookingId Unique booking identifier
     * @param startDateTime Original service start date and time
     * @param reason Cancellation reason
     */
    public void sendCustomerCancellation(String customerEmail, String serviceName, int bookingId, LocalDateTime startDateTime, String reason) {
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {}", LocalDateTime.now());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        String subject = "Booking Cancelled by Provider on ShaadiSarthi";
        String body = String.format(
                "Dear %s,\n\n" +
                "We regret to inform you that your booking has been cancelled by the service provider.\n\n" +
                "Booking Details:\n" +
                "Service: %s\n" +
                "Booking ID: %d\n" +
                "Scheduled Date/Time: %s\n" +
                "Cancellation Reason: %s\n\n" +
                "If you have already made any payment, it will be refunded within 5-7 business days.\n" +
                "We apologize for any inconvenience caused.\n\n" +
                "Best regards,\nShaadiSarthi Team",
                customerEmail.split("@")[0], serviceName, bookingId, startDateTime.toString(),
                reason.isEmpty() ? "No reason provided" : reason
        );

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(customerEmail));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Cancellation email sent successfully to {} for booking_id: {} at {}", customerEmail, bookingId, LocalDateTime.now());
        } catch (MessagingException e) {
            logger.error("Failed to send cancellation email to {} for booking_id {}: {} at {}", customerEmail, bookingId, e.getMessage(), LocalDateTime.now());
        }
    }
}