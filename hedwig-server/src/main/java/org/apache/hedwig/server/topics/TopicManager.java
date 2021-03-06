/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hedwig.server.topics;

import com.google.protobuf.ByteString;
import org.apache.hedwig.exceptions.PubSubException;
import org.apache.hedwig.server.persistence.PersistenceManager;
import org.apache.hedwig.util.Callback;
import org.apache.hedwig.util.HedwigSocketAddress;

import java.util.List;

/**
 * An implementor of this interface is basically responsible for ensuring that
 * there is at most a single host responsible for a given topic at a given time.
 * Also, it is desirable that on a host failure, some other hosts in the cluster
 * claim responsibilities for the topics that were at the failed host. On
 * claiming responsibility for a topic, a host should call its
 * {@link TopicOwnershipChangeListener}.
 *
 */

public interface TopicManager {
    /**
     * Get the name of the host responsible for the given topic.
     *
     * @param topic
     *            The topic whose owner to get.
     * @param cb
     *            Callback.
     * @return The name of host responsible for the given topic
     * @throws ServiceDownException
     *             If there is an error looking up the information
     */
    public void getOwner(ByteString topic, boolean shouldClaim,
                         Callback<HedwigSocketAddress> cb, Object ctx);

    /**
     * Whenever the topic manager finds out that the set of topics owned by this
     * node has changed, it can notify a set of
     * {@link TopicOwnershipChangeListener} objects. Any component of the system
     * (e.g., the {@link PersistenceManager}) can listen for such changes by
     * implementing the {@link TopicOwnershipChangeListener} interface and
     * registering themselves with the {@link TopicManager} using this method.
     * It is important that the {@link TopicOwnershipChangeListener} reacts
     * immediately to such notifications, and with no blocking (because multiple
     * listeners might need to be informed and they are all informed by the same
     * thread).
     *
     * @param listener
     */
    public void addTopicOwnershipChangeListener(TopicOwnershipChangeListener listener);

    /**
     * Give up ownership of a topic. If I don't own it, do nothing.
     *
     * @throws ServiceDownException
     *             If there is an error in claiming responsibility for the topic
     */
    public void releaseTopic(ByteString topic, Callback<Void> cb, Object ctx);

    /**
     * Release numTopics topics. If you hold fewer, release all.
     * @param numTopics
     *          Number of topics to release.
     * @param callback
     *          The callback should be invoked with the number of topics the hub
     *          released successfully.
     * @param ctx
     */
    public void releaseTopics(int numTopics, Callback<Long> callback, Object ctx);

    /**
     * Check if the topic has been subscribed from the region, callback finished if so,
     * failed otherwise.
     * @param topic
     *          The topic.
     * @param regionAddress
     *          The region VIP address representation
     * @param cb
     *          Callback notification
     * @param ctx
     *          Callback context
     * @param exception
     *          The exception to be passed along if failure
     */
    public void checkTopicSubscribedFromRegion(final ByteString topic, final String regionAddress,
                                               final Callback<Void> cb, final Object ctx,
                                               final PubSubException exception);

    /**
     * Remember the fact that the topic is unsubscribed from the region, callback finished
     * if successful, failed otherwise.
     * @param topic
     *          The topic.
     * @param regionAddress
     *          The region VIP address representation
     * @param cb
     *          Callback notification
     * @param ctx
     *          Callback context
     */
    public void setTopicUnsubscribedFromRegion(final ByteString topic, final String regionAddress,
                                               final Callback<Void> cb, final Object ctx);

    /**
     * Remember the fact that the topic has been subscribed from the region, callback finished
     * if successful, failed otherwise.
     * @param topic
     *          The topic.
     * @param regionAddress
     *          The region VIP address representation
     * @param cb
     *          Callback notification
     * @param ctx
     *          Callback context
     */
    public void setTopicSubscribedFromRegion(final ByteString topic, final String regionAddress,
                                                final Callback<Void> cb, final Object ctx);

    /*
     * Get the current number of topics the hub believes it owns
     * @return
     */
    public long getNumTopics();

  /**
     * Stop topic manager
     */
    public void stop();

}
