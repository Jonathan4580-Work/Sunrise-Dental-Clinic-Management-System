package com.mycompany.sunrisedentalclinicmanagementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.sunrisedentalclinicmanagementsystem.model.Bill;
import com.mycompany.sunrisedentalclinicmanagementsystem.model.GeneratedBill;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReceiptEmailServiceTest {

    private static final String API_KEY = "test-api-key";
    private static final String SENDER
            = "Sunrise Dental Clinic <onboarding@resend.dev>";
    private static final String BILL_NUMBER = "BIL-20260901-ABC2345";
    private static final String RECEIPT_TEXT = "FINAL RECEIPT\nTotal: LKR 5,000.00";

    @Test
    void normalizesValidRecipientEmail() throws Exception {
        ReceiptEmailService service = serviceReturning(202);

        assertEquals(
                "patient@example.com",
                service.normalizeRecipientEmail("  Patient@Example.COM  ")
        );
    }

    @Test
    void rejectsEmptyRecipientEmail() {
        ReceiptEmailService service = serviceReturning(202);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt("   ", generatedBill(), RECEIPT_TEXT)
        );

        assertEquals(
                ReceiptEmailException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsInvalidRecipientEmail() {
        ReceiptEmailService service = serviceReturning(202);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "not-an-email",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsNullGeneratedBill() {
        ReceiptEmailService service = serviceReturning(202);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        null,
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsBlankFinalReceiptText() {
        ReceiptEmailService service = serviceReturning(202);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        "  \n "
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.VALIDATION,
                exception.getReason()
        );
    }

    @Test
    void rejectsMissingApiKey() {
        ReceiptEmailService service = new ReceiptEmailService(
                null,
                SENDER,
                request -> 202
        );

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.NOT_CONFIGURED,
                exception.getReason()
        );
    }

    @Test
    void rejectsMissingSender() {
        ReceiptEmailService service = new ReceiptEmailService(
                API_KEY,
                " ",
                request -> 202
        );

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.NOT_CONFIGURED,
                exception.getReason()
        );
    }

    @Test
    void acceptsSuccessfulProviderResponse() throws Exception {
        AtomicReference<HttpRequest> capturedRequest = new AtomicReference<>();
        ReceiptEmailService service = new ReceiptEmailService(
                API_KEY,
                SENDER,
                request -> {
                    capturedRequest.set(request);
                    return 202;
                }
        );

        service.sendReceipt(
                "patient@example.com",
                generatedBill(),
                RECEIPT_TEXT
        );

        HttpRequest request = capturedRequest.get();
        assertEquals("POST", request.method());
        assertEquals("https://api.resend.com/emails", request.uri().toString());
        assertEquals(
                "application/json",
                request.headers().firstValue("Content-Type").orElseThrow()
        );
        assertEquals(
                "Bearer " + API_KEY,
                request.headers().firstValue("Authorization").orElseThrow()
        );
    }

    @Test
    void categorizesAuthenticationFailure() {
        ReceiptEmailService service = serviceReturning(401);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.AUTHENTICATION_FAILURE,
                exception.getReason()
        );
    }

    @Test
    void categorizesProviderRejection() {
        ReceiptEmailService service = serviceReturning(422);

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.PROVIDER_REJECTED,
                exception.getReason()
        );
    }

    @Test
    void categorizesNetworkFailure() {
        ReceiptEmailService service = new ReceiptEmailService(
                API_KEY,
                SENDER,
                request -> {
                    throw new IOException("Simulated connection failure");
                }
        );

        ReceiptEmailException exception = assertThrows(
                ReceiptEmailException.class,
                () -> service.sendReceipt(
                        "patient@example.com",
                        generatedBill(),
                        RECEIPT_TEXT
                )
        );

        assertEquals(
                ReceiptEmailException.Reason.NETWORK_FAILURE,
                exception.getReason()
        );
    }

    @Test
    void subjectContainsGeneratedBillNumber() {
        ReceiptEmailService service = serviceReturning(202);

        assertEquals(
                "Sunrise Dental Clinic Receipt - " + BILL_NUMBER,
                service.buildSubject(BILL_NUMBER)
        );
    }

    @Test
    void escapesReceiptTextForJson() {
        ReceiptEmailService service = serviceReturning(202);
        String payload = service.buildJsonPayload(
                SENDER,
                "patient@example.com",
                "Receipt \"Special\"",
                "Line 1\nLine 2\r\nTab\tSlash \\ Quote \"\b\f\u0001"
        );

        assertTrue(payload.contains("Receipt \\\"Special\\\""));
        assertTrue(payload.contains("Line 1\\nLine 2\\r\\n"));
        assertTrue(payload.contains("Tab\\tSlash \\\\"));
        assertTrue(payload.contains("Quote \\\"\\b\\f\\u0001"));
    }

    private ReceiptEmailService serviceReturning(int statusCode) {
        return new ReceiptEmailService(
                API_KEY,
                SENDER,
                request -> statusCode
        );
    }

    private GeneratedBill generatedBill() {
        Bill bill = new Bill();
        bill.setBillNumber(BILL_NUMBER);
        return new GeneratedBill(bill, null);
    }
}
