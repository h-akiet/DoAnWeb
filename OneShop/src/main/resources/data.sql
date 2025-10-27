
SET IDENTITY_INSERT CATEGORIES ON; 
-- Lệnh này cần thiết trong SQL Server để chèn ID vào cột IDENTITY

INSERT INTO CATEGORIES (category_id, name, parent_id) 
VALUES (1001, N'Chưa Phân Loại', NULL);

SET IDENTITY_INSERT CATEGORIES OFF;