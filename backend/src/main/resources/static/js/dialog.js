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

const editMessageButton =
    document.getElementById(
        'edit-message-button'
    );

const editPreview =
    document.getElementById('edit-preview');

const editPreviewText =
    document.getElementById('edit-preview-text');

const cancelEditButton =
    document.getElementById('cancel-edit-button');

/*
 * =========================
 * Reply
 * =========================
 */

const replyMessageButton =
    document.getElementById(
        'reply-message-button'
    );

const replyPreview =
    document.getElementById(
        'reply-preview'
    );

const replyPreviewAuthor =
    document.getElementById(
        'reply-preview-author'
    );

const replyPreviewText =
    document.getElementById(
        'reply-preview-text'
    );

const cancelReplyButton =
    document.getElementById(
        'cancel-reply-button'
    );

const emojiButton =
    document.getElementById('emoji-button');

const emojiPicker =
    document.getElementById('emoji-picker');

const emojiPickerContent =
    document.getElementById('emoji-picker-content');

let replyToMessageId = null;

let editingMessageId = null;

/*
 * =========================
 * Поиск сообщений
 * =========================
 */

const messageSearchButton =
    document.getElementById('messageSearchButton');

const messageSearch =
    document.getElementById('messageSearch');

const messageSearchInput =
    document.getElementById('messageSearchInput');

const messageSearchClose =
    document.getElementById('messageSearchClose');

const messageSearchResults =
    document.getElementById('messageSearchResults');

let messageSearchTimeout = null;

let messageSearchController = null;


/*
 * =========================
 * Открыть поиск
 * =========================
 */

function openMessageSearch() {

    if (!messageSearch) {
        return;
    }

    messageSearch.classList.add(
        'open'
    );

    if (messageSearchInput) {

        messageSearchInput.focus();
    }
}


/*
 * =========================
 * Закрыть поиск
 * =========================
 */

function closeMessageSearch() {

    if (!messageSearch) {
        return;
    }

    messageSearch.classList.remove(
        'open'
    );

    if (messageSearchInput) {

        messageSearchInput.value =
            '';
    }

    if (messageSearchResults) {

        messageSearchResults.innerHTML =
            '';
    }

    if (messageSearchController) {

        messageSearchController.abort();

        messageSearchController =
            null;
    }

    clearTimeout(
        messageSearchTimeout
    );
}


if (messageSearchButton) {

    messageSearchButton.addEventListener(
        'click',
        openMessageSearch
    );
}


if (messageSearchClose) {

    messageSearchClose.addEventListener(
        'click',
        closeMessageSearch
    );
}


/*
 * =========================
 * Ввод поискового запроса
 * =========================
 */

if (messageSearchInput) {

    messageSearchInput.addEventListener(
        'input',
        () => {

            clearTimeout(
                messageSearchTimeout
            );

            const query =
                messageSearchInput.value.trim();

            /*
             * Не отправляем запросы
             * для пустого или слишком короткого
             * запроса.
             */

            if (query.length < 2) {

                if (messageSearchResults) {

                    messageSearchResults.innerHTML =
                        '';
                }

                if (messageSearchController) {

                    messageSearchController.abort();

                    messageSearchController =
                        null;
                }

                return;
            }

            messageSearchTimeout =
                setTimeout(
                    () => searchMessages(query),
                    350
                );
        }
    );
}


/*
 * =========================
 * Поиск сообщений через API
 * =========================
 */

async function searchMessages(
    query
) {

    if (!messageSearchResults) {
        return;
    }

    /*
     * Отменяем предыдущий запрос,
     * если пользователь уже ввёл новый текст.
     */

    if (messageSearchController) {

        messageSearchController.abort();
    }

    messageSearchController =
        new AbortController();

    messageSearchResults.innerHTML =
        `
            <div class="message-search-loading">
                Поиск...
            </div>
        `;

    try {

        const url =
            `/api/dialogs/${dialogId}/messages/search` +
            `?q=${encodeURIComponent(query)}` +
            `&limit=50` +
            `&offset=0`;

        const response =
            await fetch(
                url,
                {
                    method: 'GET',

                    signal:
                        messageSearchController.signal
                }
            );

        if (!response.ok) {

            let errorMessage =
                'Не удалось выполнить поиск';

            try {

                const body =
                    await response.text();

                if (body) {

                    const parser =
                        new DOMParser();

                    const document =
                        parser.parseFromString(
                            body,
                            'text/html'
                        );

                    const bodyMessage =
                        document.body
                            ?.textContent
                            ?.trim();

                    if (bodyMessage) {

                        errorMessage =
                            bodyMessage;
                    }
                }

            } catch (_) {
            }

            throw new Error(
                errorMessage
            );
        }

        const messages =
            await response.json();

        renderMessageSearchResults(
            messages,
            query
        );

    } catch (error) {

        /*
         * AbortError возникает нормально,
         * когда пользователь быстро меняет запрос.
         */

        if (
            error.name ===
            'AbortError'
        ) {
            return;
        }

        console.error(
            'Message search failed:',
            error
        );

        messageSearchResults.innerHTML =
            `
                <div class="message-search-empty">
                    ${escapeHtml(
                        error.message ||
                        'Не удалось выполнить поиск'
                    )}
                </div>
            `;

    } finally {

        messageSearchController =
            null;
    }
}


