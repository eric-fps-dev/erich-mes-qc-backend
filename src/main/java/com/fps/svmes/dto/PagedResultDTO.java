package com.fps.svmes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果 DTO，使用 Lombok 自动生成 Getter/Setter/构造器等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResultDTO<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;
}
