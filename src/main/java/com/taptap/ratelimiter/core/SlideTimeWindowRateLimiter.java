package com.taptap.ratelimiter.core;

import com.taptap.ratelimiter.model.LuaScript;
import com.taptap.ratelimiter.model.Mode;
import com.taptap.ratelimiter.model.Result;
import com.taptap.ratelimiter.model.Rule;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.taptap.ratelimiter.configuration.RateLimiterAutoConfiguration.REDISSON_BEAN_NAME;

public class SlideTimeWindowRateLimiter implements RateLimiter{
    private final RScript rscript;
    private final String  scriptSha1;

    public SlideTimeWindowRateLimiter(@Qualifier(REDISSON_BEAN_NAME) RedissonClient redissonClient) {
        rscript = redissonClient.getScript(LongCodec.INSTANCE);
        scriptSha1 = rscript.scriptLoad(LuaScript.getSlideWindowRateLimiterScript());
    }
    @Override
    public Result isAllowed(Rule rule) {
        String randomMember = UUID.randomUUID().toString();
        List<Object> keys = getKeys(rule.getKey());
        String script = LuaScript.getSlideWindowRateLimiterScript();

        List<Long> results = null;
        try {
            results = rscript.evalSha(RScript.Mode.READ_WRITE, scriptSha1,
                    RScript.ReturnType.MULTI,
                    keys,
                    rule.getRateInterval(),
                    rule.getRate(),
                    randomMember);
            results = rscript.eval(RScript.Mode.READ_WRITE, script,
                    RScript.ReturnType.MULTI,
                    keys,
                    rule.getRateInterval(),
                    rule.getRate(),
                    randomMember);
        } catch (Exception e) {
            results = rscript.eval(RScript.Mode.READ_WRITE, script,
                    RScript.ReturnType.MULTI,
                    keys,
                    rule.getRateInterval(),
                    rule.getRate(),
                    randomMember);
        }
        boolean allowed = results.get(0) == 1L;
        long remainToken = results.get(1);
        return new Result(allowed, remainToken);
    }
    static List<Object> getKeys(String key) {
        String prefix = "request_rate_limiter_slider.{" + key;
        String keys = prefix + "}";
        return Collections.singletonList(keys);
    }
}
