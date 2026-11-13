const messagesContainer =
    document.getElementById('messages');

const dialogId =
    messagesContainer.dataset.dialogId;

const currentUserId =
    messagesContainer.dataset.currentUserId;

const loadingIndicator =
    document.getElementById('loading-indicator');

const sendForm =
    document.getElementById('send-form');

const messageInput =
    document.getElementById('message-content');

const sendError =
    document.getElementById('send-error');


/*
 * =========================
 * Attachments
 * =========================
 */

const attachmentButton =
    document.getElementById('attachment-button');

const attachmentPicker =
    document.getElementById('attachment-picker');

const closeAttachmentPicker =
    document.getElementById('close-attachment-picker');

const attachmentSearch =
    document.getElementById('attachment-search');

const myAttachments =
    document.getElementById('my-attachments');

const uploadAttachmentButton =
    document.getElementById('upload-attachment-button');

const attachmentFileInput =
    document.getElementById('attachment-file-input');

const selectedAttachmentsContainer =
    document.getElementById('selected-attachments');


let myAttachmentsLoaded =
    false;

let myAttachmentsList =
    [];

const selectedAttachments =
    new Map();


/*
 * =========================
 * Attachment picker
 * =========================
 */

if (attachmentButton) {

    attachmentButton.addEventListener(
        'click',
        async event => {

            event.stopPropagation();

            if (!attachmentPicker) {
                return;
            }

            attachmentPicker.classList.toggle(
                'open'
            );

            if (
                attachmentPicker.classList.contains(
                    'open'
                )
                &&
                !myAttachmentsLoaded
            ) {
                await loadMyAttachments();
            }
        }
    );
}


if (closeAttachmentPicker) {

    closeAttachmentPicker.addEventListener(
        'click',
        () => {

            attachmentPicker.classList.remove(
                'open'
            );
        }
    );
}


document.addEventListener(
    'click',
    event => {

        if (
            !attachmentPicker
            ||
            !attachmentButton
        ) {
            return;
        }

        if (
            !attachmentPicker.contains(
                event.target
            )
            &&
            event.target !== attachmentButton
        ) {
            attachmentPicker.classList.remove(
                'open'
            );
        }
    }
);


/*
 * =========================
 * Загрузка моих файлов
 * =========================
 */

async function loadMyAttachments() {

    if (!myAttachments) {
        return;
    }

    myAttachments.innerHTML =
        `
            <div class="attachments-loading">
                Загрузка файлов...
            </div>
        `;

    try {

        const response =
            await fetch(
                '/api/attachments'
            );

        if (!response.ok) {

            throw new Error(
                'Не удалось загрузить мои файлы'
            );
        }


        const attachments =
            await response.json();


        myAttachmentsList =
            Array.isArray(attachments)
                ? attachments
                : [];


        myAttachmentsLoaded =
            true;


        renderMyAttachments(
            myAttachmentsList
        );

    } catch (error) {

        console.error(
            'Failed to load attachments:',
            error
        );


        myAttachments.innerHTML =
            `
                <div class="attachments-error">
                    ${escapeHtml(error.message)}
                </div>
            `;
    }
}


/*
 * =========================
 * Отображение моих файлов
 * =========================
 */

function renderMyAttachments(
    attachments
) {

    if (!myAttachments) {
        return;
    }


    myAttachments.innerHTML =
        '';


    const search =
        attachmentSearch
            ? attachmentSearch.value
                .trim()
                .toLowerCase()
            : '';


    const filteredAttachments =
        (attachments || []).filter(
            attachment => {

                if (!search) {
                    return true;
                }


                const fileName =
                    (
                        attachment.fileName ||
                        ''
                    ).toLowerCase();


                return fileName.includes(
                    search
                );
            }
        );


    if (
        !attachments ||
        attachments.length === 0
    ) {

        myAttachments.innerHTML =
            `
                <div class="attachments-empty">
                    У вас пока нет файлов
                </div>
            `;

        return;
    }


    if (
        filteredAttachments.length === 0
    ) {

        myAttachments.innerHTML =
            `
                <div class="attachments-empty">
                    Файлы не найдены
                </div>
            `;

        return;
    }


    filteredAttachments.forEach(
        attachment => {

            const element =
                document.createElement(
                    'button'
                );


            element.type =
                'button';


            element.className =
                'my-attachment';


            element.dataset.attachmentId =
                attachment.id;


            const selected =
                selectedAttachments.has(
                    attachment.id
                );


            if (selected) {

                element.classList.add(
                    'selected'
                );
            }


            element.innerHTML =
                `
                    <div class="my-attachment-icon">
                        📎
                    </div>

                    <div class="my-attachment-info">

                        <div class="my-attachment-name">
                            ${escapeHtml(
                                attachment.fileName || ''
                            )}
                        </div>

                        <div class="my-attachment-meta">
                            ${formatFileSize(
                                attachment.size
                            )}
                        </div>

                    </div>

                    <div class="my-attachment-check">
                        ${selected ? '✓' : ''}
                    </div>
                `;


            element.addEventListener(
                'click',
                () => {

                    toggleAttachment(
                        attachment
                    );

                    updateAttachmentElement(
                        element,
                        attachment
                    );
                }
            );


            myAttachments.appendChild(
                element
            );
        }
    );
}