/*
 * =========================
 * Отображение результатов поиска
 * =========================
 */

function renderMessageSearchResults(
    messages,
    query
) {

    if (!messageSearchResults) {
        return;
    }

    messageSearchResults.innerHTML =
        '';

    if (
        !Array.isArray(messages) ||
        messages.length === 0
    ) {

        messageSearchResults.innerHTML =
            `
                <div class="message-search-empty">
                    Ничего не найдено
                </div>
            `;

        return;
    }

    messages.forEach(
        message => {

            const result =
                document.createElement(
                    'div'
                );

            result.className =
                'message-search-result';

            result.dataset.messageId =
                message.id;


            /*
             * Header
             */

            const header =
                document.createElement(
                    'div'
                );

            header.className =
                'message-search-result-header';


            /*
             * Автор
             */

            const sender =
                document.createElement(
                    'span'
                );

            sender.className =
                'message-search-result-sender';

            sender.textContent =
                message.senderName ||
                message.senderEmail ||
                'Участник';


            /*
             * Дата
             */

            const date =
                document.createElement(
                    'span'
                );

            date.className =
                'message-search-result-date';

            date.textContent =
                formatSearchMessageDate(
                    message.createdAt
                );


            header.appendChild(
                sender
            );

            header.appendChild(
                date
            );


            /*
             * Текст
             */

            const content =
                document.createElement(
                    'div'
                );

            content.className =
                'message-search-result-content';

            content.innerHTML =
                highlightSearchQuery(
                    escapeHtml(
                        message.content || ''
                    ),
                    query
                );


            result.appendChild(
                header
            );

            result.appendChild(
                content
            );


            /*
             * Клик по результату
             */

            result.addEventListener(
                'click',
                () => {

                    focusMessage(
                        message.id
                    );
                }
            );


            messageSearchResults.appendChild(
                result
            );
        }
    );
}


/*
 * =========================
 * Безопасное экранирование HTML
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
 * Подсветка поискового запроса
 * =========================
 */

function highlightSearchQuery(
    html,
    query
) {

    if (!query) {
        return html;
    }

    const escapedQuery =
        query.replace(
            /[.*+?^${}()|[\]\\]/g,
            '\\$&'
        );

    return html.replace(
        new RegExp(
            `(${escapedQuery})`,
            'gi'
        ),
        '<span class="message-search-highlight">$1</span>'
    );
}


/*
 * =========================
 * Дата результата поиска
 * =========================
 */

function formatSearchMessageDate(
    value
) {

    if (!value) {
        return '';
    }

    const date =
        new Date(value);

    if (
        Number.isNaN(
            date.getTime()
        )
    ) {
        return '';
    }

    return date.toLocaleString(
        'ru-RU',
        {
            day: '2-digit',
            month: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }
    );
}


/*
 * =========================
 * Переход к сообщению
 * =========================
 */

async function focusMessage(messageId) {

    if (!messageId) {
        return;
    }

    /*
     * scrollToMessage уже умеет:
     * 1. Найти сообщение в текущем DOM.
     * 2. Если его нет — загрузить контекст
     *    через /messages/around/{id}.
     * 3. Подсветить и прокрутить.
     */

    await scrollToMessage(messageId);
}

function initializeEmojiPicker() {

    if (
        !emojiButton
        || !emojiPicker
        || !emojiPickerContent
    ) {
        return;
    }

    const emojis = [
        '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣',
        '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰',
        '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜',
        '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏',
        '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣',
        '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠',
        '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨',
        '😰', '😥', '😓', '🤗', '🤔', '🫡', '🤭', '🤫',
        '🤥', '😶', '😐', '😑', '😬', '🙄', '😯', '😦',
        '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵',
        '🤐', '🤢', '🤮', '🤧', '😷', '🤒', '🤕', '🤑',

        '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',
        '👈', '👉', '👆', '👇', '☝️', '✋', '🤚', '🖐️',
        '👏', '🙌', '👐', '🤝', '🙏', '💪', '❤️', '🧡',
        '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
        '🔥', '⭐', '✨', '💯', '🎉', '🎊', '💥', '💫'
    ];

    emojis.forEach(emoji => {

        const button =
            document.createElement('button');

        button.type = 'button';

        button.dataset.emoji = emoji;

        button.textContent = emoji;

        button.title = emoji;

        emojiPickerContent.appendChild(button);

    });


    emojiButton.addEventListener(
        'click',
        event => {

            event.stopPropagation();

            emojiPicker.classList.toggle('open');

        }
    );


    emojiPicker.addEventListener(
        'click',
        event => {

            const button =
                event.target.closest(
                    '[data-emoji]'
                );

            if (!button) {
                return;
            }

            insertEmoji(
                button.dataset.emoji
            );

        }
    );


    document.addEventListener(
        'click',
        event => {

            if (
                !emojiPicker.contains(event.target)
                && event.target !== emojiButton
            ) {
                emojiPicker.classList.remove('open');
            }

        }
    );
}

