-- Only the current send reservation can install a code.
if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return 0
end

redis.call('HSET', KEYS[2], 'digest', ARGV[2], 'attempts', ARGV[3])
redis.call('EXPIRE', KEYS[2], ARGV[4])
return 1
