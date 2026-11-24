package com.reley.messenger.tg.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;

public final class TgQrCodeService {

    private TgQrCodeService() {
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
}