function insertEmoji(emoji) {

    if (!messageInput) {
        return;
    }

    const start =
        messageInput.selectionStart;

    const end =
        messageInput.selectionEnd;

    const value =
        messageInput.value;

    messageInput.value =
        value.slice(0, start)
        + emoji
        + value.slice(end);

    const cursorPosition =
        start + emoji.length;

    messageInput.focus();

    messageInput.setSelectionRange(
        cursorPosition,
        cursorPosition
    );

    messageInput.dispatchEvent(
        new Event('input', {
            bubbles: true
        })
    );
}

/*
 * =========================
 * Реакции
 * =========================
 *
 * Список реакций приходит с backend.
 */

let availableReactions = [];


/*
 * =========================
 * Загрузка каталога реакций
 * =========================
 */

async function loadAvailableReactions() {

    try {

        const response =
            await fetch(
                '/api/messages/reactions'
            );

        if (!response.ok) {

            throw new Error(
                'Не удалось загрузить список реакций'
            );
        }

        const reactions =
            await response.json();

        availableReactions =
            Array.isArray(reactions)
                ? reactions
                : [];

        renderAllReactionPickers();

    } catch (error) {

        console.error(
            'Failed to load reactions:',
            error
        );

        availableReactions = [];
    }
}


function renderReactionPicker(
    reactionPicker
) {

    if (!reactionPicker) {
        return;
    }

    reactionPicker.innerHTML =
        '';

    availableReactions.forEach(
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
}


function renderAllReactionPickers() {

    document
        .querySelectorAll(
            '.reaction-picker'
        )
        .forEach(
            reactionPicker => {

                renderReactionPicker(
                    reactionPicker
                );
            }
        );
}


/*
 * =========================
 * Audio notifications
 * =========================
 */

let notificationAudioContext = null;


function getNotificationAudioContext() {

    if (!notificationAudioContext) {

        const AudioContextClass =
            window.AudioContext ||
            window.webkitAudioContext;

        if (!AudioContextClass) {
            return null;
        }

        notificationAudioContext =
            new AudioContextClass();
    }

    return notificationAudioContext;
}


async function unlockNotificationAudio() {

    const audioContext =
        getNotificationAudioContext();

    if (!audioContext) {
        return;
    }

    if (
        audioContext.state ===
        'suspended'
    ) {

        try {

            await audioContext.resume();

        } catch (error) {

            console.warn(
                'Unable to unlock notification audio:',
                error
            );
        }
    }
}


function playMessageNotificationSound() {

    const audioContext =
        getNotificationAudioContext();

    if (!audioContext) {
        return;
    }

    try {

        if (
            audioContext.state ===
            'suspended'
        ) {

            audioContext
                .resume()
                .catch(
                    () => {}
                );
        }

        if (
            audioContext.state !==
            'running'
        ) {
            return;
        }

        const oscillator =
            audioContext.createOscillator();

        const gain =
            audioContext.createGain();

        oscillator.type =
            'sine';

        oscillator.frequency.setValueAtTime(
            880,
            audioContext.currentTime
        );

        oscillator.frequency.exponentialRampToValueAtTime(
            660,
            audioContext.currentTime + 0.12
        );

        gain.gain.setValueAtTime(
            0.0001,
            audioContext.currentTime
        );

        gain.gain.exponentialRampToValueAtTime(
            0.12,
            audioContext.currentTime + 0.01
        );

        gain.gain.exponentialRampToValueAtTime(
            0.0001,
            audioContext.currentTime + 0.16
        );

        oscillator.connect(
            gain
        );

        gain.connect(
            audioContext.destination
        );

        oscillator.start();

        oscillator.stop(
            audioContext.currentTime + 0.16
        );

    } catch (error) {

        console.warn(
            'Unable to play message notification sound:',
            error
        );
    }
}


document.addEventListener(
    'click',
    () =>
        unlockNotificationAudio(),
    {
        once: true,
        passive: true
    }
);


document.addEventListener(
    'keydown',
    () =>
        unlockNotificationAudio(),
    {
        once: true,
        passive: true
    }
);


document.addEventListener(
    'touchstart',
    () =>
        unlockNotificationAudio(),
    {
        once: true,
        passive: true
    }
);


/*
 * =========================
 * Attachments
 * =========================
 */

const attachmentButton =
    document.getElementById(
        'attachment-button'
    );

const attachmentPicker =
    document.getElementById(
        'attachment-picker'
    );

const closeAttachmentPicker =
    document.getElementById(
        'close-attachment-picker'
    );

const attachmentSearch =
    document.getElementById(
        'attachment-search'
    );

const myAttachments =
    document.getElementById(
        'my-attachments'
    );

const uploadAttachmentButton =
    document.getElementById(
        'upload-attachment-button'
    );

const attachmentFileInput =
    document.getElementById(
        'attachment-file-input'
    );

const selectedAttachmentsContainer =
    document.getElementById(
        'selected-attachments'
    );

let myAttachmentsLoaded =
    false;

let myAttachmentsList =
    [];

const selectedAttachments =
    new Map();


/*
 * =========================
 * Attachment menu
 * =========================
 */

const messageAttachmentMenu =
    document.getElementById(
        'message-attachment-menu'
    );

const downloadMessageAttachmentButton =
    document.getElementById(
        'download-message-attachment'
    );

const saveMessageAttachmentButton =
    document.getElementById(
        'save-message-attachment'
    );

let selectedMessageAttachment =
    null;


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
            !attachmentPicker ||
            !attachmentButton
        ) {
            return;
        }

        if (
            !attachmentPicker.contains(
                event.target
            )
            &&
            event.target !==
            attachmentButton
        ) {

            attachmentPicker.classList.remove(
                'open'
            );
        }
    }
);


