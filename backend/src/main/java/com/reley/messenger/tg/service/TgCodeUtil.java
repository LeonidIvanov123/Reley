package com.reley.messenger.tg.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.reley.messenger.tg.dto.TgDialogResponse;
import it.tdlight.jni.TdApi;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class TgCodeUtil {

    private TgCodeUtil() {
    }

    public static byte[] generate(String value) throws Exception {

        BitMatrix matrix =
                new MultiFormatWriter()
                        .encode(
                                value,
                                BarcodeFormat.QR_CODE,
                                320,
                                320
                        );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                matrix,
                "PNG",
                output
        );

        return output.toByteArray();
    }

    public static TgDialogResponse toDialogResponse(TdApi.Chat chat) {

        String type;

        if (chat.type instanceof TdApi.ChatTypePrivate) {
            type = "PRIVATE";
        } else if (chat.type instanceof TdApi.ChatTypeBasicGroup) {
            type = "GROUP";
        } else if (chat.type instanceof TdApi.ChatTypeSupergroup) {
            type = "SUPERGROUP";
        } else if (chat.type instanceof TdApi.ChatTypeSecret) {
            type = "SECRET";
        } else {
            type = "UNKNOWN";
        }

        String lastMessage = null;

        OffsetDateTime lastMessageAt = null;

        if (chat.lastMessage != null) {

            lastMessageAt =
                    OffsetDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                    chat.lastMessage.date
                            ),
                            ZoneOffset.UTC
                    );

            lastMessage =
                    extractMessagePreview(
                            chat.lastMessage
                    );
        }

        return new TgDialogResponse(
                chat.id,
                chat.title,
                type,
                lastMessage,
                lastMessageAt
        );
    }

    private static String extractMessagePreview(
            TdApi.Message message
    ) {

        if (message.content instanceof TdApi.MessageText text) {

            return text.text.text;
        }

        if (message.content instanceof TdApi.MessagePhoto) {
            return "📷 Фото";
        }

        if (message.content instanceof TdApi.MessageVideo) {
            return "🎥 Видео";
        }

        if (message.content instanceof TdApi.MessageDocument) {
            return "📎 Документ";
        }

        if (message.content instanceof TdApi.MessageAudio) {
            return "🎵 Аудио";
        }

        if (message.content instanceof TdApi.MessageVoiceNote) {
            return "🎤 Голосовое сообщение";
        }

        if (message.content instanceof TdApi.MessageSticker) {
            return "🙂 Стикер";
        }

        return null;
    }

}