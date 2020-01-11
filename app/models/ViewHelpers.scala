package models

import java.io.{FileInputStream, FileNotFoundException}

import controllers.routes
import play.api.mvc.Call
import play.twirl.api.Html

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Using}

object ViewHelpers {

  def public(f: String): Call = routes.Assets.versioned(f)

  def css(f:String): Call = public(f)

  def js(f:String): Call = public(f)

  def html(f:String): String = {
    f.replaceAll("^views/(.*)\\.scala\\.html$","$1")
  }

  def img(name: String, resize: Resize): String = img(name, resize.width, resize.height, resize.quality, resize.watermark.isDefined)

  def img(name: String, width: Int = 0, height: Int = 0:Int, quality:Int = 70, watermark: Boolean = false): String = {
    val lastDotPosition = name.lastIndexOf('.')
    val nameWithoutExt = if(lastDotPosition == -1) name else name.substring(0,lastDotPosition)
    val ext = if(lastDotPosition == -1) "" else name.substring(lastDotPosition+1)
    val prefix = if(nameWithoutExt.headOption.contains('/')) "" else "/"
    val wm = if(watermark) "wm" else ""
    s"$prefix$nameWithoutExt-${width}x${height}x$quality-$wm.$ext"
  }

  def img(name: String): String = name

  val assetsDir: String = {
    import scala.jdk.CollectionConverters._
    val allViews = ViewHelpers.getClass.getClassLoader.getResources("myRouter")
    val vv = allViews.asScala.to(LazyList).head.getFile
    vv+"/../../../web/public/main/"
  }

  val imageExtensions:List[String] = List("jpg","jpeg","png","gif")

  val imageExtensionsLossCompressedFormats:List[String] = List("jpg","jpeg")

  def imgList(path: String, resize: Resize):Seq[(String,String,String)] = imgList(path, resize.width, resize.height, resize.quality)

  def imgList(path: String, width:Int = 0, height: Int = 0, quality:Int = 70, watermark: Boolean = false):Seq[(String,String,String)] = {
    val vf = new java.io.File(assetsDir+path)


    try {
      val titlesList = new mutable.ListBuffer[(String,String)]()

      val titlesFile = new FileInputStream(assetsDir+path+"/titles.yml")
      val titlesSource = Source.fromInputStream(titlesFile)
      for(l<-titlesSource.getLines()){
        val sepPos = l.indexOf(':')
        if(sepPos>0){
          titlesList.append( (l.take(sepPos), l.drop(sepPos+1)) )
        }
      }
      titlesFile.close()
      titlesList.map {
        case (name, title) =>
          (
            img(path+"/"+name, width, height, quality, watermark),
            title,
            img(path+"/"+name, 0, 0, quality, watermark)
          )
      }.toSeq
    } catch {
      case _: FileNotFoundException =>
        val fileNames: Array[(String, String, String)] =
          vf.listFiles().collect {
            case f if f.isFile && imageExtensions.exists(f.getName.toLowerCase.endsWith(_)) =>
              (
                img(f.getAbsolutePath.substring(assetsDir.length), width, height, quality, watermark),
                img(f.getAbsolutePath.substring(assetsDir.length),0,0,quality,watermark)
              )
          }
          .sortBy(_._1).map {
            case (x,y) => (x, "", y)
          } //Empty titles
        fileNames.toIndexedSeq
    }
  }

  private val fontAwesomeFilesCache = TrieMap[String,String]()

  def faSvg(icon: FontAwesomeIcon, fill: FontAwesomeIcon.Fill = FontAwesomeIcon.Fill.Regular, cssClass:String=""):Html =
    faSvgInner(icon.toString,fill.toString,cssClass)

  def faSvg(icon: FontAwesomeIconBrand):Html = faSvgInner(icon.toString,"brands","")

  def faSvg(icon: FontAwesomeIconBrand, cssClass:String):Html = faSvgInner(icon.toString,"brands",cssClass)

  def faSvg(icon:String):Html = {
    val ii = icon.toLowerCase.split(" ")
    if(ii.isEmpty){
      Html("")
    } else {
      val typePart:PartialFunction[String, String] = {
        case "far" => "regular"
        case "fal" => "light"
        case "fas" => "solid"
        case "fab" => "brands"
      }
      val fill = ii.collectFirst(typePart).getOrElse("regular")
      val iconsSet = ( if(fill=="brands") FontAwesomeIconBrand.values() else FontAwesomeIcon.values() ).to(LazyList).map(_.toString)
      val iconResOption = ii.find(x => x.startsWith("fa-") && iconsSet.contains(x.substring(3)))
      iconResOption.fold(Html("")){x =>
        val cssClass = ii.filter(y => !typePart.isDefinedAt(y) && x!=y).mkString(" ")
        faSvgInner(x.substring(3), fill,cssClass)
      }
    }
  }


  private def faSvgInner(icon:String, fill:String, cssClass:String):Html = {
    val name = fill + "/" + icon
    val res:String = fontAwesomeFilesCache.get(name).fold{
      val vf = new java.io.File(assetsDir + "/svg/font-awesome/raw-svg/" + name + ".svg")
      Using(Source.fromFile(vf)(Codec.UTF8))(_.mkString) match {
        case Success(s) =>
          val r = s.replace("<path ", """<path fill="currentColor" """)
          val r2 = r.replace("<svg ", s"""<svg class="svg-inline--fa fa-$icon $cssClass" """)
          fontAwesomeFilesCache.putIfAbsent(name, r2)
          r2
        case Failure(exception) => throw exception
      }

    }(identity)

    Html(res)
  }


}
