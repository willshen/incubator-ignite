/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.cache.eviction.fifo;

import org.apache.ignite.cache.eviction.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.jsr166.*;
import org.jsr166.ConcurrentLinkedDeque8.*;

import java.io.*;
import java.util.*;

/**
 * Eviction policy based on {@code First In First Out (FIFO)} algorithm and supports batch eviction.
 * <p>
 * The eviction starts when the cache size becomes {@code batchSize} elements greater than the maximum size.
 * {@code batchSize} elements will be evicted in this case. The default {@code batchSize} value is {@code 1}.
 * <p>
 * This implementation is very efficient since it does not create any additional
 * table-like data structures. The {@code FIFO} ordering information is
 * maintained by attaching ordering metadata to cache entries.
 */
public class FifoEvictionPolicy<K, V> implements EvictionPolicy<K, V>, FifoEvictionPolicyMBean, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Maximum size. */
    private volatile int max = CacheConfiguration.DFLT_CACHE_SIZE;

    /** Batch size. */
    private volatile int batchSize = 1;

    /** FIFO queue. */
    private final ConcurrentLinkedDeque8<EvictableEntry<K, V>> queue =
        new ConcurrentLinkedDeque8<>();

    /**
     * Constructs FIFO eviction policy with all defaults.
     */
    public FifoEvictionPolicy() {
        // No-op.
    }

    /**
     * Constructs FIFO eviction policy with maximum size. Empty entries are allowed.
     *
     * @param max Maximum allowed size of cache before entry will start getting evicted.
     */
    public FifoEvictionPolicy(int max) {
        A.ensure(max > 0, "max > 0");

        this.max = max;
    }

    /**
     * Constructs FIFO eviction policy with maximum size and given batch size. Empty entries are allowed.
     *
     * @param max Maximum allowed size of cache before entry will start getting evicted.
     * @param batchSize Batch size.
     */
    public FifoEvictionPolicy(int max, int batchSize) {
        A.ensure(max > 0, "max > 0");
        A.ensure(batchSize > 0, "batchSize > 0");

        this.max = max;
        this.batchSize = batchSize;
    }

    /**
     * Gets maximum allowed size of cache before entry will start getting evicted.
     *
     * @return Maximum allowed size of cache before entry will start getting evicted.
     */
    @Override public int getMaxSize() {
        return max;
    }

    /**
     * Sets maximum allowed size of cache before entry will start getting evicted.
     *
     * @param max Maximum allowed size of cache before entry will start getting evicted.
     */
    @Override public void setMaxSize(int max) {
        A.ensure(max > 0, "max > 0");

        this.max = max;
    }

    /** {@inheritDoc} */
    @Override public int getBatchSize() {
        return batchSize;
    }

    /** {@inheritDoc} */
    @Override public void setBatchSize(int batchSize) {
        A.ensure(batchSize > 0, "batchSize > 0");

        this.batchSize = batchSize;
    }

    /** {@inheritDoc} */
    @Override public int getCurrentSize() {
        return queue.size();
    }

    /**
     * Gets read-only view on internal {@code FIFO} queue in proper order.
     *
     * @return Read-only view ono internal {@code 'FIFO'} queue.
     */
    public Collection<EvictableEntry<K, V>> queue() {
        return Collections.unmodifiableCollection(queue);
    }

    /** {@inheritDoc} */
    @Override public void onEntryAccessed(boolean rmv, EvictableEntry<K, V> entry) {
        if (!rmv) {
            if (!entry.isCached())
                return;

            // Shrink only if queue was changed.
            if (touch(entry))
                shrink();
        }
        else {
            Node<EvictableEntry<K, V>> node = entry.removeMeta();

            if (node != null)
                queue.unlinkx(node);
        }
    }

    /**
     * @param entry Entry to touch.
     * @return {@code True} if queue has been changed by this call.
     */
    private boolean touch(EvictableEntry<K, V> entry) {
        Node<EvictableEntry<K, V>> node = entry.meta();

        // Entry has not been enqueued yet.
        if (node == null) {
            while (true) {
                node = queue.offerLastx(entry);

                if (entry.putMetaIfAbsent(node) != null) {
                    // Was concurrently added, need to clear it from queue.
                    queue.unlinkx(node);

                    // Queue has not been changed.
                    return false;
                }
                else if (node.item() != null) {
                    if (!entry.isCached()) {
                        // Was concurrently evicted, need to clear it from queue.
                        queue.unlinkx(node);

                        return false;
                    }

                    return true;
                }
                // If node was unlinked by concurrent shrink() call, we must repeat the whole cycle.
                else if (!entry.removeMeta(node))
                    return false;
            }
        }

        // Entry is already in queue.
        return false;
    }

    /**
     * Shrinks FIFO queue to maximum allowed size.
     */
    private void shrink() {
        int max = this.max;

        int batchSize = this.batchSize;

        int startSize = queue.sizex();

        // Shrink only if queue is full.
        if (startSize >= max + batchSize) {
            for (int i = max; i < startSize && queue.sizex() > max; i++) {
                EvictableEntry<K, V> entry = queue.poll();

                if (entry == null)
                    break;

                Node<EvictableEntry<K, V>> meta = entry.removeMeta();

                if (meta != null && !entry.evict())
                    touch(entry);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(max);
        out.writeInt(batchSize);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        max = in.readInt();
        batchSize = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(FifoEvictionPolicy.class, this);
    }
}
