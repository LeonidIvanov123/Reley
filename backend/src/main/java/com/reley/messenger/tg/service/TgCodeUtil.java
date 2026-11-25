package com.reley.messenger.tg.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.reley.messenger.tg.dto.TgDialogResponse;
import com.reley.messenger.tg.dto.TgMessageResponse;
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

    public static TgMessageResponse toMessageResponse(TdApi.Message message) {

        String text = extractText(message);

        boolean hasAttachments = hasAttachments(message);

        Long senderId = null;
        String senderName = null;

        if (message.senderId instanceof TdApi.MessageSenderUser userSender) {
            senderId = userSender.userId;
        }

        return new TgMessageResponse(
                message.id,
                message.chatId,
                senderId,
                senderName,
                text,
                OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(message.date),
                        ZoneOffset.UTC
                ),
                hasAttachments
        );
    }

    private static String extractText(TdApi.Message message) {

        if (message.content instanceof TdApi.MessageText text) {
            return text.text.text;
        }

        if (message.content instanceof TdApi.MessagePhoto photo) {
            return photo.caption.text;
        }

        if (message.content instanceof TdApi.MessageVideo video) {
            return video.caption.text;
        }

        if (message.content instanceof TdApi.MessageAnimation animation) {
            return animation.caption.text;
        }

        if (message.content instanceof TdApi.MessageDocument document) {
            return document.caption.text;
        }

        if (message.content instanceof TdApi.MessageAudio audio) {
            return audio.caption.text;
        }

        if (message.content instanceof TdApi.MessageVoiceNote voice) {
            return voice.caption.text;
        }

        return null;
    }

    private static boolean hasAttachments(TdApi.Message message) {

        return switch (message.content) {
            case TdApi.MessagePhoto ignored -> true;
            case TdApi.MessageVideo ignored -> true;
            case TdApi.MessageDocument ignored -> true;
            case TdApi.MessageAudio ignored -> true;
            case TdApi.MessageVoiceNote ignored -> true;
            case TdApi.MessageVideoNote ignored -> true;
            case TdApi.MessageAnimation ignored -> true;
            case TdApi.MessageSticker ignored -> true;
            case TdApi.MessageLocation ignored -> true;
            case TdApi.MessageContact ignored -> true;
            case TdApi.MessagePoll ignored -> true;
            default -> false;
        };
    }

}