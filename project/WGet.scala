import java.io.File

import play.sbt.PlayRunHook
import sbt.internal.util.ManagedLogger

import scala.util.matching.Regex

object WGet {
    def apply(allSourceFiles: Seq[File],targetDir: File, bucket: String, gzip: Boolean, sbtLog: ManagedLogger): PlayRunHook = {

      object WGetProcess extends PlayRunHook {
        override def afterStarted: Unit = {

          if (targetDir.exists()) {
            def delDir(file: File):Unit = {
              if(file.isDirectory) for(f<-file.listFiles()) delDir(f)
              file.delete()
            }
            delDir(targetDir)
          }

          targetDir.mkdir()

          val twirlTemplate: Regex = ".*/twirl/main/views/html/([^_].*).template.scala".r

          val urlsToDownload: Seq[String] = allSourceFiles.map(_.getPath).collect { case twirlTemplate(name) => s"http://localhost:9000/$name" }

          val wGetProcessLogger = sbt.util.Logger.absLog2PLog(new sbt.internal.util.FullLogger(sbtLog){
            override def log(level: sbt.util.Level.Value, message: => String): Unit = {
              val forceLevel = sbt.util.Level.Info
              if (atLevel(forceLevel))
                sbtLog.log(forceLevel, message)
            }
          })

          scala.sys.process.Process(s"wget -r -p -nH -nv  ${urlsToDownload.mkString(" ")}", targetDir).!(wGetProcessLogger)//-e robots=off
          // number of files limited by system run command line mac/unix: getconf ARG_MAX, windows: 8191

          try {
            S3Deploy.deployDir(targetDir, bucket, gzip, sbtLog)
          } catch {
            case e:Exception => sbtLog.error("Could not deploy to S3 "+e.getLocalizedMessage)
          }

        }

      }

      WGetProcess
    }
}
