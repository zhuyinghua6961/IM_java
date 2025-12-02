package com.im.user.service;

import com.im.user.dto.FavoriteEmojiDTO;
import com.im.user.vo.EmojiVO;

import java.util.List;

public interface EmojiService {

    /**
     * 获取当前登录用户的收藏表情列表
     */
    List<EmojiVO> listCurrentUserEmojis();

    /**
     * 为当前登录用户添加收藏表情
     */
    EmojiVO addEmoji(FavoriteEmojiDTO dto);

    /**
     * 删除（取消收藏）表情
     */
    void deleteEmoji(Long id);
}
