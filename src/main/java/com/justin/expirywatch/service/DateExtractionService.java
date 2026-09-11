package com.justin.expirywatch.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DateExtractionService {

    private final Tesseract tesseract;

    public DateExtractionService() {
        this.tesseract = new Tesseract();
        // Point to the local tessdata directory we just created
        this.tesseract.setDatapath(new File("tessdata").getAbsolutePath());
    }

    public static class ExtractResult {
        public String rawText;
        public LocalDate date;
        public BigDecimal confidenceScore;

        public ExtractResult(String rawText, LocalDate date, BigDecimal confidenceScore) {
            this.rawText = rawText;
            this.date = date;
            this.confidenceScore = confidenceScore;
        }
    }

    public ExtractResult extractDateFromImage(MultipartFile file) throws IOException {
        String rawText = "";
        
        File tempFile = File.createTempFile("ocr_", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        try {
            rawText = tesseract.doOCR(tempFile);
        } catch (TesseractException e) {
            System.err.println("OCR Error: " + e.getMessage());
            // return empty result if OCR fails
            return new ExtractResult(null, null, BigDecimal.ZERO);
        } finally {
            tempFile.delete();
        }

        return findBestDate(rawText);
    }

    private ExtractResult findBestDate(String rawText) {
        if (rawText == null || rawText.isBlank()) return new ExtractResult(rawText, null, BigDecimal.ZERO);
        
        String lowerText = rawText.toLowerCase();
        
        // Regex patterns for dd/mm/yyyy, dd-mm-yyyy, yyyy-mm-dd
        Pattern datePattern = Pattern.compile("\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\b|\\b(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\b");
        Matcher matcher = datePattern.matcher(rawText);
        
        LocalDate bestDate = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        String[] keywords = {"expiry", "valid until", "validity", "expires", "renewal", "due"};

        while (matcher.find()) {
            String dateStr = matcher.group();
            LocalDate parsedDate = tryParseDate(dateStr);
            if (parsedDate != null) {
                // Calculate proximity score
                int index = matcher.start();
                BigDecimal score = BigDecimal.valueOf(0.1); // Base score for just being a date
                
                for (String keyword : keywords) {
                    int kwIndex = lowerText.lastIndexOf(keyword, index);
                    if (kwIndex != -1) {
                        int distance = index - (kwIndex + keyword.length());
                        if (distance > 0 && distance < 50) {
                            score = score.add(BigDecimal.valueOf(0.5)); // High confidence if near a keyword
                        } else if (distance >= 50 && distance < 200) {
                            score = score.add(BigDecimal.valueOf(0.2));
                        }
                    }
                }

                if (score.compareTo(bestScore) > 0) {
                    bestScore = score;
                    bestDate = parsedDate;
                }
            }
        }

        return new ExtractResult(rawText, bestDate, bestScore);
    }

    private LocalDate tryParseDate(String dateStr) {
        String[] patterns = {
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy", "dd/MM/yy"
        };
        for (String p : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
