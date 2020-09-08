package app
import bean.AdsInfo
import org.apache.spark.streaming.{Minutes, Seconds}
import org.apache.spark.streaming.dstream.DStream
import org.json4s.jackson.JsonMethods
import util.RedisUtil

object LastHourApp extends App {
  override def doSomething(adsInfoStream: DStream[AdsInfo]): Unit = {
    adsInfoStream
      //1.把窗口分好
      .window(Minutes(60),Seconds(3))
    //2.按照广告分组，进行聚合
      .map(info=>((info.adsId,info.hmString),1))
      .reduceByKey(_+_)
    //((4,10:14),100)
     // .print(1000)
    //3.按照广告分组，把这个广告下所有的分钟记录在一起
      .map{
        case ((ads,hm),count)=>(ads,(hm,count))
      }
      .groupByKey()
    //.print(10000)
    //4。写入到redis
      .foreachRDD(rdd=>{
        rdd.foreachPartition((it: Iterator[(String, Iterable[(String, Int)])]) => {
          if (it.nonEmpty){   //只是判断是否有下一个元素，指针不会跳过这个元素
            //先建立到redis连接
            val client = RedisUtil.getClient
            //2.写元素到redis
            //2.1 一个一个的写(需求1用)
            //2.2 批次写入(需求二用)
            import org.json4s.JsonDSL._
            val key = "last:ads:hour:count"
            val map = it.toMap.map{
              case (adsId,it) => (adsId,JsonMethods.compact(JsonMethods.render(it)))
            }
            //sacla集合转换成java集合
            import scala.collection.JavaConversions._
            println(map)
            client.hmset(key,map)

            //3.关闭redis (用的是连接池，实际上是把连接归还给连接池)
            client.close()
          }
        })
      })

  }
}
/*
统计各广告最近1个小时内的点击量趋势：各广告最近1个小时内分钟的点击量，每6秒统计一次
1.各广告      ->          按照广告分钟
2.最近1个小时，每6秒统计一次      ->     窗口：窗口长度1个小时   窗口的滑动步长5s
-------------

1.先把窗口分好
2.按照广告分组，进行聚合
3.按照广告分组，把这个广告下所有的分钟记录在一起


//写到redis的时候数据类型
1.
        key                           value
        广告id                        json字符传每分钟的点击
2.
        key                           value
        "last:ads:hour:count"         hash
                                      field      value
                                      adisId     json字符串
                                      "1"        {"09:24":100,"13:24":100}
 */