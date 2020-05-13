package it.agilelab.darwin.connector.dynamo

import java.net.InetAddress

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.{ClientConfiguration, ClientConfigurationFactory, Protocol}
import com.typesafe.config.{Config, ConfigValue}
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.manager.util.ConfigUtil._
import ConfigurationKeys._

case class DynamoConnectorConfig(
                                  awsAccessKeyId: Option[String],
                                  awsSecretAccessKey: Option[String],
                                  endpoint: Option[String],
                                  region: Option[String],
                                  enableEndpointDiscovery: Option[Boolean],
                                  connectionTimeout: Option[Int],
                                  maxConnections: Option[Int],
                                  maxErrorRetry: Option[Int],
                                  useThrottleRetries: Option[Boolean],
                                  localAddress: Option[InetAddress],
                                  protocol: Option[Protocol],
                                  proxyProtocol: Option[Protocol],
                                  proxyDomain: Option[String],
                                  proxyHost: Option[String],
                                  proxyPassword: Option[String],
                                  proxyPort: Option[Int],
                                  proxyUsername: Option[String],
                                  proxyWorkstation: Option[String],
                                  nonProxyHosts: Option[String],
                                  disableSocketProxy: Option[Boolean],
                                  preemptiveBasicProxyAuth: Option[Boolean],
                                  socketTimeout: Option[Int],
                                  requestTimeout: Option[Int],
                                  clientExecutionTimeout: Option[Int],
                                  userAgentPrefix: Option[String],
                                  userAgentSuffix: Option[String],
                                  useReaper: Option[Boolean],
                                  useGzip: Option[Boolean],
                                  socketSendBufferSizeHint: Option[Int],
                                  socketReceiveBufferSizeHint: Option[Int],
                                  signerOverride: Option[String],
                                  responseMetadataCacheSize: Option[Int],
                                  useExpectContinue: Option[Boolean],
                                  cacheResponseMetadata: Option[Boolean],
                                  connectionTTL: Option[Long],
                                  connectionMaxIdleMillis: Option[Long],
                                  validateAfterInactivityMillis: Option[Int],
                                  useTcpKeepAlive: Option[Boolean],
                                  maxConsecutiveRetriesBeforeThrottling: Option[Int],
                                  disableHostPrefixInjection: Option[Boolean],
                                  headers: Map[String, String]
                                ) {
  def endpointConfiguration: Option[EndpointConfiguration] = for {
    endpoint <- this.endpoint
    region <- this.region
  } yield new EndpointConfiguration(endpoint, region)

  def credentials: Option[AWSStaticCredentialsProvider] = {
    for {
      accessKeyId <- awsAccessKeyId
      secretAccessKey <- awsSecretAccessKey
    } yield new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey))
  }

  def clientConfiguration: ClientConfiguration = {
    val c = new ClientConfigurationFactory().getConfig
    this.connectionTimeout.foreach(c.setConnectionTimeout)
    this.maxConnections.foreach(c.setMaxConnections)
    this.maxErrorRetry.foreach(c.setMaxErrorRetry)
    this.useThrottleRetries.foreach(c.setUseThrottleRetries)
    this.localAddress.foreach(c.setLocalAddress)
    this.protocol.foreach(c.setProtocol)
    this.proxyProtocol.foreach(c.setProxyProtocol)
    this.proxyDomain.foreach(c.setProxyDomain)
    this.proxyHost.foreach(c.setProxyHost)
    this.proxyPassword.foreach(c.setProxyPassword)
    this.proxyPort.foreach(c.setProxyPort)
    this.proxyUsername.foreach(c.setProxyUsername)
    this.proxyWorkstation.foreach(c.setProxyWorkstation)
    this.nonProxyHosts.foreach(c.setNonProxyHosts)
    this.disableSocketProxy.foreach(c.setDisableSocketProxy)
    this.preemptiveBasicProxyAuth.foreach(c.setPreemptiveBasicProxyAuth(_)) // boxed booleans <3
    this.socketTimeout.foreach(c.setSocketTimeout)
    this.requestTimeout.foreach(c.setRequestTimeout)
    this.clientExecutionTimeout.foreach(c.setClientExecutionTimeout)
    this.userAgentPrefix.foreach(c.setUserAgentPrefix)
    this.userAgentSuffix.foreach(c.setUserAgentSuffix)
    this.useReaper.foreach(c.setUseReaper)
    this.useGzip.foreach(c.setUseGzip)
    for {
      sendHint <- this.socketSendBufferSizeHint
      recHint <- this.socketReceiveBufferSizeHint
    } c.setSocketBufferSizeHints(sendHint, recHint)
    this.signerOverride.foreach(c.setSignerOverride)
    this.responseMetadataCacheSize.foreach(c.setResponseMetadataCacheSize)
    this.useExpectContinue.foreach(c.setUseExpectContinue)
    this.cacheResponseMetadata.foreach(c.setCacheResponseMetadata)
    this.connectionTTL.foreach(c.setConnectionTTL)
    this.connectionMaxIdleMillis.foreach(c.setConnectionMaxIdleMillis)
    this.validateAfterInactivityMillis.foreach(c.setValidateAfterInactivityMillis)
    this.useTcpKeepAlive.foreach(c.setUseTcpKeepAlive)
    this.maxConsecutiveRetriesBeforeThrottling.foreach(c.setMaxConsecutiveRetriesBeforeThrottling)
    this.disableHostPrefixInjection.foreach(c.setDisableHostPrefixInjection)
    this.headers.foreach { case (k, v) =>
      c.addHeader(k, v)
    }
    c
  }
}

