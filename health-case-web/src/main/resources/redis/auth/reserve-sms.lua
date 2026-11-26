-- All keys share a Redis Cluster hash tag. Counters use windows starting with the first request.
local cooldown = redis.call('TTL', KEYS[1])
if cooldown >= 0 then
    return math.max(1, cooldown)
end

local function at_limit(key, limit)
    return tonumber(redis.call('GET', key) or '0') >= tonumber(limit)
end

if at_limit(KEYS[2], ARGV[3]) then
    return -1
end
if at_limit(KEYS[3], ARGV[4]) or at_limit(KEYS[4], ARGV[5]) then
    return -2
end

local function count_send(key, window)
    if redis.call('INCR', key) == 1 then
        redis.call('EXPIRE', key, window)
    end
end

count_send(KEYS[2], ARGV[6])
count_send(KEYS[3], ARGV[6])
count_send(KEYS[4], ARGV[7])
redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
return 0
