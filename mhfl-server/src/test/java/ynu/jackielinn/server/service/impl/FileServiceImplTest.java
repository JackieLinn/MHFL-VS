package ynu.jackielinn.server.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAvatarShouldThrowWhenFileNullOrEmpty() {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088");

        assertThatThrownBy(() -> service.saveAvatar(1L, null))
                .isInstanceOf(IllegalArgumentException.class);

        MultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.saveAvatar(1L, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveAvatarShouldThrowWhenFilenameInvalid() {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088");

        MultipartFile nullName = mock(MultipartFile.class);
        when(nullName.isEmpty()).thenReturn(false);
        when(nullName.getOriginalFilename()).thenReturn(null);
        assertThatThrownBy(() -> service.saveAvatar(1L, nullName))
                .isInstanceOf(IllegalArgumentException.class);

        MultipartFile blankName = mock(MultipartFile.class);
        when(blankName.isEmpty()).thenReturn(false);
        when(blankName.getOriginalFilename()).thenReturn("   ");
        assertThatThrownBy(() -> service.saveAvatar(1L, blankName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveAvatarShouldThrowWhenExtensionNotAllowed() {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088");

        MultipartFile txt = new MockMultipartFile("file", "avatar.txt", "text/plain", "x".getBytes());
        assertThatThrownBy(() -> service.saveAvatar(1L, txt))
                .isInstanceOf(IllegalArgumentException.class);

        MultipartFile noExt = new MockMultipartFile("file", "avatar", "application/octet-stream", "x".getBytes());
        assertThatThrownBy(() -> service.saveAvatar(1L, noExt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveAvatarShouldThrowWhenTransferFails() throws Exception {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("a.png");
        org.mockito.Mockito.doThrow(new IOException("io-fail")).when(file).transferTo(org.mockito.ArgumentMatchers.any(java.io.File.class));

        assertThatThrownBy(() -> service.saveAvatar(1L, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("io-fail");
    }

    @Test
    void saveAvatarShouldSaveAndReturnUrlWhenBaseUrlNoTrailingSlash() throws Exception {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088");

        MultipartFile file = new MockMultipartFile("file", "Me.JPG", "image/jpeg", "abc".getBytes());

        String url = service.saveAvatar(7L, file);

        assertThat(url).startsWith("http://localhost:8088/uploads/avatar/7_").endsWith(".jpg");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path saved = tempDir.resolve("avatar").resolve(filename);
        assertThat(Files.exists(saved)).isTrue();
    }

    @Test
    void saveAvatarShouldSaveAndReturnUrlWhenBaseUrlHasTrailingSlash() {
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8088/");

        MultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", "abc".getBytes());

        String url = service.saveAvatar(9L, file);

        assertThat(url).startsWith("http://localhost:8088/uploads/avatar/9_").endsWith(".webp");
    }
}

