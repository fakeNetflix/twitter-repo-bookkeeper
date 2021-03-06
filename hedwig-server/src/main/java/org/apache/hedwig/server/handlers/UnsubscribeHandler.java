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
package org.apache.hedwig.server.handlers;

import org.apache.bookkeeper.stats.OpStatsLogger;
import org.apache.hedwig.server.delivery.ChannelEndPoint;
import org.apache.hedwig.server.stats.ServerStatsProvider;
import org.jboss.netty.channel.Channel;
import com.google.protobuf.ByteString;

import org.apache.bookkeeper.util.MathUtils;
import org.apache.hedwig.client.data.TopicSubscriber;
import org.apache.hedwig.exceptions.PubSubException;
import org.apache.hedwig.protocol.PubSubProtocol.OperationType;
import org.apache.hedwig.protocol.PubSubProtocol.PubSubRequest;
import org.apache.hedwig.protocol.PubSubProtocol.SubscriptionEvent;
import org.apache.hedwig.protocol.PubSubProtocol.UnsubscribeRequest;
import org.apache.hedwig.protoextensions.PubSubResponseUtils;
import org.apache.hedwig.server.common.ServerConfiguration;
import org.apache.hedwig.server.delivery.DeliveryManager;
import org.apache.hedwig.server.netty.UmbrellaHandler;
import org.apache.hedwig.server.subscriptions.SubscriptionManager;
import org.apache.hedwig.server.topics.TopicManager;
import org.apache.hedwig.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hedwig.util.VarArgs.va;

public class UnsubscribeHandler extends BaseHandler {
    final static Logger logger = LoggerFactory.getLogger(UnsubscribeHandler.class);
    SubscriptionManager subMgr;
    DeliveryManager deliveryMgr;
    SubscriptionChannelManager subChannelMgr;
    // op stats
    final OpStatsLogger unsubStatsLogger;

    public UnsubscribeHandler(ServerConfiguration cfg,
                              TopicManager tm,
                              SubscriptionManager subMgr,
                              DeliveryManager deliveryMgr,
                              SubscriptionChannelManager subChannelMgr) {
        super(tm, cfg);
        this.subMgr = subMgr;
        this.deliveryMgr = deliveryMgr;
        this.subChannelMgr = subChannelMgr;
        unsubStatsLogger = ServerStatsProvider.getStatsLoggerInstance().getOpStatsLogger(OperationType.UNSUBSCRIBE);
    }

    @Override
    public void handleRequestAtOwner(final PubSubRequest request, final Channel channel) {
        final long requestTimeNanos = MathUtils.nowInNano();
        if (!request.hasUnsubscribeRequest()) {
            logger.error("Received a request: {} on channel: {} without a Unsubscribe request.",
                    request, channel);
            UmbrellaHandler.sendErrorResponseToMalformedRequest(channel, request.getTxnId(),
                    "Missing unsubscribe request data");
            unsubStatsLogger.registerFailedEvent(MathUtils.elapsedMicroSec(requestTimeNanos));
            return;
        }

        final UnsubscribeRequest unsubRequest = request.getUnsubscribeRequest();
        final ByteString topic = request.getTopic();
        final ByteString subscriberId = unsubRequest.getSubscriberId();

        logger.info("Received unsubscribe request: {} on channel: {}.", request, channel);
        subMgr.unsubscribe(topic, subscriberId, new Callback<Void>() {
            @Override
            public void operationFailed(Object ctx, PubSubException exception) {
                logger.error("Unsubscribe request: {} on channel: {} failed.", request, channel);
                channel.write(PubSubResponseUtils.getResponseForException(exception, request.getTxnId()));
                unsubStatsLogger.registerFailedEvent(MathUtils.elapsedMicroSec(requestTimeNanos));
            }

            @Override
            public void operationFinished(Object ctx, Void resultOfOperation) {
                logger.info("Unsubscribe request: {} on channel: {} succeeded. Issuing a stop delivery" +
                        " request to the delivery manager.", request, channel);
                // we should not close the channel in delivery manager
                // since client waits the response for closeSubscription request
                // client side would close the channel
                deliveryMgr.stopServingSubscriber(topic, subscriberId, null, new ChannelEndPoint(channel),
                new Callback<Void>() {
                    @Override
                    public void operationFailed(Object ctx, PubSubException exception) {
                        logger.error("Failed to stop delivery for unsubscribe request: {} on channel: {}",
                                request, channel);
                        channel.write(PubSubResponseUtils.getResponseForException(exception, request.getTxnId()));
                        unsubStatsLogger.registerFailedEvent(MathUtils.elapsedMicroSec(requestTimeNanos));
                    }
                    @Override
                    public void operationFinished(Object ctx, Void resultOfOperation) {
                        // remove the topic subscription from subscription channels
                        logger.info("Stopped delivery. Unsubscribe request: {} on channel: {} is successful.",
                                request, channel);
                        subChannelMgr.remove(new TopicSubscriber(topic, subscriberId),
                                             channel);
                        channel.write(PubSubResponseUtils.getSuccessResponse(request.getTxnId()));
                        unsubStatsLogger.registerSuccessfulEvent(MathUtils.elapsedMicroSec(requestTimeNanos));
                    }
                }, ctx);
            }
        }, null);

    }

}
