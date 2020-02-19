<?php

	echo "<br><h3>The below table shows a breakdown of the number of texts sent/received per contact.</h3>";
	
	function start_table($border=1, $cellspacing=2, $cellpadding=2) {
		echo "<table border=$border cellspacing=$cellspacing cellpadding=$cellpadding>\n";
	}
	
	function end_table() {
		$output = "</table>";
		return $output;
	}
	
	function create_ul($array) {
		echo "<tr>";
		while (list( , $value) = each ($array)) {
			echo "<td>$value</td>";			
		}
		echo "</tr>";
	}

	$mysqli = new mysqli("localhost", "root", "", "phone_backup");
	
	if ($mysqli->connect_errno) {
		printf("Connect failed: %s\n", $mysqli->connect_error);
		exit;
	}

	$query = "SELECT c.person_name AS 'person_name', COUNT(*) AS 'cnt'
		FROM text_messages t 
		JOIN (SELECT person_name, phone_number FROM contacts) c ON c.phone_number = t.sender_phone_number 
		WHERE c.person_name != 'Me'
		GROUP BY t.sender_phone_number
		ORDER BY COUNT(*) DESC";
	if ($result = $mysqli->query($query)) {
		start_table(4,2,5);
		while ($row = $result->fetch_assoc()) {
			create_ul($row);
		}		
		end_table();
		$result->free();
	}
	$mysqli->close();
	
?>
