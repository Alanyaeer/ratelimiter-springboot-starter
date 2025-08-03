local rateLimitKey = KEYS[1]
local rateInterval = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local randomMember = ARGV[3]
local allow = 1

-- 获取时间戳通过Unix的格式
local time = tonumber(redis.call("time")[1])
local newCount = 0
-- 先进行更新滑动窗口
redis.call("ZREMRANGEBYSCORE", rateLimitKey, "-inf", time - rateInterval)
newCount = capacity - redis.call("ZCOUNT", rateLimitKey, time - rateInterval, time);
redis.debug(redis.call("ZCOUNT", rateLimitKey, time - rateInterval, time))
if (newCount <= 0) then
    allow = 0;
else
    redis.call("ZADD", rateLimitKey, time, randomMember)
    allow = 1;
end

return { allow, math.max(0, newCount - 1)};