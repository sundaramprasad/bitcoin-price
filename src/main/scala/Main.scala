import scala.io._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import java.sql.{Connection,DriverManager}
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import scala.math.BigDecimal
import com.cloudera.sparkts.models.ARIMA
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{Row, SparkSession}


object Main extends App{


///////////// This part reads response.json and inserts into mysql db ///////////////////
  val filename = "response.json"
  val json = Source.fromFile(filename)
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  val jsonmapper = new ObjectMapper()
  jsonmapper.registerModule(DefaultScalaModule)
  val parsedJson = mapper.readValue[Map[String, Object]](json.reader())
  val prices = parsedJson("data").asInstanceOf[scala.collection.immutable.Map[String, Object]]
  val pricelist = prices("prices").asInstanceOf[scala.collection.immutable.List[String]]
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  var date = new Date()
  var realdate =  "2018-06-14T00:00:00Z"
  date = dateFormat.parse(realdate)
  var mytime = Calendar.getInstance()
  var currentDay = mytime.get(Calendar.DAY_OF_MONTH)
  var currentMonth = mytime.get(Calendar.MONTH)
  var currentYear = mytime.get(Calendar.YEAR)
  var currentWeek = mytime.get(Calendar.WEEK_OF_YEAR)

  val url = "jdbc:mysql://localhost:3306/tookitaki"
  val driver = "com.mysql.jdbc.Driver"
  val username = "root"
  val password = "root"
  var connection:Connection = _
  println("Inserting into mysql DB")
  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
    val statement = connection.createStatement
    val tr = statement.executeQuery("truncate tookitaki.bitcoin")
    for (i <- 0 to 364){
      val item = pricelist(i).asInstanceOf[scala.collection.immutable.Map[String, Object]]
      realdate = item("time").toString
      date = dateFormat.parse(realdate)
      mytime.setTime(date)
      currentDay = mytime.get(Calendar.DAY_OF_MONTH)
      currentMonth = mytime.get(Calendar.MONTH)+1
      currentYear = mytime.get(Calendar.YEAR)
      currentWeek = mytime.get(Calendar.WEEK_OF_YEAR)
      val time = item("time").toString.slice(0,item("time").toString.length - 1)
      val price = item("price").toString
      val rs = statement.addBatch(s"insert into bitcoin (time, price, day, month, year, week) values ('$time', $price, $currentDay, $currentMonth, $currentYear, $currentWeek)")
    }
    statement.executeBatch()
  } catch {
    case e: Exception => e.printStackTrace
  }
  connection.close
  println("Insertion over")
////////////////////////////////////////////////////////////////


///////// This part fetches data for ARIMA model from mysql db /////////////////
  val APP_NAME = "Bitcoin Prediction"
  var period = 15
  var lastdate =  "2018-06-14"
  val conf = new SparkConf().setAppName(APP_NAME).setMaster("local[2]")
  val sparkcontext = new SparkContext(conf)
  val spark = SparkSession
    .builder()
    .appName(APP_NAME)
    .getOrCreate()

  var dataread = spark.read
    .format("jdbc")
    .option("url", "jdbc:mysql://localhost:3306/tookitaki") //db name is tookitaki and table name is bitcoin
    .option("user", "root")
    .option("password", "root")

  val df = dataread.option("dbtable","(SELECT time, price FROM bitcoin order by time) as btc").load()
  df.createOrReplaceTempView("opp")
  val days = df.collect().flatMap((row: Row) => Array(row.get(0)))
  val btcprice = df.collect().flatMap((row: Row) => Array(row.get(1).toString.toDouble))
  lastdate = days(days.length-1).toString //// storing this to append date in prediction data
//////////////////////// end data fetching for ARIMA ///////////

