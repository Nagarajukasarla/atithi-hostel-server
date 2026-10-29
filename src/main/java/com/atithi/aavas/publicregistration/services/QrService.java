package com.atithi.aavas.publicregistration.services;

public interface QrService {
    String generateSvg(String content, int width, int height);
    String generateSvg(String content);
}