/*
 * =========================
 * Обновить состояние файла
 * =========================
 */

function updateAttachmentElement(
    element,
    attachment
) {

    const selected =
        selectedAttachments.has(
            attachment.id
        );


    element.classList.toggle(
        'selected',
        selected
    );


    const check =
        element.querySelector(
            '.my-attachment-check'
        );


    if (check) {

        check.textContent =
            selected
                ? '✓'
                : '';
    }
}


/*
 * =========================
 * Получить текущий список
 * =========================
 */

function getCurrentMyAttachments() {

    return myAttachmentsList;
}


/*
 * =========================
 * Выбор attachment
 * =========================
 */

function toggleAttachment(
    attachment
) {

    if (
        selectedAttachments.has(
            attachment.id
        )
    ) {

        selectedAttachments.delete(
            attachment.id
        );

    } else {

        selectedAttachments.set(
            attachment.id,
            attachment
        );
    }


    renderSelectedAttachments();
}


/*
 * =========================
 * Отображение выбранных
 * =========================
 */

function renderSelectedAttachments() {

    if (!selectedAttachmentsContainer) {
        return;
    }


    selectedAttachmentsContainer.innerHTML =
        '';


    selectedAttachments.forEach(
        attachment => {

            const element =
                document.createElement(
                    'div'
                );


            element.className =
                'selected-attachment';


            element.innerHTML =
                `
                    <span>📎</span>

                    <span class="selected-attachment-name">
                        ${escapeHtml(
                            attachment.fileName || ''
                        )}
                    </span>

                    <button
                        type="button"
                        class="selected-attachment-remove"
                        title="Убрать">
                        ×
                    </button>
                `;


            const removeButton =
                element.querySelector(
                    '.selected-attachment-remove'
                );


            removeButton.addEventListener(
                'click',
                event => {

                    event.preventDefault();
                    event.stopPropagation();


                    selectedAttachments.delete(
                        attachment.id
                    );


                    renderSelectedAttachments();


                    renderMyAttachments(
                        getCurrentMyAttachments()
                    );
                }
            );


            selectedAttachmentsContainer.appendChild(
                element
            );
        }
    );
}


/*
 * =========================
 * Поиск файлов
 * =========================
 */

if (attachmentSearch) {

    attachmentSearch.addEventListener(
        'input',
        () => {

            renderMyAttachments(
                getCurrentMyAttachments()
            );
        }
    );
}


/*
 * =========================
 * Загрузка нового файла
 * =========================
 */

if (
    uploadAttachmentButton &&
    attachmentFileInput
) {

    uploadAttachmentButton.addEventListener(
        'click',
        () => {

            attachmentFileInput.click();
        }
    );


    attachmentFileInput.addEventListener(
        'change',
        async () => {

            const file =
                attachmentFileInput.files?.[0];


            if (!file) {
                return;
            }


            await uploadAttachment(
                file
            );


            attachmentFileInput.value =
                '';
        }
    );
}


/*
 * =========================
 * Upload attachment
 * =========================
 */

