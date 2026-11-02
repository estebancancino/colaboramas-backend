package com.agora.util;

import com.agora.user.service.UserImageService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializa el campo imageUrl de usuarios como una URL firmada cuando lo guardado
 * es una key del bucket. Spring inyecta UserImageService al crear el serializer.
 */
public class ImageUrlSerializer extends JsonSerializer<String> {

    private final UserImageService userImageService;

    public ImageUrlSerializer(UserImageService userImageService) {
        this.userImageService = userImageService;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(userImageService.resolveImageUrl(value));
    }
}
