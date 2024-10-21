DROP TABLE IF EXISTS `member`;

CREATE TABLE `member` (
    `seq` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(30) NOT NULL,
    `id` varchar(50) NOT NULL,
    `password` varchar(65) NOT NULL,
    `phone` varchar(20) NOT NULL,
    PRIMARY KEY (`seq`)
);

DROP TABLE IF EXISTS `reservation`;

CREATE TABLE `reservation` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `member_seq` bigint NOT NULL,
    `restaurant_id` bigint NOT NULL,
    `status` varchar(10) NOT NULL,
    `member_count` bigint NOT NULL,
    `created_at` timestamp NOT NULL,
    `reservation_start_at` timestamp NOT NULL,
    `reservation_end_at` timestamp NOT NULL,
    `cancel_reason` varchar(10) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `restaurant`;

CREATE TABLE `restaurant` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL,
    `address` varchar(100) NOT NULL,
    `latitude` decimal(13,10) NOT NULL,
    `longitude` decimal(13,10) NOT NULL,
    `max_member_count` bigint NOT NULL,
    `max_available_day` bigint NOT NULL,
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `daily_schedule`;

CREATE TABLE `daily_schedule` (
    `id` bigint NOT NULL,
    `day` varchar(10) NOT NULL,
    `open_time` time NOT NULL,
    `close_time` time NOT NULL,
    `split_time` bigint NOT NULL
);

DROP TABLE IF EXISTS `specific_schedule`;

CREATE TABLE `specific_schedule` (
    `id` bigint NOT NULL,
    `date` date NOT NULL,
    `open_time` time NOT NULL,
    `close_time` time NOT NULL,
    `split_time` bigint NOT NULL
);

INSERT INTO member(name, id, password, phone)
VALUES
("오소영", "syoh", "qwer123", "010-1111-2222"),
("메버릭", "maverick", "asdf123", "010-3333-4444");

INSERT INTO restaurant(name, address, latitude, longitude, max_member_count, max_available_day)
VALUES
('R1', '서울 강서구 마곡서로 152', 37.5676859105, 126.8259794500, 10, 5),
('R2', '서울 강서구 마곡중앙로 136', 33.2588494316, 126.4061074950, 4, 3),
('R3', '서울 마포구 양화로 151', 37.5562674192411, 126.922368229397, 20, 3),
('R4', '서울 마포구 독막로 18', 37.5483281538652, 126.915245638074, 7, 6);

INSERT INTO daily_schedule(id, day, open_time, close_time, split_time)
VALUES
(1, 'MONDAY', '00:00:00', '23:59:59', 60),
(1, 'TUESDAY', '00:00:00', '23:59:59', 60),
(1, 'WEDNESDAY', '00:00:00', '23:59:59', 60),
(1, 'THURSDAY', '00:00:00', '23:59:59', 60),
(1, 'FRIDAY', '00:00:00', '23:59:59', 60),
(1, 'SATURDAY', '00:00:00', '23:59:59', 30),
(1, 'SUNDAY', '00:00:00', '23:59:59', 30),
(2, 'MONDAY', '00:00:00', '23:59:59', 60),
(2, 'TUESDAY', '00:00:00', '23:59:59', 60),
(2, 'WEDNESDAY', '00:00:00', '23:59:59', 60),
(2, 'THURSDAY', '00:00:00', '23:59:59', 60),
(2, 'FRIDAY', '00:00:00', '23:59:59', 60),
(2, 'SATURDAY', '00:00:00', '23:59:59', 60),
(2, 'SUNDAY', '00:00:00', '23:59:59', 60),
(4, 'MONDAY', '00:00:00', '23:59:59', 30),
(4, 'TUESDAY', '00:00:00', '23:59:59', 30),
(4, 'WEDNESDAY', '00:00:00', '23:59:59', 30),
(4, 'THURSDAY', '00:00:00', '23:59:59', 30),
(4, 'FRIDAY', '00:00:00', '23:59:59', 30),
(4, 'SATURDAY', '00:00:00', '23:59:59', 30),
(4, 'SUNDAY', '00:00:00', '23:59:59', 30);
