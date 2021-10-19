package P0

import org.mongodb.scala._
import scala.io.Source
import net.liftweb.json._
import java.io._

import P0.Helpers._

object Project0 {
   def main(args: Array[String]) {
      importJSON("C:/Users/natsh/ScalaCode/mock_bank_data.json")
   }

   def importJSON(jsonFilePath: String) {
      // Connect to the server.
      val client: MongoClient = MongoClient()
	   // Get an object to the db.
      val database: MongoDatabase = client.getDatabase("banking")
      // Get a Collection.
      val collection: MongoCollection[Document] = database.getCollection("customerInfo")

      // Use default formats for Lift parameters
      implicit val formats = DefaultFormats

      // Define the structure of a bank account to be extracted from the JSON array
      case class BankAccount(
         account_num: Int,
         first_name: String,
         last_name: String,
         email: String,
         phone_num: String,
         cc_num: String,
         savings: String,
         checking: String
      )

      try {
         val json = parse(scala.io.Source.fromFile(jsonFilePath).mkString)
         
         // Get each object from the parsed JSON
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
               "cc_num" -> acc.cc_num,
               "savings" -> acc.savings,
               "checking" -> acc.checking
            )

            // Add each document to the database
            collection.insertOne(doc).results()
         }
      } catch {
         case e: FileNotFoundException => println("File not found at specified path")
      } 
   }
}