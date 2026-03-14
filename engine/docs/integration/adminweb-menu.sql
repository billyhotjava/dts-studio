-- ========================================
-- 园林平台菜单表 Copilot 菜单项
-- ========================================
-- 注意：实际表名和字段需根据园林平台数据库结构调整
-- 执行前请确认 sys_menu 表结构与以下 SQL 兼容
-- 建议在测试环境验证后再应用到生产环境

-- 父菜单：AI 智能助手
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('AI 智能助手', 0, 99, 'copilot', NULL, 'M', '0', '0', '', 'robot', 'admin', NOW(), 'DTS Copilot AI 助手');

-- 子菜单：AI 对话
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('AI 对话', (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI 智能助手' LIMIT 1), 1, 'chat', 'copilot/chat', 'C', '0', '0', 'copilot:chat:view', 'message', 'admin', NOW(), 'AI Copilot 对话');

-- 子菜单：数据分析
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('数据分析', (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI 智能助手' LIMIT 1), 2, 'analytics', 'copilot/analytics', 'C', '0', '0', 'copilot:analytics:view', 'chart', 'admin', NOW(), 'BI 数据分析仪表盘');

-- 子菜单：报表中心
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('报表中心', (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI 智能助手' LIMIT 1), 3, 'reports', 'copilot/reports', 'C', '0', '0', 'copilot:reports:view', 'documentation', 'admin', NOW(), '报表与大屏');
