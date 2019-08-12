using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using MySql.Data.MySqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace SMSGui
{
    public partial class Form1 : Form
    {
        public string connStr;
        public Form1()
        {
            InitializeComponent();

            //Setup the server connection.            
            connStr = new SqlConnection().connection;

            // Don't allow the user to change the text in the text box.
            messagesRichTextBox.ReadOnly = true;

            // Get the contacts from the database.
            getContactNames();

            // If there are contacts, then say we are displaying the messages with the selected contact.
            if (contactsComboBox.Items.Count > 0)
            {
                this.Text = "Text Messages with " + contactsComboBox.SelectedItem;
            }
        }

        public void getContactNames()
        {
            MySqlConnection connection = new MySqlConnection(connStr);


            //Create the SQL statement that gets the number of the last submitted refill request.
            string sql = "SELECT DISTINCT c.name FROM text_messages m JOIN(SELECT * FROM contacts) c ON c.id = m.contact_id ORDER BY c.name; ";

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
            string sql = "SELECT * FROM text_messages m join (SELECT id FROM contacts WHERE name = '" + contactName + "') c ON c.id = m.contact_id ORDER BY m.sent_timestamp ASC;";

            connection = new MySqlConnection(connStr);    //create the new connection using the parameters of connStr
            try
            {
                connection.Open();                            //open the connection
                var cmd = new MySqlCommand(sql, connection);  //create an executable command
                var reader = cmd.ExecuteReader();             //execute the command             

                // Null out text.
                messagesRichTextBox.Clear();
                if (!reader.HasRows)
                {
                    Console.WriteLine("Error fetching messages for " + contactName);
                    messagesRichTextBox.AppendText("Error fetching messages for contact " + contactsComboBox.SelectedItem);
                    reader.Close();
                }
                else
                {
                    this.Text = "Loading Messages...";
                    while (reader.Read())                        //read through the results
                    {
                        if (!reader.IsDBNull(0))
                        {

                            if (reader.GetBoolean(2))  // Message was sent from me to the contact.
                            {
                                AppendText(reader.GetString(1), Color.White);
                                messagesRichTextBox.SelectionFont = new Font(messagesRichTextBox.Font, FontStyle.Bold);
                                AppendText("\nSent: ", Color.White);
                                AppendText(reader.GetString(4) + "\n\n", Color.White);
                                messagesRichTextBox.SelectionFont = new Font(messagesRichTextBox.Font, FontStyle.Regular);
                            }
                            else  // Message was to me from the contact.
                            {
                                // Make the background text a different color, so user can distinguish between sent and received texts.
                                AppendText(reader.GetString(1), Color.LightGray);
                                messagesRichTextBox.SelectionFont = new Font(messagesRichTextBox.Font, FontStyle.Bold);
                                AppendText("\nSent: ", Color.LightGray);
                                AppendText(reader.GetString(4) + "\n\n", Color.LightGray);
                                messagesRichTextBox.SelectionFont = new Font(messagesRichTextBox.Font, FontStyle.Regular);
                            }
                        }
                    }

                    // If there are contacts, then say we are displaying the messages with the selected contact.
                    if (contactsComboBox.Items.Count > 0)
                    {
                        this.Text = "Text Messages with " + contactsComboBox.SelectedItem;
                    }


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

        public void AppendText(string text, Color color)
        {
            messagesRichTextBox.SelectionStart = messagesRichTextBox.TextLength;
            messagesRichTextBox.SelectionLength = 0;

            messagesRichTextBox.SelectionBackColor = color;
            messagesRichTextBox.AppendText(text);
            messagesRichTextBox.SelectionBackColor = messagesRichTextBox.BackColor;
        }

        private void contactsComboBox_SelectedIndexChanged(object sender, EventArgs e)
        {
            // Get the messages for that contact.
            getMessages(contactsComboBox.SelectedItem.ToString().Replace("\'", "\\'"));
            messagesRichTextBox.Focus();  // Place the focus on the text box (taking it away from the combo box).
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            this.ActiveControl = messagesRichTextBox;  // Set the message box as the component that has control (this way, you can scroll without having to click on it).
        }
    }
}
