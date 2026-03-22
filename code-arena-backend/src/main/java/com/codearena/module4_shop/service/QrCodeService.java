package com.codearena.module4_shop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class QrCodeService {

    // Generate QR code as Base64 string
    public String generateQrCode(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (WriterException | IOException e) {
            log.error("QR generation failed: {}", e.getMessage());
            return null;
        }
    }

    // Generate order QR — encodes order details
    public String generateOrderQr(String orderId, String participantId, Double total) {
        String content = String.format(
                "CODEARENA-ORDER\nID: %s\nPARTICIPANT: %s\nTOTAL: $%.2f",
                orderId.substring(0, 8).toUpperCase(),
                participantId,
                total
        );
        return generateQrCode(content, 200, 200);
    }
}