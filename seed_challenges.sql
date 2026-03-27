-- Seed challenges for CodeArena
-- Run against `codearena` database

-- Alter challenge.description to TEXT if not already
ALTER TABLE challenge MODIFY COLUMN description TEXT;
ALTER TABLE challenge MODIFY COLUMN tags VARCHAR(500);

-- Alter submission columns
ALTER TABLE submission MODIFY COLUMN code TEXT;
ALTER TABLE submission ADD COLUMN IF NOT EXISTS judge_token VARCHAR(255);
ALTER TABLE submission ADD COLUMN IF NOT EXISTS execution_time FLOAT;
ALTER TABLE submission ADD COLUMN IF NOT EXISTS memory_used FLOAT;
ALTER TABLE submission ADD COLUMN IF NOT EXISTS error_output TEXT;
ALTER TABLE submission MODIFY COLUMN submitted_at DATETIME(6);

-- Alter test_case columns
ALTER TABLE test_case MODIFY COLUMN input TEXT;
ALTER TABLE test_case MODIFY COLUMN expected_output TEXT;
ALTER TABLE test_case MODIFY COLUMN challenge_id BINARY(16);
ALTER TABLE test_case MODIFY COLUMN is_hidden BIT(1);

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 1: Two Sum
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x11111111111111111111111111111111, 'Two Sum',
'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nYou can return the answer in any order.\n\nConstraints:\n- 2 <= nums.length <= 10^4\n- -10^9 <= nums[i] <= 10^9\n- -10^9 <= target <= 10^9\n- Only one valid answer exists.',
'EASY', 'Array,Hash Table', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA1111111111111111111111111111111, 0x11111111111111111111111111111111, '4\n2 7 11 15\n9', '0 1', b'0'),
(0xA1111111111111111111111111111112, 0x11111111111111111111111111111111, '3\n3 2 4\n6', '1 2', b'0'),
(0xA1111111111111111111111111111113, 0x11111111111111111111111111111111, '2\n3 3\n6', '0 1', b'1'),
(0xA1111111111111111111111111111114, 0x11111111111111111111111111111111, '5\n1 5 3 7 2\n9', '1 3', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 2: Reverse String
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x22222222222222222222222222222222, 'Reverse String',
'Write a function that reverses a string. The input string is given as a single line.\n\nPrint the reversed string.\n\nExample:\nInput: hello\nOutput: olleh\n\nConstraints:\n- 1 <= s.length <= 10^5\n- s consists of printable ASCII characters.',
'EASY', 'String,Two Pointers', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA2222222222222222222222222222221, 0x22222222222222222222222222222222, 'hello', 'olleh', b'0'),
(0xA2222222222222222222222222222222, 0x22222222222222222222222222222222, 'world', 'dlrow', b'0'),
(0xA2222222222222222222222222222223, 0x22222222222222222222222222222222, 'a', 'a', b'1'),
(0xA2222222222222222222222222222224, 0x22222222222222222222222222222222, 'racecar', 'racecar', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 3: FizzBuzz
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x33333333333333333333333333333333, 'FizzBuzz',
'Given an integer n, print a string for each number from 1 to n:\n\n- "FizzBuzz" if the number is divisible by both 3 and 5\n- "Fizz" if the number is divisible by 3\n- "Buzz" if the number is divisible by 5\n- The number itself if none of the above conditions are true\n\nEach value should be on a new line.\n\nConstraints:\n- 1 <= n <= 10^4',
'EASY', 'Math,String,Simulation', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA3333333333333333333333333333331, 0x33333333333333333333333333333333, '3', '1\n2\nFizz', b'0'),
(0xA3333333333333333333333333333332, 0x33333333333333333333333333333333, '5', '1\n2\nFizz\n4\nBuzz', b'0'),
(0xA3333333333333333333333333333333, 0x33333333333333333333333333333333, '15', '1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 4: Palindrome Number
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x44444444444444444444444444444444, 'Palindrome Number',
'Given an integer x, return true if x is a palindrome, and false otherwise.\n\nAn integer is a palindrome when it reads the same forward and backward.\n\nFor example, 121 is a palindrome while 123 is not.\n\nConstraints:\n- -2^31 <= x <= 2^31 - 1\n\nFollow up: Could you solve it without converting the integer to a string?',
'EASY', 'Math', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA4444444444444444444444444444441, 0x44444444444444444444444444444444, '121', 'true', b'0'),
(0xA4444444444444444444444444444442, 0x44444444444444444444444444444444, '-121', 'false', b'0'),
(0xA4444444444444444444444444444443, 0x44444444444444444444444444444444, '10', 'false', b'1'),
(0xA4444444444444444444444444444444, 0x44444444444444444444444444444444, '0', 'true', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 5: Maximum Subarray
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x55555555555555555555555555555555, 'Maximum Subarray',
'Given an integer array nums, find the subarray with the largest sum, and return its sum.\n\nA subarray is a contiguous non-empty sequence of elements within an array.\n\nExample:\nInput: 9\n-2 1 -3 4 -1 2 1 -5 4\nOutput: 6\nExplanation: The subarray [4,-1,2,1] has the largest sum 6.\n\nConstraints:\n- 1 <= nums.length <= 10^5\n- -10^4 <= nums[i] <= 10^4\n\nFollow up: If you have figured out the O(n) solution, try coding another solution using the divide and conquer approach, which is more subtle.',
'MEDIUM', 'Array,Divide and Conquer,Dynamic Programming', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA5555555555555555555555555555551, 0x55555555555555555555555555555555, '9\n-2 1 -3 4 -1 2 1 -5 4', '6', b'0'),
(0xA5555555555555555555555555555552, 0x55555555555555555555555555555555, '1\n1', '1', b'0'),
(0xA5555555555555555555555555555553, 0x55555555555555555555555555555555, '5\n5 4 -1 7 8', '23', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 6: Valid Parentheses
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x66666666666666666666666666666666, 'Valid Parentheses',
'Given a string s containing just the characters ''('', '')'', ''{'', ''}'', ''['' and '']'', determine if the input string is valid.\n\nAn input string is valid if:\n1. Open brackets must be closed by the same type of brackets.\n2. Open brackets must be closed in the correct order.\n3. Every close bracket has a corresponding open bracket of the same type.\n\nPrint "true" or "false".\n\nConstraints:\n- 1 <= s.length <= 10^4\n- s consists of parentheses only ''()[]{}''.',
'EASY', 'String,Stack', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA6666666666666666666666666666661, 0x66666666666666666666666666666666, '()', 'true', b'0'),
(0xA6666666666666666666666666666662, 0x66666666666666666666666666666666, '()[]{}', 'true', b'0'),
(0xA6666666666666666666666666666663, 0x66666666666666666666666666666666, '(]', 'false', b'0'),
(0xA6666666666666666666666666666664, 0x66666666666666666666666666666666, '([)]', 'false', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 7: Merge Two Sorted Lists (conceptual, stdin based)
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x77777777777777777777777777777777, 'Merge Two Sorted Arrays',
'You are given two integer arrays nums1 and nums2, sorted in non-decreasing order.\n\nMerge nums2 into nums1 as one sorted array and print the result space-separated.\n\nInput format:\n- First line: size of nums1\n- Second line: elements of nums1\n- Third line: size of nums2\n- Fourth line: elements of nums2\n\nConstraints:\n- 0 <= nums1.length, nums2.length <= 200\n- -10^9 <= nums1[i], nums2[j] <= 10^9',
'EASY', 'Array,Two Pointers,Sorting', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA7777777777777777777777777777771, 0x77777777777777777777777777777777, '3\n1 2 4\n3\n1 3 5', '1 1 2 3 4 5', b'0'),
(0xA7777777777777777777777777777772, 0x77777777777777777777777777777777, '1\n1\n0\n', '1', b'0');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 8: Longest Common Prefix
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x88888888888888888888888888888888, 'Longest Common Prefix',
'Write a function to find the longest common prefix string amongst an array of strings.\n\nIf there is no common prefix, print an empty line.\n\nInput format:\n- First line: number of strings n\n- Next n lines: one string per line\n\nConstraints:\n- 1 <= strs.length <= 200\n- 0 <= strs[i].length <= 200\n- strs[i] consists of only lowercase English letters.',
'EASY', 'String,Trie', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA8888888888888888888888888888881, 0x88888888888888888888888888888888, '3\nflower\nflow\nflight', 'fl', b'0'),
(0xA8888888888888888888888888888882, 0x88888888888888888888888888888888, '3\ndog\nracecar\ncar', '', b'0');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 9: Container With Most Water
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0x99999999999999999999999999999999, 'Container With Most Water',
'You are given an integer array height of length n. There are n vertical lines drawn such that the two endpoints of the ith line are (i, 0) and (i, height[i]).\n\nFind two lines that together with the x-axis form a container, such that the container contains the most water.\n\nReturn the maximum amount of water a container can store.\n\nInput:\n- First line: n (number of elements)\n- Second line: n space-separated integers\n\nOutput: a single integer\n\nConstraints:\n- n == height.length\n- 2 <= n <= 10^5\n- 0 <= height[i] <= 10^4',
'MEDIUM', 'Array,Two Pointers,Greedy', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xA9999999999999999999999999999991, 0x99999999999999999999999999999999, '9\n1 8 6 2 5 4 8 3 7', '49', b'0'),
(0xA9999999999999999999999999999992, 0x99999999999999999999999999999999, '2\n1 1', '1', b'0');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 10: Binary Search
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, 'Binary Search',
'Given a sorted array of distinct integers and a target value, return the index if the target is found. If not, return -1.\n\nYou must write an algorithm with O(log n) runtime complexity.\n\nInput:\n- First line: n (size of array)\n- Second line: n sorted integers\n- Third line: target\n\nOutput: index or -1\n\nConstraints:\n- 1 <= nums.length <= 10^4\n- -10^4 < nums[i], target < 10^4\n- All the integers in nums are unique.\n- nums is sorted in ascending order.',
'EASY', 'Array,Binary Search', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, 0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, '6\n-1 0 3 5 9 12\n9', '4', b'0'),
(0xBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2, 0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, '6\n-1 0 3 5 9 12\n2', '-1', b'0');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 11: Climbing Stairs
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB, 'Climbing Stairs',
'You are climbing a staircase. It takes n steps to reach the top.\n\nEach time you can either climb 1 or 2 steps. In how many distinct ways can you climb to the top?\n\nInput: a single integer n\nOutput: number of distinct ways\n\nConstraints:\n- 1 <= n <= 45',
'EASY', 'Math,Dynamic Programming,Memoization', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xCBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB1, 0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB, '2', '2', b'0'),
(0xCBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB2, 0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB, '3', '3', b'0'),
(0xCBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB3, 0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB, '5', '8', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 12: Longest Substring Without Repeating Characters
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0xCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC, 'Longest Substring Without Repeating Characters',
'Given a string s, find the length of the longest substring without repeating characters.\n\nInput: a single string\nOutput: an integer (length)\n\nExample 1:\nInput: abcabcbb\nOutput: 3\nExplanation: The answer is "abc", with the length of 3.\n\nExample 2:\nInput: bbbbb\nOutput: 1\n\nExample 3:\nInput: pwwkew\nOutput: 3\n\nConstraints:\n- 0 <= s.length <= 5 * 10^4\n- s consists of English letters, digits, symbols and spaces.',
'MEDIUM', 'Hash Table,String,Sliding Window', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xDCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC1, 0xCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC, 'abcabcbb', '3', b'0'),
(0xDCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC2, 0xCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC, 'bbbbb', '1', b'0'),
(0xDCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC3, 0xCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC, 'pwwkew', '3', b'1');

-- ═══════════════════════════════════════════════════════════
-- CHALLENGE 13: 3Sum (HARD)
-- ═══════════════════════════════════════════════════════════
INSERT INTO challenge (id, title, description, difficulty, tags, author_id, created_at) VALUES
(0xDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD, '3Sum',
'Given an integer array nums, return all the triplets [nums[i], nums[j], nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] + nums[k] == 0.\n\nNotice that the solution set must not contain duplicate triplets.\n\nPrint each triplet on a separate line, space-separated, sorted in ascending order. Print triplets in lexicographic order.\n\nInput:\n- First line: n\n- Second line: n integers\n\nConstraints:\n- 3 <= nums.length <= 3000\n- -10^5 <= nums[i] <= 10^5',
'HARD', 'Array,Two Pointers,Sorting', 'admin-123', NOW());

INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden) VALUES
(0xEDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD1, 0xDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD, '6\n-1 0 1 2 -1 -4', '-1 -1 2\n-1 0 1', b'0'),
(0xEDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD2, 0xDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD, '3\n0 1 1', '', b'1');

COMMIT;
