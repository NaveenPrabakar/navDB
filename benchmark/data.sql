CREATE TABLE kv (
  k VARCHAR(64) PRIMARY KEY,
  v VARCHAR(256)
);

DELIMITER //
CREATE PROCEDURE load_data()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 100000 DO
    INSERT INTO kv VALUES (
      CONCAT('key', i),
      CONCAT('value', i)
    );
    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;

CALL load_data();
