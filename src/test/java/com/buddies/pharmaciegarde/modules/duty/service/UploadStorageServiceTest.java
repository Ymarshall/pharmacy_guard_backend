package com.buddies.pharmaciegarde.modules.duty.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class UploadStorageServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void storesPdfOutsideTheOriginalNameAndRejectsTraversal() throws Exception {
		UploadStorageService storage = new UploadStorageService(tempDir.toString());
		byte[] pdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.US_ASCII);
		MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.pdf", "application/pdf", pdf);

		UploadStorageService.StoredUpload stored = storage.store(file, UploadStorageService.UploadKind.DUTY_DOCUMENT);

		assertTrue(stored.path().startsWith(tempDir.toAbsolutePath().normalize()));
		assertFalse(stored.path().getFileName().toString().contains(".."));
		assertFalse(stored.originalName().contains("/"));
		assertEquals(64, stored.sha256().length());
		assertTrue(Files.exists(stored.path()));
	}

	@Test
	void rejectsHtmlDisguisedAsPdf() {
		UploadStorageService storage = new UploadStorageService(tempDir.toString());
		MockMultipartFile file = new MockMultipartFile("file", "liste.pdf", "application/pdf",
				"<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
		assertThrows(IllegalArgumentException.class,
				() -> storage.store(file, UploadStorageService.UploadKind.DUTY_DOCUMENT));
	}

	@Test
	void rejectsPathOutsideTheUploadRoot() {
		UploadStorageService storage = new UploadStorageService(tempDir.toString());
		assertThrows(IllegalArgumentException.class, () -> storage.resolveInsideRoot("/etc/passwd"));
	}
}
