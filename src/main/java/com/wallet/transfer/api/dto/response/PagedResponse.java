package com.wallet.transfer.api.dto.response;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data
public class PagedResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public static <E,D> PagedResponse<D> of(Page<E> page, Function<E,D> mapper) {
        PagedResponse<D> r=new PagedResponse<>();
        r.setContent(page.getContent().stream().map(mapper).toList());
        r.setTotalElements(page.getTotalElements());
        r.setTotalPages(page.getTotalPages());
        r.setPage(page.getNumber());
        r.setSize(page.getSize());
        return r;
    }
}