async function uploadAttachment(
    file
) {

    if (!file) {
        return;
    }


    /*
     * Сервер сейчас ограничивает
     * размер файла 15 MB.
     */

    const maxFileSize =
        15 * 1024 * 1024;


    if (
        file.size >
        maxFileSize
    ) {

        showSendError(
            'Размер файла не должен превышать 15 MB'
        );

        return;
    }


    if (uploadAttachmentButton) {

        uploadAttachmentButton.disabled =
            true;
    }


    showSendError('');


    try {

        const formData =
            new FormData();


        formData.append(
            'file',
            file
        );


        const response =
            await fetch(
                '/api/attachments',
                {
                    method: 'POST',
                    body: formData
                }
            );


        if (!response.ok) {

            let message =
                'Не удалось загрузить файл';


            try {

                const body =
                    await response.json();


                if (body.message) {

                    message =
                        body.message;
                }

            } catch (_) {
            }


            throw new Error(
                message
            );
        }


        const attachment =
            await response.json();


        /*
         * Добавляем новый файл
         * в локальный список.
         */

        myAttachmentsList =
            [
                attachment,
                ...myAttachmentsList
            ];


        myAttachmentsLoaded =
            true;


        /*
         * Сразу выбираем загруженный файл
         * для отправки.
         */

        selectedAttachments.set(
            attachment.id,
            attachment
        );


        renderSelectedAttachments();


        renderMyAttachments(
            myAttachmentsList
        );


    } catch (error) {

        console.error(
            'Failed to upload attachment:',
            error
        );


        showSendError(
            error.message
        );

    } finally {

        if (uploadAttachmentButton) {

            uploadAttachmentButton.disabled =
                false;
        }
    }
}


/*
 * =========================
 * Enter — отправка сообщения
 * =========================
 */

messageInput.addEventListener(
    'keydown',
    event => {

        if (
            event.key === 'Enter' &&
            !event.shiftKey
        ) {

            event.preventDefault();

            sendForm.requestSubmit();
        }

    }
);


const contextMenu =
    document.getElementById('context-menu');

const deleteMessageButton =
    document.getElementById('delete-message-button');

const pageSize =
    15;

let loadingOlderMessages =
    false;

let hasMoreMessages =
    true;

let selectedMessage =
    null;

let longPressTimer =
    null;


/*
 * =========================
 * Разрешённые реакции
 * =========================
 */

const allowedReactions = [
    '👍',
    '❤️',
    '😂',
    '😮',
    '😢',
    '😡'
];


/*
 * =========================
 * WebSocket
 * =========================
 */

const wsProtocol =
    window.location.protocol === 'https:'
        ? 'wss:'
        : 'ws:';


const socket =
    new WebSocket(
        `${wsProtocol}//${window.location.host}/ws`
    );


socket.onopen = () => {

    console.log(
        'WebSocket connected'
    );
};


socket.onclose = () => {

    console.log(
        'WebSocket disconnected'
    );
};


socket.onerror = error => {

    console.error(
        'WebSocket error:',
        error
    );
};


socket.onmessage = event => {

    try {

        const wsEvent =
            JSON.parse(
                event.data
            );


        console.log(
            'WebSocket event:',
            wsEvent
        );


        switch (wsEvent.type) {

            case 'NEW_MESSAGE':

                handleNewMessage(
                    wsEvent
                );

                break;


            case 'MESSAGE_READ':

                handleMessageRead(
                    wsEvent
                );

                break;


            case 'MESSAGE_REACTION_CHANGED':

                handleMessageReactionChanged(
                    wsEvent
                );

                break;


            default:
                break;
        }

    } catch (error) {

        console.error(
            'Failed to process WebSocket event:',
            error
        );
    }
};


/*
 * =========================
 * Новое сообщение
 * =========================
 */

function handleNewMessage(
    wsEvent
) {

    if (
        Number(wsEvent.dialogId) !==
        Number(dialogId)
    ) {
        return;
    }


    const message =
        wsEvent.payload;


    if (!message) {
        return;
    }


    const existingMessage =
        messagesContainer.querySelector(
            `[data-message-id="${message.id}"]`
        );


    if (existingMessage) {
        return;
    }


    const messageForUi = {
        ...message,
        readByOther: false,
        reactions:
            message.reactions || [],
        attachments:
            message.attachments || []
    };


    const emptyMessage =
        document.getElementById(
            'empty-messages'
        );


    if (emptyMessage) {
        emptyMessage.remove();
    }


    const isNearBottom =
        messagesContainer.scrollHeight -
        messagesContainer.scrollTop -
        messagesContainer.clientHeight < 100;


    const element =
        createMessageElement(
            messageForUi
        );


    messagesContainer.appendChild(
        element
    );


    if (isNearBottom) {
        scrollToBottom();
    }
}


