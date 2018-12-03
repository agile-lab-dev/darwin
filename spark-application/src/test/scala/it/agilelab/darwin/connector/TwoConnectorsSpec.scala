package it.agilelab.darwin.connector

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.ConnectorFactory
import it.agilelab.darwin.connector.hbase.HBaseConnectorCreator
import it.agilelab.darwin.connector.postgres.PostgresConnectorCreator
import it.agilelab.darwin.manager.util.ConfigurationKeys
import org.scalatest.{FlatSpec, Matchers}

class TwoConnectorsSpec extends FlatSpec with Matchers {
  it should "have both HBase and Postgresql available" in {
    ConnectorFactory.creators().map(_.getClass) should contain theSameElementsAs (
      classOf[HBaseConnectorCreator] :: classOf[PostgresConnectorCreator] :: Nil
      )
  }

  it should "choose HBase connector over Postgresql one" in {
    val config = ConfigFactory.parseString(s"""${ConfigurationKeys.CONNECTOR}: hbase""")
    ConnectorFactory.creator(config).map(_.getClass) should be(Some(classOf[HBaseConnectorCreator]))
  }

  it should "choose Postgresql connector over HBase one" in {
    val config = ConfigFactory.parseString(s"""${ConfigurationKeys.CONNECTOR}: postgresql""")
    ConnectorFactory.creator(config).map(_.getClass) should be(Some(classOf[PostgresConnectorCreator]))
  }

}
