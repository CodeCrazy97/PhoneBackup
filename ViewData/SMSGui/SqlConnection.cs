using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace SMSGui
{
    public class SqlConnection
    {

        //Below are the definitions for the connection to the database. 
        public string connection = "server=localhost;user=root;database=phone_backup;port=3306;password=;";

        public string quoteFilter(string str)
        {
            //Method to allow single quotes and newline characters in MySQL statements
            string newStr = "";

            for (int i = 0; i < str.Length; i++)
            {
                //Allow insertion of apostrophes and newline characters.
                if (str[i] == (char)39)
                {
                    newStr += "\\'";
                }
                else if (str[i].Equals("\n"))
                {
                    newStr += "\\n";
                }
                else
                {
                    newStr = newStr + str[i];
                }
            }
            return newStr;
        }
    }
}

