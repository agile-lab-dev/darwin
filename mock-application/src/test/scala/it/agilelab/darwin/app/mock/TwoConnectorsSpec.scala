package it.agilelab.darwin.app.mock

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.ConnectorFactory
import it.agilelab.darwin.connector.hbase.HBaseConnectorCreator
import it.agilelab.darwin.connector.mock.MockConnectorCreator
import it.agilelab.darwin.connector.postgres.PostgresConnectorCreator
import it.agilelab.darwin.manager.util.ConfigurationKeys
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TwoConnectorsSpec extends AnyFlatSpec with Matchers {
  it should "have both HBase and Postgresql available" in {
    ConnectorFactory.creators().map(_.getClass) should contain theSameElementsAs (
      classOf[HBaseConnectorCreator] :: classOf[PostgresConnectorCreator] :: classOf[MockConnectorCreator] :: Nil
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
