package misc

import models.FontAwesomeIcon
import models.ViewHelpers._
import org.apache.commons.lang3.StringUtils
import org.scalatest.FunSuite

class ViewHelpersTests  extends  FunSuite {

  test("html"){
    assert(html("views/index3.scala.html") == "index3.html")
  }

  test("FontAwesomeIcon toString"){
    assert(FontAwesomeIcon.AddressBook.toString=="address-book")
  }

  test("FA icon names"){
    val  d = new java.io.File(assetsDir + "/svg/font-awesome/raw-svg/"+"brands")
    val ff = d.listFiles()
    val rr = ff.map{ f =>
      val nWExt = f.getName
      val n = nWExt.substring(0, nWExt.indexOf('.'))
      n.split("-").map(StringUtils.capitalize).mkString
    }.sorted.mkString(", ")
    assert(rr.length == 3981)
  }

}
