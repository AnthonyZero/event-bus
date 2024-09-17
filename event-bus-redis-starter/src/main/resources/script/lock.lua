-- 如果锁不存在或者这是自己的锁，就通过hincrby（不存在就新增并加1，存在就加1）获取锁或者锁次数加1。
if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1)
then
    redis.call('hincrby', KEYS[1], ARGV[1], 1);
    redis.call('expire', KEYS[1], ARGV[2]);
    return true;
else
    return false;
end