/*
 * =========================
 * Прочитано
 * =========================
 */

function handleMessageRead(
    wsEvent
) {

    if (
        Number(wsEvent.dialogId) !==
        Number(dialogId)
    ) {
        return;
    }


    const lastReadMessageId =
        Number(
            wsEvent.payload?.lastReadMessageId
        );


    if (!lastReadMessageId) {
        return;
    }


    const messageElements =
        messagesContainer.querySelectorAll(
            '.message.mine'
        );


    messageElements.forEach(
        messageElement => {

            const messageId =
                Number(
                    messageElement.dataset.messageId
                );


            if (
                messageId >
                lastReadMessageId
            ) {
                return;
            }


            const status =
                messageElement.querySelector(
                    '.message-status'
                );


            if (!status) {
                return;
            }


            status.textContent =
                '✓✓';


            status.classList.remove(
                'unread'
            );


            status.classList.add(
                'read'
            );
        }
    );
}


/*
 * =========================
 * Изменение реакций
 * =========================
 */

function handleMessageReactionChanged(
    wsEvent
) {

    if (
        Number(wsEvent.dialogId) !==
        Number(dialogId)
    ) {
        return;
    }


    const payload =
        wsEvent.payload;


    if (!payload) {
        return;
    }


    const messageId =
        payload.messageId;


    if (!messageId) {
        return;
    }


    const messageElement =
        messagesContainer.querySelector(
            `[data-message-id="${messageId}"]`
        );


    if (!messageElement) {
        return;
    }


    renderReactions(
        messageElement,
        payload.reactions || []
    );
}


/*
 * =========================
 * Автор сообщения
 * =========================
 */

function getMessageAuthor(
    message
) {

    if (
        message.senderName &&
        message.senderName.trim()
    ) {

        return {
            text:
                message.senderName.trim(),
            removed:
                false
        };
    }


    if (
        message.senderEmail &&
        message.senderEmail.trim()
    ) {

        return {
            text:
                message.senderEmail.trim(),
            removed:
                false
        };
    }


    return {
        text:
            'Участник исключен',
        removed:
            true
    };
}


/*
 * =========================
 * Создание сообщения
 * =========================
 */

