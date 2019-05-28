# SaveSMSMessages
Extracts message text from my Android SMS/MMS messages and places them in a MySQL database.

An example of an SMS message in XML format that this program would use is shown below....
  ![alt text](https://github.com/CodeCrazy97/SaveSMSMessages/blob/master/exampleSMS.png)
  
An example MMS message (MMS messages contain pictures or a very large amount of text) is below:
 ![alt text](https://github.com/CodeCrazy97/SaveSMSMessages/blob/master/exampleMMS.png)
 
Finally, an example phone call:
 ![alt text](https://github.com/CodeCrazy97/SaveSMSMessages/blob/master/examplePhoneCall.png)
 
A prerequisite to running this program is having MySQL installed and running on your machine. This application will work only on Windows computers.

To run this program, you will first need to save your text messages and phone calls into XML files in the formats shown above. (I saved mine using SMS Backup & Restore.) Then, change the line of code that points to the XML file containing your text messages (shown below).
 ![alt text](https://github.com/CodeCrazy97/SaveSMSMessages/blob/master/changeThisLineOfCodeSMSBackup.png)
 
 Similarly, change the line of code for backing up your phone calls. Make the CallsBackup.java file look at the XML file containing your phone calls. 
 
 The GUI part of this project provides a nice way for you to view the messages. It does not allow you to change/delete messages from the database.