object DynamoConnectorConfig {
  def ofConf(config: Config): DynamoConnectorConfig = {
    DynamoConnectorConfig(
      awsAccessKeyId = getOptString(config, AWS_ACCESS_KEY_ID),
      awsSecretAccessKey = getOptString(config, AWS_SECRET_ACCESS_KEY),
      endpoint = getOptString(config, ENDPOINT),
      region = getOptString(config, REGION),
      enableEndpointDiscovery = getOptBoolean(config, ENABLE_ENDPOINT_DISCOVERY),
      connectionTimeout = getOptInt(config, CONNECTION_TIMEOUT),
      maxConnections = getOptInt(config, MAX_CONNECTIONS),
      maxErrorRetry = getOptInt(config, MAX_ERROR_RETRY),
      useThrottleRetries = getOptBoolean(config, USE_THROTTLE_RETRIES),
      localAddress = getOptString(config, LOCAL_ADDRESS).map(InetAddress.getByName),
      protocol = getOptString(config, PROTOCOL).map(Protocol.valueOf),
      proxyProtocol = getOptString(config, PROXY_PROTOCOL).map(Protocol.valueOf),
      proxyDomain = getOptString(config, PROXY_DOMAIN),
      proxyHost = getOptString(config, PROXY_HOST),
      proxyPassword = getOptString(config, PROXY_PASSWORD),
      proxyPort = getOptInt(config, PROXY_PORT),
      proxyUsername = getOptString(config, PROXY_USERNAME),
      proxyWorkstation = getOptString(config, PROXY_WORKSTATION),
      nonProxyHosts = getOptString(config, NON_PROXY_HOSTS),
      disableSocketProxy = getOptBoolean(config, DISABLE_SOCKET_PROXY),
      preemptiveBasicProxyAuth = getOptBoolean(config, PREEMPTIVE_BASIC_PROXY_AUTH),
      socketTimeout = getOptInt(config, SOCKET_TIMEOUT),
      requestTimeout = getOptInt(config, REQUEST_TIMEOUT),
      clientExecutionTimeout = getOptInt(config, CLIENT_EXECUTION_TIMEOUT),
      userAgentPrefix = getOptString(config, USER_AGENT_PREFIX),
      userAgentSuffix = getOptString(config, USER_AGENT_SUFFIX),
      useReaper = getOptBoolean(config, USE_REAPER),
      useGzip = getOptBoolean(config, USE_GZIP),
      socketSendBufferSizeHint = getOptInt(config, SOCKET_SEND_BUFFER_SIZE_HINT),
      socketReceiveBufferSizeHint = getOptInt(config, SOCKET_RECEIVE_BUFFER_SIZE_HINT),
      signerOverride = getOptString(config, SIGNER_OVERRIDE),
      responseMetadataCacheSize = getOptInt(config, RESPONSE_METADATA_CACHE_SIZE),
      useExpectContinue = getOptBoolean(config, USE_EXPECT_CONTINUE),
      cacheResponseMetadata = getOptBoolean(config, CACHE_RESPONSE_METADATA),
      connectionTTL = getOptLong(config, CONNECTION_TTL),
      connectionMaxIdleMillis = getOptLong(config, CONNECTION_MAX_IDLE_MILLIS),
      validateAfterInactivityMillis = getOptInt(config, VALIDATE_AFTER_INACTIVITY_MILLIS),
      useTcpKeepAlive = getOptBoolean(config, USE_TCP_KEEP_ALIVE),
      maxConsecutiveRetriesBeforeThrottling = getOptInt(config, MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING),
      disableHostPrefixInjection = getOptBoolean(config, DISABLE_HOST_PREFIX_INJECTION),
      headers = getOptConfig(config, HEADERS).map {
        _.root().entrySet().toScala().map {
          entry: java.util.Map.Entry[String, ConfigValue] =>
            entry.getKey -> entry.getValue.unwrapped().toString
        }.toMap
      }.getOrElse(Map.empty))
  }

