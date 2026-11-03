package com.reley.messenger.dialog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("dialog_participants")
public class DialogParticipant {

    @Column("dialog_id")
    private Long dialogId;

    @Column("user_id")
    private Long userId;

    @Column("joined_at")
    private OffsetDateTime joinedAt;
}