///// This part creates http server for REST API /////////
  implicit val system = ActorSystem("bitcoin")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  var month=1
  var year=1
  var week=1
  var n=0

  /******these variables are used in prediction api**********/
  var futuredate =  "2018-06-14"
  val pdateFormat = new SimpleDateFormat("yyyy-MM-dd")
  var pdate = new Date()
  var ptime = Calendar.getInstance()
  /***************************************/

  val route =
      get {
        path("monthly") { ///////api to get monthly data
        parameter('month.as[String], 'year.as[String]){ (month,year) =>
          var dm  = List[String]()
          var res = List[Map[String, Any]]()
          try {
            Class.forName(driver)
            connection = DriverManager.getConnection(url, username, password)
            val statement = connection.createStatement
            val rs = statement.executeQuery(s"SELECT time, price FROM bitcoin where month = $month and year=$year order by time desc")
            while (rs.next) {
              val price = rs.getString("price")
              val time = rs.getString("time")
              res ::= Map("time"-> time, "price"->price)
            }
          } catch {
            case e: Exception => e.printStackTrace
          }
          connection.close
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, jsonmapper.writeValueAsString(res)))
        }

      } ~
          path("weekly") {   ///////api to get weekly data
            parameter('week.as[String], 'year.as[String]){ (week,year) =>
              var dm  = List[String]()
              var res = List[Map[String, Any]]()
              try {
                Class.forName(driver)
                connection = DriverManager.getConnection(url, username, password)
                val statement = connection.createStatement
                val rs = statement.executeQuery(s"SELECT time, price FROM bitcoin where week = $week and year=$year order by time desc")
                while (rs.next) {
                  val price = rs.getString("price")
                  val time = rs.getString("time")
                  res ::= Map("time"-> time, "price"->price)
                }
              } catch {
                case e: Exception => e.printStackTrace
              }
              connection.close
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, jsonmapper.writeValueAsString(res)))
            }

          } ~
          path("custom") { ///////api to get custom date data
            parameter('start.as[String], 'end.as[String]){ (start,end) =>
              var dm  = List[String]()
              var res = List[Map[String, Any]]()
              try {
                Class.forName(driver)
                connection = DriverManager.getConnection(url, username, password)
                val statement = connection.createStatement
                val rs = statement.executeQuery(s"SELECT time, price FROM bitcoin where time between '$start' and '$end' order by time desc")
                while (rs.next) {
                  val price = rs.getString("price")
                  val time = rs.getString("time")
                  res ::= Map("time"-> time, "price"->price)
                }
              } catch {
                case e: Exception => e.printStackTrace
              }
              connection.close
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, jsonmapper.writeValueAsString(res)))
            }

          } ~
          path("movavg") { ////////API to get n days moving average of bitcoin price
            parameter('start.as[String], 'end.as[String], 'n.as[Int]){ (start,end,n) =>
              var btcp  = List[Double]()
              var res = List[Map[String, Any]]()
              try {
                Class.forName(driver)
                connection = DriverManager.getConnection(url, username, password)
                val statement = connection.createStatement
                var runprice : Double=0;
                val rs = statement.executeQuery(s"SELECT time, price FROM bitcoin where time between '$start' and '$end' order by time asc")
                while (rs.next) {
                  val price = rs.getString("price").toDouble
                  val time = rs.getString("time")
                  if (res.size>=n){
                    val temp = btcp(n-1)
                    runprice=((runprice*n) - temp + price)/n
                    //runprice = (price+(runprice*res.size))/(res.size+1)
                    runprice = BigDecimal(runprice).setScale(2,BigDecimal.RoundingMode.HALF_UP).toDouble
                    res :+= Map("time"-> time, "price"->runprice.toString)
                  }
                  else{
                    runprice = (price+(runprice*res.size))/(res.size+1)
                    res :+= Map("time"-> time, "price"->price.toString)
                  }
                  btcp ::=price //////// storing the actual btc price in separate list because mapped list will contain moving average price
                }
              } catch {
                case e: Exception => e.printStackTrace
              }
              connection.close
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, jsonmapper.writeValueAsString(res)))
            }

          } ~
          path("prediction") {
            pdate = pdateFormat.parse(lastdate)
            ptime = Calendar.getInstance()
            ptime.setTime(pdate)
            //var pptime = ptime.clone().asInstanceOf[Calendar]
            var res = List[Map[String, Any]]()

            {
              // Prediction
              var tmp : Double = 0.0
              val actual = new DenseVector(btcprice)
              val model = ARIMA.fitModel(1,0,1,actual)
              println("ARIMA model with parameter = (" + model.p + "," + model.d + "," + model.q + ") and AIC=" + model.approxAIC(actual)  )
              val predicted = model.forecast(actual, period)
              for (i <- 365 until predicted.size) {
                println("" + (i-364) + " day prediction" + "     " + predicted(i))
                ptime.add(Calendar.DATE,1)
                futuredate = pdateFormat.format(ptime.getTime)
                tmp = BigDecimal(predicted(i)).setScale(2,BigDecimal.RoundingMode.HALF_UP).toDouble
                res :+= Map("time"->futuredate,"price"-> tmp.toString)
              }
            }
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, jsonmapper.writeValueAsString(res)))
          }


    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://public-ip:8080/  .....\n")
}