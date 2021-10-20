package P0

import org.mongodb.scala._
import org.mongodb.scala.model.Filters._ // needed for equal()
import scala.io.Source
import net.liftweb.json._
import java.io._
import scala.io.StdIn._
import scala.util.Random

import P0.Helpers._

object Project0 {
   // Connect to the server.
   val client: MongoClient = MongoClient()
   // Get an object to the db.
   val database: MongoDatabase = client.getDatabase("banking")
   // Get a Collection.
   val collection: MongoCollection[Document] = database.getCollection("customerInfo")

   var accountNum: Int = 164868000

   def main(args: Array[String]) {
      // Thread.sleep is used to allow the connection to MongoDB to occur before 
      // prompting the user (for console readability)
      Thread.sleep(1000)
      println(
         "\nWelcome to the First National Bank of Mongo!\n" +
         "It appears that our customer data has been lost, would you mind supplying a source for some new data? Maybe in JSON format?"
      )

      importJSON()
      println("\nNew data added successfully!")

      var exit = false
      while (!exit) {
         println(
            "\nWelcome to the First National Bank of Mongo!\n" +
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

         val userSelection = readLine("Enter your selection # > ")

         userSelection match {
            case "1" =>
               addAccount()
            case "2" =>
               deleteAccount()
            case "3" =>
               updateAccount()
            case "4" =>
               getSavings()
            case "5" =>
               getChecking()
            case "6" =>
               makeDeposit()
            case "7" =>
               makeWithdrawal()
            case "8" => 
               println("See you next time!")
               exit = true
               client.close()
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
         savings: Double,
         checking: Double
      )

      var fileFound = false

      while (!fileFound) {
         try {
            // Set fileFound to true: it will be changed to false if 
            // a FileNotFoundException occurs (in the catch block)
            fileFound = true

            val dataPath = readLine("Please enter a filepath > ")

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

   /**
     * Prompts the user for personal info, then assigns them an account and
     * cc number as well as assigning their balance to be $0 before adding
     * to the database
     */
   def addAccount() {
      // Get personal info
      val fname = readLine("First name > ")
      val lname = readLine("Last name > ")
      val email = readLine("Email > ")
      val phone = readLine("Phone number > ")
      val state = readLine("State > ")

      // Increment the global account number tracker for a new, unique number
      accountNum += 1

      // Generate a random Mastercard card number
      val rnd = new scala.util.Random
      val cc: String = "5108750" + rnd.nextInt(999999999).toString()

      // Turn the data into the proper Document format
      val doc: Document = Document (
         "account_num" -> accountNum.toString(),
         "first_name" -> fname,
         "last_name" -> lname,
         "email" -> email,
         "phone_num" -> phone,
         "state" -> state,
         "cc_num" -> cc,
         "savings" -> 0.00,
         "checking" -> 0.00
      )

      collection.insertOne(doc).results()
      println(s"New account created! Your account number is $accountNum\n" +
              s"Your new credit card with the number $cc is ready to be activated")

      // Allow the user to see the success message before reprompting
      Thread.sleep(5000)
   }

   def deleteAccount() {
      val accountId = readLine("Account number to delete > ")
      val confirm = readLine("Are you sure? (y/n) > ")
      if (confirm == "y") {
         collection.deleteOne(equal("account_num", accountId)).results()
         println("Account deleted. We're sorry to see you go!")
      } else {
         println("Account deletion cancelled")
      }
      Thread.sleep(2000)
   }

   /**
     * Prompts the user for their account number and what info they want to update
     * before acquiring said new info and updating the matching document in Mongo
     */
   def updateAccount() {
      import org.mongodb.scala.model.Updates._ // for set()

      val accountId = readLine("Account number to update > ")

      println(
         "Select which info to change:\n" +
         "1 : First name\n" +
         "2 : Last name\n" +
         "3 : Email\n" +
         "4 : Phone number\n" +
         "5 : State"
      )
      val selection = readLine("Enter your selection # > ")

      // TODO: clean up redundant cases
      selection match {
         case "1" => 
            val fname = readLine("Please enter new first name > ")
            collection.updateOne(equal("account_num", accountId), set("first_name", fname)).results()
            println("Account info updated")
         case "2" => 
            val lname = readLine("Please enter new last name > ")
            collection.updateOne(equal("account_num", accountId), set("last_name", lname)).results()
            println("Account info updated")
         case "3" => 
            val email = readLine("Please enter new email > ")
            collection.updateOne(equal("account_num", accountId), set("email", email)).results()
            println("Account info updated")
         case "4" => 
            val phone = readLine("Please enter new phone number > ")
            collection.updateOne(equal("account_num", accountId), set("phone_num", phone)).results()
            println("Account info updated")
         case "5" => 
            val state = readLine("Please enter new state > ")
            collection.updateOne(equal("account_num", accountId), set("state", state)).results()
            println("Account info updated")
         case _ => 
            println("Invalid option. Returning to main menu...")
      }
      // Allow the user to read the output
      Thread.sleep(2000)
   }

   /**
     * Gets the account ID from the user then returns just the savings balance
     */
   def getSavings() {
      import org.mongodb.scala.model.Projections._

      val accountId = readLine("Account number > ")

      collection.find(equal("account_num", accountId))
         .projection(fields(include("savings"), excludeId())).printHeadResult()
      
      Thread.sleep(3000)
   }

   /**
     * Gets the account ID from the user then returns just the checking balance
     */
   def getChecking() {
      import org.mongodb.scala.model.Projections._

      val accountId = readLine("Account number > ")

      collection.find(equal("account_num", accountId))
         .projection(fields(include("checking"), excludeId())).printHeadResult()
      
      Thread.sleep(3000)
   }

   /**
     * Gets the account ID to find the account then allows the user to
     * choose their checking or savings account to deposit into along
     * with how much before updating the document
     */
   def makeDeposit() {
      import org.mongodb.scala.model.Updates._ // for set()

      val accountId = readLine("Account number > ")

      println(
         "Select which account to deposit into:\n" +
         "1 : Savings\n" +
         "2 : Checking"
      )
      val selection = readLine("Enter your selection # > ")

      // TODO: clean up redundant cases
      selection match {
         case "1" => 
            print("How much to deposit? $")
            val deposit = readDouble()
            collection.updateOne(equal("account_num", accountId), inc("savings", deposit)).results()
            println(s"$deposit added to savings account")
         case "2" => 
            print("How much to deposit? $")
            val deposit = readDouble()
            collection.updateOne(equal("account_num", accountId), inc("checking", deposit)).results()
            println(s"$deposit added to checking account")
         case _ => 
            println("Invalid option. Returning to main menu...")
      }
      // Allow the user to read the output
      Thread.sleep(2000)
   }

   /**
     * Gets the account ID to find the account then allows the user to
     * choose their checking or savings account to withdraw from along
     * with how much before updating the document
     */
   def makeWithdrawal() {
      import org.mongodb.scala.model.Updates._ // for set()

      val accountId = readLine("Account number > ")

      println(
         "Select which account to withdraw from:\n" +
         "1 : Savings\n" +
         "2 : Checking"
      )
      val selection = readLine("Enter your selection # > ")

      // TODO: clean up redundant cases
      selection match {
         case "1" => 
            print("How much to withdraw? $")
            val deposit = readDouble()
            collection.updateOne(equal("account_num", accountId), inc("savings", -deposit)).results()
            println(s"$deposit withdrawn from savings account")
         case "2" => 
            print("How much to withdraw? $")
            val deposit = readDouble()
            collection.updateOne(equal("account_num", accountId), inc("checking", -deposit)).results()
            println(s"$deposit withdrawn from checking account")
         case _ => 
            println("Invalid option. Returning to main menu...")
      }
      // Allow the user to read the output
      Thread.sleep(2000)
   }
}