/*
 * =========================
 * Всплывающее меню attachment
 * =========================
 */

function closeMessageAttachmentMenu() {

    if (!messageAttachmentMenu) {
        return;
    }

    messageAttachmentMenu.hidden =
        true;

    messageAttachmentMenu.style.display =
        'none';

    selectedMessageAttachment =
        null;
}


function openMessageAttachmentMenu(
    attachmentElement
) {

    if (
        !messageAttachmentMenu ||
        !attachmentElement
    ) {
        return;
    }

    const attachmentId =
        attachmentElement.dataset.attachmentId;

    const downloadUrl =
        attachmentElement.dataset.downloadUrl;

    if (
        !attachmentId ||
        !downloadUrl
    ) {
        return;
    }

    selectedMessageAttachment = {
        id:
            attachmentId,

        downloadUrl:
            downloadUrl
    };

    messageAttachmentMenu.hidden =
        false;

    messageAttachmentMenu.style.display =
        'block';

    const rect =
        attachmentElement.getBoundingClientRect();

    const menuWidth =
        messageAttachmentMenu.offsetWidth;

    const menuHeight =
        messageAttachmentMenu.offsetHeight;

    const margin =
        8;

    let left =
        rect.left;

    let top =
        rect.bottom +
        margin;

    if (
        left +
        menuWidth >
        window.innerWidth -
        margin
    ) {

        left =
            window.innerWidth -
            menuWidth -
            margin;
    }

    if (
        top +
        menuHeight >
        window.innerHeight -
        margin
    ) {

        top =
            rect.top -
            menuHeight -
            margin;
    }

    if (top < margin) {
        top = margin;
    }

    if (left < margin) {
        left = margin;
    }

    if (
        left +
        menuWidth >
        window.innerWidth -
        margin
    ) {

        left =
            Math.max(
                margin,
                window.innerWidth -
                menuWidth -
                margin
            );
    }

    messageAttachmentMenu.style.left =
        `${left}px`;

    messageAttachmentMenu.style.top =
        `${top}px`;
}


/*
 * =========================
 * Клик по attachment
 * =========================
 */

messagesContainer.addEventListener(
    'click',
    event => {

        const attachment =
            event.target.closest(
                '.message-attachment'
            );

        if (!attachment) {
            return;
        }

        event.preventDefault();

        event.stopPropagation();

        closeContextMenu();

        if (attachmentPicker) {

            attachmentPicker.classList.remove(
                'open'
            );
        }

        openMessageAttachmentMenu(
            attachment
        );
    }
);


/*
 * =========================
 * Скачать attachment
 * =========================
 */

if (downloadMessageAttachmentButton) {

    downloadMessageAttachmentButton.addEventListener(
        'click',
        event => {

            event.preventDefault();

            event.stopPropagation();

            if (
                !selectedMessageAttachment
            ) {
                return;
            }

            const downloadUrl =
                selectedMessageAttachment
                    .downloadUrl;

            closeMessageAttachmentMenu();

            window.location.href =
                downloadUrl;
        }
    );
}


/*
 * =========================
 * Добавить в "Мои файлы"
 * =========================
 */

