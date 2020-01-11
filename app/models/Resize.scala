package models

case class Resize(width: Int, height: Int = 0, quality:Int = 70, watermark: Option[Watermark] = None)

object Resize {
  val regularThumbnail: Resize = Resize(300)
}



