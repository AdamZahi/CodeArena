package com.codearena.shared.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagedResponse<T> extends ApiResponse<T> {
    private int page;
    private int size;
    private long total;
}
