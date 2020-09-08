package app


import bean.AdsInfo
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import util.MyKafkaUtils


trait App {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[*]").setAppName("App")
    val ssc = new StreamingContext(conf, Seconds(3))
    ssc.checkpoint("ck1602")
    val sourceStream = MyKafkaUtils.getkafkaStream(ssc, "ads_log1603")


    val adsInfoStream: DStream[AdsInfo] = sourceStream.map(s => {
      val split = s.split(",")
      AdsInfo(split(0).toLong, split(1), split(2), split(3), split(4))
    })

    doSomething(adsInfoStream)
    ssc.start()
    ssc.awaitTermination()
  }
  def doSomething(adsInfoStream: DStream[AdsInfo]):Unit


}



