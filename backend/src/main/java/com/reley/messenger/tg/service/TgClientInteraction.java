package com.reley.messenger.tg.service;

import it.tdlight.client.ClientInteraction;
import it.tdlight.client.InputParameter;
import it.tdlight.client.ParameterInfo;
import it.tdlight.client.ParameterInfoNotifyLink;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TgClientInteraction implements ClientInteraction {

    private final Consumer<String> qrConsumer;

    public TgClientInteraction(
            Consumer<String> qrConsumer
    ) {
        this.qrConsumer = qrConsumer;
    }

    @Override
    public CompletableFuture<String> onParameterRequest(
            InputParameter parameter,
            ParameterInfo parameterInfo
    ) {

        if (parameter == InputParameter.NOTIFY_LINK) {

            String link =
                    ((ParameterInfoNotifyLink) parameterInfo)
                            .getLink();

            qrConsumer.accept(link);

            return CompletableFuture.completedFuture("");
        }

        return CompletableFuture.failedFuture(
                new IllegalStateException(
                        "Unexpected Telegram auth parameter: "
                                + parameter
                )
        );
    }
}