package models

trait PositionX {
  def getX(imgWidth: Int, watermarkWidth: Int): Int
}

trait PositionY {
  def getY(imgHeight: Int, watermarkHeight: Int): Int
}

object Top extends PositionY {
  override def getY(imgHeight: Int, watermarkHeight: Int): Int = 0
}

object Bottom extends PositionY {
  override def getY(imgHeight: Int, watermarkHeight: Int): Int = imgHeight - watermarkHeight
}

object Middle extends PositionY {
  override def getY(imgHeight: Int, watermarkHeight: Int): Int = (imgHeight - watermarkHeight) / 2
}

object Left extends PositionX {
  override def getX(imgWidth: Int, watermarkWidth: Int): Int = 0
}

object Right extends PositionX {
  override def getX(imgWidth: Int, watermarkWidth: Int): Int = imgWidth - watermarkWidth
}

object Center extends PositionX {
  override def getX(imgWidth: Int, watermarkWidth: Int): Int = (imgWidth - watermarkWidth) / 2
}

case class Watermark(imgPath: String, positionX: PositionX, positionY: PositionY)

object Watermark {
  val wm: Watermark = Watermark("img/wm.png", Right, Bottom)
}