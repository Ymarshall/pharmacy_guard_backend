package com.buddies.pharmaciegarde.modules.duty.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stocke les fichiers hors de toute racine web, sous un nom généré, après contrôle du contenu réel.
 */
@Service
public class UploadStorageService {

	public static final long MAX_BYTES = 10L * 1024 * 1024;

	public enum UploadKind {
		DUTY_DOCUMENT,
		EXCEL
	}

	public record StoredUpload(String originalName, Path path, long size, String sha256) {
	}

	private final Path root;

	public UploadStorageService(@Value("${file.upload-dir:uploads/duty-files}") String uploadDir) {
		this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
	}

	public Path root() {
		return root;
	}

	public StoredUpload store(MultipartFile file, UploadKind kind) throws IOException {
		byte[] bytes = readLimited(file);
		assertSignature(bytes, kind, file.getOriginalFilename());
		Files.createDirectories(root);
		String safeOriginal = sanitizeFileName(file.getOriginalFilename());
		Path target = root.resolve(UUID.randomUUID() + "_" + safeOriginal).normalize();
		if (!target.startsWith(root)) {
			throw new IllegalArgumentException("Nom de fichier refusé");
		}
		Files.copy(new java.io.ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);
		return new StoredUpload(safeOriginal, target, bytes.length, sha256(bytes));
	}

	public void assertAllowed(MultipartFile file, UploadKind kind) throws IOException {
		byte[] header = readLimited(file);
		assertSignature(header, kind, file.getOriginalFilename());
	}

	public Path resolveInsideRoot(String storedPath) {
		if (storedPath == null || storedPath.isBlank()) {
			throw new IllegalArgumentException("Chemin de fichier absent");
		}
		Path path = Paths.get(storedPath).toAbsolutePath().normalize();
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Accès au fichier refusé");
		}
		return path;
	}

	static String sanitizeFileName(String original) {
		String base = "fichier";
		if (original != null && !original.isBlank()) {
			base = Paths.get(original).getFileName().toString();
		}
		base = base.replaceAll("[^A-Za-z0-9._-]", "_");
		if (base.equals(".") || base.equals("..") || base.contains("..")) {
			base = "fichier";
		}
		if (base.length() > 80) {
			base = base.substring(base.length() - 80);
		}
		return base.toLowerCase(Locale.ROOT);
	}

	static String sha256(byte[] bytes) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponible", ex);
		}
	}

	private byte[] readLimited(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Le fichier est vide");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 10 Mo");
		}
		try (InputStream input = file.getInputStream()) {
			byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
			if (bytes.length > MAX_BYTES) {
				throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 10 Mo");
			}
			if (bytes.length == 0) {
				throw new IllegalArgumentException("Le fichier est vide");
			}
			return bytes;
		}
	}

	private void assertSignature(byte[] bytes, UploadKind kind, String originalName) {
		boolean pdf = startsWith(bytes, new byte[] { '%', 'P', 'D', 'F' });
		boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
				&& (bytes[2] & 0xFF) == 0xFF;
		boolean png = startsWith(bytes, new byte[] { (byte) 0x89, 'P', 'N', 'G' });
		boolean xlsx = startsWith(bytes, new byte[] { 'P', 'K', 0x03, 0x04 });
		boolean xls = startsWith(bytes, new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 });
		boolean allowed = switch (kind) {
			case DUTY_DOCUMENT -> pdf || jpeg || png;
			case EXCEL -> xlsx || xls;
		};
		if (!allowed) {
			throw new IllegalArgumentException(
					"Contenu de fichier refusé. Formats acceptés : "
							+ (kind == UploadKind.EXCEL ? "Excel .xlsx/.xls" : "PDF, JPEG ou PNG"));
		}
		String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
		Set<String> blocked = Set.of(".html", ".htm", ".svg", ".js", ".exe", ".sh", ".php");
		for (String extension : blocked) {
			if (name.endsWith(extension)) {
				throw new IllegalArgumentException("Extension de fichier refusée");
			}
		}
	}

	private boolean startsWith(byte[] bytes, byte[] prefix) {
		if (bytes.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (bytes[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}
}
