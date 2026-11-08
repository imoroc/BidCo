package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.service.contract.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;

    // Inyectamos las claves desde el application.properties
    public CloudinaryServiceImpl(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            validateImage(file.getContentType(), bytes);

            Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", "image"));
            // Se guarda la URL HTTPS para no provocar contenido mixto en el frontend.
            Object url = uploadResult.get("secure_url");
            if (url == null) {
                throw new IllegalStateException("Cloudinary no devolvió la URL de la imagen");
            }
            return url.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al subir la imagen a Cloudinary", e);
        }
    }

    /** Comprueba el tipo declarado, el tamaño y la firma real del archivo. */
    private void validateImage(String contentType, byte[] bytes) {
        if (bytes.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 8 MB");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato de imagen no permitido (usa JPG, PNG, WEBP o GIF)");
        }
        if (!hasImageSignature(bytes)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }
    }

    private boolean hasImageSignature(byte[] b) {
        boolean jpeg = b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean gif = b.length > 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
        boolean webp = b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return jpeg || png || gif || webp;
    }
}
