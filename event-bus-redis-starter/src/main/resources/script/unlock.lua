--判断hashset可重入key的值是否等于0
--如果为nil代表自己的锁已不存在,在尝试解其他线程的锁,解锁失败
--如果为0代表可重入次数被减1
--如果为1代表该可重入key解锁成功
if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then
    return nil;
elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) > 0) then
    return 0;
else
    redis.call('del', KEYS[1]);
    return 1;
end;
