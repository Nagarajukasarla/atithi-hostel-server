package com.atithi.aavas.publicregistration.implementations;

import com.atithi.aavas.publicregistration.services.QrService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class QrServiceImpl implements QrService {

    private static final int DEFAULT_SIZE = 300;

    @Override
    public String generateSvg(String content) {
        return generateSvg(content, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Override
    public String generateSvg(String content, int width, int height) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content for QR code generation cannot be null or blank");
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int matrixWidth = bitMatrix.getWidth();
            int matrixHeight = bitMatrix.getHeight();

            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"%d\" height=\"%d\" shape-rendering=\"crispEdges\">\n",
                    matrixWidth, matrixHeight, width, height));
            svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
            svg.append("  <path fill=\"#000000\" d=\"");

            for (int y = 0; y < matrixHeight; y++) {
                for (int x = 0; x < matrixWidth; x++) {
                    if (bitMatrix.get(x, y)) {
                        svg.append(String.format("M%d,%dh1v1h-1z ", x, y));
                    }
                }
            }
            svg.append("\"/>\n");
            svg.append("</svg>");

            return svg.toString();
        } catch (WriterException e) {
            log.error("Failed to generate QR code SVG for content: {}", content, e);
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
