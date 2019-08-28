package it.agilelab.darwin.common

object JavaVersion {

  /**
    * @return the JVM version in use, It returns an Integer indicating the major version i
    */
  def current(): Int = {
    val propertyValue = System.getProperty("java.version")
    parseJavaVersion(propertyValue)
  }

  /**
    * @return the JVM version represented by the input string, It returns an Integer indicating the major version i
    */
  def parseJavaVersion(propertyValue: String): Int = {
    val splits = propertyValue.split("\\.")
    if (propertyValue.startsWith("1.")) {
      splits(1).toInt
    } else {
      splits(0).toInt
    }
  }
}
