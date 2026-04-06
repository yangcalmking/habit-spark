-- 学习习惯培养积分奖励系统 - 初始数据脚本

-- 默认家庭组
INSERT INTO family_group (id, name) VALUES (1, '小雨家');
INSERT OR IGNORE INTO user (id, family_group_id, username, password_hash, nickname, role)
VALUES (1, 1, 'admin', '$2a$10$placeholder', '系统管理员', 'admin');
INSERT OR IGNORE INTO user (id, family_group_id, username, password_hash, nickname, role)
VALUES (2, 1, 'xue', '$2a$10$placeholder', '小雨', 'student');
INSERT OR IGNORE INTO user (id, family_group_id, username, password_hash, nickname, role)
VALUES (3, 1, 'jia', '$2a$10$placeholder', '妈妈', 'parent');

INSERT OR IGNORE INTO point_account (student_id, total_points, available_points, frozen_points)
VALUES (2, 0, 0, 0);

-- 任务模板 - 基础学习习惯
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('课前预习', 'study', 1, 1, '{"subject_count":3,"bonus":1}', 4, '语文：标出生字词、分段；数学：看懂例题；英语：听读课文', 1, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('课后作业', 'homework', 2, 1, '{"neat_bonus":1}', 3, '按时完成当天作业，书写工整', 2, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('错题整理', 'homework', 0, 1, '{"per_item":0.5}', 3, '整理当天错题，分析错误原因', 3, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('课后复习', 'study', 0, 1, '{"per_subject":0.5}', 2, '复习当天所学内容，每科至少10分钟', 4, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('自主阅读', 'study', 2, 5, '{"book_bonus":5}', -1, '课外自主阅读至少30分钟', 5, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, extra_points_rule, daily_cap, description, sort_order, is_active)
VALUES ('规律作息', 'study', 1, 0, NULL, 1, '21:30 前完成洗漱、上床睡觉', 6, 1);

-- 任务模板 - 进阶学习
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('单元测验优秀', 'extra', 10, 0, -1, '单元测验90分以上', 7, 1, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('作文获优', 'extra', 8, 0, -1, '作文获得老师"优+"评价', 8, 1, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, extra_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('英语听写全对', 'extra', 3, 0, -1, '英语单词听写全部正确', 9, 1, 1);

-- 任务模板 - 兴趣特长
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('唱歌练习', 'interest', 1, -1, '每日练歌15分钟', 10, 1, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('手工作品', 'interest', 5, -1, '完成一件手工作品', 11, 1, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active, allow_custom)
VALUES ('漫画创作', 'interest', 3, -1, '完成一幅漫画作品', 12, 1, 1);

-- 任务模板 - 家务德育
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('整理房间', 'morality', 3, 1, '整理自己的房间', 20, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('洗碗', 'morality', 2, 1, '帮忙洗碗筷', 21, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('扫地拖地', 'morality', 2, 1, '帮忙扫地拖地', 22, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('帮忙做饭', 'morality', 5, 1, '帮忙准备饭菜', 23, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('礼貌行为', 'morality', 1, -1, '主动问好、说谢谢', 25, 1);
INSERT OR IGNORE INTO task_template (name, category, base_points, daily_cap, description, sort_order, is_active)
VALUES ('诚实勇敢', 'morality', 5, -1, '承认错误、勇敢面对困难', 27, 1);

-- 预置礼品
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('家庭K歌时间15分钟', 'singing', 5, -1, '获得15分钟家庭K歌时间', 'instant', 1, 1);
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('漫画阅读时间10分钟', 'comic', 1, -1, '额外10分钟漫画阅读', 'instant', 20, 1);
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('手工彩纸/贴纸', 'craft', 3, -1, '精美手工彩纸或贴纸一包', 'instant', 10, 1);
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('周末动画时间30分钟', 'playground', 15, -1, '周末可额外看30分钟动画片', 'instant', 30, 1);
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('K歌房1小时体验', 'singing', 60, 3, 'KTV包房1小时', 'mid', 2, 1);
INSERT OR IGNORE INTO product (name, category, points_cost, stock, description, level, sort_order, is_active)
VALUES ('正版漫画单行本', 'comic', 50, 5, '自选一本正版漫画书', 'mid', 21, 1);
