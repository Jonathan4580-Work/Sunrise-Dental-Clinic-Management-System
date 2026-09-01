package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.GeneratedBill;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Sends a previously generated final receipt through the Resend REST API.
 *
 * <p>Emailing is deliberately separate from bill creation. A delivery
 * failure therefore cannot roll back, duplicate, or otherwise modify an
 * already committed bill.</p>
 */
public final class ReceiptEmailService {

    private static final String API_KEY_ENVIRONMENT_VARIABLE
            = "SUNRISE_EMAIL_API_KEY";
    private static final String SENDER_ENVIRONMENT_VARIABLE
            = "SUNRISE_EMAIL_FROM";
    private static final URI EMAIL_ENDPOINT
            = URI.create("https://api.resend.com/emails");
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_EMAIL_ADDRESS_LENGTH = 254;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
                    + "[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
                    + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
            Pattern.CASE_INSENSITIVE
    );

    private final String apiKey;
    private final String sender;
    private final HttpTransport httpTransport;

    /**
     * Creates the production email service using Windows/user environment
     * configuration and Java's built-in HTTP client.
     */
    public ReceiptEmailService() {
        this(
                System.getenv(API_KEY_ENVIRONMENT_VARIABLE),
                System.getenv(SENDER_ENVIRONMENT_VARIABLE),
                createDefaultTransport()
        );
    }

    ReceiptEmailService(
            String apiKey,
            String sender,
            HttpTransport httpTransport
    ) {
        this.apiKey = normalizeConfigurationValue(apiKey);
        this.sender = normalizeConfigurationValue(sender);
        this.httpTransport = Objects.requireNonNull(
                httpTransport,
                "httpTransport must not be null"
        );
    }

    /**
     * Sends the authoritative final receipt for a stored bill.
     *
     * @param recipientEmail destination entered by clinic staff
     * @param generatedBill stored bill and its authoritative calculation
     * @param finalReceiptText final receipt text produced after bill creation
     * @throws ReceiptEmailException if validation, configuration, transport,
     *         or provider acceptance fails
     */
    public void sendReceipt(
            String recipientEmail,
            GeneratedBill generatedBill,
            String finalReceiptText
    ) throws ReceiptEmailException {
        String normalizedRecipient = normalizeRecipientEmail(recipientEmail);
        String billNumber = validateGeneratedBill(generatedBill);
        String receiptText = validateReceiptText(finalReceiptText);
        validateConfiguration();

        String subject = buildSubject(billNumber);
        String requestBody = buildJsonPayload(
                sender,
                normalizedRecipient,
                subject,
                receiptText
        );
        HttpRequest request = HttpRequest.newBuilder(EMAIL_ENDPOINT)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        final int statusCode;
        try {
            statusCode = httpTransport.send(request);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw networkFailure(exception);
        } catch (IOException exception) {
            throw networkFailure(exception);
        }

        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        if (statusCode == 401 || statusCode == 403) {
            throw new ReceiptEmailException(
                    ReceiptEmailException.Reason.AUTHENTICATION_FAILURE,
                    "The email service could not authenticate. Please ask an "
                            + "administrator to check this computer's email configuration."
            );
        }
        throw new ReceiptEmailException(
                ReceiptEmailException.Reason.PROVIDER_REJECTED,
                "The email provider did not accept the receipt. Please check "
                        + "the recipient address and try again."
        );
    }

    String normalizeRecipientEmail(String recipientEmail)
            throws ReceiptEmailException {
        if (recipientEmail == null) {
            throw validationError("Recipient email is required.");
        }

        String normalized = recipientEmail.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw validationError("Recipient email is required.");
        }
        if (normalized.length() > MAX_EMAIL_ADDRESS_LENGTH
                || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw validationError(
                    "Enter a valid recipient email address, such as name@example.com."
            );
        }
        return normalized;
    }

    String buildJsonPayload(
            String from,
            String to,
            String subject,
            String text
    ) {
        return "{"
                + "\"from\":\"" + jsonEscape(from) + "\","
                + "\"to\":\"" + jsonEscape(to) + "\","
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"text\":\"" + jsonEscape(text) + "\""
                + "}";
    }

    String buildSubject(String billNumber) {
        return "Sunrise Dental Clinic Receipt - " + billNumber;
    }

    String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format(
                                Locale.ROOT,
                                "\\u%04x",
                                (int) character
                        ));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String validateGeneratedBill(GeneratedBill generatedBill)
            throws ReceiptEmailException {
        if (generatedBill == null || generatedBill.bill() == null) {
            throw validationError(
                    "Generate the bill before emailing its receipt."
            );
        }

        String billNumber = generatedBill.bill().getBillNumber();
        if (billNumber == null || billNumber.isBlank()) {
            throw validationError(
                    "The generated bill does not contain a valid bill number."
            );
        }
        return billNumber.strip();
    }

    private String validateReceiptText(String finalReceiptText)
            throws ReceiptEmailException {
        if (finalReceiptText == null || finalReceiptText.isBlank()) {
            throw validationError(
                    "The final receipt is unavailable. Generate the bill again before emailing."
            );
        }
        return finalReceiptText;
    }

    private void validateConfiguration() throws ReceiptEmailException {
        if (apiKey == null || sender == null) {
            throw new ReceiptEmailException(
                    ReceiptEmailException.Reason.NOT_CONFIGURED,
                    "Email service is not configured on this computer."
            );
        }
    }

    private ReceiptEmailException validationError(String message) {
        return new ReceiptEmailException(
                ReceiptEmailException.Reason.VALIDATION,
                message
        );
    }

    private ReceiptEmailException networkFailure(Throwable cause) {
        return new ReceiptEmailException(
                ReceiptEmailException.Reason.NETWORK_FAILURE,
                "The receipt could not be emailed because the email service "
                        + "could not be reached. Check the internet connection and try again.",
                cause
        );
    }

    private static String normalizeConfigurationValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static HttpTransport createDefaultTransport() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .build();
        return request -> httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        ).statusCode();
    }

    /**
     * Minimal test seam that avoids a real network call in unit tests.
     */
    @FunctionalInterface
    interface HttpTransport {

        int send(HttpRequest request) throws IOException, InterruptedException;
    }
}