if (saveMessageAttachmentButton) {

    saveMessageAttachmentButton.addEventListener(
        'click',
        async event => {

            event.preventDefault();

            event.stopPropagation();

            if (
                !selectedMessageAttachment
            ) {
                return;
            }

            const attachmentId =
                selectedMessageAttachment.id;

            closeMessageAttachmentMenu();

            showSendError('');

            try {

                const response =
                    await fetch(
                        `/api/attachments/${attachmentId}/copy`,
                        {
                            method:
                                'POST'
                        }
                    );

                if (!response.ok) {

                    let message =
                        'Не удалось добавить файл в «Мои файлы»';

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

                if (
                    response.status !==
                    204
                ) {

                    try {

                        const attachment =
                            await response.json();

                        if (attachment) {

                            const exists =
                                myAttachmentsList.some(
                                    item =>
                                        item.id ===
                                        attachment.id
                                );

                            if (!exists) {

                                myAttachmentsList = [
                                    attachment,
                                    ...myAttachmentsList
                                ];
                            }

                            myAttachmentsLoaded =
                                true;
                        }

                    } catch (_) {
                    }
                }

                if (
                    response.status ===
                    204
                ) {

                    myAttachmentsLoaded =
                        false;

                    await loadMyAttachments();
                }

                showSendError(
                    'Файл добавлен в «Мои файлы»'
                );

                setTimeout(
                    () => {

                        if (
                            sendError &&
                            sendError.textContent ===
                            'Файл добавлен в «Мои файлы»'
                        ) {

                            showSendError('');
                        }

                    },
                    2000
                );

            } catch (error) {

                console.error(
                    'Failed to save attachment:',
                    error
                );

                showSendError(
                    error.message
                );
            }
        }
    );
}


/*
 * =========================
 * Закрытие attachment menu
 * =========================
 */

document.addEventListener(
    'click',
    event => {

        if (
            !messageAttachmentMenu
        ) {
            return;
        }

        if (
            messageAttachmentMenu.hidden
        ) {
            return;
        }

        if (
            messageAttachmentMenu.contains(
                event.target
            )
        ) {
            return;
        }

        if (
            event.target.closest(
                '.message-attachment'
            )
        ) {
            return;
        }

        closeMessageAttachmentMenu();
    }
);


/*
 * =========================
 * Закрытие при прокрутке
 * =========================
 */

messagesContainer.addEventListener(
    'scroll',
    () => {

        if (
            messageAttachmentMenu &&
            !messageAttachmentMenu.hidden
        ) {

            closeMessageAttachmentMenu();
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
                    ${escapeHtml(
                        error.message
                    )}
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

            const files =
                Array.from(
                    attachmentFileInput.files ||
                    []
                );

            if (files.length === 0) {
                return;
            }

            for (const file of files) {

                await uploadAttachment(
                    file
                );
            }

            attachmentFileInput.value =
                '';
        }
    );
}


async function uploadAttachment(
    file
) {

    if (!file) {
        return;
    }

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
                    method:
                        'POST',

                    body:
                        formData
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

        myAttachmentsList = [
            attachment,
            ...myAttachmentsList
        ];

        myAttachmentsLoaded =
            true;

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
    document.getElementById(
        'context-menu'
    );

const deleteMessageButton =
    document.getElementById(
        'delete-message-button'
    );

const pageSize =
    15;

let loadingOlderMessages =
    false;

let loadingNewerMessages =
    false;

let hasMoreMessages =
    true;

let hasMoreNewerMessages =
    false;

let selectedMessage =
    null;

let longPressTimer =
    null;


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

            case 'MESSAGE_EDITED':

                handleMessageEdited(
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

function handleMessageEdited(wsEvent) {

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

    const messageElement =
        messagesContainer.querySelector(
            `[data-message-id="${payload.messageId}"]`
        );

    if (!messageElement) {
        return;
    }

    const contentElement =
        messageElement.querySelector(
            '.message-content'
        );

    if (contentElement) {
        contentElement.textContent =
            payload.content || '';
    }

    messageElement.dataset.editedAt =
        payload.editedAt || '';

    const existingEdited =
        messageElement.querySelector(
            '.message-edited'
        );

    if (existingEdited) {
        existingEdited.remove();
    }

    if (payload.editedAt) {

        const timeElement =
            messageElement.querySelector(
                '.message-time'
            );

        if (timeElement) {

            const editedElement =
                document.createElement('span');

            editedElement.className =
                'message-edited';

            editedElement.textContent =
                ' · изменено';

            timeElement.appendChild(
                editedElement
            );
        }
    }
}

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

        readByOther:
            false,

        reactions:
            message.reactions || [],

        attachments:
            message.attachments || []
    };

    if (
        String(message.senderId) !==
        String(currentUserId)
    ) {

        playMessageNotificationSound();
    }

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
        messagesContainer.clientHeight <
        100;

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
            wsEvent.payload
                ?.lastReadMessageId
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
                    messageElement.dataset
                        .messageId
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
 * Прокрутка к сообщению
 * =========================
 */

async function scrollToMessage(
    messageId
) {

    if (!messageId) {
        return;
    }

    const messageElement =
        messagesContainer.querySelector(
            `[data-message-id="${messageId}"]`
        );

    /*
     * Сообщение уже загружено.
     */
    if (messageElement) {

        highlightMessage(
            messageElement
        );

        return;
    }

    /*
     * Сообщения нет в текущем окне.
     * Загружаем его вместе с контекстом.
     */
    await loadMessageContext(
        messageId
    );
}


function highlightMessage(
    messageElement
) {

    if (!messageElement) {
        return;
    }

    messageElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
    });

    messageElement.classList.remove(
        'reply-highlight'
    );

    void messageElement.offsetWidth;

    messageElement.classList.add(
        'reply-highlight'
    );

    setTimeout(
        () => {

            messageElement.classList.remove(
                'reply-highlight'
            );

        },
        1500
    );
}


