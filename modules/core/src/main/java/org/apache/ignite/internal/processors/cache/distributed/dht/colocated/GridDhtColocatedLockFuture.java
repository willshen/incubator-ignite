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

package org.apache.ignite.internal.processors.cache.distributed.dht.colocated;

import org.apache.ignite.*;
import org.apache.ignite.cluster.*;
import org.apache.ignite.internal.*;
import org.apache.ignite.internal.cluster.*;
import org.apache.ignite.internal.processors.affinity.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.internal.processors.cache.distributed.*;
import org.apache.ignite.internal.processors.cache.distributed.dht.*;
import org.apache.ignite.internal.processors.cache.distributed.near.*;
import org.apache.ignite.internal.processors.cache.transactions.*;
import org.apache.ignite.internal.processors.cache.version.*;
import org.apache.ignite.internal.processors.timeout.*;
import org.apache.ignite.internal.util.future.*;
import org.apache.ignite.internal.util.tostring.*;
import org.apache.ignite.internal.util.typedef.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.transactions.*;
import org.jetbrains.annotations.*;
import org.jsr166.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.apache.ignite.events.EventType.*;

/**
 * Colocated cache lock future.
 */
public final class GridDhtColocatedLockFuture<K, V> extends GridCompoundIdentityFuture<Boolean>
    implements GridCacheFuture<Boolean> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Logger reference. */
    private static final AtomicReference<IgniteLogger> logRef = new AtomicReference<>();

    /** Logger. */
    private static IgniteLogger log;

    /** Cache registry. */
    @GridToStringExclude
    private GridCacheContext<K, V> cctx;

    /** Lock owner thread. */
    @GridToStringInclude
    private long threadId;

    /** Keys to lock. */
    private Collection<KeyCacheObject> keys;

    /** Future ID. */
    private IgniteUuid futId;

    /** Lock version. */
    private GridCacheVersion lockVer;

    /** Read flag. */
    private boolean read;

    /** Flag to return value. */
    private boolean retval;

    /** Error. */
    private AtomicReference<Throwable> err = new AtomicReference<>(null);

    /** Timeout object. */
    @GridToStringExclude
    private LockTimeoutObject timeoutObj;

    /** Lock timeout. */
    private long timeout;

    /** Filter. */
    private CacheEntryPredicate[] filter;

    /** Transaction. */
    @GridToStringExclude
    private GridNearTxLocal tx;

    /** Topology snapshot to operate on. */
    private AtomicReference<AffinityTopologyVersion> topVer = new AtomicReference<>();

    /** Map of current values. */
    private Map<KeyCacheObject, IgniteBiTuple<GridCacheVersion, CacheObject>> valMap;

    /** Trackable flag (here may be non-volatile). */
    private boolean trackable;

    /** TTL for read operation. */
    private long accessTtl;

    /** Skip store flag. */
    private final boolean skipStore;

    /**
     * @param cctx Registry.
     * @param keys Keys to lock.
     * @param tx Transaction.
     * @param read Read flag.
     * @param retval Flag to return value or not.
     * @param timeout Lock acquisition timeout.
     * @param accessTtl TTL for read operation.
     * @param filter Filter.
     * @param skipStore
     */
    public GridDhtColocatedLockFuture(
        GridCacheContext<K, V> cctx,
        Collection<KeyCacheObject> keys,
        @Nullable GridNearTxLocal tx,
        boolean read,
        boolean retval,
        long timeout,
        long accessTtl,
        CacheEntryPredicate[] filter,
        boolean skipStore) {
        super(cctx.kernalContext(), CU.boolReducer());

        assert keys != null;

        this.cctx = cctx;
        this.keys = keys;
        this.tx = tx;
        this.read = read;
        this.retval = retval;
        this.timeout = timeout;
        this.accessTtl = accessTtl;
        this.filter = filter;
        this.skipStore = skipStore;

        ignoreInterrupts(true);

        threadId = tx == null ? Thread.currentThread().getId() : tx.threadId();

        lockVer = tx != null ? tx.xidVersion() : cctx.versions().next();

        futId = IgniteUuid.randomUuid();

        if (log == null)
            log = U.logger(cctx.kernalContext(), logRef, GridDhtColocatedLockFuture.class);

        if (timeout > 0) {
            timeoutObj = new LockTimeoutObject();

            cctx.time().addTimeoutObject(timeoutObj);
        }

        valMap = new ConcurrentHashMap8<>(keys.size(), 1f);
    }

    /**
     * @return Participating nodes.
     */
    @Override public Collection<? extends ClusterNode> nodes() {
        return F.viewReadOnly(futures(), new IgniteClosure<IgniteInternalFuture<?>, ClusterNode>() {
            @Nullable @Override public ClusterNode apply(IgniteInternalFuture<?> f) {
                if (isMini(f))
                    return ((MiniFuture)f).node();

                return cctx.discovery().localNode();
            }
        });
    }

    /** {@inheritDoc} */
    @Override public GridCacheVersion version() {
        return lockVer;
    }

    /**
     * @return Future ID.
     */
    @Override public IgniteUuid futureId() {
        return futId;
    }

    /** {@inheritDoc} */
    @Override public boolean trackable() {
        return trackable;
    }

    /** {@inheritDoc} */
    @Override public void markNotTrackable() {
        trackable = false;
    }

    /**
     * @return {@code True} if transaction is not {@code null}.
     */
    private boolean inTx() {
        return tx != null;
    }

    /**
     * @return {@code True} if implicit-single-tx flag is set.
     */
    private boolean implicitSingleTx() {
        return tx != null && tx.implicitSingle();
    }

    /**
     * @return {@code True} if transaction is not {@code null} and has invalidate flag set.
     */
    private boolean isInvalidate() {
        return tx != null && tx.isInvalidate();
    }

    /**
     * @return {@code True} if commit is synchronous.
     */
    private boolean syncCommit() {
        return tx != null && tx.syncCommit();
    }

    /**
     * @return {@code True} if rollback is synchronous.
     */
    private boolean syncRollback() {
        return tx != null && tx.syncRollback();
    }

    /**
     * @return Transaction isolation or {@code null} if no transaction.
     */
    @Nullable private TransactionIsolation isolation() {
        return tx == null ? null : tx.isolation();
    }

    /**
     * @return {@code true} if related transaction is implicit.
     */
    private boolean implicitTx() {
        return tx != null && tx.implicit();
    }

    /**
     * Adds entry to future.
     *
     * @param entry Entry to add.
     * @return Non-reentry candidate if lock should be acquired on remote node,
     *      reentry candidate if locks has been already acquired and {@code null} if explicit locks is held and
     *      implicit transaction accesses locked entry.
     * @throws IgniteCheckedException If failed to add entry due to external locking.
     */
    @Nullable private GridCacheMvccCandidate addEntry(GridDistributedCacheEntry entry) throws IgniteCheckedException {
        GridCacheMvccCandidate cand = cctx.mvcc().explicitLock(threadId, entry.key());

        if (inTx()) {
            IgniteTxEntry txEntry = tx.entry(entry.txKey());

            txEntry.cached(entry);

            if (cand != null) {
                if (!tx.implicit())
                    throw new IgniteCheckedException("Cannot access key within transaction if lock is " +
                        "externally held [key=" + entry.key() + ", entry=" + entry + ']');
                else
                    return null;
            }
            else {
                // Check transaction entries (corresponding tx entries must be enlisted in transaction).
                cand = new GridCacheMvccCandidate(entry,
                    cctx.localNodeId(),
                    null,
                    null,
                    threadId,
                    lockVer,
                    timeout,
                    true,
                    tx.entry(entry.txKey()).locked(),
                    inTx(),
                    inTx() && tx.implicitSingle(),
                    false,
                    false);

                cand.topologyVersion(new AffinityTopologyVersion(topVer.get().topologyVersion()));
            }
        }
        else {
            if (cand == null) {
                cand = new GridCacheMvccCandidate(entry,
                    cctx.localNodeId(),
                    null,
                    null,
                    threadId,
                    lockVer,
                    timeout,
                    true,
                    false,
                    inTx(),
                    inTx() && tx.implicitSingle(),
                    false,
                    false);

                cand.topologyVersion(new AffinityTopologyVersion(topVer.get().topologyVersion()));
            }
            else
                cand = cand.reenter();

            cctx.mvcc().addExplicitLock(threadId, cand, topVer.get());
        }

        return cand;
    }

    /**
     * Undoes all locks.
     *
     * @param dist If {@code true}, then remove locks from remote nodes as well.
     */
    private void undoLocks(boolean dist) {
        // Transactions will undo during rollback.
        if (dist && tx == null)
            cctx.colocated().removeLocks(threadId, lockVer, keys);
        else {
            if (tx != null) {
                if (tx.setRollbackOnly()) {
                    if (log.isDebugEnabled())
                        log.debug("Marked transaction as rollback only because locks could not be acquired: " + tx);
                }
                else if (log.isDebugEnabled())
                    log.debug("Transaction was not marked rollback-only while locks were not acquired: " + tx);
            }
        }

        cctx.mvcc().recheckPendingLocks();
    }

    /**
     *
     * @param dist {@code True} if need to distribute lock release.
     */
    private void onFailed(boolean dist) {
        undoLocks(dist);

        complete(false);
    }

    /**
     * @param success Success flag.
     */
    public void complete(boolean success) {
        onComplete(success, true);
    }

    /**
     * @param nodeId Left node ID
     * @return {@code True} if node was in the list.
     */
    @Override public boolean onNodeLeft(UUID nodeId) {
        boolean found = false;

        for (IgniteInternalFuture<?> fut : futures()) {
            if (isMini(fut)) {
                MiniFuture f = (MiniFuture)fut;

                if (f.node().id().equals(nodeId)) {
                    if (log.isDebugEnabled())
                        log.debug("Found mini-future for left node [nodeId=" + nodeId + ", mini=" + f + ", fut=" +
                            this + ']');

                    f.onResult(newTopologyException(null, nodeId));

                    found = true;
                }
            }
        }

        if (log.isDebugEnabled())
            log.debug("Near lock future does not have mapping for left node (ignoring) [nodeId=" + nodeId + ", fut=" +
                this + ']');

        return found;
    }

    /**
     * @param nodeId Sender.
     * @param res Result.
     */
    void onResult(UUID nodeId, GridNearLockResponse res) {
        if (!isDone()) {
            if (log.isDebugEnabled())
                log.debug("Received lock response from node [nodeId=" + nodeId + ", res=" + res + ", fut=" +
                    this + ']');

            for (IgniteInternalFuture<Boolean> fut : pending()) {
                if (isMini(fut)) {
                    MiniFuture mini = (MiniFuture)fut;

                    if (mini.futureId().equals(res.miniId())) {
                        assert mini.node().id().equals(nodeId);

                        if (log.isDebugEnabled())
                            log.debug("Found mini future for response [mini=" + mini + ", res=" + res + ']');

                        mini.onResult(res);

                        if (log.isDebugEnabled())
                            log.debug("Future after processed lock response [fut=" + this + ", mini=" + mini +
                                ", res=" + res + ']');

                        return;
                    }
                }
            }

            U.warn(log, "Failed to find mini future for response (perhaps due to stale message) [res=" + res +
                ", fut=" + this + ']');
        }
        else if (log.isDebugEnabled())
            log.debug("Ignoring lock response from node (future is done) [nodeId=" + nodeId + ", res=" + res +
                ", fut=" + this + ']');
    }

    /**
     * @param t Error.
     */
    private void onError(Throwable t) {
        err.compareAndSet(null, t instanceof GridCacheLockTimeoutException ? null : t);
    }

    /** {@inheritDoc} */
    @Override public boolean cancel() {
        if (onCancelled())
            onComplete(false, true);

        return isCancelled();
    }

    /** {@inheritDoc} */
    @Override public boolean onDone(Boolean success, Throwable err) {
        if (log.isDebugEnabled())
            log.debug("Received onDone(..) callback [success=" + success + ", err=" + err + ", fut=" + this + ']');

        if (isDone())
            return false;

        this.err.compareAndSet(null, err instanceof GridCacheLockTimeoutException ? null : err);

        if (err != null)
            success = false;

        return onComplete(success, true);
    }

    /**
     * Completeness callback.
     *
     * @param success {@code True} if lock was acquired.
     * @param distribute {@code True} if need to distribute lock removal in case of failure.
     * @return {@code True} if complete by this operation.
     */
    private boolean onComplete(boolean success, boolean distribute) {
        if (log.isDebugEnabled())
            log.debug("Received onComplete(..) callback [success=" + success + ", distribute=" + distribute +
                ", fut=" + this + ']');

        if (!success)
            undoLocks(distribute);

        if (tx != null)
            cctx.tm().txContext(tx);

        if (super.onDone(success, err.get())) {
            if (log.isDebugEnabled())
                log.debug("Completing future: " + this);

            // Clean up.
            cctx.mvcc().removeFuture(this);

            if (timeoutObj != null)
                cctx.time().removeTimeoutObject(timeoutObj);

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return futId.hashCode();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDhtColocatedLockFuture.class, this, "inTx", inTx(), "super", super.toString());
    }

    /**
     * @param f Future.
     * @return {@code True} if mini-future.
     */
    private boolean isMini(IgniteInternalFuture<?> f) {
        return f.getClass().equals(MiniFuture.class);
    }

    /**
     * Basically, future mapping consists from two parts. First, we must determine the topology version this future
     * will map on. Locking is performed within a user transaction, we must continue to map keys on the same
     * topology version as it started. If topology version is undefined, we get current topology future and wait
     * until it completes so the topology is ready to use.
     * <p/>
     * During the second part we map keys to primary nodes using topology snapshot we obtained during the first
     * part. Note that if primary node leaves grid, the future will fail and transaction will be rolled back.
     */
    void map() {
        // Obtain the topology version to use.
        AffinityTopologyVersion topVer = cctx.mvcc().lastExplicitLockTopologyVersion(threadId);

        if (topVer != null && tx != null)
            tx.topologyVersion(topVer);

        if (topVer == null && tx != null)
            topVer = tx.topologyVersionSnapshot();

        if (topVer != null) {
            for (GridDhtTopologyFuture fut : cctx.shared().exchange().exchangeFutures()){
                if (fut.topologyVersion().equals(topVer)){
                    if (!fut.isCacheTopologyValid(cctx)) {
                        onDone(new IgniteCheckedException("Failed to perform cache operation (cache topology is not valid): " +
                            cctx.name()));

                        return;
                    }

                    break;
                }
            }

            // Continue mapping on the same topology version as it was before.
            this.topVer.compareAndSet(null, topVer);

            map(keys);

            markInitialized();

            return;
        }

        // Must get topology snapshot and map on that version.
        mapOnTopology();
    }

    /**
     * Acquires topology future and checks it completeness under the read lock. If it is not complete,
     * will asynchronously wait for it's completeness and then try again.
     */
    private void mapOnTopology() {
        // We must acquire topology snapshot from the topology version future.
        cctx.topology().readLock();

        try {
            if (cctx.topology().stopping()) {
                onDone(new IgniteCheckedException("Failed to perform cache operation (cache is stopped): " +
                    cctx.name()));

                return;
            }

            GridDhtTopologyFuture fut = cctx.topologyVersionFuture();

            if (fut.isDone()) {
                if (!fut.isCacheTopologyValid(cctx)) {
                    onDone(new IgniteCheckedException("Failed to perform cache operation (cache topology is not valid): " +
                        cctx.name()));

                    return;
                }

                AffinityTopologyVersion topVer = fut.topologyVersion();

                if (tx != null)
                    tx.topologyVersion(topVer);

                this.topVer.compareAndSet(null, topVer);

                map(keys);

                markInitialized();
            }
            else {
                fut.listen(new CI1<IgniteInternalFuture<AffinityTopologyVersion>>() {
                    @Override public void apply(IgniteInternalFuture<AffinityTopologyVersion> t) {
                        mapOnTopology();
                    }
                });
            }
        }
        finally {
            cctx.topology().readUnlock();
        }
    }

    /**
     * Maps keys to nodes. Note that we can not simply group keys by nodes and send lock request as
     * such approach does not preserve order of lock acquisition. Instead, keys are split in continuous
     * groups belonging to one primary node and locks for these groups are acquired sequentially.
     *
     * @param keys Keys.
     */
    private void map(Collection<KeyCacheObject> keys) {
        try {
            AffinityTopologyVersion topVer = this.topVer.get();

            assert topVer != null;

            assert topVer.topologyVersion() > 0;

            if (CU.affinityNodes(cctx, topVer).isEmpty()) {
                onDone(new ClusterTopologyServerNotFoundException("Failed to map keys for cache " +
                    "(all partition nodes left the grid): " + cctx.name()));

                return;
            }

            // First assume this node is primary for all keys passed in.
            if (mapAsPrimary(keys, topVer))
                return;

            Deque<GridNearLockMapping> mappings = new ConcurrentLinkedDeque8<>();

            // Assign keys to primary nodes.
            GridNearLockMapping map = null;

            for (KeyCacheObject key : keys) {
                GridNearLockMapping updated = map(key, map, topVer);

                // If new mapping was created, add to collection.
                if (updated != map) {
                    mappings.add(updated);

                    if (tx != null && updated.node().isLocal())
                        tx.colocatedLocallyMapped(true);
                }

                map = updated;
            }

            if (isDone()) {
                if (log.isDebugEnabled())
                    log.debug("Abandoning (re)map because future is done: " + this);

                return;
            }

            if (log.isDebugEnabled())
                log.debug("Starting (re)map for mappings [mappings=" + mappings + ", fut=" + this + ']');

            boolean hasRmtNodes = false;

            // Create mini futures.
            for (Iterator<GridNearLockMapping> iter = mappings.iterator(); iter.hasNext(); ) {
                GridNearLockMapping mapping = iter.next();

                ClusterNode node = mapping.node();
                Collection<KeyCacheObject> mappedKeys = mapping.mappedKeys();

                boolean loc = node.equals(cctx.localNode());

                assert !mappedKeys.isEmpty();

                GridNearLockRequest req = null;

                Collection<KeyCacheObject> distributedKeys = new ArrayList<>(mappedKeys.size());

                for (KeyCacheObject key : mappedKeys) {
                    IgniteTxKey txKey = cctx.txKey(key);

                    GridDistributedCacheEntry entry = null;

                    if (tx != null) {
                        IgniteTxEntry txEntry = tx.entry(txKey);

                        if (txEntry != null) {
                            entry = (GridDistributedCacheEntry)txEntry.cached();

                            if (entry != null && !(loc ^ entry.detached())) {
                                entry = cctx.colocated().entryExx(key, topVer, true);

                                txEntry.cached(entry);
                            }
                        }
                    }

                    boolean explicit;

                    while (true) {
                        try {
                            if (entry == null)
                                entry = cctx.colocated().entryExx(key, topVer, true);

                            if (!cctx.isAll(entry, filter)) {
                                if (log.isDebugEnabled())
                                    log.debug("Entry being locked did not pass filter (will not lock): " + entry);

                                onComplete(false, false);

                                return;
                            }

                            assert loc ^ entry.detached() : "Invalid entry [loc=" + loc + ", entry=" + entry + ']';

                            GridCacheMvccCandidate cand = addEntry(entry);

                            // Will either return value from dht cache or null if this is a miss.
                            IgniteBiTuple<GridCacheVersion, CacheObject> val = entry.detached() ? null :
                                ((GridDhtCacheEntry)entry).versionedValue(topVer);

                            GridCacheVersion dhtVer = null;

                            if (val != null) {
                                dhtVer = val.get1();

                                valMap.put(key, val);
                            }

                            if (cand != null && !cand.reentry()) {
                                if (req == null) {
                                    req = new GridNearLockRequest(
                                        cctx.cacheId(),
                                        topVer,
                                        cctx.nodeId(),
                                        threadId,
                                        futId,
                                        lockVer,
                                        inTx(),
                                        implicitTx(),
                                        implicitSingleTx(),
                                        read,
                                        retval,
                                        isolation(),
                                        isInvalidate(),
                                        timeout,
                                        mappedKeys.size(),
                                        inTx() ? tx.size() : mappedKeys.size(),
                                        inTx() && tx.syncCommit(),
                                        inTx() ? tx.groupLockKey() : null,
                                        inTx() && tx.partitionLock(),
                                        inTx() ? tx.subjectId() : null,
                                        inTx() ? tx.taskNameHash() : 0,
                                        read ? accessTtl : -1L,
                                        skipStore);

                                    mapping.request(req);
                                }

                                distributedKeys.add(key);

                                if (tx != null)
                                    tx.addKeyMapping(txKey, mapping.node());

                                req.addKeyBytes(
                                    key,
                                    retval,
                                    dhtVer, // Include DHT version to match remote DHT entry.
                                    cctx);
                            }

                            explicit = inTx() && cand == null;

                            if (explicit)
                                tx.addKeyMapping(txKey, mapping.node());

                            break;
                        }
                        catch (GridCacheEntryRemovedException ignored) {
                            if (log.isDebugEnabled())
                                log.debug("Got removed entry in lockAsync(..) method (will retry): " + entry);
                        }
                    }

                    // Mark mapping explicit lock flag.
                    if (explicit) {
                        boolean marked = tx != null && tx.markExplicit(node.id());

                        assert tx == null || marked;
                    }
                }

                if (inTx() && req != null)
                    req.hasTransforms(tx.hasTransforms());

                if (!distributedKeys.isEmpty()) {
                    mapping.distributedKeys(distributedKeys);

                    hasRmtNodes |= !mapping.node().isLocal();
                }
                else {
                    assert mapping.request() == null;

                    iter.remove();
                }
            }

            if (hasRmtNodes) {
                trackable = true;

                if (!cctx.mvcc().addFuture(this))
                    throw new IllegalStateException("Duplicate future ID: " + this);
            }
            else
                trackable = false;

            proceedMapping(mappings);
        }
        catch (IgniteCheckedException ex) {
            onDone(false, ex);
        }
    }

    /**
     * Gets next near lock mapping and either acquires dht locks locally or sends near lock request to
     * remote primary node.
     *
     * @param mappings Queue of mappings.
     * @throws IgniteCheckedException If mapping can not be completed.
     */
    private void proceedMapping(final Deque<GridNearLockMapping> mappings)
        throws IgniteCheckedException {
        GridNearLockMapping map = mappings.poll();

        // If there are no more mappings to process, complete the future.
        if (map == null)
            return;

        final GridNearLockRequest req = map.request();
        final Collection<KeyCacheObject> mappedKeys = map.distributedKeys();
        final ClusterNode node = map.node();

        if (filter != null && filter.length != 0)
            req.filter(filter, cctx);

        if (node.isLocal())
            lockLocally(mappedKeys, req.topologyVersion(), mappings);
        else {
            final MiniFuture fut = new MiniFuture(node, mappedKeys, mappings);

            req.miniId(fut.futureId());

            add(fut); // Append new future.

            IgniteInternalFuture<?> txSync = null;

            if (inTx())
                txSync = cctx.tm().awaitFinishAckAsync(node.id(), tx.threadId());

            if (txSync == null || txSync.isDone()) {
                try {
                    if (log.isDebugEnabled())
                        log.debug("Sending near lock request [node=" + node.id() + ", req=" + req + ']');

                    cctx.io().send(node, req, cctx.ioPolicy());
                }
                catch (ClusterTopologyCheckedException ex) {
                    assert fut != null;

                    fut.onResult(ex);
                }
            }
            else {
                txSync.listen(new CI1<IgniteInternalFuture<?>>() {
                    @Override public void apply(IgniteInternalFuture<?> t) {
                        try {
                            if (log.isDebugEnabled())
                                log.debug("Sending near lock request [node=" + node.id() + ", req=" + req + ']');

                            cctx.io().send(node, req, cctx.ioPolicy());
                        }
                        catch (ClusterTopologyCheckedException ex) {
                            assert fut != null;

                            fut.onResult(ex);
                        }
                        catch (IgniteCheckedException e) {
                            onError(e);
                        }
                    }
                });
            }
        }
    }

    /**
     * Locks given keys directly through dht cache.
     *
     * @param keys Collection of keys.
     * @param topVer Topology version to lock on.
     * @param mappings Optional collection of mappings to proceed locking.
     */
    private void lockLocally(
        final Collection<KeyCacheObject> keys,
        AffinityTopologyVersion topVer,
        @Nullable final Deque<GridNearLockMapping> mappings
    ) {
        if (log.isDebugEnabled())
            log.debug("Before locally locking keys : " + keys);

        IgniteInternalFuture<Exception> fut = cctx.colocated().lockAllAsync(cctx,
            tx,
            threadId,
            lockVer,
            topVer,
            keys,
            read,
            retval,
            timeout,
            accessTtl,
            filter,
            skipStore);

        // Add new future.
        add(new GridEmbeddedFuture<>(
            new C2<Exception, Exception, Boolean>() {
                @Override public Boolean apply(Exception resEx, Exception e) {
                    if (CU.isLockTimeoutOrCancelled(e) ||
                        (resEx != null && CU.isLockTimeoutOrCancelled(resEx)))
                        return false;

                    if (e != null) {
                        onError(e);

                        return false;
                    }

                    if (resEx != null) {
                        onError(resEx);

                        return false;
                    }

                    if (log.isDebugEnabled())
                        log.debug("Acquired lock for local DHT mapping [locId=" + cctx.nodeId() +
                            ", mappedKeys=" + keys + ", fut=" + GridDhtColocatedLockFuture.this + ']');

                    if (inTx()) {
                        for (KeyCacheObject key : keys)
                            tx.entry(cctx.txKey(key)).markLocked();
                    }
                    else {
                        for (KeyCacheObject key : keys)
                            cctx.mvcc().markExplicitOwner(key, threadId);
                    }

                    try {
                        // Proceed and add new future (if any) before completing embedded future.
                        if (mappings != null)
                            proceedMapping(mappings);
                    }
                    catch (IgniteCheckedException ex) {
                        onError(ex);

                        return false;
                    }

                    return true;
                }
            },
            fut));
    }

    /**
     * Tries to map this future in assumption that local node is primary for all keys passed in.
     * If node is not primary for one of the keys, then mapping is reverted and full remote mapping is performed.
     *
     * @param keys Keys to lock.
     * @param topVer Topology version.
     * @return {@code True} if all keys were mapped locally, {@code false} if full mapping should be performed.
     * @throws IgniteCheckedException If key cannot be added to mapping.
     */
    private boolean mapAsPrimary(Collection<KeyCacheObject> keys, AffinityTopologyVersion topVer) throws IgniteCheckedException {
        // Assign keys to primary nodes.
        Collection<KeyCacheObject> distributedKeys = new ArrayList<>(keys.size());

        boolean explicit = false;

        for (KeyCacheObject key : keys) {
            if (!cctx.affinity().primary(cctx.localNode(), key, topVer)) {
                // Remove explicit locks added so far.
                for (KeyCacheObject k : keys)
                    cctx.mvcc().removeExplicitLock(threadId, k, lockVer);

                return false;
            }

            explicit |= addLocalKey(key, topVer, distributedKeys);

            if (isDone())
                return true;
        }

        trackable = false;

        if (tx != null) {
            if (explicit)
                tx.markExplicit(cctx.localNodeId());

            tx.colocatedLocallyMapped(true);
        }

        if (!distributedKeys.isEmpty()) {
            if (tx != null) {
                for (KeyCacheObject key : distributedKeys)
                    tx.addKeyMapping(cctx.txKey(key), cctx.localNode());
            }

            lockLocally(distributedKeys, topVer, null);
        }

        return true;
    }

    /**
     * Adds local key future.
     *
     * @param key Key to add.
     * @param topVer Topology version.
     * @param distributedKeys Collection of keys needs to be locked.
     * @return {@code True} if transaction accesses key that was explicitly locked before.
     * @throws IgniteCheckedException If lock is externally held and transaction is explicit.
     */
    private boolean addLocalKey(
        KeyCacheObject key,
        AffinityTopologyVersion topVer,
        Collection<KeyCacheObject> distributedKeys
    ) throws IgniteCheckedException {
        GridDistributedCacheEntry entry = cctx.colocated().entryExx(key, topVer, false);

        assert !entry.detached();

        if (!cctx.isAll(entry, filter)) {
            if (log.isDebugEnabled())
                log.debug("Entry being locked did not pass filter (will not lock): " + entry);

            onComplete(false, false);

            return false;
        }

        GridCacheMvccCandidate cand = addEntry(entry);

        if (cand != null && !cand.reentry())
            distributedKeys.add(key);

        return inTx() && cand == null;
    }

    /**
     * @param mapping Mappings.
     * @param key Key to map.
     * @param topVer Topology version.
     * @return Near lock mapping.
     * @throws IgniteCheckedException If mapping failed.
     */
    private GridNearLockMapping map(
        KeyCacheObject key,
        @Nullable GridNearLockMapping mapping,
        AffinityTopologyVersion topVer
    ) throws IgniteCheckedException {
        assert mapping == null || mapping.node() != null;

        ClusterNode primary = cctx.affinity().primary(key, topVer);

        if (primary == null)
            throw new ClusterTopologyServerNotFoundException("Failed to lock keys " +
                "(all partition nodes left the grid).");

        if (cctx.discovery().node(primary.id()) == null)
            // If primary node left the grid before lock acquisition, fail the whole future.
            throw newTopologyException(null, primary.id());

        if (inTx() && tx.groupLock() && !primary.isLocal())
            throw new IgniteCheckedException("Failed to start group lock transaction (local node is not primary for " +
                " key) [key=" + key + ", primaryNodeId=" + primary.id() + ']');

        if (mapping == null || !primary.id().equals(mapping.node().id()))
            mapping = new GridNearLockMapping(primary, key);
        else
            mapping.addKey(key);

        return mapping;
    }

    /**
     * Creates new topology exception for cases when primary node leaves grid during mapping.
     *
     * @param nested Optional nested exception.
     * @param nodeId Node ID.
     * @return Topology exception with user-friendly message.
     */
    private ClusterTopologyCheckedException newTopologyException(@Nullable Throwable nested, UUID nodeId) {
        return new ClusterTopologyCheckedException("Failed to acquire lock for keys (primary node left grid, " +
            "retry transaction if possible) [keys=" + keys + ", node=" + nodeId + ']', nested);
    }

    /**
     * Lock request timeout object.
     */
    private class LockTimeoutObject extends GridTimeoutObjectAdapter {
        /**
         * Default constructor.
         */
        LockTimeoutObject() {
            super(timeout);
        }

        /** {@inheritDoc} */
        @Override public void onTimeout() {
            if (log.isDebugEnabled())
                log.debug("Timed out waiting for lock response: " + this);

            onComplete(false, true);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(LockTimeoutObject.class, this);
        }
    }

    /**
     * Mini-future for get operations. Mini-futures are only waiting on a single
     * node as opposed to multiple nodes.
     */
    private class MiniFuture extends GridFutureAdapter<Boolean> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final IgniteUuid futId = IgniteUuid.randomUuid();

        /** Node ID. */
        @GridToStringExclude
        private ClusterNode node;

        /** Keys. */
        @GridToStringInclude
        private Collection<KeyCacheObject> keys;

        /** Mappings to proceed. */
        @GridToStringExclude
        private Deque<GridNearLockMapping> mappings;

        /** */
        private AtomicBoolean rcvRes = new AtomicBoolean(false);

        /**
         * @param node Node.
         * @param keys Keys.
         * @param mappings Mappings to proceed.
         */
        MiniFuture(ClusterNode node,
            Collection<KeyCacheObject> keys,
            Deque<GridNearLockMapping> mappings) {
            this.node = node;
            this.keys = keys;
            this.mappings = mappings;
        }

        /**
         * @return Future ID.
         */
        IgniteUuid futureId() {
            return futId;
        }

        /**
         * @return Node ID.
         */
        public ClusterNode node() {
            return node;
        }

        /**
         * @return Keys.
         */
        public Collection<KeyCacheObject> keys() {
            return keys;
        }

        /**
         * @param e Error.
         */
        void onResult(Throwable e) {
            if (rcvRes.compareAndSet(false, true)) {
                if (log.isDebugEnabled())
                    log.debug("Failed to get future result [fut=" + this + ", err=" + e + ']');

                // Fail.
                onDone(e);
            }
            else
                U.warn(log, "Received error after another result has been processed [fut=" +
                    GridDhtColocatedLockFuture.this + ", mini=" + this + ']', e);
        }

        /**
         * @param e Node left exception.
         */
        void onResult(ClusterTopologyCheckedException e) {
            if (isDone())
                return;

            if (rcvRes.compareAndSet(false, true)) {
                if (log.isDebugEnabled())
                    log.debug("Remote node left grid while sending or waiting for reply (will fail): " + this);

                if (tx != null)
                    tx.removeMapping(node.id());

                // Primary node left the grid, so fail the future.
                GridDhtColocatedLockFuture.this.onDone(newTopologyException(e, node.id()));

                onDone(true);
            }
        }

        /**
         * @param res Result callback.
         */
        void onResult(GridNearLockResponse res) {
            if (rcvRes.compareAndSet(false, true)) {
                if (res.error() != null) {
                    if (log.isDebugEnabled())
                        log.debug("Finishing mini future with an error due to error in response [miniFut=" + this +
                            ", res=" + res + ']');

                    // Fail.
                    if (res.error() instanceof GridCacheLockTimeoutException)
                        onDone(false);
                    else
                        onDone(res.error());

                    return;
                }

                int i = 0;

                for (KeyCacheObject k : keys) {
                    IgniteBiTuple<GridCacheVersion, CacheObject> oldValTup = valMap.get(k);

                    CacheObject newVal = res.value(i);

                    GridCacheVersion dhtVer = res.dhtVersion(i);

                    if (newVal == null) {
                        if (oldValTup != null) {
                            if (oldValTup.get1().equals(dhtVer))
                                newVal = oldValTup.get2();
                        }
                    }

                    if (inTx()) {
                        IgniteTxEntry txEntry = tx.entry(cctx.txKey(k));

                        // In colocated cache we must receive responses only for detached entries.
                        assert txEntry.cached().detached();

                        txEntry.markLocked();

                        GridDhtDetachedCacheEntry entry = (GridDhtDetachedCacheEntry)txEntry.cached();

                        try {
                            if (res.dhtVersion(i) == null) {
                                onDone(new IgniteCheckedException("Failed to receive DHT version from remote node " +
                                    "(will fail the lock): " + res));

                                return;
                            }

                            // Set value to detached entry.
                            entry.resetFromPrimary(newVal, dhtVer);

                            if (log.isDebugEnabled())
                                log.debug("Processed response for entry [res=" + res + ", entry=" + entry + ']');
                        }
                        catch (IgniteCheckedException e) {
                            onDone(e);

                            return;
                        }
                    }
                    else
                        cctx.mvcc().markExplicitOwner(k, threadId);

                    if (retval && cctx.events().isRecordable(EVT_CACHE_OBJECT_READ)) {
                        cctx.events().addEvent(cctx.affinity().partition(k),
                            k,
                            tx,
                            null,
                            EVT_CACHE_OBJECT_READ,
                            newVal,
                            newVal != null,
                            null,
                            false,
                            CU.subjectId(tx, cctx.shared()),
                            null,
                            tx == null ? null : tx.resolveTaskName());
                    }

                    i++;
                }

                try {
                    proceedMapping(mappings);
                }
                catch (IgniteCheckedException e) {
                    onDone(e);
                }

                onDone(true);
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(MiniFuture.class, this, "node", node.id(), "super", super.toString());
        }
    }
}
