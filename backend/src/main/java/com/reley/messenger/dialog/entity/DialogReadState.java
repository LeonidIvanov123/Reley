package com.reley.messenger.dialog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Table("dialog_read_state")
public class DialogReadState {

    @Id
    private Long id;

    @Column("dialog_id")
    private Long dialogId;

    @Column("user_id")
    private Long userId;

    @Column("last_read_message_id")
    private Long lastReadMessageId;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}