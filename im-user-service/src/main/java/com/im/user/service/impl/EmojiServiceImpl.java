package com.im.user.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.user.context.UserContext;
import com.im.user.dto.FavoriteEmojiDTO;
import com.im.user.entity.UserFavoriteEmoji;
import com.im.user.mapper.UserFavoriteEmojiMapper;
import com.im.user.service.EmojiService;
import com.im.user.vo.EmojiVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmojiServiceImpl implements EmojiService {

    @Autowired
    private UserFavoriteEmojiMapper emojiMapper;

    @Override
    public List<EmojiVO> listCurrentUserEmojis() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        List<UserFavoriteEmoji> list = emojiMapper.selectByUserId(userId);
        List<EmojiVO> result = new ArrayList<>();
        for (UserFavoriteEmoji emoji : list) {
            EmojiVO vo = toVO(emoji);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmojiVO addEmoji(FavoriteEmojiDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (dto == null || dto.getUrl() == null || dto.getUrl().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "表情URL不能为空");
        }

        log.info("添加收藏表情，userId={}, url={}", userId, dto.getUrl());

        // 已存在则直接返回（若之前被删除则恢复）
        UserFavoriteEmoji exist = emojiMapper.selectByUserIdAndUrl(userId, dto.getUrl());
        if (exist != null) {
            if (exist.getStatus() != null && exist.getStatus() == 1) {
                return toVO(exist);
            }
            // 之前删除过，现在恢复为正常状态
            emojiMapper.updateStatusByIdAndUserId(exist.getId(), userId, 1);
            exist.setStatus(1);
            return toVO(exist);
        }

        UserFavoriteEmoji emoji = new UserFavoriteEmoji();
        emoji.setUserId(userId);
        emoji.setUrl(dto.getUrl());
        emoji.setFileName(dto.getFileName());
        emoji.setSize(dto.getSize());
        emoji.setWidth(dto.getWidth());
        emoji.setHeight(dto.getHeight());
        emoji.setContentType(dto.getContentType());
        emoji.setStatus(1);

        emojiMapper.insert(emoji);
        log.info("添加收藏表情成功，userId={}, emojiId={}", userId, emoji.getId());

        return toVO(emoji);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmoji(Long id) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "表情ID不能为空");
        }

        UserFavoriteEmoji emoji = emojiMapper.selectById(id);
        if (emoji == null || emoji.getStatus() == null || emoji.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "表情不存在");
        }
        if (!userId.equals(emoji.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该表情");
        }

        emojiMapper.updateStatusByIdAndUserId(id, userId, 0);
        log.info("删除收藏表情成功，userId={}, emojiId={}", userId, id);
    }

    private EmojiVO toVO(UserFavoriteEmoji emoji) {
        if (emoji == null) {
            return null;
        }
        EmojiVO vo = new EmojiVO();
        vo.setId(emoji.getId());
        vo.setUrl(emoji.getUrl());
        vo.setFileName(emoji.getFileName());
        vo.setSize(emoji.getSize());
        vo.setWidth(emoji.getWidth());
        vo.setHeight(emoji.getHeight());
        vo.setContentType(emoji.getContentType());
        return vo;
    }
}
