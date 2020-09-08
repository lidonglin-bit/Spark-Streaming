package app
import bean.AdsInfo
import org.apache.spark.streaming.dstream.DStream
import org.json4s.jackson.JsonMethods
import util.RedisUtil

object AreaTopApp extends App {
  override def doSomething(adsInfoStream: DStream[AdsInfo]): Unit = {
    val dayAreaGrouped = adsInfoStream.map(adsInfo => ((adsInfo.dayString, adsInfo.area, adsInfo.adsId), 1))
      //1.先计算每天每地区每广告的点击量
      .updateStateByKey((seq: Seq[Int], opt: Option[Int]) => {
        Some(seq.sum + opt.getOrElse(0))
      })
      //2.map出来 (day,area)作key
      .map {
        case ((day, area, ads), count) => ((day, area), (ads, count))
      }
      .groupByKey()

    //3 4.每组内进行排序取前三
    val result: DStream[((String, String), List[(String, Int)])] = dayAreaGrouped.map {
      case (key, it) =>
        (key,it.toList.sortBy(-_._2).take(3))
    }

   //5.把数据写入到redis
  /*  result.foreachRDD(rdd=>{
      rdd.foreachPartition((it: Iterator[((String, String), List[(String, Int)])]) =>{
        //1.建立到redis的连接
        val client = RedisUtil.getClient
        //2.写数据到redis
        it.foreach{
          //((2020-09-07,华北),List((3,13), (2,8), (1,6)))
          case ((day,area),adsCountList)=>
            val key = "area:ads:count" + day
            val field = area
            //把集合转换成json字符串  json4s
            //专门用于聚合转换成字符传(样例类不行)
            import org.json4s.JsonDSL._
            val value = JsonMethods.compact(JsonMethods.render(adsCountList))
            client.hset(key,field,value)

        }
        //3.关闭到redis的连接
        client.close()  //其实是把这个客户端还给连接池
      })
    })*/
    import util.RealUtil._
    result.saveToRedis

  }
}

/*
每天每地区热门广告top3

1.先计算每天每地区每广告的点击量
  ((day,area,ads),1) => updateStateByKey

2.按照每天每地区分组

3.每组内排序，取前三

数据类型：
   k-v 形式数据库（nosql 数据）
   k： 都是字符串
   v的数据类型：
       5打数据类型
        1. string
        2. set 不重复
        3. list 允许重复
        4. hash map，存的是field-value
        5.zset

----
((2020-09-07,华北),List((3,13), (2,8), (1,6)))
((2020-09-07,华南),List((5,14), (1,9), (4,6)))
((2020-09-07,华东),List((2,13), (1,10), (5,7)))
((2020-09-07,华中),List((1,6), (5,3), (3,2)))
----
选择什么类型的数据
每天一个key
key                                   value
"area:ads:count" + day                hash
                                      field     value
                                      area      json
                                      "华中"    {3:13,2:8,1:6}



 */


