local stored = redis.call('HGET', KEYS[1], 'digest')
if not stored then
    return 0
end

local provided = ARGV[1]
local difference = 0
for i = 1, #stored do
    difference = bit.bor(difference, bit.bxor(string.byte(stored, i), string.byte(provided, i) or 0))
end

if difference == 0 and #stored == #provided then
    redis.call('DEL', KEYS[1])
    return 1
end
if redis.call('HINCRBY', KEYS[1], 'attempts', -1) <= 0 then
    redis.call('DEL', KEYS[1])
end
return 0