  def toDynamoClientBuilder(conf: DynamoConnectorConfig): AmazonDynamoDBClientBuilder = {
    val builder = AmazonDynamoDBClientBuilder.standard()

    conf.credentials.foreach(builder.setCredentials)
    conf.endpointConfiguration.foreach(builder.setEndpointConfiguration)
    conf.region.foreach(builder.setRegion)
    conf.enableEndpointDiscovery.foreach {
      case true => builder.enableEndpointDiscovery()
      case false => builder.disableEndpointDiscovery()
    }
    builder.setClientConfiguration(conf.clientConfiguration)
    builder
  }
}

object ConfigurationKeys {
  val AWS_ACCESS_KEY_ID = "awsAccessKeyId"
  val AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey"
  val ENDPOINT = "endpoint"
  val REGION = "region"
  val ENABLE_ENDPOINT_DISCOVERY = "enableEndpointDiscovery"
  val CONNECTION_TIMEOUT = "connectionTimeout"
  val MAX_CONNECTIONS = "maxConnections"
  val MAX_ERROR_RETRY = "maxErrorRetry"
  val USE_THROTTLE_RETRIES = "useThrottleRetries"
  val LOCAL_ADDRESS = "localAddress"
  val PROTOCOL = "protocol"
  val PROXY_PROTOCOL = "proxyProtocol"
  val PROXY_DOMAIN = "proxyDomain"
  val PROXY_HOST = "proxyHost"
  val PROXY_PASSWORD = "proxyPassword"
  val PROXY_PORT = "proxyPort"
  val PROXY_USERNAME = "proxyUsername"
  val PROXY_WORKSTATION = "proxyWorkstation"
  val NON_PROXY_HOSTS = "nonProxyHosts"
  val DISABLE_SOCKET_PROXY = "disableSocketProxy"
  val PREEMPTIVE_BASIC_PROXY_AUTH = "preemptiveBasicProxyAuth"
  val SOCKET_TIMEOUT = "socketTimeout"
  val REQUEST_TIMEOUT = "requestTimeout"
  val CLIENT_EXECUTION_TIMEOUT = "clientExecutionTimeout"
  val USER_AGENT_PREFIX = "userAgentPrefix"
  val USER_AGENT_SUFFIX = "userAgentSuffix"
  val USE_REAPER = "useReaper"
  val USE_GZIP = "useGzip"
  val SOCKET_SEND_BUFFER_SIZE_HINT = "socketSendBufferSizeHint"
  val SOCKET_RECEIVE_BUFFER_SIZE_HINT = "socketReceiveBufferSizeHint"
  val SIGNER_OVERRIDE = "signerOverride"
  val RESPONSE_METADATA_CACHE_SIZE = "responseMetadataCacheSize"
  val USE_EXPECT_CONTINUE = "useExpectContinue"
  val CACHE_RESPONSE_METADATA = "cacheResponseMetadata"
  val CONNECTION_TTL = "connectionTTL"
  val CONNECTION_MAX_IDLE_MILLIS = "connectionMaxIdleMillis"
  val VALIDATE_AFTER_INACTIVITY_MILLIS = "validateAfterInactivityMillis"
  val USE_TCP_KEEP_ALIVE = "useTcpKeepAlive"
  val MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING = "maxConsecutiveRetriesBeforeThrottling"
  val DISABLE_HOST_PREFIX_INJECTION = "disableHostPrefixInjection"
  val HEADERS = "headers"
}
