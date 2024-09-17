package com.anthonyzero.eventbus.provider.support;


import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;

import java.util.Optional;

@SuppressWarnings("all")
enum XReadOffsetStrategy {

    /**
     * Use the last seen message Id.
     */
    NextMessage {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return readOffset;
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return ReadOffset.from(lastConsumedMessageId);
        }
    },

    /**
     * Last consumed strategy.
     */
    LastConsumed {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return consumer.map(it -> ReadOffset.lastConsumed()).orElseGet(ReadOffset::latest);
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return consumer.map(it -> ReadOffset.lastConsumed()).orElseGet(() -> ReadOffset.from(lastConsumedMessageId));
        }
    },

    /**
     * Use always the latest stream message.
     */
    Latest {
        @Override
        public ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer) {
            return ReadOffset.latest();
        }

        @Override
        public ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId) {
            return ReadOffset.latest();
        }
    };

    /**
     * Return a {@link XReadOffsetStrategy} given the initial {@link ReadOffset}.
     *
     * @param offset must not be {@literal null}.
     * @return the {@link XReadOffsetStrategy}.
     */
    static XReadOffsetStrategy getStrategy(ReadOffset offset) {

        if (ReadOffset.latest().equals(offset)) {
            return Latest;
        }

        if (ReadOffset.lastConsumed().equals(offset)) {
            return LastConsumed;
        }

        return NextMessage;
    }

    /**
     * Determine the first {@link ReadOffset}.
     *
     * @param readOffset
     * @param consumer
     * @return
     */
    public abstract ReadOffset getFirst(ReadOffset readOffset, Optional<Consumer> consumer);

    /**
     * Determine the next {@link ReadOffset} given {@code lastConsumedMessageId}.
     *
     * @param readOffset
     * @param consumer
     * @param lastConsumedMessageId
     * @return
     */
    public abstract ReadOffset getNext(ReadOffset readOffset, Optional<Consumer> consumer, String lastConsumedMessageId);
}