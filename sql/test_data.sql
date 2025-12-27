-- ----------------------------
-- 测试数据 - 用户表 (ID: 10001-10010)
-- ----------------------------
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`, `gender`, `phone`, `email`, `signature`, `status`) VALUES
(10001, 'zhangsan', '123456', '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhangSan', 1, '13800138001', 'zhangsan@test.com', '今天天气真好', 1),
(10002, 'lisi', '123456', '李四', 'https://api.dicebear.com/7.x/avataaars/svg?seed=LiSi', 1, '13800138002', 'lisi@test.com', '人生若只如初见', 1),
(10003, 'wangwu', '123456', '王五', 'https://api.dicebear.com/7.x/avataaars/svg?seed=WangWu', 1, '13800138003', 'wangwu@test.com', '热爱编程', 1),
(10004, 'zhaoliu', '123456', '赵六', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhaoLiu', 1, '13800138004', 'zhaoliu@test.com', '一只特立独行的猪', 1),
(10005, 'sunqi', '123456', '孙七', 'https://api.dicebear.com/7.x/avataaars/svg?seed=SunQi', 1, '13800138005', 'sunqi@test.com', '生活不止眼前的苟且', 1),
(10006, 'zhouba', '123456', '周八', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhouBa', 1, '13800138006', 'zhouba@test.com', '技术改变世界', 1),
(10007, 'wujiu', '123456', '吴九', 'https://api.dicebear.com/7.x/avataaars/svg?seed=WuJiu', 1, '13800138007', 'wujiu@test.com', '保持热爱', 1),
(10008, 'zhengshi', '123456', '郑十', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhengShi', 1, '13800138008', 'zhengshi@test.com', '未来可期', 1),
(10009, 'test01', '123456', '测试用户1', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Test01', 2, '13800138009', 'test01@test.com', '我是测试用户', 1),
(10010, 'test02', '123456', '测试用户2', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Test02', 2, '13800138010', 'test02@test.com', '测试用户二号', 1);

-- ----------------------------
-- 测试数据 - 好友关系表 (ID: 10001-10014)
-- ----------------------------
INSERT INTO `friend` (`id`, `user_id`, `friend_id`, `remark`, `status`) VALUES
(10001, 10001, 10002, '老铁', 1),
(10002, 10001, 10003, '同事', 1),
(10003, 10001, 10004, '邻居', 1),
(10004, 10002, 10001, '好朋友', 1),
(10005, 10002, 10003, '球友', 1),
(10006, 10003, 10001, '兄弟', 1),
(10007, 10003, 10002, '同学', 1),
(10008, 10004, 10001, NULL, 1),
(10009, 10005, 10001, '闺蜜', 1),
(10010, 10006, 10003, '技术大牛', 1),
(10011, 10007, 10004, '驴友', 1),
(10012, 10008, 10005, '钓友', 1),
(10013, 10009, 10010, '情侣', 1),
(10014, 10010, 10009, '宝贝', 1);

-- ----------------------------
-- 测试数据 - 好友申请表 (ID: 10001-10005)
-- ----------------------------
INSERT INTO `friend_request` (`id`, `from_user_id`, `to_user_id`, `message`, `status`, `create_time`) VALUES
(10001, 10005, 10001, '我是孙七，通过一下呗', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10002, 10006, 10001, '周八请求添加您为好友', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10003, 10003, 10001, '好久不见，加个好友吧', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10004, 10007, 10002, '我是吴九，听说你很厉害', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10005, 10008, 10003, '郑十请求添加好友', 2, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ----------------------------
-- 测试数据 - 群组表 (ID: 10001-10005)
-- ----------------------------
INSERT INTO `group` (`id`, `group_name`, `avatar`, `owner_id`, `notice`, `max_members`, `status`, `create_time`) VALUES
(10001, '技术交流群', 'https://api.dicebear.com/7.x/identicon/svg?seed=TechGroup', 10001, '欢迎大家交流技术，禁止广告', 500, 1, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10002, '篮球爱好者', 'https://api.dicebear.com/7.x/identicon/svg?seed=Basketball', 10002, '周末一起打球', 200, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10003, '旅游俱乐部', 'https://api.dicebear.com/7.x/identicon/svg?seed=Travel', 10003, '分享旅行见闻', 200, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(10004, '产品经理群', 'https://api.dicebear.com/7.x/identicon/svg?seed=Product', 10004, '产品讨论，欢迎交流', 300, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10005, '测试小分队', 'https://api.dicebear.com/7.x/identicon/svg?seed=TestTeam', 10009, '测试专用群', 100, 1, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ----------------------------
-- 测试数据 - 群成员表 (ID: 10001-10019)
-- ----------------------------
INSERT INTO `group_member` (`id`, `group_id`, `user_id`, `role`, `nickname`, `join_time`, `status`) VALUES
-- 技术交流群 (群主: 张三)
(10001, 10001, 10001, 2, '群主', DATE_SUB(NOW(), INTERVAL 30 DAY), 1),
(10002, 10001, 10002, 1, '管理员李四', DATE_SUB(NOW(), INTERVAL 29 DAY), 1),
(10003, 10001, 10003, 0, '王五', DATE_SUB(NOW(), INTERVAL 28 DAY), 1),
(10004, 10001, 10004, 0, '赵六', DATE_SUB(NOW(), INTERVAL 27 DAY), 1),
(10005, 10001, 10005, 0, '孙七', DATE_SUB(NOW(), INTERVAL 26 DAY), 1),
(10006, 10001, 10006, 0, '周八', DATE_SUB(NOW(), INTERVAL 25 DAY), 1),
-- 篮球爱好者 (群主: 李四)
(10007, 10002, 10002, 2, '群主', DATE_SUB(NOW(), INTERVAL 20 DAY), 1),
(10008, 10002, 10001, 0, '张三', DATE_SUB(NOW(), INTERVAL 19 DAY), 1),
(10009, 10002, 10003, 0, '王五', DATE_SUB(NOW(), INTERVAL 18 DAY), 1),
(10010, 10002, 10007, 0, '吴九', DATE_SUB(NOW(), INTERVAL 17 DAY), 1),
-- 旅游俱乐部 (群主: 王五)
(10011, 10003, 10003, 2, '群主', DATE_SUB(NOW(), INTERVAL 15 DAY), 1),
(10012, 10003, 10004, 0, '赵六', DATE_SUB(NOW(), INTERVAL 14 DAY), 1),
(10013, 10003, 10005, 0, '孙七', DATE_SUB(NOW(), INTERVAL 13 DAY), 1),
(10014, 10003, 10008, 0, '郑十', DATE_SUB(NOW(), INTERVAL 12 DAY), 1),
-- 产品经理群 (群主: 赵六)
(10015, 10004, 10004, 2, '群主', DATE_SUB(NOW(), INTERVAL 10 DAY), 1),
(10016, 10004, 10001, 0, '张三', DATE_SUB(NOW(), INTERVAL 9 DAY), 1),
(10017, 10004, 10006, 0, '周八', DATE_SUB(NOW(), INTERVAL 8 DAY), 1),
-- 测试小分队 (群主: 测试用户1)
(10018, 10005, 10009, 2, '群主', DATE_SUB(NOW(), INTERVAL 5 DAY), 1),
(10019, 10005, 10010, 0, '测试用户2', DATE_SUB(NOW(), INTERVAL 4 DAY), 1);

-- ----------------------------
-- 测试数据 - 群公告表 (ID: 10001-10004)
-- ----------------------------
INSERT INTO `group_announcement` (`id`, `group_id`, `publisher_id`, `title`, `content`, `is_top`, `create_time`) VALUES
(10001, 10001, 10001, '群规', '1. 禁止广告 2. 文明发言 3. 欢迎技术讨论', 1, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10002, 10001, 10002, '本周话题', '本周讨论：AI在软件开发中的应用', 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10003, 10002, 10002, '周末活动', '本周六下午3点，篮球场不见不散', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10004, 10003, 10003, '国庆出游', '国庆节组织西藏自驾游，有意向的私聊', 0, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ----------------------------
-- 测试数据 - 会话表 (ID: 10001-10006)
-- ----------------------------
INSERT INTO `conversation` (`id`, `user_id`, `target_id`, `chat_type`, `unread_count`, `top`, `hidden`, `create_time`) VALUES
(10001, 10001, 10002, 1, 0, 1, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10002, 10001, 10003, 1, 2, 0, 0, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(10003, 10001, 10004, 1, 0, 0, 0, DATE_SUB(NOW(), INTERVAL 27 DAY)),
(10004, 10001, 10001, 2, 5, 1, 0, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(10005, 10001, 10002, 2, 0, 0, 0, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10006, 10002, 10001, 1, 1, 0, 0, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ----------------------------
-- 测试数据 - 消息表 (ID: 10001-10022)
-- ----------------------------
INSERT INTO `message` (`id`, `from_user_id`, `to_id`, `chat_type`, `msg_type`, `content`, `status`, `send_time`) VALUES
-- 张三和李四的聊天
(10001, 10001, 10002, 1, 1, '在吗？', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(10002, 10002, 10001, 1, 1, '在的，什么事？', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 55 MINUTE)),
(10003, 10001, 10002, 1, 1, '晚上一起吃饭？', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 50 MINUTE)),
(10004, 10002, 10001, 1, 1, '好啊，去哪儿？', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 45 MINUTE)),
(10005, 10001, 10002, 1, 1, '老地方吧', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 40 MINUTE)),
(10006, 10002, 10001, 1, 1, 'OK，几点？', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 35 MINUTE)),
(10007, 10001, 10002, 1, 1, '7点见', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR 30 MINUTE)),
-- 张三和王五的聊天
(10008, 10001, 10003, 1, 1, '王五，项目进度怎么样了？', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10009, 10003, 10001, 1, 1, '差不多了，这周能上线', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR 50 MINUTE)),
(10010, 10001, 10003, 1, 1, '好的，辛苦', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR 40 MINUTE)),
(10011, 10003, 10001, 1, 1, '不辛苦，应该的', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR 30 MINUTE)),
-- 技术交流群聊天
(10012, 10001, 10001, 2, 1, '大家好，今天我们讨论什么话题？', 1, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(10013, 10002, 10001, 2, 1, '聊聊微服务架构吧', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR 50 MINUTE)),
(10014, 10003, 10001, 2, 1, '我最近在做Spring Cloud项目', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR 40 MINUTE)),
(10015, 10004, 10001, 2, 1, '有什么问题可以一起讨论', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR 30 MINUTE)),
(10016, 10005, 10001, 2, 1, '我遇到一个难题，分布式事务怎么处理？', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR 20 MINUTE)),
(10017, 10006, 10001, 2, 1, '可以用Seata，挺好用的', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR 10 MINUTE)),
(10018, 10005, 10001, 2, 1, '谢谢推荐，我研究一下', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
-- 篮球爱好者群
(10019, 10002, 10002, 2, 1, '周六打球，有人来吗？', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10020, 10001, 10002, 2, 1, '我来', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10021, 10003, 10002, 2, 1, '算我一个', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10022, 10007, 10002, 2, 1, '加一', 1, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ----------------------------
-- 测试数据 - 广场帖子表 (ID: 10001-10020)
-- ----------------------------
INSERT INTO `square_post` (`id`, `user_id`, `title`, `content`, `images`, `tags`, `visible_type`, `like_count`, `comment_count`, `status`, `create_time`) VALUES
(10001, 10001, '今天入职第一天', '新公司环境很好，同事也很热情，期待在这里学到更多东西！', '["https://picsum.photos/400/300?random=1", "https://picsum.photos/400/300?random=2"]', '["职场", "新人"]', 1, 15, 3, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10002, 10002, '周末爬山记录', '上周去爬了白云山，风景真是太美了，累但是值得！', '["https://picsum.photos/400/300?random=3", "https://picsum.photos/400/300?random=4", "https://picsum.photos/400/300?random=5"]', '["户外", "爬山", "风景"]', 1, 28, 7, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10003, 10003, '推荐一本好书', '最近在读《代码大全》，感觉对编程思想提升很大，推荐给各位同行！', '["https://picsum.photos/400/300?random=6"]', '["读书", "编程", "推荐"]', 1, 45, 12, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10004, 10004, '我的烘焙初体验', '第一次尝试做蛋糕，虽然卖相一般，但味道还可以哈哈', '["https://picsum.photos/400/300?random=7", "https://picsum.photos/400/300?random=8"]', '["烘焙", "美食", "DIY"]', 1, 32, 8, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10005, 10005, '电影推荐《肖申克的救赎》', '重温经典，每次看都有不同的感悟，人生需要希望和坚持', NULL, '["电影", "经典", "推荐"]', 1, 67, 20, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10006, 10006, '分享我的桌面配置', '程序员的工作台，显示器支架是亮点', '["https://picsum.photos/400/300?random=9", "https://picsum.photos/400/300?random=10"]', '["数码", "桌面", "程序员"]', 1, 89, 25, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10007, 10007, '今天学会了一道新菜', '红烧排骨，色泽红亮，味道鲜美', '["https://picsum.photos/400/300?random=11"]', '["美食", "烹饪", "红烧肉"]', 1, 23, 5, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10008, 10008, '养猫一年心得', '养了一只英短，分享一下一年来的养猫经验', '["https://picsum.photos/400/300?random=12", "https://picsum.photos/400/300?random=13"]', '["宠物", "猫", "养猫"]', 1, 56, 18, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(10009, 10003, 'Spring Boot 3.0 新特性', '分享一篇Spring Boot 3.0的学习笔记，包含虚拟线程等新特性', NULL, '["技术", "SpringBoot", "Java"]', 1, 120, 35, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(10010, 10001, '北京三日游攻略', '刚去完北京，给大家整理了一份旅游攻略', '["https://picsum.photos/400/300?random=14", "https://picsum.photos/400/300?random=15", "https://picsum.photos/400/300?random=16", "https://picsum.photos/400/300?random=17"]', '["旅游", "北京", "攻略"]', 1, 78, 22, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10011, 10002, '健身三个月的变化', '坚持健身三个月，体重从150减到130，整个人精神多了', '["https://picsum.photos/400/300?random=18"]', '["健身", "运动", "减肥"]', 1, 95, 30, 1, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(10012, 10004, '我的咖啡角', '在家布置了一个小咖啡角，周末在这里喝咖啡看书记', '["https://picsum.photos/400/300?random=19", "https://picsum.photos/400/300?random=20"]', '["咖啡", "生活", "家居"]', 1, 42, 11, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10013, 10005, '推荐几个TED演讲', '工作累了可以看看，受益匪浅', NULL, '["TED", "演讲", "成长"]', 1, 38, 9, 1, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(10014, 10006, 'Docker 入门教程', '整理了一份Docker从入门到实战的教程，适合新手', NULL, '["技术", "Docker", "运维"]', 1, 156, 45, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10015, 10007, '我的吉他练习曲', '学吉他两个月，弹唱了一首《小星星》', '["https://picsum.photos/400/300?random=21"]', '["吉他", "音乐", "爱好"]', 1, 29, 7, 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(10016, 10008, '阳台花园改造', '把阳台改造成了小花园，种了一些多肉和绿植', '["https://picsum.photos/400/300?random=22", "https://picsum.photos/400/300?random=23"]', '["园艺", "阳台", "绿植"]', 1, 51, 14, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10017, 10009, '测试帖子1号', '这是测试用户1发布的测试帖子', NULL, '["测试"]', 1, 5, 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10018, 10010, '测试帖子2号', '这是测试用户2发布的测试帖子', '["https://picsum.photos/400/300?random=24"]', '["测试", "图片"]', 1, 8, 2, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(10019, 10001, '深夜感悟', '有时候慢下来，才能看清生活的本质', NULL, '["感悟", "生活"]', 1, 88, 26, 1, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(10020, 10002, '今日早餐', '周末给自己做了一份丰富的早餐，元气满满的一天', '["https://picsum.photos/400/300?random=25", "https://picsum.photos/400/300?random=26"]', '["早餐", "美食", "生活"]', 1, 63, 15, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- ----------------------------
-- 测试数据 - 广场点赞表 (ID: 10001-10021)
-- ----------------------------
INSERT INTO `square_like` (`id`, `post_id`, `user_id`, `create_time`) VALUES
(10001, 10001, 10002, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10002, 10001, 10003, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10003, 10001, 10004, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(10004, 10002, 10001, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10005, 10002, 10003, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10006, 10002, 10005, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10007, 10003, 10001, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10008, 10003, 10002, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10009, 10003, 10004, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10010, 10003, 10006, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10011, 10004, 10001, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10012, 10005, 10001, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10013, 10005, 10002, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10014, 10006, 10003, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10015, 10009, 10001, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(10016, 10009, 10002, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(10017, 10009, 10004, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(10018, 10014, 10001, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10019, 10014, 10003, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10020, 10019, 10002, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10021, 10019, 10003, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ----------------------------
-- 测试数据 - 广场评论表 (ID: 10001-10018)
-- ----------------------------
INSERT INTO `square_comment` (`id`, `post_id`, `user_id`, `parent_id`, `content`, `create_time`) VALUES
(10001, 10001, 10002, 0, '恭喜恭喜！新工作加油！', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10002, 10001, 10003, 0, '羡慕嫉妒恨，我也想换工作', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10003, 10001, 10002, 10001, '谢谢李四！', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(10004, 10002, 10001, 0, '风景真不错，下次带上我！', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10005, 10002, 10004, 0, '白云山在哪里拍的？', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10006, 10002, 10002, 10005, '广州白云山', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10007, 10003, 10001, 0, '这本书确实经典，我也在看', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10008, 10003, 10004, 0, '有没有电子版分享一下？', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10009, 10005, 10003, 0, '经典中的经典，看了好几遍', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10010, 10005, 10006, 0, '银行家肖申克', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10011, 10006, 10001, 0, '显示器支架什么牌子的？', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10012, 10006, 10002, 0, '这个桌面太整洁了吧', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10013, 10009, 10001, 0, '写得不错，收藏了', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(10014, 10009, 10005, 0, '虚拟线程是真的香', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(10015, 10014, 10001, 0, 'Docker我也在学，互相交流', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10016, 10019, 10002, 0, '深夜哲学家', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10017, 10020, 10001, 0, '看起来好好吃！', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(10018, 10020, 10003, 0, '厨艺不错啊', DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- ----------------------------
-- 测试数据 - 广场关注表 (ID: 10001-10015)
-- ----------------------------
INSERT INTO `square_follow` (`id`, `follower_id`, `followee_id`, `status`, `create_time`) VALUES
(10001, 10001, 10002, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10002, 10001, 10003, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(10003, 10001, 10006, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10004, 10002, 10001, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(10005, 10002, 10004, 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(10006, 10003, 10001, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10007, 10003, 10002, 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(10008, 10004, 10001, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10009, 10005, 10001, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10010, 10005, 10002, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10011, 10006, 10003, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10012, 10007, 10004, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(10013, 10008, 10005, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10014, 10009, 10010, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10015, 10010, 10009, 1, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ----------------------------
-- 测试数据 - 广场收藏表 (ID: 10001-10005)
-- ----------------------------
INSERT INTO `square_favorite` (`id`, `post_id`, `user_id`, `create_time`) VALUES
(10001, 10003, 10001, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10002, 10006, 10001, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10003, 10009, 10003, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(10004, 10014, 10006, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(10005, 10014, 10001, DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- ----------------------------
-- 测试数据 - 黑名单表 (ID: 10001-10003)
-- ----------------------------
INSERT INTO `blacklist` (`id`, `user_id`, `blocked_user_id`, `create_time`) VALUES
(10001, 10001, 10008, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(10002, 10002, 10007, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(10003, 10003, 10006, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ----------------------------
-- 测试数据 - 白名单表 (ID: 10001-10005)
-- ----------------------------
INSERT INTO `user_whitelist` (`id`, `user_id`, `friend_id`, `create_time`) VALUES
(10001, 10001, 10002, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(10002, 10001, 10003, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(10003, 10002, 10001, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(10004, 10003, 10001, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(10005, 10009, 10010, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ----------------------------
-- 测试数据 - AI客服知识库表 (ID: 10001-10010)
-- ----------------------------
INSERT INTO `faq` (`id`, `question`, `answer`, `category`, `keywords`, `priority`, `status`) VALUES
(10001, '如何修改密码？', '进入"我的-设置-账号安全-修改密码"，输入旧密码和新密码即可完成修改。', '账号', '密码 修改 找回', 10, 1),
(10002, '如何注销账号？', '进入"我的-设置-账号安全-注销账号"，注销后所有数据将被清除且不可恢复，请谨慎操作。', '账号', '注销 删除账号 销号', 10, 1),
(10003, '会员多少钱？', '月度会员19元，年度会员168元，连续包月享9折优惠。开通后享受专属表情、个性装饰等特权。', '会员', '会员 收费 价格 开通 付费', 10, 1),
(10004, '如何退款？', '未使用的虚拟服务可在7天内申请退款，请联系客服处理。实物商品请按退换货政策处理。', '财务', '退款 退钱 退还', 5, 1),
(10005, '如何添加好友？', '在搜索框输入对方用户名或手机号，找到后点击"添加好友"，等待对方同意即可。', '功能', '添加好友 加好友 朋友', 5, 1),
(10006, '如何创建群聊？', '点击右上角"+"，选择"创建群聊"，选择好友后点击"完成"即可创建群聊。', '功能', '创建群聊 建群 群组', 5, 1),
(10007, '消息撤回有时间限制吗？', '消息发出后2分钟内可以撤回，超时后无法撤回。', '功能', '撤回 删除消息 收回', 5, 1),
(10008, '如何联系人工客服？', '请发送"转人工"或"人工客服"，我们将为您转接专人服务。', '客服', '人工 客服 转人工', 20, 1),
(10009, '如何设置免打扰？', '进入聊天详情页，点击右上角设置，开启"消息免打扰"即可。', '功能', '免打扰 静音 消息免打扰', 5, 1),
(10010, '如何删除聊天记录？', '进入聊天详情页，点击"清空聊天记录"即可删除所有聊天记录。', '功能', '删除 清空 聊天记录', 5, 1);
