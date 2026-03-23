-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 22, 2026 at 02:58 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `codearena`
--

-- --------------------------------------------------------

--
-- Table structure for table `achievement`
--

CREATE TABLE `achievement` (
  `id` binary(16) NOT NULL,
  `badge_id` varchar(255) DEFAULT NULL,
  `earned_at` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `badge`
--

CREATE TABLE `badge` (
  `id` binary(16) NOT NULL,
  `criteria` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `battle_participant`
--

CREATE TABLE `battle_participant` (
  `id` binary(16) NOT NULL,
  `joined_at` varchar(255) DEFAULT NULL,
  `rank` varchar(255) DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `score` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `battle_result`
--

CREATE TABLE `battle_result` (
  `id` binary(16) NOT NULL,
  `ended_at` varchar(255) DEFAULT NULL,
  `room_id` varchar(255) DEFAULT NULL,
  `winner_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `battle_room`
--

CREATE TABLE `battle_room` (
  `id` binary(16) NOT NULL,
  `challenge_id` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `host_id` varchar(255) DEFAULT NULL,
  `room_key` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `challenge`
--

CREATE TABLE `challenge` (
  `id` binary(16) NOT NULL,
  `author_id` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `difficulty` varchar(255) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `coach`
--

CREATE TABLE `coach` (
  `id` binary(16) NOT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `rating` varchar(255) DEFAULT NULL,
  `specializations` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `coaching_session`
--

CREATE TABLE `coaching_session` (
  `id` binary(16) NOT NULL,
  `coach_id` varchar(255) DEFAULT NULL,
  `duration_minutes` varchar(255) DEFAULT NULL,
  `learner_id` varchar(255) DEFAULT NULL,
  `meeting_url` varchar(255) DEFAULT NULL,
  `scheduled_at` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `event_registration`
--

CREATE TABLE `event_registration` (
  `id` binary(16) NOT NULL,
  `event_id` varchar(255) DEFAULT NULL,
  `registered_at` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `permission`
--

CREATE TABLE `permission` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `programming_event`
--

CREATE TABLE `programming_event` (
  `id` binary(16) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `end_date` varchar(255) DEFAULT NULL,
  `max_participants` varchar(255) DEFAULT NULL,
  `organizer_id` varchar(255) DEFAULT NULL,
  `start_date` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `purchases`
--

CREATE TABLE `purchases` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `participant_id` varchar(255) NOT NULL,
  `status` enum('CANCELLED','CONFIRMED','DELIVERED','PENDING','SHIPPED') NOT NULL,
  `total_price` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `purchases`
--

INSERT INTO `purchases` (`id`, `created_at`, `participant_id`, `status`, `total_price`) VALUES
(0x4b4ed0f43067483484aeb8fabc15261a, '2026-03-22 12:05:33.000000', 'participant-001', 'PENDING', 89.97),
(0x6e964bb3151744f3bc5ee1ac8f863584, '2026-03-22 03:51:22.000000', 'participant-001', 'SHIPPED', 44.99),
(0x807d3b045e714c509469ec134cabd233, '2026-03-22 13:55:03.000000', 'participant-001', 'PENDING', 44.99),
(0x8772ec9eb59e4955a3d73a7d265475da, '2026-03-22 12:42:13.000000', 'participant-001', 'PENDING', 19.99),
(0x96be0a2e42e6464b8afbe10a298395f7, '2026-03-22 03:50:07.000000', 'participant-001', 'PENDING', 104.98),
(0xa8febf1673f44e2bb5c33641c449eea2, '2026-03-22 03:44:56.000000', 'participant-001', 'CANCELLED', 104.98),
(0xbfa55d628f224fdebeac5a51ae1a0808, '2026-03-22 03:42:05.000000', 'participant-001', 'DELIVERED', 44.99),
(0xfaa5dd91c9da43179300dc923f941318, '2026-03-22 03:44:39.000000', 'participant-001', 'CANCELLED', 44.99);

-- --------------------------------------------------------

--
-- Table structure for table `purchase_items`
--

CREATE TABLE `purchase_items` (
  `id` binary(16) NOT NULL,
  `quantity` int(11) NOT NULL,
  `unit_price` double NOT NULL,
  `purchase_id` binary(16) NOT NULL,
  `shop_item_id` binary(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `purchase_items`
--

INSERT INTO `purchase_items` (`id`, `quantity`, `unit_price`, `purchase_id`, `shop_item_id`) VALUES
(0x0b967a5d02754eaa9c9dce3722203d5e, 1, 39.99, 0x4b4ed0f43067483484aeb8fabc15261a, 0x5acb16a3773d4f248ad5704dfedfaf62),
(0x0fce0635f9ca435eb592452d07800bdf, 1, 19.99, 0x8772ec9eb59e4955a3d73a7d265475da, 0xe4c6127c7c3947788253d3195fd56d50),
(0x11d993dd4e5e4771b1793c17082bf2fd, 1, 44.99, 0x6e964bb3151744f3bc5ee1ac8f863584, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0x278a390295be406f9e18576a577298b0, 1, 44.99, 0x807d3b045e714c509469ec134cabd233, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0x4ef14beb19384bab99963421b6df8511, 1, 44.99, 0x96be0a2e42e6464b8afbe10a298395f7, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0x7e8edc0c402941c88f5d9a2072b75532, 1, 44.99, 0xfaa5dd91c9da43179300dc923f941318, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0xcd63570888094ce590a95659fb6a8266, 1, 44.99, 0xbfa55d628f224fdebeac5a51ae1a0808, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0xdd2be36b48c641e7bad6df39e4fdcd86, 1, 29.99, 0x4b4ed0f43067483484aeb8fabc15261a, 0x45376c5f36134671bd307269ed4ff5ea),
(0xdd46a2fdaf934d41b433ace55ee3a026, 1, 59.99, 0xa8febf1673f44e2bb5c33641c449eea2, 0x34128c30d72f4552889d7252e0742fe0),
(0xe4bec5717e4b479ba0872573f61a2b9f, 1, 59.99, 0x96be0a2e42e6464b8afbe10a298395f7, 0x34128c30d72f4552889d7252e0742fe0),
(0xf6f6d4b2ff4e4ad189dff65aa34eb144, 1, 44.99, 0xa8febf1673f44e2bb5c33641c449eea2, 0x1809364f2fa64510b6f2337d6fc1f3ac),
(0xf8cbc16d01d4446698fc2698c3f4657e, 1, 19.99, 0x4b4ed0f43067483484aeb8fabc15261a, 0x3fde3b3deb8c4c86ba8c4f6ebab63a06);

-- --------------------------------------------------------

--
-- Table structure for table `question`
--

CREATE TABLE `question` (
  `id` binary(16) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `points` varchar(255) DEFAULT NULL,
  `quiz_id` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz`
--

CREATE TABLE `quiz` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `difficulty` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_attempt`
--

CREATE TABLE `quiz_attempt` (
  `id` binary(16) NOT NULL,
  `completed_at` varchar(255) DEFAULT NULL,
  `quiz_id` varchar(255) DEFAULT NULL,
  `score` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `rank`
--

CREATE TABLE `rank` (
  `id` binary(16) NOT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `max_xp` varchar(255) DEFAULT NULL,
  `min_xp` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `report`
--

CREATE TABLE `report` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `reporter_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `target_id` varchar(255) DEFAULT NULL,
  `target_type` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `shop_items`
--

CREATE TABLE `shop_items` (
  `id` binary(16) NOT NULL,
  `category` enum('ACCESSORY','CAP','HOODIE','KEYBOARD','MOUSEPAD','MUG','OTHER','STICKER','TSHIRT') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` double NOT NULL,
  `stock` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `shop_items`
--

INSERT INTO `shop_items` (`id`, `category`, `created_at`, `description`, `image_url`, `name`, `price`, `stock`) VALUES
(0x1809364f2fa64510b6f2337d6fc1f3ac, 'HOODIE', '2026-03-22 03:14:44.000000', 'Full-zip premium hoodie perfect for late night coding sessions', 'https://images.pexels.com/photos/6311392/pexels-photo-6311392.jpeg', 'CodeArena Zip Hoodie', 44.99, 16),
(0x34128c30d72f4552889d7252e0742fe0, 'ACCESSORY', '2026-03-22 03:16:34.000000', 'Premium 25L backpack with CodeArena logo. USB charging port included', 'https://images.pexels.com/photos/1294731/pexels-photo-1294731.jpeg', 'CodeArena Backpack', 59.99, 17),
(0x389431e88f7b4713b4e070ac380cdbf4, 'KEYBOARD', '2026-03-22 03:15:38.000000', 'Compact mechanical keyboard with blue switches. The weapon of every developer', 'https://images.pexels.com/photos/1714205/pexels-photo-1714205.jpeg', 'Mechanical Keyboard 60%', 89.99, 10),
(0x3fde3b3deb8c4c86ba8c4f6ebab63a06, 'TSHIRT', '2026-03-22 03:14:54.000000', 'High quality cotton tee. Show your dev pride every day', 'https://images.pexels.com/photos/8532616/pexels-photo-8532616.jpeg', 'I Solve Problems T-Shirt', 19.99, 99),
(0x45376c5f36134671bd307269ed4ff5ea, 'ACCESSORY', '2026-03-22 03:16:10.000000', 'Protective neoprene sleeve with CodeArena design. Fits 15 inch laptops', 'https://images.pexels.com/photos/205421/pexels-photo-205421.jpeg', 'Laptop Sleeve 15 inch', 29.99, 34),
(0x5acb16a3773d4f248ad5704dfedfaf62, 'HOODIE', '2026-03-22 02:53:34.000000', 'Premium black hoodie with CodeArena logo', 'https://images.pexels.com/photos/4210854/pexels-photo-4210854.jpeg', 'CodeArena Hoodie UPDATED', 39.99, 9),
(0x716c5b25042e4e4c961c5a54fb8d6e77, 'ACCESSORY', '2026-03-22 03:16:18.000000', 'Premium ruled notebook for algorithms and system design. 200 pages', 'https://images.pexels.com/photos/733857/pexels-photo-733857.jpeg', 'Developer Notebook', 12.99, 90),
(0x77d3ce6ac0114a0fa95b80a21e8debd9, 'MUG', '2026-03-22 03:15:28.000000', 'Heat-sensitive mug — cold shows dark mode hot switches to light mode', 'https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg', 'Dark Mode Mug', 17.99, 45),
(0x9880c0242a314de2a04dd3083c51afbc, 'MOUSEPAD', '2026-03-22 03:15:45.000000', 'Extra large desk mat 90x40cm with arena design. Non-slip base', 'https://images.pexels.com/photos/2115257/pexels-photo-2115257.jpeg', 'CodeArena XL Mouse Pad', 24.99, 60),
(0xa7d992613ba14731afcaeffefaf8ad47, 'MUG', '2026-03-22 03:15:18.000000', 'ERROR 404 Coffee Not Found — 350ml ceramic. Start every session right', 'https://images.pexels.com/photos/1684032/pexels-photo-1684032.jpeg', 'ERROR 404 Mug', 14.99, 80),
(0xb21aee39dc4c42359c90feccc8b130b0, 'CAP', '2026-03-22 03:16:01.000000', 'Classic dad cap with CodeArena embroidered logo', 'https://images.pexels.com/photos/1124465/pexels-photo-1124465.jpeg', 'Eat Sleep Code Cap', 22.99, 55),
(0xb40f7a30710d4078bee5cc64678594ce, 'STICKER', '2026-03-22 03:15:53.000000', '10 waterproof laptop stickers — Linux git sudo and more', 'https://images.pexels.com/photos/1148820/pexels-photo-1148820.jpeg', 'Developer Sticker Pack x10', 9.99, 200),
(0xc3bcb945ee7640458ae691e9cbb30ead, 'TSHIRT', '2026-03-22 03:15:02.000000', 'The most dangerous command ever — wear it with pride. Linux devs only', 'https://images.pexels.com/photos/5632399/pexels-photo-5632399.jpeg', 'sudo rm -rf T-Shirt', 19.99, 75),
(0xe4c6127c7c3947788253d3195fd56d50, 'ACCESSORY', '2026-03-22 03:16:26.000000', 'USB-powered RGB light bar for your setup. Set the gamer mood', 'https://images.pexels.com/photos/1036936/pexels-photo-1036936.jpeg', 'RGB Desk Light Bar', 19.99, 29);

-- --------------------------------------------------------

--
-- Table structure for table `submission`
--

CREATE TABLE `submission` (
  `id` binary(16) NOT NULL,
  `challenge_id` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `language` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `submitted_at` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `xp_earned` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `support_ticket`
--

CREATE TABLE `support_ticket` (
  `id` binary(16) NOT NULL,
  `assigned_admin_id` varchar(255) DEFAULT NULL,
  `report_id` varchar(255) DEFAULT NULL,
  `resolution` varchar(255) DEFAULT NULL,
  `resolved_at` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `test_case`
--

CREATE TABLE `test_case` (
  `id` binary(16) NOT NULL,
  `challenge_id` varchar(255) DEFAULT NULL,
  `expected_output` varchar(255) DEFAULT NULL,
  `input` varchar(255) DEFAULT NULL,
  `is_hidden` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

CREATE TABLE `user` (
  `id` binary(16) NOT NULL,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `keycloak_id` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_profile`
--

CREATE TABLE `user_profile` (
  `id` binary(16) NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `current_rank_id` varchar(255) DEFAULT NULL,
  `total_xp` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `achievement`
--
ALTER TABLE `achievement`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `badge`
--
ALTER TABLE `badge`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `battle_participant`
--
ALTER TABLE `battle_participant`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `battle_result`
--
ALTER TABLE `battle_result`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `battle_room`
--
ALTER TABLE `battle_room`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `challenge`
--
ALTER TABLE `challenge`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `coach`
--
ALTER TABLE `coach`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `coaching_session`
--
ALTER TABLE `coaching_session`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `permission`
--
ALTER TABLE `permission`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `programming_event`
--
ALTER TABLE `programming_event`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `purchases`
--
ALTER TABLE `purchases`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `purchase_items`
--
ALTER TABLE `purchase_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKhcski0jcuja0o3vhb7o15yqvi` (`purchase_id`),
  ADD KEY `FK5gpw6iikw72khhi1la4kbkcpm` (`shop_item_id`);

--
-- Indexes for table `question`
--
ALTER TABLE `question`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `quiz`
--
ALTER TABLE `quiz`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `quiz_attempt`
--
ALTER TABLE `quiz_attempt`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `rank`
--
ALTER TABLE `rank`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `report`
--
ALTER TABLE `report`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `shop_items`
--
ALTER TABLE `shop_items`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `submission`
--
ALTER TABLE `submission`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `support_ticket`
--
ALTER TABLE `support_ticket`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `test_case`
--
ALTER TABLE `test_case`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `user_profile`
--
ALTER TABLE `user_profile`
  ADD PRIMARY KEY (`id`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `purchase_items`
--
ALTER TABLE `purchase_items`
  ADD CONSTRAINT `FK5gpw6iikw72khhi1la4kbkcpm` FOREIGN KEY (`shop_item_id`) REFERENCES `shop_items` (`id`),
  ADD CONSTRAINT `FKhcski0jcuja0o3vhb7o15yqvi` FOREIGN KEY (`purchase_id`) REFERENCES `purchases` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
