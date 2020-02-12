package rateLimiter.job;

/**
 * This class is responsible for the final decision whether to allow the arrived request or not.
 * <p>
 * On one side, it uses the client's key (which can be a login name, IP, or any other client's identifier).
 * On the other side, it uses the rules configurations (stored on the cache).
 * Based on these two pieces of data, it ends up with a decision whether to allow the coming request or not.
 */
public class TokenBucket {
    private final long maxBucketSize;
    private final long refillRate;

    private double currentBucketSize;
    private long lastRefillTimestamp;

    /**
     * @param maxBucketSize The maximum size a bucket can be loaded with tokens
     * @param refillRate    The rate used in order to refill the bucket
     */
    public TokenBucket(long maxBucketSize, long refillRate) {
        this.maxBucketSize = maxBucketSize;
        this.refillRate = refillRate;

        // The number of tokens initially is equal to the maximum capacity
        currentBucketSize = maxBucketSize;
        lastRefillTimestamp = System.nanoTime(); // Current time in nano
    }

    /**
     * This method keeps track of requests and limits it based on the rate rules.
     * This method needs to be synchronized since it made be called by several threads simultaneously.
     *
     * @param tokens int
     * @return boolean
     */
    public synchronized boolean allowRequest(int tokens) {

        // Fill bucket with tokens accumulated since the last call
        refill();

        // If bucket has enough tokens, pass it on (allow)
        if (currentBucketSize > tokens) {
            currentBucketSize -= tokens;

            return true;
        }

        // Otherwise, the request is throttled because the bucket doesn't have enough tokens
        return false;
    }

    /**
     * This method refills the bucket based on the time elapsed since the last call.
     */
    private void refill() {
        long now = System.nanoTime();

        // Number of tokens that need to be added to the bucket
        double tokensToAdd = (now - lastRefillTimestamp) * refillRate / 1e9; // 10^9

        // The number of the tokens within the bucket should never exceed the maximum capacity
        currentBucketSize = Math.min(currentBucketSize + tokensToAdd, maxBucketSize);
        lastRefillTimestamp = now;
    }
}