const { Redis } = require("@upstash/redis");

let redis = null;
let redis2 = null;

if (process.env.UPSTASH_REDIS_REST_URL && process.env.UPSTASH_REDIS_REST_TOKEN) {
  try {
    redis = new Redis({
      url: process.env.UPSTASH_REDIS_REST_URL,
      token: process.env.UPSTASH_REDIS_REST_TOKEN,
    });
    console.log("Redis client 1 initialized");
  } catch (error) {
    console.error("Failed to initialize Redis client 1:", error);
  }
} else {
  console.log("Redis credentials 1 not provided, primary Redis integration disabled.");
}

// Optional second Redis instance. Configure with UPSTASH_REDIS2_REST_URL and
// UPSTASH_REDIS2_REST_TOKEN in the backend environment.
if (process.env.UPSTASH_REDIS2_REST_URL && process.env.UPSTASH_REDIS2_REST_TOKEN) {
  try {
    redis2 = new Redis({
      url: process.env.UPSTASH_REDIS2_REST_URL,
      token: process.env.UPSTASH_REDIS2_REST_TOKEN,
    });
    console.log("Redis client 2 initialized");
  } catch (error) {
    console.error("Failed to initialize Redis client 2:", error);
  }
} else {
  console.log("Redis credentials 2 not provided, secondary Redis integration disabled.");
}

async function setOnlineStatus(userId) {
  if (!redis) return;
  try {
    await redis.set(`user:online:${userId}`, "1", { ex: 3600 });
  } catch (error) {
    console.error(`Failed to set online status for user ${userId} in Redis 1:`, error.message);
  }
}

async function removeOnlineStatus(userId) {
  if (!redis) return;
  try {
    await redis.del(`user:online:${userId}`);
  } catch (error) {
    console.error(`Failed to remove online status for user ${userId} in Redis 1:`, error.message);
  }
}

async function setSecondaryStatus(userId) {
  if (!redis2) return;
  try {
    await redis2.set(`user:online:${userId}`, "1", { ex: 3600 });
  } catch (error) {
    console.error(`Failed to set online status for user ${userId} in Redis 2:`, error.message);
  }
}

async function removeSecondaryStatus(userId) {
  if (!redis2) return;
  try {
    await redis2.del(`user:online:${userId}`);
  } catch (error) {
    console.error(`Failed to remove online status for user ${userId} in Redis 2:`, error.message);
  }
}

module.exports = {
  redis,
  redis2,
  setOnlineStatus,
  removeOnlineStatus,
  setSecondaryStatus,
  removeSecondaryStatus,
};
