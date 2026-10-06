package com.kaii.dentix.global.security;

import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageUploadValidatorTest {
    @Test void validImageIsRecognizedFromContentDespiteForgedFilenameAndMime() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var image = ImageUploadValidator.validate(new MockMultipartFile("file", "../../patient-name.html", "text/html", bytes.toByteArray()));
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.extension()).isEqualTo("png");
    }
    @Test void htmlMasqueradingAsJpegIsRejected() {
        assertThatThrownBy(() -> ImageUploadValidator.validate(new MockMultipartFile("file", "photo.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes())))
                .isInstanceOf(BadRequestApiException.class);
    }
    @Test void oversizedFileIsRejectedWithoutReadingBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(ImageUploadValidator.MAX_BYTES + 1);
        assertThatThrownBy(() -> ImageUploadValidator.validate(file)).isInstanceOf(BadRequestApiException.class);
        verify(file, never()).getInputStream();
    }
    @Test void truncatedImageIsRejected() {
        assertThatThrownBy(() -> ImageUploadValidator.validate(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{(byte)137,80,78,71,13,10,26,10})))
                .isInstanceOf(BadRequestApiException.class);
    }
}
