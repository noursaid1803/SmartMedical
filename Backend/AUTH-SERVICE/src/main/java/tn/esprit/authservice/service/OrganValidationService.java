package tn.esprit.authservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.authservice.dto.OrganValidationResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

@Service
public class OrganValidationService {

    private static final double MIN_ACCEPT_CONFIDENCE = 55.0;
    private static final double MIN_MARGIN = 8.0;

    private static final Map<String, String> ORGAN_NAMES = Map.of(
            "cerveau", "Cerveau",
            "sein", "Sein",
            "peau", "Peau",
            "oeil", "Œil",
            "poumon", "Poumon",
            "foie", "Foie",
            "coeur", "Cœur"
    );

    public OrganValidationResponse validate(MultipartFile file, String expectedOrgan) throws IOException {
        if (expectedOrgan == null || !ORGAN_NAMES.containsKey(expectedOrgan)) {
            return OrganValidationResponse.builder()
                    .success(false)
                    .valid(false)
                    .reason("Organe attendu invalide")
                    .build();
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            return OrganValidationResponse.builder()
                    .success(false)
                    .valid(false)
                    .reason("Fichier image invalide")
                    .build();
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        for (String organ : ORGAN_NAMES.keySet()) {
            scores.put(organ, scoreOrgan(image, organ));
        }

        List<Map.Entry<String, Double>> ranked = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();

        String bestOrgan = ranked.get(0).getKey();
        double bestConf = ranked.get(0).getValue();
        double expectedConf = scores.get(expectedOrgan);
        double runnerUp = ranked.size() > 1 ? ranked.get(1).getValue() : 0.0;

        String expectedName = ORGAN_NAMES.get(expectedOrgan);
        String bestName = ORGAN_NAMES.get(bestOrgan);

        if (bestOrgan.equals(expectedOrgan)
                && expectedConf >= MIN_ACCEPT_CONFIDENCE
                && (expectedConf - runnerUp) >= MIN_MARGIN) {
            return buildResponse(true, expectedOrgan, expectedOrgan, expectedConf, null, scores, "auth-fallback");
        }

        if (!bestOrgan.equals(expectedOrgan) && bestConf >= MIN_ACCEPT_CONFIDENCE) {
            String reason = String.format(
                    "Image incompatible : %s détecté (%.0f%%) au lieu de %s (%.0f%%). Veuillez importer une image de %s.",
                    bestName, bestConf, expectedName, expectedConf, expectedName.toLowerCase()
            );
            return buildResponse(false, expectedOrgan, bestOrgan, bestConf, reason, scores, "auth-fallback");
        }

        String reason = String.format(
                "Image refusée pour %s : confiance insuffisante (%.0f%%, minimum %.0f%%).",
                expectedName, expectedConf, MIN_ACCEPT_CONFIDENCE
        );
        String detected = bestConf >= MIN_ACCEPT_CONFIDENCE ? bestOrgan : "unknown";
        double conf = detected.equals("unknown") ? expectedConf : bestConf;
        return buildResponse(false, expectedOrgan, detected, conf, reason, scores, "auth-fallback");
    }

    private OrganValidationResponse buildResponse(
            boolean valid,
            String expectedOrgan,
            String detectedOrgan,
            double confidence,
            String reason,
            Map<String, Double> scores,
            String method
    ) {
        String detectedName = "unknown".equals(detectedOrgan)
                ? "Inconnu"
                : ORGAN_NAMES.getOrDefault(detectedOrgan, detectedOrgan);

        return OrganValidationResponse.builder()
                .success(true)
                .valid(valid)
                .organ(detectedOrgan)
                .organName(detectedName)
                .confidence(Math.round(confidence * 100.0) / 100.0)
                .expectedOrgan(expectedOrgan)
                .expectedOrganName(ORGAN_NAMES.get(expectedOrgan))
                .reason(reason)
                .allScores(scores)
                .method(method)
                .build();
    }

    private double scoreOrgan(BufferedImage image, String organ) {
        return switch (organ) {
            case "cerveau" -> scoreBrain(image);
            case "poumon" -> scoreLung(image);
            case "coeur" -> scoreHeart(image);
            case "foie" -> scoreLiver(image);
            case "sein" -> scoreBreast(image);
            case "peau" -> scoreSkin(image);
            case "oeil" -> scoreEye(image);
            default -> 0.0;
        };
    }

    private double scoreBrain(BufferedImage image) {
        if (!isGrayscale(image)) return 10;
        int w = image.getWidth();
        int h = image.getHeight();
        double aspect = (double) w / h;
        double symmetry = bilateralSymmetry(image);
        double border = meanBorder(image);
        double center = meanRegion(image, w / 4, h / 4, 3 * w / 4, 3 * h / 4);

        int score = 0;
        if (border < 50) score++;
        if (center > border * 1.2) score++;
        if (aspect > 0.8 && aspect < 1.2) score++;
        if (symmetry > 0.5) score++;
        if (lungLike(image) < 0.5) score++;

        return Math.min(35 + score * 14.0, 95);
    }

    private double scoreLung(BufferedImage image) {
        if (!isGrayscale(image)) return 8;
        int w = image.getWidth();
        int h = image.getHeight();
        double left = meanRegion(image, 0, h / 5, w / 2, 4 * h / 5);
        double right = meanRegion(image, w / 2, h / 5, w, 4 * h / 5);
        double center = meanRegion(image, w / 3, h / 5, 2 * w / 3, 4 * h / 5);
        double symmetry = bilateralSymmetry(image);

        int score = 0;
        if (Math.abs(left - right) / Math.max(left, right) < 0.2) score++;
        if (center > Math.max(left, right) * 1.05) score++;
        if (symmetry > 0.45) score++;

        return Math.min(30 + score * 20.0, 92);
    }

    private double scoreHeart(BufferedImage image) {
        if (!isGrayscale(image)) return 8;
        int w = image.getWidth();
        int h = image.getHeight();
        double aspect = (double) w / h;
        double center = meanRegion(image, w / 4, h / 4, 3 * w / 4, 3 * h / 4);
        double border = meanBorder(image);
        double symmetry = bilateralSymmetry(image);

        int score = 0;
        if (center > border * 1.08) score++;
        if (symmetry > 0.55) score++;
        if (aspect > 0.85 && aspect < 1.15) score++;

        return Math.min(32 + score * 18.0, 90);
    }

    private double scoreLiver(BufferedImage image) {
        if (!isGrayscale(image)) return 10;
        int w = image.getWidth();
        int h = image.getHeight();
        double rightStd = stdRegion(image, w / 2, 0, w, h);
        double leftStd = stdRegion(image, 0, 0, w / 2, h);
        double rightMean = meanRegion(image, w / 2, 0, w, h);
        double leftMean = meanRegion(image, 0, 0, w / 2, h);

        int score = 0;
        if (rightStd < leftStd * 0.9) score++;
        if (rightMean > leftMean * 0.95) score++;

        return Math.min(28 + score * 22.0, 88);
    }

    private double scoreBreast(BufferedImage image) {
        if (!isGrayscale(image)) return 12;
        int w = image.getWidth();
        int h = image.getHeight();
        double left = meanHighDensity(image, 0, h / 5, w / 2, 4 * h / 5);
        double right = meanHighDensity(image, w / 2, h / 5, w, 4 * h / 5);
        double symmetry = bilateralSymmetry(image);

        int score = 0;
        if (left > 0.08 && right > 0.08) score++;
        if (symmetry > 0.45) score++;

        return Math.min(30 + score * 22.0, 90);
    }

    private double scoreSkin(BufferedImage image) {
        if (isGrayscale(image)) return 12;
        double color = colorfulness(image);
        return Math.min(25 + (color > 18 ? 45 : color > 10 ? 25 : 5), 93);
    }

    private double scoreEye(BufferedImage image) {
        BufferedImage rgb = toRgb(image);
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double red = meanChannel(rgb, 0);
        double green = meanChannel(rgb, 1);
        double blue = meanChannel(rgb, 2);

        int score = 0;
        if (red > green * 1.05) score++;
        if (red - blue > 12) score++;
        if (green > blue) score++;

        return Math.min(24 + score * 18.0, 92);
    }

    private boolean isGrayscale(BufferedImage image) {
        BufferedImage rgb = toRgb(image);
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double diff = 0;
        int samples = Math.min(w * h, 5000);
        Random rnd = new Random(42);
        for (int i = 0; i < samples; i++) {
            int x = rnd.nextInt(w);
            int y = rnd.nextInt(h);
            int rgbVal = rgb.getRGB(x, y);
            int r = (rgbVal >> 16) & 0xFF;
            int g = (rgbVal >> 8) & 0xFF;
            int b = rgbVal & 0xFF;
            diff += Math.abs(r - g) + Math.abs(g - b);
        }
        return (diff / samples) < 16;
    }

    private double colorfulness(BufferedImage image) {
        BufferedImage rgb = toRgb(image);
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double rg = 0;
        double yb = 0;
        int samples = Math.min(w * h, 4000);
        Random rnd = new Random(7);
        for (int i = 0; i < samples; i++) {
            int x = rnd.nextInt(w);
            int y = rnd.nextInt(h);
            int rgbVal = rgb.getRGB(x, y);
            int r = (rgbVal >> 16) & 0xFF;
            int g = (rgbVal >> 8) & 0xFF;
            int b = rgbVal & 0xFF;
            rg += Math.abs(r - g);
            yb += Math.abs(0.5 * (r + g) - b);
        }
        return (rg + yb) / samples;
    }

    private double bilateralSymmetry(BufferedImage image) {
        BufferedImage gray = toGray(image);
        int w = gray.getWidth();
        int h = gray.getHeight();
        int cx = w / 2;
        int samples = Math.min(h * cx, 3000);
        double sumDiff = 0;
        Random rnd = new Random(11);
        for (int i = 0; i < samples; i++) {
            int y = rnd.nextInt(h);
            int dx = rnd.nextInt(cx);
            int left = gray.getRGB(dx, y) & 0xFF;
            int right = gray.getRGB(w - 1 - dx, y) & 0xFF;
            sumDiff += Math.abs(left - right);
        }
        double avgDiff = sumDiff / samples;
        return Math.max(0, 1.0 - avgDiff / 128.0);
    }

    private double lungLike(BufferedImage image) {
        BufferedImage gray = toGray(image);
        int w = gray.getWidth();
        int h = gray.getHeight();
        double leftDark = darkRatio(gray, 0, h / 5, w / 2, 4 * h / 5);
        double rightDark = darkRatio(gray, w / 2, h / 5, w, 4 * h / 5);
        if (leftDark > 0.25 && rightDark > 0.25 && Math.abs(leftDark - rightDark) < 0.2) {
            return 1.0;
        }
        return 0.0;
    }

    private double darkRatio(BufferedImage gray, int x1, int y1, int x2, int y2) {
        int count = 0;
        int dark = 0;
        for (int y = y1; y < y2; y += 2) {
            for (int x = x1; x < x2; x += 2) {
                int val = gray.getRGB(x, y) & 0xFF;
                count++;
                if (val < 80) dark++;
            }
        }
        return count == 0 ? 0 : (double) dark / count;
    }

    private double meanBorder(BufferedImage image) {
        BufferedImage gray = toGray(image);
        int w = gray.getWidth();
        int h = gray.getHeight();
        int border = Math.max(1, Math.min(w, h) / 12);
        double sum = 0;
        int count = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < border; y++) {
                sum += gray.getRGB(x, y) & 0xFF;
                sum += gray.getRGB(x, h - 1 - y) & 0xFF;
                count += 2;
            }
        }
        for (int y = border; y < h - border; y++) {
            for (int x = 0; x < border; x++) {
                sum += gray.getRGB(x, y) & 0xFF;
                sum += gray.getRGB(w - 1 - x, y) & 0xFF;
                count += 2;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private double meanRegion(BufferedImage image, int x1, int y1, int x2, int y2) {
        BufferedImage gray = toGray(image);
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        x2 = Math.min(gray.getWidth(), x2);
        y2 = Math.min(gray.getHeight(), y2);
        double sum = 0;
        int count = 0;
        for (int y = y1; y < y2; y += 2) {
            for (int x = x1; x < x2; x += 2) {
                sum += gray.getRGB(x, y) & 0xFF;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private double stdRegion(BufferedImage image, int x1, int y1, int x2, int y2) {
        BufferedImage gray = toGray(image);
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        x2 = Math.min(gray.getWidth(), x2);
        y2 = Math.min(gray.getHeight(), y2);
        double sum = 0;
        double sumSq = 0;
        int count = 0;
        for (int y = y1; y < y2; y += 2) {
            for (int x = x1; x < x2; x += 2) {
                double val = gray.getRGB(x, y) & 0xFF;
                sum += val;
                sumSq += val * val;
                count++;
            }
        }
        if (count == 0) return 0;
        double mean = sum / count;
        return Math.sqrt(Math.max(0, sumSq / count - mean * mean));
    }

    private double meanHighDensity(BufferedImage image, int x1, int y1, int x2, int y2) {
        BufferedImage gray = toGray(image);
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);
        x2 = Math.min(gray.getWidth(), x2);
        y2 = Math.min(gray.getHeight(), y2);
        List<Integer> values = new ArrayList<>();
        for (int y = y1; y < y2; y += 2) {
            for (int x = x1; x < x2; x += 2) {
                values.add(gray.getRGB(x, y) & 0xFF);
            }
        }
        if (values.isEmpty()) return 0;
        values.sort(Integer::compareTo);
        int threshold = values.get((int) (values.size() * 0.6));
        long high = values.stream().filter(v -> v > threshold).count();
        return (double) high / values.size();
    }

    private double meanChannel(BufferedImage rgb, int channelShift) {
        int w = rgb.getWidth();
        int h = rgb.getHeight();
        double sum = 0;
        int count = 0;
        for (int y = 0; y < h; y += 3) {
            for (int x = 0; x < w; x += 3) {
                int rgbVal = rgb.getRGB(x, y);
                sum += (rgbVal >> channelShift) & 0xFF;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    private BufferedImage toGray(BufferedImage image) {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(image, 0, 0, null);
        return gray;
    }

    private BufferedImage toRgb(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(image, 0, 0, null);
        return rgb;
    }
}
