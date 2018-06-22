# Bitcoin price movement and prediction
This project provides a set of REST APIs to see the historical movement of bitcoin prices and also predicts the next 15 days price based on ARIMA timeseries model

## Getting started
These set of instructions will help you get the project running on you machine and interact with the APIs

### Setup
This is a sbt based scala project, so you need to have sbt and scala sdk installed on your system. And java is also required as scala uses jvm. I have used java 8 for this project development. This project uses MySQL as backend DB. You can use any remote MySQL db and change the endpoint in code accordingly

Use following commands to setup java 8 on your ubuntu system
```
sudo add-apt-repository ppa:webupd8team/java
sudo apt update; sudo apt install oracle-java8-installer
sudo apt install oracle-java8-set-default
```

Use following commands to setup scala. I have used scala 2.11.8 in my project
```
sudo apt-get remove scala-library scala
wget http://www.scala-lang.org/files/archive/scala-2.11.8.deb
sudo dpkg -i scala-2.11.8.deb
sudo apt-get update
sudo apt-get install scala
```

Use following command to setup sbt
```
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt
```

Use following command to setup and start mysql server
```
sudo apt-get update
sudo apt-get install mysql-server
systemctl start mysql
```

Once mysql is setup, login to mysql server, create a database and use following command to create the table
```
CREATE TABLE `bitcoin` (
  `time` date DEFAULT NULL,
  `price` double NOT NULL,
  `day` int(11) NOT NULL,
  `month` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `week` int(11) NOT NULL,
  PRIMARY KEY (`day`,`month`,`year`,`week`)
)
```
Once done with all these steps, you are ready with an environment to run this project

## Running the project
As this project runs, it uses the file response.json in the project directory to insert the last 365 days bitcoin prices in mysql db. Use the following api from coinbase to get the latest 365 days data. Use it in browser to directly download response.json and put this file in the project directory

https://www.coinbase.com/api/v2/prices/BTC-USD/historic?period=year

By default this code start a server on localhost as host and 8080 as port, but if you want to change it, edit the code part where http server is being setup. Make the host as 0.0.0.0 to access the APIs publicly. 

ARIMA model from cloudera spark timeseries library has been used. One can change model-parameters in the code to optimise the model further.

Once you have done above changes, you can run the the code with following command in project directory
```
sbt run
```
It will download all the required libraries and then start running the code in your system, mysql db insertion might take a little time, so wait for the logging to say "Insertion over". After all the preprocessing is done, you'll see the message "Server online at http://public-ip:8080/  ....."
Once server has started, you can access the APIs in your web-browser. API details are given in next section

## API details
This project provides 5 APIs to get the bitcoin prices and see next 15 days prediction

### API 1
This API provides the bitcoin price for a particular month of the year ordered by day

http://host:8080/monthly?month=7&year=2017

Change the month and year in the url to get the data of that particular month

### API 2
This API provides the bitcoin price for a particular week of the year ordered by day

http://host:8080/weekly?week=22&year=2018

Change the week and year in the url to get the data of that particular week

### API 3
This API provides the bitcoin price between two custom dates provided by the user ordered by day

http://host:8080/custom?start=2018-06-14&end=2018-06-18

Change the start and end to provide the start date and end date between which you want to get the prices

### API 4
This API provides the n days moving average between two custom dates provided by the user ordered by day

http://host:8080/movavg?start=2018-06-10&end=2018-06-18&n=2

Change the start day , end day, and n to get the n days moving average between two dates.

### API 5
This API provides the next 15 days prediction of the bitcoin prices as predicted by the ARIMA model

http://host:8080/prediction


## Built with
* [Akka http](https://doc.akka.io/docs/akka-http/current/introduction.html) - The web framework used
* [Cloudera sparkts](https://mvnrepository.com/artifact/com.cloudera.sparkts/sparkts) - The library used for timeseries analysis with ARIMA model
* [MySQL](https://www.mysql.com/) - The backend DB used to store historical data