async function loadMessageContext(messageId) {

    try {

        showSendError('');

        const response =
            await fetch(
                `/api/dialogs/${dialogId}/messages/around/${messageId}` +
                `?before=10&after=10`
            );

        if (!response.ok) {
            // ... обработка ошибки как есть ...
        }

        const messages =
            await response.json();

        if (!Array.isArray(messages) || messages.length === 0) {
            throw new Error('Сообщение не найдено');
        }

        /*
         * Сбрасываем состояние пагинации,
         * чтобы после перехода можно было
         * снова грузить историю в обе стороны.
         */

        loadingOlderMessages = false;
        loadingNewerMessages = false;

        messagesContainer.innerHTML = '';

        /*
         * Возвращаем индикатор загрузки,
         * так как loadOlderMessages ищет его
         * по id при каждой загрузке.
         */

        const indicator =
            document.createElement('div');

        indicator.id = 'loading-indicator';

        indicator.className = 'loading-indicator';

        indicator.style.display = 'none';

        indicator.textContent = 'Загрузка сообщений...';

        messagesContainer.appendChild(indicator);

        const fragment =
            document.createDocumentFragment();

        messages.forEach(
            message => {

                const element =
                    createMessageElement(message);

                fragment.appendChild(element);
            }
        );

        messagesContainer.appendChild(fragment);

        hasMoreMessages = true;
        hasMoreNewerMessages = true;

        const target =
            messagesContainer.querySelector(
                `[data-message-id="${messageId}"]`
            );

        if (!target) {
            return;
        }

        target.scrollIntoView({
            behavior: 'instant',
            block: 'center'
        });

        highlightMessage(target);

    } catch (error) {

        console.error(
            'Failed to load message context:',
            error
        );

        showSendError(
            error.message ||
            'Не удалось перейти к сообщению'
        );
    }
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

    element.dataset.editedAt =
        message.editedAt || '';

    if (message.deletedAt) {

        element.classList.add(
            'deleted'
        );
    }

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

            authorElement.textContent =
                author.text;

        } else {

            const authorLink =
                document.createElement(
                    'a'
                );

            authorLink.className =
                'message-author-link';

            authorLink.href =
                `/users/${message.senderId}`;

            authorLink.textContent =
                author.text;

            authorElement.appendChild(
                authorLink
            );
        }

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
         * Reply
         */

        if (
            message.replyTo
        ) {

            const replyElement =
                document.createElement(
                    'div'
                );

            replyElement.className =
                'message-reply';

            replyElement.dataset.replyMessageId =
                message.replyTo.id;

            const replyLine =
                document.createElement(
                    'div'
                );

            replyLine.className =
                'message-reply-line';

            const replyContent =
                document.createElement(
                    'div'
                );

            replyContent.className =
                'message-reply-content';

            const replyAuthor =
                document.createElement(
                    'div'
                );

            replyAuthor.className =
                'message-reply-author';

            replyAuthor.textContent =
                message.replyTo.senderName ||
                'Пользователь';

            const replyText =
                document.createElement(
                    'div'
                );

            replyText.className =
                'message-reply-text';

            if (
                message.replyTo.deleted
            ) {

                replyText.classList.add(
                    'deleted'
                );

                replyText.textContent =
                    'Сообщение удалено';

            } else {

                replyText.textContent =
                    message.replyTo.content ||
                    'Вложение';
            }

            replyContent.appendChild(
                replyAuthor
            );

            replyContent.appendChild(
                replyText
            );

            replyElement.appendChild(
                replyLine
            );

            replyElement.appendChild(
                replyContent
            );

            element.appendChild(
                replyElement
            );
        }


        /*
         * Текст
         */

    /*
     * Picker
     */

    const reactionPicker =
        document.createElement(
            'div'
        );

    reactionPicker.className =
        'reaction-picker';

    availableReactions.forEach(
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

    if (
        message.content &&
        message.content.trim()
    ) {

        const content =
            document.createElement(
                'div'
            );

        content.className =
            'message-content';

        content.textContent =
            message.content;

        element.appendChild(
            content
        );
    }


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

                const attachmentElement =
                    document.createElement(
                        'div'
                    );

                attachmentElement.className =
                    'message-attachment';

                attachmentElement.setAttribute(
                    'role',
                    'button'
                );

                attachmentElement.setAttribute(
                    'tabindex',
                    '0'
                );

                attachmentElement.dataset.attachmentId =
                    attachment.id;

                attachmentElement.dataset.downloadUrl =
                    attachment.downloadUrl;

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

                attachmentElement.appendChild(
                    icon
                );

                attachmentElement.appendChild(
                    info
                );

                attachmentsContainer.appendChild(
                    attachmentElement
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

        const content =
            element.querySelector(
                '.message-content'
            );

        if (content) {

            content.after(
                deletedLabel
            );

        } else {

            element.appendChild(
                deletedLabel
            );
        }
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
                hour:
                    '2-digit',

                minute:
                    '2-digit'
            }
        );

    time.appendChild(
        timeText
    );

    if (message.editedAt) {

        const edited =
            document.createElement(
                'span'
            );

        edited.className =
            'message-edited';

        edited.textContent =
            ' · изменено';

        time.appendChild(
            edited
        );
    }


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
 * Reply
 * =========================
 */

