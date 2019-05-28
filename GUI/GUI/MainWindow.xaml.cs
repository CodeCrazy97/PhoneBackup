using System;
using MySql.Data.MySqlClient;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace GUI
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        public string connStr;
        public MainWindow()
        {
            InitializeComponent();
            //Setup the server connection.
            connStr = new SqlConnection().connection;

            this.Title = "My Text Messages";

            // Don't allow the user to change the text in the text box.
            messagesRichTextBox.IsReadOnly = true;

            // Get the contacts from the database.
            getContactNames();
        }


        public void getContactNames()
        {
            MySqlConnection connection = new MySqlConnection(connStr);


            //Create the SQL statement that gets the number of the last submitted refill request.
            string sql = "SELECT DISTINCT c.name FROM messages m JOIN(SELECT * FROM contacts) c ON c.id = m.contact ORDER BY c.name; ";

            connection = new MySqlConnection(connStr);    //create the new connection using the parameters of connStr
            try
            {
                connection.Open();                            //open the connection
                var cmd = new MySqlCommand(sql, connection);  //create an executable command
                var reader = cmd.ExecuteReader();             //execute the command             

                if (!reader.HasRows)
                {
                    Console.WriteLine("No contacts and/or messages in the database.");
                    connection.Close();
                }
                else
                {
                    while (reader.Read())                        //read through the results
                    {
                        if (!reader.IsDBNull(0))
                        {
                            contactsComboBox.Items.Add(reader.GetString(0));
                        }
                    }
                    // Select the first contact.
                    contactsComboBox.SelectedIndex = 0;
                    reader.Close();
                    connection.Close();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.ToString());
            }
            connection.Close();
        }

        public void getMessages(String contactName)
        {
            MySqlConnection connection = new MySqlConnection(connStr);


            //Create the SQL statement that gets the number of the last submitted refill request.
            string sql = "SELECT * FROM messages m join (SELECT id FROM contacts WHERE name = '" + contactName + "') c ON c.id = m.contact ORDER BY m.sent_timestamp ASC;";

            connection = new MySqlConnection(connStr);    //create the new connection using the parameters of connStr
            try
            {
                connection.Open();                            //open the connection
                var cmd = new MySqlCommand(sql, connection);  //create an executable command
                var reader = cmd.ExecuteReader();             //execute the command             

                // Null out text.
                messagesRichTextBox.Document.Blocks.Clear();
                if (!reader.HasRows)
                {
                    Console.WriteLine("Error fetching messages for " + contactName);
                    messagesRichTextBox.AppendText("Error fetching messages for contact " + contactsComboBox.SelectedItem);
                    reader.Close();
                }
                else
                {
                    this.Title = "Loading...";                    
                    while (reader.Read())                        //read through the results
                    {
                        if (!reader.IsDBNull(0))
                        {
                            if (reader.GetBoolean(2))  // Message was sent from me to the contact.
                            {
                                messagesRichTextBox.AppendText("\t\t" + reader.GetString(1));
                                messagesRichTextBox.AppendText("\n\t\tSent: " + reader.GetString(4) + "\n\n");
                            }
                            else  // Message was to me from the contact.
                            {
                                messagesRichTextBox.AppendText(reader.GetString(1));
                                messagesRichTextBox.AppendText("\nSENT: " + reader.GetString(4) + "\n\n");
                            }
                        }
                    }
                    this.Title = "Displaying Text Messages with " + contactsComboBox.SelectedItem;
                    reader.Close();
                    connection.Close();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exception: " + ex.ToString());
            }
            connection.Close();
        }

        private void contactsComboBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            // Get the messages for that contact.
            getMessages(contactsComboBox.SelectedItem.ToString());
        }
    }
}
