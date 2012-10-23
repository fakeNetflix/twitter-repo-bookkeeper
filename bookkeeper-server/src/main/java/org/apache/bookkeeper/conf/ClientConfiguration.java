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
package org.apache.bookkeeper.conf;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.client.BookKeeper.DigestType;

import org.apache.commons.lang.StringUtils;

/**
 * Configuration settings for client side
 */
public class ClientConfiguration extends AbstractConfiguration {

    // Zookeeper Parameters
    protected final static String ZK_TIMEOUT = "zkTimeout";
    protected final static String ZK_SERVERS = "zkServers";

    // A total of "throttle" permits are available for read and write operations to bookies
    // However, reads can only take up to "readThrottle" permits. Write operations always have
    // at least "throttle - readThrottle" permits available.

    // Throttle value
    protected final static String THROTTLE = "throttle";

    // Read Throttle value. This should be less than or equal to the total throttle value.
    protected final static String READ_THROTTLE = "readThrottle";

    // Digest Type
    protected final static String DIGEST_TYPE = "digestType";
    // Passwd
    protected final static String PASSWD = "passwd";

    // NIO Parameters
    protected final static String CLIENT_TCP_NODELAY = "clientTcpNoDelay";
    protected final static String READ_TIMEOUT = "readTimeout";

    protected final static String TIMEOUT_TASK_INTERVAL_MILLIS = "timeoutTaskIntervalMillis";

    // Number Woker Threads
    protected final static String NUM_WORKER_THREADS = "numWorkerThreads";

    /**
     * Construct a default client-side configuration
     */
    public ClientConfiguration() {
        super();
    }

    /**
     * Construct a client-side configuration using a base configuration
     *
     * @param conf
     *          Base configuration
     */
    public ClientConfiguration(AbstractConfiguration conf) {
        super();
        loadConf(conf);
    }

    /**
     * Get throttle value
     *
     * @return throttle value
     * @see #setThrottleValue
     */
    public int getThrottleValue() {
        return this.getInt(THROTTLE, 5000);
    }

    /**
     * Get the maximum available read permits from the total number of permits
     * returned by getThrottleValue. You can have only as many read operations
     * outstanding as the number of permits provided by this function. Add operations
     * can take up all permits though.
     *
     * The default behavior is to have no special add permits. So, we just return
     * the total throttle value.
     * @return
     */
    public int getReadThrottleValue() {
        return this.getInt(READ_THROTTLE, getThrottleValue());
    }

    /**
     * Set throttle value.
     *
     * Since BookKeeper process requests in asynchrous way, it will holds
     * those pending request in queue. You may easily run it out of memory
     * if producing too many requests than the capability of bookie servers can handle.
     * To prevent that from happeding, you can set a throttle value here.
     *
     * @param throttle
     *          Throttle Value
     * @return client configuration
     */
    public ClientConfiguration setThrottleValue(int throttle) {
        this.setProperty(THROTTLE, Integer.toString(throttle));
        return this;
    }

    /**
     * We don't want potentially slow read operations to take up all permits, thereby blocking
     * the add operations.
     * @param readThrottleValue
     * @return
     */
    public ClientConfiguration setReadThrottleValue(int readThrottleValue) {
        this.setProperty(READ_THROTTLE, Integer.toString(readThrottleValue));
        return this;
    }

    /**
     * Get digest type used in bookkeeper admin
     *
     * @return digest type
     * @see #setBookieRecoveryDigestType
     */
    public DigestType getBookieRecoveryDigestType() {
        return DigestType.valueOf(this.getString(DIGEST_TYPE, DigestType.CRC32.toString()));
    }

    /**
     * Set digest type used in bookkeeper admin.
     *
     * Digest Type and Passwd used to open ledgers for admin tool
     * For now, assume that all ledgers were created with the same DigestType
     * and password. In the future, this admin tool will need to know for each
     * ledger, what was the DigestType and password used to create it before it
     * can open it. These values will come from System properties, though fixed
     * defaults are defined here.
     *
     * @param digestType
     *          Digest Type
     * @return client configuration
     */
    public ClientConfiguration setBookieRecoveryDigestType(DigestType digestType) {
        this.setProperty(DIGEST_TYPE, digestType.toString());
        return this;
    }

    /**
     * Get passwd used in bookkeeper admin
     *
     * @return password
     * @see #setBookieRecoveryPasswd
     */
    public byte[] getBookieRecoveryPasswd() {
        return this.getString(PASSWD, "").getBytes();
    }

