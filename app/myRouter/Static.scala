package myRouter

import java.io.{FileInputStream, FileNotFoundException}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.japi.Option.Some
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import models.{ViewHelpers, Watermark}
import play.api.http.HttpEntity
import play.api.mvc._
import play.api.routing.sird._
import play.api.routing.{Router, SimpleRouter}

import scala.language.reflectiveCalls

class Static(controllerComponents: ControllerComponents) extends SimpleRouter {
  private val log = Logger[Static]
  override def routes: Router.Routes = {
    case GET(a) if a.path.equals("/@kill") =>
      System.exit(0)
      throw new RuntimeException("Could not exit")

    case GET(a) if a.path.endsWith(".html") || a.path == "/" || !a.path.contains('.') =>

      val path = a.path
      val (obj,cType) = if(path=="/"){("/index","html")} else {
        val lastDot = path.lastIndexOf('.')
        if (lastDot == -1) {
          (path, "html")
        } else {
          val obj = path.substring(0, lastDot)
          val cType = path.substring(lastDot + 1)
          (obj,cType)
        }
      }

      import scala.reflect.runtime.universe
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val module = runtimeMirror.staticModule("views."+cType+obj.replace('/','.'))
      val objO = runtimeMirror.reflectModule(module)

      val ins = objO.instance

      val ri = ins.asInstanceOf[{def apply():play.twirl.api.HtmlFormat.Appendable}]

      controllerComponents.actionBuilder{Result(new ResponseHeader(200),HttpEntity.Strict(ByteString.apply(ri.apply().toString(), StandardCharsets.UTF_8), Some("text/"+cType)))}


    case GET(p"${path}*-${int(width)}x${int(height)}x${int(quality)}-${wm}.$ext") if ViewHelpers.imageExtensions.contains(ext.toLowerCase) =>

      val cachePath:String = ViewHelpers.assetsDir+"/generated"+s"$path-${width}x${height}x$quality-$wm.${ext.toLowerCase}"
      val generatedFile = new java.io.File(cachePath)

      val watermark: Option[Watermark] = if(wm=="wm") Some(Watermark.wm) else None

      controllerComponents.actionBuilder {
        if (generatedFile.exists()) {
          Result(new ResponseHeader(200), HttpEntity.Strict(ByteString(Files.readAllBytes(Paths.get(cachePath))), Some("image/" + ext)))
        } else {

          val vf = new java.io.File(ViewHelpers.assetsDir + path + "." + ext)

          if (width == 0 && height == 0 && (quality == 100 || ViewHelpers.imageExtensionsLossCompressedFormats.contains(ext.toLowerCase)) ) {
            Result(new ResponseHeader(200), HttpEntity.Strict(ByteString(Files.readAllBytes(Paths.get(vf.getAbsolutePath))), Some("image/" + ext)))
          } else {

            val img = new javaxt.io.Image(vf)
            if (img.getBufferedImage() == null) {
              Result(new ResponseHeader(404), HttpEntity.Strict(ByteString.empty, Some("text/html")))
            } else {
              img.rotate()

              if(width!=0 || height!=0){

                val originalWidth = img.getWidth
                val originalHeight = img.getHeight

                if(width==0 || height==0) {
                  val resizeWidth = if (width == 0) originalWidth * height / originalHeight else width
                  val resizeHeight = if (height == 0) originalHeight * width / originalWidth else height
                  img.resize(resizeWidth, resizeHeight, true)
                } else {
                  val ratioWidth:Double = (originalWidth + 0.0D) / width
                  val ratioHeight:Double = (originalHeight +0.0D) / height
                  val (cropX, cropY, croppedWidth, croppedHeight) = if(ratioHeight > ratioWidth){
                    val adjOriginalHeight = Math.round(height * ratioWidth).toInt
                    (0, (originalHeight - adjOriginalHeight)/2, originalWidth, adjOriginalHeight)
                  } else {
                    val adjOriginalWidth = Math.round(width * ratioHeight).toInt
                    ((originalWidth - adjOriginalWidth)/2,0,adjOriginalWidth,originalHeight)
                  }
                  img.crop(cropX,cropY,croppedWidth,croppedHeight)
                  img.resize(width, height, true)
                }
              }

              watermark.foreach{ watermark =>
                try {
                  val watermarkImgPath = new java.io.File(ViewHelpers.assetsDir + watermark.imgPath)
                  val wmIs = new FileInputStream(watermarkImgPath)
                  val watermarkImg = new javaxt.io.Image(wmIs).getBufferedImage
                  if (watermarkImg != null) {
                    img.addImage(watermarkImg, watermark.positionX.getX(img.getWidth, watermarkImg.getWidth()), watermark.positionY.getY(img.getHeight, watermarkImg.getHeight()), true)
                  }
                } catch {
                  case _: FileNotFoundException => log.error("Watermark image not found")
                }
              }

              img.setOutputQuality(quality)
              img.saveAs(generatedFile)
              Result(new ResponseHeader(200), HttpEntity.Strict(ByteString(img.getByteArray(ext)), Some("image/" + ext)))
            }
          }
        }
      }
  }

}
