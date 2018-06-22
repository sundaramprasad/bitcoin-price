# Bitcoin price movement and prediction
This project provides a set of REST APIs to see the historical movement of bitcoin prices and also predicts the next 15 days price based on ARIMA timeseries model

## Getting started
These set of instructions will help you get the project running on you machine and interact with the APIs

### Setup
This is a sbt based scala project, so you need to have sbt and scala sdk installed on your system. And java is also required as scala uses jvm. I have used java 8 for this project development. This project uses mysql db as backend. You can use any remote mysql db and change the endpoint in url accordingly

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

By default this code start a server on localhost as host and 8080 as port, but if you want to change it, edit the code part where http server is being setup. Make the host as 0.0.0.0 to access the APIs publicly

Once you have done above changes, you can run the the code with following command in project directory
```
sbt run
```
It will start running the code in your system, mysql db insertion might take a little time, so wait for the logging to say "Insertion over". After all the preprocessing is done, you'll see the message "Server online at http://public-ip:8080/  ....."
Once server has started, you can access the APIs in your web-browser. API details are given in next section