    /**
     * Set passwd used in bookkeeper admin.
     *
     * Digest Type and Passwd used to open ledgers for admin tool
     * For now, assume that all ledgers were created with the same DigestType
     * and password. In the future, this admin tool will need to know for each
     * ledger, what was the DigestType and password used to create it before it
     * can open it. These values will come from System properties, though fixed
     * defaults are defined here.
     *
     * @param passwd
     *          Password
     * @return client configuration
     */
    public ClientConfiguration setBookieRecoveryPasswd(byte[] passwd) {
        setProperty(PASSWD, new String(passwd));
        return this;
    }

    /**
     * Is tcp connection no delay.
     *
     * @return tcp socket nodelay setting
     * @see #setClientTcpNoDelay
     */
    public boolean getClientTcpNoDelay() {
        return getBoolean(CLIENT_TCP_NODELAY, true);
    }

    /**
     * Set socket nodelay setting.
     *
     * This settings is used to enabled/disabled Nagle's algorithm, which is a means of
     * improving the efficiency of TCP/IP networks by reducing the number of packets
     * that need to be sent over the network. If you are sending many small messages,
     * such that more than one can fit in a single IP packet, setting client.tcpnodelay
     * to false to enable Nagle algorithm can provide better performance.
     * <br>
     * Default value is true.
     *
     * @param noDelay
     *          NoDelay setting
     * @return client configuration
     */
    public ClientConfiguration setClientTcpNoDelay(boolean noDelay) {
        setProperty(CLIENT_TCP_NODELAY, Boolean.toString(noDelay));
        return this;
    }

    /**
     * Get zookeeper servers to connect
     *
     * @return zookeeper servers
     */
    public String getZkServers() {
        List<Object> servers = getList(ZK_SERVERS, null);
        if (null == servers || 0 == servers.size()) {
            return "localhost";
        }
        return StringUtils.join(servers, ",");
    }

    /**
     * Set zookeeper servers to connect
     *
     * @param zkServers
     *          ZooKeeper servers to connect
     */
    public ClientConfiguration setZkServers(String zkServers) {
        setProperty(ZK_SERVERS, zkServers);
        return this;
    }

    /**
     * Get zookeeper timeout
     *
     * @return zookeeper client timeout
     */
    public int getZkTimeout() {
        return getInt(ZK_TIMEOUT, 10000);
    }

    /**
     * Set zookeeper timeout
     *
     * @param zkTimeout
     *          ZooKeeper client timeout
     * @return client configuration
     */
    public ClientConfiguration setZkTimeout(int zkTimeout) {
        setProperty(ZK_TIMEOUT, Integer.toString(zkTimeout));
        return this;
    }

    /**
     * Get the socket read timeout. This is the number of
     * seconds we wait without hearing a response from a bookie
     * before we consider it failed.
     *
     * The default is 5 seconds.
     *
     * @return the current read timeout in seconds
     */
    public int getReadTimeout() {
        return getInt(READ_TIMEOUT, 5);
    }

    /**
     * Set the socket read timeout.
     * @see #getReadTimeout()
     * @param timeout The new read timeout in seconds
     * @return client configuration
     */
    public ClientConfiguration setReadTimeout(int timeout) {
        setProperty(READ_TIMEOUT, Integer.toString(timeout));
        return this;
    }

    /**
     * Get the interval between successive executions of the PerChannelBookieClient's
     * TimeoutTask. This value is in milliseconds. Every X milliseconds, the timeout task
     * will be executed and it will error out entries that have timed out.
     * @return
     */
    public long getTimeoutTaskIntervalMillis() {
        return getLong(TIMEOUT_TASK_INTERVAL_MILLIS, TimeUnit.SECONDS.toMillis(getReadTimeout()) + 10);
    }

    public ClientConfiguration setTimeoutTaskIntervalMillis(long timeoutMillis) {
        setProperty(TIMEOUT_TASK_INTERVAL_MILLIS, Long.toString(timeoutMillis));
        return this;
    }

    /**
     * Get the number of worker threads. This is the number of
     * worker threads used by bookkeeper client to submit operations.
     *
     * @return the number of worker threads
     */
    public int getNumWorkerThreads() {
        return getInt(NUM_WORKER_THREADS, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Set the number of worker threads.
     *
     * <p>
     * NOTE: setting the number of worker threads after BookKeeper object is constructed
     * will not take any effect on the number of threads in the pool.
     * </p>
     *
     * @see #getNumWorkerThreads()
     * @param numThreads number of worker threads used for bookkeeper
     * @return client configuration
     */
    public ClientConfiguration setNumWorkerThreads(int numThreads) {
        setProperty(NUM_WORKER_THREADS, numThreads);
        return this;
    }
}
