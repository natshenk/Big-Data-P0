package P0

import org.mongodb.scala._
import scala.io.Source
import net.liftweb.json._
import java.io._
import scala.io.StdIn._

import P0.Helpers._

object Project0 {
   // Connect to the server.
   val client: MongoClient = MongoClient()
   // Get an object to the db.
   val database: MongoDatabase = client.getDatabase("banking")
   // Get a Collection.
   val collection: MongoCollection[Document] = database.getCollection("customerInfo")

   def main(args: Array[String]) {
      // Thread.sleep is used to allow the connection to MongoDB to occur before 
      // prompting the user (for console readability)
      Thread.sleep(1000)
      println(
         "\nWelcome to the First National Bank of Mongo!\n" +
         "It appears that our customer data has been lost, would you mind supplying a source for some new data? Maybe in JSON format?"
      )

      importJSON()

      var exit = false
      while (!exit) {
         println(
            "\nWelcome to the First National Bank of Mongo\n" +
            "Please select an action from the list below by typing its number:\n" +
            "1 : Create an account\n" +
            "2 : Delete an account\n" +
            "3 : Update account details\n" +
            "4 : See savings balance\n" +
            "5 : See checking balance\n" +
            "6 : Make a deposit\n" +
            "7 : Make a withdrawal\n" +
            "8 : Exit"
         )

         val userSelection = readLine("Enter your selection #>")

         userSelection match {
            /*
            case "1" =>
            case "2" =>
            case "3" =>
            case "4" =>
            case "5" =>
            case "6" =>
            case "7" =>
            */
            case "8" => 
               println("See you next time!")
               exit = true
            case _ => 
               println("Please enter a valid option 1-8")
         }
      }
      
      // Remove the data for the next iteration
      // DOESN'T WORK (why? idk.)
      // collection.drop()
   }

   /**
    * Takes a filepath to a folder containing a JSON array and inserts each object as 
    * a document in MongoDB
   */
   def importJSON() {
      // Use default formats for Lift parameters
      implicit val formats = DefaultFormats

      // Define the structure of a bank account to be extracted from the JSON array
      case class BankAccount(
         account_num: Int,
         first_name: String,
         last_name: String,
         email: String,
         phone_num: String,
         state: String,
         cc_num: String,
         savings: String,
         checking: String
      )

      var fileFound = false

      while (!fileFound) {
         try {
            // Set fileFound to true: it will be changed to false if 
            // a FileNotFoundException occurs (in the catch block)
            fileFound = true

            val dataPath = readLine("Please enter a filepath >")

            val json = parse(scala.io.Source.fromFile(dataPath).mkString)

            // Get a list of objects from the parsed JSON
            val accounts = json.children

            // Extract the bank acct info from each object and convert it into a Document
            for (account <- accounts) {
               val acc = account.extract[BankAccount]
               val doc: Document = Document (
                  "account_num" -> acc.account_num,
                  "first_name" -> acc.first_name,
                  "last_name" -> acc.last_name,
                  "email" -> acc.email,
                  "phone_num" -> acc.phone_num,
                  "state" -> acc.state,
                  "cc_num" -> acc.cc_num,
                  "savings" -> acc.savings,
                  "checking" -> acc.checking
               )

               // Add each document to the database
               collection.insertOne(doc).results()
            }
         } catch {
            case e: FileNotFoundException => 
               println("File not found at specified path, please try again")
               fileFound = false
         } 
      }
   }
}