function createMessageElement(
    message
) {

    const element =
        document.createElement(
            'div'
        );


    const mine =
        String(message.senderId) ===
        String(currentUserId);


    element.className =
        'message ' +
        (
            mine
                ? 'mine'
                : 'theirs'
        );


    element.dataset.messageId =
        message.id;


    element.dataset.senderId =
        message.senderId;


    element.dataset.deletedAt =
        message.deletedAt || '';


    if (message.deletedAt) {

        element.classList.add(
            'deleted'
        );
    }


    /*
     * Автор
     */

    if (!mine) {

        const author =
            getMessageAuthor(
                message
            );


        const authorElement =
            document.createElement(
                'div'
            );


        authorElement.className =
            'message-author';


        if (author.removed) {

            authorElement.classList.add(
                'removed'
            );
        }


        authorElement.textContent =
            author.text;


        element.appendChild(
            authorElement
        );
    }


    /*
     * Реакция
     */

    const reactionTrigger =
        document.createElement(
            'button'
        );


    reactionTrigger.type =
        'button';


    reactionTrigger.className =
        'reaction-trigger';


    reactionTrigger.title =
        'Поставить реакцию';


    reactionTrigger.textContent =
        '☺';


    element.appendChild(
        reactionTrigger
    );


    /*
     * Picker
     */

    const reactionPicker =
        document.createElement(
            'div'
        );


    reactionPicker.className =
        'reaction-picker';


    allowedReactions.forEach(
        reaction => {

            const button =
                document.createElement(
                    'button'
                );


            button.type =
                'button';


            button.dataset.reaction =
                reaction;


            button.textContent =
                reaction;


            reactionPicker.appendChild(
                button
            );
        }
    );


    element.appendChild(
        reactionPicker
    );


    /*
     * Текст
     */

    const content =
        document.createElement(
            'div'
        );


    content.className =
        'message-content';


    content.textContent =
        message.content || '';


    element.appendChild(
        content
    );


    /*
     * Attachments
     */

    if (
        message.attachments &&
        message.attachments.length > 0
    ) {

        const attachmentsContainer =
            document.createElement(
                'div'
            );


        attachmentsContainer.className =
            'message-attachments';


        message.attachments.forEach(
            attachment => {

                const link =
                    document.createElement(
                        'a'
                    );


                link.className =
                    'message-attachment';


                link.href =
                    attachment.downloadUrl;


                link.target =
                    '_blank';


                link.rel =
                    'noopener noreferrer';


                const icon =
                    document.createElement(
                        'div'
                    );


                icon.className =
                    'attachment-icon';


                icon.textContent =
                    '📎';


                const info =
                    document.createElement(
                        'div'
                    );


                info.className =
                    'attachment-info';


                const name =
                    document.createElement(
                        'div'
                    );


                name.className =
                    'attachment-name';


                name.textContent =
                    attachment.fileName ||
                    'Файл';


                const meta =
                    document.createElement(
                        'div'
                    );


                meta.className =
                    'attachment-meta';


                meta.textContent =
                    attachment.extension
                        ? `${attachment.extension} · ${formatFileSize(attachment.size)}`
                        : formatFileSize(
                            attachment.size
                        );


                info.appendChild(
                    name
                );


                info.appendChild(
                    meta
                );


                link.appendChild(
                    icon
                );


                link.appendChild(
                    info
                );


                attachmentsContainer.appendChild(
                    link
                );
            }
        );


        element.appendChild(
            attachmentsContainer
        );
    }


    /*
     * Удаление
     */

    if (message.deletedAt) {

        const deletedLabel =
            document.createElement(
                'div'
            );


        deletedLabel.className =
            'message-deleted-label';


        deletedLabel.textContent =
            'удалено';


        element.appendChild(
            deletedLabel
        );
    }


    /*
     * Контейнер реакций
     */

    const reactionsContainer =
        document.createElement(
            'div'
        );


    reactionsContainer.className =
        'message-reactions';


    element.appendChild(
        reactionsContainer
    );


    renderReactions(
        element,
        message.reactions || []
    );


    /*
     * Время
     */

    const time =
        document.createElement(
            'div'
        );


    time.className =
        'message-time';


    const timeText =
        document.createElement(
            'span'
        );


    timeText.textContent =
        new Date(
            message.createdAt
        ).toLocaleTimeString(
            [],
            {
                hour: '2-digit',
                minute: '2-digit'
            }
        );


    time.appendChild(
        timeText
    );


    /*
     * Статус
     */

    if (mine) {

        const status =
            document.createElement(
                'span'
            );


        status.className =
            'message-status ' +
            (
                message.readByOther
                    ? 'read'
                    : 'unread'
            );


        status.textContent =
            message.readByOther
                ? '✓✓'
                : '✓';


        time.appendChild(
            status
        );
    }


    element.appendChild(
        time
    );


    return element;
}


/*
 * =========================
 * Реакции
 * =========================
 */

function renderReactions(
    messageElement,
    reactions
) {

    let reactionsContainer =
        messageElement.querySelector(
            '.message-reactions'
        );


    if (!reactionsContainer) {

        reactionsContainer =
            document.createElement(
                'div'
            );


        reactionsContainer.className =
            'message-reactions';


        const messageTime =
            messageElement.querySelector(
                '.message-time'
            );


        if (messageTime) {

            messageElement.insertBefore(
                reactionsContainer,
                messageTime
            );

        } else {

            messageElement.appendChild(
                reactionsContainer
            );
        }
    }


    reactionsContainer.innerHTML =
        '';


    if (
        !reactions ||
        reactions.length === 0
    ) {

        reactionsContainer.style.display =
            'none';

        return;
    }


    reactionsContainer.style.display =
        'flex';


    reactions.forEach(
        reaction => {

            const button =
                document.createElement(
                    'button'
                );


            button.type =
                'button';


            button.className =
                'reaction-item';


            if (reaction.reactedByMe) {

                button.classList.add(
                    'reacted'
                );
            }


            button.dataset.reaction =
                reaction.reaction;


            const emoji =
                document.createElement(
                    'span'
                );


            emoji.textContent =
                reaction.reaction;


            const count =
                document.createElement(
                    'span'
                );


            count.className =
                'reaction-count';


            count.textContent =
                reaction.count;


            button.appendChild(
                emoji
            );


            button.appendChild(
                count
            );


            reactionsContainer.appendChild(
                button
            );
        }
    );
}


