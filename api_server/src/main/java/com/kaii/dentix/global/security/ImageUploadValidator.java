package com.kaii.dentix.global.security;

import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

public final class ImageUploadValidator {
    private ImageUploadValidator() {}
    public static final long MAX_BYTES = 10 * 1024 * 1024;
    public record ImageType(String extension, String contentType) {}

    public static ImageType validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BadRequestApiException("사진은 10MB 이하의 JPEG 또는 PNG 파일이어야 합니다.");
        }
        // Read metadata before decoding pixels to reject decompression bombs.
        try (var stream = file.getInputStream(); ImageInputStream input = ImageIO.createImageInputStream(stream)) {
            if (input == null) throw invalid();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) throw invalid();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 25_000_000) throw invalid();
                if (reader.read(0) == null) throw invalid();
                return format.equalsIgnoreCase("PNG")
                        ? new ImageType("png", "image/png") : new ImageType("jpg", "image/jpeg");
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException exception) {
            throw invalid();
        }
    }
    private static BadRequestApiException invalid() {
        return new BadRequestApiException("유효한 JPEG 또는 PNG 사진을 선택해 주세요.");
    }
}
