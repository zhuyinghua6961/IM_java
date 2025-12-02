package com.im.user.controller;

import com.im.common.vo.Result;
import com.im.user.dto.FavoriteEmojiDTO;
import com.im.user.service.EmojiService;
import com.im.user.vo.EmojiVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏表情相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/emoji")
public class EmojiController {

    @Autowired
    private EmojiService emojiService;

    /**
     * 获取当前用户的收藏表情列表
     */
    @GetMapping("/list")
    public Result<List<EmojiVO>> list() {
        List<EmojiVO> list = emojiService.listCurrentUserEmojis();
        return Result.success(list);
    }

    /**
     * 添加收藏表情
     */
    @PostMapping
    public Result<EmojiVO> add(@RequestBody FavoriteEmojiDTO dto) {
        EmojiVO vo = emojiService.addEmoji(dto);
        return Result.success(vo);
    }

    /**
     * 删除（取消收藏）表情
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        emojiService.deleteEmoji(id);
        return Result.success();
    }
}
