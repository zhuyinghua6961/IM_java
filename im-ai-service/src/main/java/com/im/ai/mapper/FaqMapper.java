package com.im.ai.mapper;

import com.im.ai.entity.Faq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * FAQ Mapper
 */
@Mapper
public interface FaqMapper {

    /**
     * 查询所有启用的FAQ
     */
    List<Faq> selectAll();

    /**
     * 根据关键词搜索FAQ
     */
    List<Faq> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 根据分类查询FAQ
     */
    List<Faq> findByCategory(@Param("category") String category);

    /**
     * 根据ID查询
     */
    Faq selectById(@Param("id") Long id);
}
