package com.reley.messenger.dialog.entity;

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
@Table("dialogs")
public class Dialog {

    @Id
    private Long id;

    @Column("dialog_name")
    private String dialogName;

    @Column("dialog_link")
    private String dialogLink;

    @Column("owner_id")
    private Long ownerId;

    @Column("participant_id")
    private Long participantId;

    @Column("is_public")
    private Boolean isPublic;

    @Column("group_chat")
    private boolean groupChat = false;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}