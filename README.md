# rateLimiter-spring-boot-starter
这个项目根据 https://github.com/taptap/ratelimiter-spring-boot-starter 做的优化

## 使用EvalSha1 实现 Script Cache优化
```java
@Override
public Result isAllowed(Rule rule) {
    List<Object> keys = getKeys(rule.getKey());
    String script = LuaScript.getTimeWindowRateLimiterScript();
    List<Long> results = null;
    try {
        results =  rScript.evalSha(RScript.Mode.READ_WRITE,
                scriptSha1,
                RScript.ReturnType.MULTI,
                keys,
                rule.getRate(),
                rule.getRateInterval());
    } catch (Exception e) {
        results =  rScript.eval(RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.MULTI,
                keys,
                rule.getRate(),
                rule.getRateInterval());
    }
    boolean isAllowed = results.get(0) == 1L;
    long ttl = results.get(1);

    return new Result(isAllowed, ttl);
}
```

## 新增滑动窗口算法
```lua
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
```