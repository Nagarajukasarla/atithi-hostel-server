package com.atithi.aavas.publicregistration;

import com.atithi.aavas.publicregistration.implementations.QrServiceImpl;
import com.atithi.aavas.publicregistration.services.QrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QrServiceTest {

    private QrService qrService;

    @BeforeEach
    void setUp() {
        qrService = new QrServiceImpl();
    }

    @Test
    @DisplayName("Should generate valid SVG string for given URL")
    void testGenerateSvgSuccess() {
        String url = "http://localhost:5173/public/customer-registration/8f4a7c9e2d3e4b119a0f7c2b5d4e6f1a";
        String svg = qrService.generateSvg(url);

        assertNotNull(svg);
        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""));
        assertTrue(svg.contains("shape-rendering=\"crispEdges\""));
        assertTrue(svg.contains("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"));
        assertTrue(svg.contains("<path fill=\"#000000\""));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when content is null or blank")
    void testGenerateSvgBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> qrService.generateSvg(null));
        assertThrows(IllegalArgumentException.class, () -> qrService.generateSvg("   "));
    }
}
