package it.agilelab.darwin.connector.mock

import java.nio.file.Paths
import java.util

import com.typesafe.config.ConfigFactory
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MockConnectorSpec extends AnyFlatSpec with Matchers {

  private val p = Paths.get(".")
    .resolve("mock-connector")
    .resolve("src")
    .resolve("test")
    .resolve("resources")
    .resolve("test")

  it should "load the schema manually inserted" in {
    val connector = new MockConnectorCreator().create(ConfigFactory.empty())
    connector.insert((3L, Schema.create(Type.BYTES)) :: Nil)
    connector.fullLoad() should have size 1
  }

  it should "load the schema automatically from resources" in {
    val connector = new MockConnectorCreator().create(ConfigFactory.parseMap {
      new java.util.HashMap[String, Object] {
        put(ConfigurationKeys.RESOURCES, util.Arrays.asList("test/MockClassAlone.avsc", "test/MockClassParent.avsc"))
      }
    })
    connector.fullLoad() should have size 2
  }

  it should "load the schema automatically from files" in {
    val connector = new MockConnectorCreator().create(ConfigFactory.parseMap {
      new java.util.HashMap[String, Object] {
        put(ConfigurationKeys.FILES, util.Arrays.asList(
          p.resolve("MockClassAlone.avsc").toString,
          p.resolve("MockClassParent.avsc").toString)
        )
      }
    })
    connector.fullLoad() should have size 2
  }

  it should "not throw any exception in case of missing file in permissive mode" in {
    val connector = new MockConnectorCreator().create(ConfigFactory.parseMap {
      new java.util.HashMap[String, Object] {
        put(ConfigurationKeys.FILES, util.Arrays.asList(
          p.resolve("DoesNotExists.avsc").toString,
          p.resolve("MockClassAlone.avsc").toString,
          p.resolve("MockClassParent.avsc").toString)
        )
        put(ConfigurationKeys.MODE, "permissive")
      }
    })
    connector.fullLoad() should have size 2
  }

  it should "throw an exception in case of missing file in strict mode" in {
    intercept[MockConnectorException] {
      new MockConnectorCreator().create(ConfigFactory.parseMap {
        new java.util.HashMap[String, Object] {
          put(ConfigurationKeys.FILES, util.Arrays.asList(
            p.resolve("DoesNotExists.avsc").toString,
            p.resolve("MockClassAlone.avsc").toString,
            p.resolve("MockClassParent.avsc").toString)
          )
        }
      }).fullLoad()
    }
  }

}
