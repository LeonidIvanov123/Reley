package com.reley.messenger.message.reaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("message_reaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {

    @Column("message_id")
    private Long messageId;

    @Column("user_id")
    private Long userId;

    private String reaction;

    @Column("created_at")
    private OffsetDateTime createdAt;

}