import java.io._
import java.util.zip.GZIPOutputStream

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{ListObjectsV2Request, ListObjectsV2Result, _}
import com.amazonaws.util.{IOUtils, Md5Utils}
import com.amazonaws.{ClientConfiguration, Protocol}
import org.apache.commons.codec.binary.Hex
import sbt.internal.util.ManagedLogger

import scala.collection.mutable



object S3Deploy {
  val mimeTypesToGzip = Map(
    "html" -> "text/html",
    "css" -> "text/css",
    "js"->"text/javascript",
    "htm"->"text/html",
    "svg"->"image/svg+xml",
    ""->"text/html"
  )

  private def makeProxyClientConfiguration(): ClientConfiguration = {
    def doWith(prop: String)(f: String => Unit): Unit = {
      sys.props.get(prop).foreach(f)
    }

    val config = new ClientConfiguration().withProtocol(Protocol.HTTPS)
    doWith("http.proxyHost")(config.setProxyHost)
    doWith("http.proxyPort")(port => config.setProxyPort(port.toInt))
    doWith("http.proxyUser")(config.setProxyUsername)
    doWith("http.proxyPassword")(config.setProxyPassword)
    config
  }


  private def getExtension(name: String): String = {
    var extension = ""
    val i = name.lastIndexOf('.')
    if (i > 0) extension = name.substring(i + 1)
    extension
  }

  def deployDir(dir: File, bucketName: String, gzip:Boolean = true, sbtLog: ManagedLogger):Boolean = {

    val uploadStartTime = System.currentTimeMillis()


    val s3client =  AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain)
      .withClientConfiguration(makeProxyClientConfiguration())
      .build()



    val deployedFiles = mutable.Map[String,String]();
    {
      val req = new ListObjectsV2Request().withBucketName(bucketName)
      var result: ListObjectsV2Result = null
      do {
        result = s3client.listObjectsV2(req)
        import scala.collection.JavaConverters
        for (objectSummary <- JavaConverters.asScalaBuffer(result.getObjectSummaries)) {
          deployedFiles.put(objectSummary.getKey, objectSummary.getETag)
        }
        val token = result.getNextContinuationToken
        req.setContinuationToken(token)
      } while ( {
        result.isTruncated
      })
    }




    val prefixSize = dir.getAbsolutePath.length

    def uploadInner(dirInner:File):Unit = for(file <- dirInner.listFiles()){
      if(file.isDirectory){
        uploadInner(file)
      } else {
        val fileName = file.getAbsolutePath
        val keyName = fileName.drop(prefixSize + 1)

        val extension = getExtension(keyName)
        val meta = new ObjectMetadata()
        mimeTypesToGzip.get(extension).foreach(meta.setContentType)

        if(Settings.neverChange.exists(keyName.startsWith)){
          meta.setCacheControl("public, max-age=31536000")
        } else {
          meta.setCacheControl("public")
        }

        val (uploadRequest,isGz,size) =
        if (gzip && mimeTypesToGzip.contains(extension)) {
          meta.setContentEncoding("gzip")

          val buffer = new ByteArrayOutputStream()
          val gzOs = new GZIPOutputStream(buffer)
          val in = new FileInputStream(file)
          IOUtils.copy(in, gzOs)
          in.close()
          gzOs.finish()
          gzOs.close()
          val ba = buffer.toByteArray
          meta.setContentLength(ba.length)

          val contentMd5 = Hex.encodeHexString(Md5Utils.computeMD5Hash(new ByteArrayInputStream(ba)))

          (if(deployedFiles.get(keyName).contains(contentMd5)) None else Some(new PutObjectRequest(bucketName, keyName, new ByteArrayInputStream(ba), meta)), true, ba.size)
        } else {
          val contentMd5 = Hex.encodeHexString(Md5Utils.computeMD5Hash(file))

          (if(deployedFiles.get(keyName).contains(contentMd5)) None else Some(new PutObjectRequest(bucketName, keyName, file).withMetadata(meta)), false, file.length())
        }
        val actionStr = uploadRequest.fold("not changed")(_=>"uploading")
        sbtLog.info(s"$actionStr gzipped=$isGz to s3  http://$bucketName/$keyName $size B")
        uploadRequest.foreach(s3client.putObject)
        deployedFiles.remove(keyName)
      }
    }

    uploadInner(dir)

    for(k <- deployedFiles.keys){
      sbtLog.info(s"deleting from s3  http://$bucketName/$k")
      s3client.deleteObject(new DeleteObjectRequest(bucketName,k))
    }

    val timeTaken = System.currentTimeMillis() - uploadStartTime
    sbtLog.info(s"Upload to s3 done in $timeTaken ms")

    true
  }
}
