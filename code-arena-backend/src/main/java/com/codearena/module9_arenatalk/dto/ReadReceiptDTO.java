package com.codearena.module9_arenatalk.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadReceiptDTO {
    private Long messageId;
    private long readCount;
    private boolean readByCurrentUser;
}