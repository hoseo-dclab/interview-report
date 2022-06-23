module TCP_Soket {
	requires javafx.controls;
	
	opens java_Tcp to javafx.graphics, javafx.fxml;
}