/*
 * =========================
 * Прокрутка вниз
 * =========================
 */

function scrollToBottom() {

    messagesContainer.scrollTop =
        messagesContainer.scrollHeight;
}


/*
 * =========================
 * Старое сообщение
 * =========================
 */

function getOldestMessageId() {

    const messages =
        messagesContainer.querySelectorAll(
            '[data-message-id]'
        );


    if (messages.length === 0) {
        return null;
    }


    return messages[0]
        .dataset
        .messageId;
}


async function loadOlderMessages() {

    if (
        loadingOlderMessages ||
        !hasMoreMessages
    ) {
        return;
    }


    const oldestMessageId =
        getOldestMessageId();


    if (!oldestMessageId) {
        return;
    }


    loadingOlderMessages =
        true;


    loadingIndicator.style.display =
        'block';


    const previousScrollHeight =
        messagesContainer.scrollHeight;


    const previousScrollTop =
        messagesContainer.scrollTop;


    try {

        const response =
            await fetch(
                `/api/dialogs/${dialogId}/messages?beforeId=${oldestMessageId}&limit=${pageSize}`
            );


        if (!response.ok) {

            throw new Error(
                'Не удалось загрузить сообщения'
            );
        }


        const messages =
            await response.json();


        if (
            !messages ||
            messages.length === 0
        ) {

            hasMoreMessages =
                false;

            return;
        }


        const fragment =
            document.createDocumentFragment();


        messages.forEach(
            message => {

                const element =
                    createMessageElement(
                        message
                    );


                fragment.appendChild(
                    element
                );
            }
        );


        messagesContainer.insertBefore(
            fragment,
            messagesContainer.querySelector(
                '[data-message-id]'
            )
        );


        messagesContainer.scrollTop =
            previousScrollTop +
            (
                messagesContainer.scrollHeight -
                previousScrollHeight
            );

    } catch (error) {

        console.error(
            error
        );


        showSendError(
            error.message
        );

    } finally {

        loadingOlderMessages =
            false;


        loadingIndicator.style.display =
            'none';
    }
}


/*
 * =========================
 * Infinite scroll
 * =========================
 */

messagesContainer.addEventListener(
    'scroll',
    () => {

        if (
            messagesContainer.scrollTop <= 30
        ) {

            loadOlderMessages();
        }
    }
);


/*
 * =========================
 * Context menu
 * =========================
 */

function closeContextMenu() {

    contextMenu.style.display =
        'none';


    selectedMessage =
        null;
}


function openContextMenu(
    messageElement,
    x,
    y
) {

    if (
        !messageElement.classList.contains(
            'mine'
        )
        ||
        messageElement.classList.contains(
            'deleted'
        )
    ) {

        closeContextMenu();

        return;
    }


    selectedMessage =
        messageElement;


    contextMenu.style.display =
        'block';


    const menuWidth =
        contextMenu.offsetWidth;


    const menuHeight =
        contextMenu.offsetHeight;


    contextMenu.style.left =
        Math.min(
            x,
            window.innerWidth -
            menuWidth -
            10
        ) + 'px';


    contextMenu.style.top =
        Math.min(
            y,
            window.innerHeight -
            menuHeight -
            10
        ) + 'px';
}


/*
 * =========================
 * Правый клик
 * =========================
 */

messagesContainer.addEventListener(
    'contextmenu',
    event => {

        const messageElement =
            event.target.closest(
                '[data-message-id]'
            );


        if (!messageElement) {
            return;
        }


        event.preventDefault();


        openContextMenu(
            messageElement,
            event.clientX,
            event.clientY
        );
    }
);


/*
 * =========================
 * Long press
 * =========================
 */

messagesContainer.addEventListener(
    'touchstart',
    event => {

        const messageElement =
            event.target.closest(
                '[data-message-id]'
            );


        if (
            !messageElement
            ||
            !messageElement.classList.contains(
                'mine'
            )
        ) {
            return;
        }


        longPressTimer =
            setTimeout(
                () => {

                    const touch =
                        event.touches[0];


                    openContextMenu(
                        messageElement,
                        touch.clientX,
                        touch.clientY
                    );

                },
                600
            );
    },
    {
        passive: true
    }
);


