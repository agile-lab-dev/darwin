package it.agilelab.darwin.connector.mock

import com.typesafe.config.ConfigFactory
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MockConnectorSpec extends AnyFlatSpec with Matchers {

  it should "load the test schemas" in {
    new MockConnectorCreator().create(ConfigFactory.empty()).fullLoad() should have size (2)
  }

  it should "load the test schemas and custom ones" in {
    val connector = new MockConnectorCreator().create(ConfigFactory.empty())
    connector.insert((3L, Schema.create(Type.BYTES)) :: Nil)
    connector.fullLoad() should have size (3)
  }

}
