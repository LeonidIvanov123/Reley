package com.reley.messenger.message.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("messages")
public class Message {

    @Id
    private Long id;

    @Column("dialog_id")
    private Long dialogId;

    @Column("sender_id")
    private Long senderId;

    private String content;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("edited_at")
    private OffsetDateTime editedAt;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

}