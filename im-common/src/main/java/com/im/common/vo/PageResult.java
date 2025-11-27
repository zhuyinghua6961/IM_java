package com.im.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 分页响应结果类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<PageResult.PageData<T>> {
    
    public PageResult(List<T> records, Long total, Long size, Long current) {
        super(200, "success", new PageData<>(records, total, size, current));
    }
    
    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        return new PageResult<>(records, total, size, current);
    }
    
    @Data
    public static class PageData<T> {
        /**
         * 数据列表
         */
        private List<T> records;
        
        /**
         * 总记录数
         */
        private Long total;
        
        /**
         * 每页大小
         */
        private Long size;
        
        /**
         * 当前页
         */
        private Long current;
        
        /**
         * 总页数
         */
        private Long pages;
        
        public PageData(List<T> records, Long total, Long size, Long current) {
            this.records = records;
            this.total = total;
            this.size = size;
            this.current = current;
            this.pages = size > 0 ? (total + size - 1) / size : 0;
        }
    }
}