messagesContainer.addEventListener(
    'touchend',
    () => {

        clearTimeout(
            longPressTimer
        );
    }
);


messagesContainer.addEventListener(
    'touchmove',
    () => {

        clearTimeout(
            longPressTimer
        );
    }
);


/*
 * =========================
 * Реакции
 * =========================
 */

messagesContainer.addEventListener(
    'click',
    async event => {

        const trigger =
            event.target.closest(
                '.reaction-trigger'
            );


        if (trigger) {

            const messageElement =
                trigger.closest(
                    '[data-message-id]'
                );


            if (!messageElement) {
                return;
            }


            const picker =
                messageElement.querySelector(
                    '.reaction-picker'
                );


            if (!picker) {
                return;
            }


            document
                .querySelectorAll(
                    '.reaction-picker.open'
                )
                .forEach(
                    element => {

                        if (
                            element !== picker
                        ) {

                            element.classList.remove(
                                'open'
                            );
                        }
                    }
                );


            picker.classList.toggle(
                'open'
            );


            return;
        }


        const pickerButton =
            event.target.closest(
                '.reaction-picker button'
            );


        if (pickerButton) {

            const messageElement =
                pickerButton.closest(
                    '[data-message-id]'
                );


            if (!messageElement) {
                return;
            }


            await setMessageReaction(
                messageElement,
                pickerButton.dataset.reaction
            );


            return;
        }


        const reactionButton =
            event.target.closest(
                '.reaction-item'
            );


        if (reactionButton) {

            const messageElement =
                reactionButton.closest(
                    '[data-message-id]'
                );


            if (!messageElement) {
                return;
            }


            await setMessageReaction(
                messageElement,
                reactionButton.dataset.reaction
            );
        }
    }
);


/*
 * =========================
 * Установка реакции
 * =========================
 */

async function setMessageReaction(
    messageElement,
    reaction
) {

    if (
        !allowedReactions.includes(
            reaction
        )
    ) {
        return;
    }


    const messageId =
        messageElement.dataset.messageId;


    showSendError('');


    try {

        const response =
            await fetch(
                `/api/dialogs/${dialogId}/messages/${messageId}/reaction`,
                {
                    method: 'PUT',

                    headers: {
                        'Content-Type':
                            'application/json'
                    },

                    body:
                        JSON.stringify({
                            reaction
                        })
                }
            );


        if (!response.ok) {

            let message =
                'Не удалось установить реакцию';


            try {

                const body =
                    await response.json();


                if (body.message) {

                    message =
                        body.message;
                }

            } catch (_) {
            }


            throw new Error(
                message
            );
        }


        const reactions =
            await response.json();


        renderReactions(
            messageElement,
            reactions
        );


        const picker =
            messageElement.querySelector(
                '.reaction-picker'
            );


        if (picker) {

            picker.classList.remove(
                'open'
            );
        }

    } catch (error) {

        showSendError(
            error.message
        );
    }
}


/*
 * =========================
 * Закрытие picker
 * =========================
 */

document.addEventListener(
    'click',
    event => {

        if (
            event.target.closest(
                '.reaction-picker'
            )
            ||
            event.target.closest(
                '.reaction-trigger'
            )
        ) {
            return;
        }


        document
            .querySelectorAll(
                '.reaction-picker.open'
            )
            .forEach(
                picker =>
                    picker.classList.remove(
                        'open'
                    )
            );
    }
);


/*
 * =========================
 * Закрытие context menu
 * =========================
 */

document.addEventListener(
    'click',
    event => {

        if (
            !contextMenu.contains(
                event.target
            )
        ) {

            closeContextMenu();
        }
    }
);


document.addEventListener(
    'keydown',
    event => {

        if (event.key === 'Escape') {

            closeContextMenu();


            document
                .querySelectorAll(
                    '.reaction-picker.open'
                )
                .forEach(
                    picker =>
                        picker.classList.remove(
                            'open'
                        )
                );


            if (attachmentPicker) {

                attachmentPicker.classList.remove(
                    'open'
                );
            }
        }
    }
);


/*
 * =========================
 * Удаление сообщения
 * =========================
 */