function startReply(
    messageElement
) {

    if (!messageElement) {
        return;
    }

    if (
        messageElement.classList.contains(
            'deleted'
        )
    ) {
        return;
    }

    const messageId =
        messageElement.dataset.messageId;

    if (!messageId) {
        return;
    }

    replyToMessageId =
        Number(messageId);

    let author =
        'Пользователь';

    const authorLink =
        messageElement.querySelector(
            '.message-author-link'
        );

    if (authorLink) {

        author =
            authorLink.textContent.trim();

    } else if (
        messageElement.classList.contains(
            'mine'
        )
    ) {

        author =
            'Вы';
    }

    let text =
        '';

    const content =
        messageElement.querySelector(
            '.message-content'
        );

    if (content) {

        text =
            content.textContent.trim();
    }

    if (!text) {

        const attachments =
            messageElement.querySelector(
                '.message-attachments'
            );

        if (attachments) {

            text =
                'Вложение';

        } else {

            text =
                'Сообщение';
        }
    }

    if (replyPreviewAuthor) {

        replyPreviewAuthor.textContent =
            author;
    }

    if (replyPreviewText) {

        replyPreviewText.textContent =
            text;
    }

    if (replyPreview) {

        replyPreview.style.display =
            'flex';
    }

    if (messageInput) {

        messageInput.focus();
    }

    closeContextMenu();
}

function startEdit(messageElement) {
    if (!messageElement) {
        return;
    }

    if (!messageElement.classList.contains('mine')) {
        return;
    }

    const messageId =
        messageElement.dataset.messageId;

    if (!messageId) {
        return;
    }

    const contentElement =
        messageElement.querySelector('.message-content');

    const content =
        contentElement
            ? contentElement.textContent
            : '';

    if (!content.trim()) {
        return;
    }

    editingMessageId = Number(messageId);

    messageInput.value = content;

    if (editPreview) {
        editPreview.style.display = 'flex';
    }

    if (editPreviewText) {
        editPreviewText.textContent = content;
    }

    cancelReply();

    selectedAttachments.clear();
    renderSelectedAttachments();
    renderMyAttachments(
        getCurrentMyAttachments()
    );

    messageInput.focus();

    messageInput.setSelectionRange(
        messageInput.value.length,
        messageInput.value.length
    );
}


function cancelEdit() {
    editingMessageId = null;

    messageInput.value = '';

    if (editPreview) {
        editPreview.style.display = 'none';
    }

    if (editPreviewText) {
        editPreviewText.textContent = '';
    }

    messageInput.focus();
}


function cancelReply() {

    replyToMessageId =
        null;

    if (replyPreview) {

        replyPreview.style.display =
            'none';
    }

    if (replyPreviewAuthor) {

        replyPreviewAuthor.textContent =
            '';
    }

    if (replyPreviewText) {

        replyPreviewText.textContent =
            '';
    }
}


if (replyMessageButton) {

    replyMessageButton.addEventListener(
        'click',
        () => {

            if (!selectedMessage) {
                return;
            }

            const messageElement =
                selectedMessage;

            startReply(
                messageElement
            );
        }
    );
}

if (cancelEditButton) {
    cancelEditButton.addEventListener(
        'click',
        () => {
            cancelEdit();
        }
    );
}

if (editMessageButton) {

    editMessageButton.addEventListener(
        'click',
        () => {

            if (!selectedMessage) {
                return;
            }

            const messageElement =
                selectedMessage;

            startEdit(
                messageElement
            );

            closeContextMenu();
        }
    );
}


