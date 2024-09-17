---
--- Created by jin.ping.
--- DateTime: 2024/9/2 11:06
---
if(redis.call('hexists', KEYS[1], ARGV[1]) == 1) then
    redis.call('expire', KEYS[1], ARGV[2]);
    return true;
else
    return false;
end;