deleteMessageButton.addEventListener(
    'click',
    async () => {

        if (!selectedMessage) {
            return;
        }


        const messageElement =
            selectedMessage;


        const messageId =
            messageElement.dataset.messageId;


        closeContextMenu();


        showSendError('');


        try {

            const response =
                await fetch(
                    `/api/dialogs/${dialogId}/messages/${messageId}`,
                    {
                        method: 'DELETE'
                    }
                );


            if (!response.ok) {

                throw new Error(
                    'Не удалось удалить сообщение'
                );
            }


            const updatedMessage =
                await response.json();


            messageElement.dataset.deletedAt =
                updatedMessage.deletedAt || '';


            messageElement.classList.add(
                'deleted'
            );


            if (
                !messageElement.querySelector(
                    '.message-deleted-label'
                )
            ) {

                const deletedLabel =
                    document.createElement(
                        'div'
                    );


                deletedLabel.className =
                    'message-deleted-label';


                deletedLabel.textContent =
                    'удалено';


                const content =
                    messageElement.querySelector(
                        '.message-content'
                    );


                content.after(
                    deletedLabel
                );
            }

        } catch (error) {

            showSendError(
                error.message
            );
        }
    }
);


/*
 * =========================
 * Первоначальная прокрутка
 * =========================
 */

scrollToBottom();


/*
 * =========================
 * Отправка сообщения
 * =========================
 */

sendForm.addEventListener(
    'submit',
    async event => {

        event.preventDefault();


        const content =
            messageInput.value.trim();


        const attachmentIds =
            Array.from(
                selectedAttachments.keys()
            );


        /*
         * Нельзя отправить полностью пустое
         * сообщение без файлов.
         *
         * Но сообщение только с файлами
         * отправлять можно.
         */

        if (
            !content &&
            attachmentIds.length === 0
        ) {
            return;
        }


        showSendError('');


        try {

            const response =
                await fetch(
                    `/api/dialogs/${dialogId}/messages`,
                    {
                        method: 'POST',

                        headers: {
                            'Content-Type':
                                'application/json'
                        },

                        body:
                            JSON.stringify({
                                content,
                                attachmentIds
                            })
                    }
                );


            if (!response.ok) {

                let message =
                    'Не удалось отправить сообщение';


                try {

                    const body =
                        await response.json();


                    if (body.message) {

                        message =
                            body.message;
                    }

                } catch (_) {
                }


                throw new Error(
                    message
                );
            }


            const message =
                await response.json();


            const existingMessage =
                messagesContainer.querySelector(
                    `[data-message-id="${message.id}"]`
                );


            if (!existingMessage) {

                const element =
                    createMessageElement(
                        message
                    );


                messagesContainer.appendChild(
                    element
                );
            }


            /*
             * Очищаем текст.
             */

            messageInput.value =
                '';


            /*
             * Очищаем выбранные attachments.
             */

            selectedAttachments.clear();


            renderSelectedAttachments();


            /*
             * Обновляем список файлов,
             * чтобы галочки исчезли.
             */

            renderMyAttachments(
                getCurrentMyAttachments()
            );


            scrollToBottom();

        } catch (error) {

            showSendError(
                error.message
            );
        }
    }
);


/*
 * =========================
 * Helpers
 * =========================
 */

function showSendError(
    message
) {

    if (!sendError) {
        return;
    }


    sendError.textContent =
        message || '';
}


/*
 * =========================
 * Escape HTML
 * =========================
 */

function escapeHtml(
    value
) {

    if (
        value === null ||
        value === undefined
    ) {
        return '';
    }


    const div =
        document.createElement(
            'div'
        );


    div.textContent =
        String(value);


    return div.innerHTML;
}


/*
 * =========================
 * Размер файла
 * =========================
 */

function formatFileSize(
    size
) {

    if (
        size === null ||
        size === undefined ||
        Number.isNaN(
            Number(size)
        )
    ) {
        return '';
    }


    const bytes =
        Number(size);


    if (bytes < 1024) {

        return `${bytes} Б`;
    }


    if (bytes < 1024 * 1024) {

        return `${(
            bytes / 1024
        ).toFixed(1)} КБ`;
    }


    if (bytes < 1024 * 1024 * 1024) {

        return `${(
            bytes /
            (1024 * 1024)
        ).toFixed(1)} МБ`;
    }


    return `${(
        bytes /
        (1024 * 1024 * 1024)
    ).toFixed(1)} ГБ`;
}