if (cancelReplyButton) {

    cancelReplyButton.addEventListener(
        'click',
        () => {

            cancelReply();

            if (messageInput) {

                messageInput.focus();
            }
        }
    );
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

function getNewestMessageId() {

    const messages =
        messagesContainer.querySelectorAll(
            '[data-message-id]'
        );

    if (messages.length === 0) {
        return null;
    }

    return messages[messages.length - 1]
        .dataset
        .messageId;
}

async function loadNewerMessages() {

    if (
        loadingNewerMessages ||
        !hasMoreNewerMessages
    ) {
        return;
    }

    const newestMessageId =
        getNewestMessageId();

    if (!newestMessageId) {
        return;
    }

    loadingNewerMessages = true;

    try {

        const response =
            await fetch(
                `/api/dialogs/${dialogId}/messages/after?afterId=${newestMessageId}`
            );

        if (!response.ok) {

            throw new Error(
                'Не удалось загрузить новые сообщения'
            );
        }

        const messages =
            await response.json();

        if (
            !messages ||
            messages.length === 0
        ) {

            hasMoreNewerMessages =
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

        messagesContainer.appendChild(
            fragment
        );

        hasMoreNewerMessages =
            messages.length >= pageSize;

    } catch (error) {

        console.error(
            'Failed to load newer messages:',
            error
        );

        showSendError(
            error.message
        );

    } finally {

        loadingNewerMessages = false;
    }
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

        hasMoreMessages =
            messages.length >= pageSize;

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

        /*
         * Дошли до верха —
         * загружаем старые сообщения.
         */
        if (
            messagesContainer.scrollTop <= 30
        ) {

            loadOlderMessages();

            return;
        }

        /*
         * Дошли до низа —
         * загружаем более новые сообщения.
         */
        const distanceToBottom =
            messagesContainer.scrollHeight -
            messagesContainer.scrollTop -
            messagesContainer.clientHeight;

        if (
            distanceToBottom <= 30
        ) {

            loadNewerMessages();
        }
    }
);


/*
 * =========================
 * Context menu
 * =========================
 */

function closeContextMenu() {

    if (!contextMenu) {
        return;
    }

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

    selectedMessage =
        messageElement;

    const editButton =
        document.getElementById(
            'edit-message-button'
        );

    const deleteButton =
        document.getElementById(
            'delete-message-button'
        );

    const isMine =
        messageElement.classList.contains(
            'mine'
        );

    if (editButton) {
        editButton.style.display =
            isMine ? '' : 'none';
    }

    if (deleteButton) {
        deleteButton.style.display =
            isMine ? '' : 'none';
    }

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

        if (
            event.target.closest(
                '.message-attachment'
            )
        ) {
            return;
        }

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

        if (!messageElement) {
            return;
        }

        if (
            event.target.closest(
                '.message-attachment'
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
        passive:
            true
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
 * Реакции — click
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
                            element !==
                            picker
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
        !availableReactions.includes(
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
                    method:
                        'PUT',

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

document.addEventListener('click', event => {
    const replyElement = event.target.closest('.message-reply');

    if (!replyElement) {
        return;
    }

    const messageId = replyElement.dataset.replyMessageId;

    if (!messageId) {
        return;
    }

    scrollToMessage(messageId);
});

/*
 * =========================
 * Закрытие context menu
 * =========================
 */

document.addEventListener(
    'click',
    event => {

        if (
            contextMenu &&
            !contextMenu.contains(
                event.target
            )
        ) {

            closeContextMenu();
        }
    }
);


/*
 * =========================
 * Escape
 * =========================
 */

document.addEventListener(
    'keydown',
    event => {

        if (event.key !== 'Escape') {
            return;
        }

        closeContextMenu();

        closeMessageAttachmentMenu();

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

        closeMessageSearch();
    }
);


/*
 * =========================
 * Удаление сообщения
 * =========================
 */

if (deleteMessageButton) {

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
                            method:
                                'DELETE'
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

                    if (content) {

                        content.after(
                            deletedLabel
                        );

                    } else {

                        messageElement.appendChild(
                            deletedLabel
                        );
                    }
                }

            } catch (error) {

                showSendError(
                    error.message
                );
            }
        }
    );
}


/*
 * =========================
 * Первоначальная загрузка
 * =========================
 */

async function initializeDialog() {

    initializeEmojiPicker();

    await loadAvailableReactions();

    scrollToBottom();
}

initializeDialog();


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

        if (!content) {
            return;
        }

        showSendError('');

        /*
         * =========================
         * Редактирование сообщения
         * =========================
         */

        if (editingMessageId !== null) {

            try {

                const response =
                    await fetch(
                        `/api/dialogs/${dialogId}/messages/${editingMessageId}`,
                        {
                            method:
                                'PATCH',

                            headers: {
                                'Content-Type':
                                    'application/json'
                            },

                            body:
                                JSON.stringify({
                                    content
                                })
                        }
                    );

                if (!response.ok) {

                    let message =
                        'Не удалось изменить сообщение';

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

                await response.json();

                cancelEdit();

            } catch (error) {

                showSendError(
                    error.message
                );
            }

            return;
        }

        /*
         * =========================
         * Обычная отправка
         * =========================
         */

        const attachmentIds =
            Array.from(
                selectedAttachments.keys()
            );

        showSendError('');

        try {

            const response =
                await fetch(
                    `/api/dialogs/${dialogId}/messages`,
                    {
                        method:
                            'POST',

                        headers: {
                            'Content-Type':
                                'application/json'
                        },

                        body:
                            JSON.stringify({
                                content,
                                attachmentIds,
                                replyToMessageId
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

            messageInput.value =
                '';

            selectedAttachments.clear();

            renderSelectedAttachments();

            renderMyAttachments(
                getCurrentMyAttachments()
            );

            cancelReply();

            hasMoreNewerMessages = false;

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

    if (
        bytes <
        1024 * 1024
    ) {

        return `${(
            bytes / 1024
        ).toFixed(1)} КБ`;
    }

    if (
        bytes <
        1024 * 1024 * 1024
    ) {

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