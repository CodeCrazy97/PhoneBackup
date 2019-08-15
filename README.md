# PhoneBackup
Saves phone calls and text messages from my Android phone and places them in a database.

You should use "SMS Backup & Restore" app to create the phone call and text message XML files. Save the file(s) to an accessible location on your computer. You will enter the path to these files when this program runs.

A prerequisite to running this program is having MySQL installed on your machine. This application will work only on Windows computers. You also should have Java installed. You must have Visual Studio isntalled if you want to use the text message GUI viewer (this lets you view the text messages in a nice GUI format). This part of the project does not allow you to delete/change messages from the database.

To run this app, simply have the text messages and/or phone calls XML file(s) downloaded. Double-click on the "run_phone_backup.bat" file. After you've selected the action you'd like this program to take (such as backup text messages), enter the file path to the XML file. The actions the program is taking will be displayed on the screen.
