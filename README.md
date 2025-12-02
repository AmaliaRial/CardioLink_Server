# CardioLink
## Project Description
The project focuses on building a telemedicine application for remote supervision of patients with cardiac diseases, specifically arrhythmias and heart failure. These conditions require continuous monitoring of both symptoms and physiological signals to detect early warning signs and prevent severe events.
## Installation
To run the server, you will need the **CardioLink_ServerApp.jar** file and the accompanying **Cardiolink.db** Database file, which should both be in the same folder as well as the **run.bat** file. Make sure you have Java 25 installed on your system. If not, download and install it from the [official Java website](https://www.oracle.com/es/java/technologies/downloads/), and add it to your system's PATH. Once everything is in place, simply run the server by executing the .jar (if that doesn't work, try the run.bat file, which will launch the server using the command java -jar Server.jar). All these files can be found in the out/artifacts folder.

## Features
* ### Role-based Access Control
Differentiate user permissions and functionalities based on roles such as patient, doctor, and administrator each with specific permissions and access levels.
* ### User Authentication
Secure login system for user authentication and access control.
* ### Patient Client Application
Installed on the patient’s device for data reading and physiological monitoring.
* ### Hospital Server Application
Installed at the hospital, storing and managing data from multiple patients using a database.
* ### Doctor Application
Enables doctors to review and update patient information remotely.
* ### Database Integration
Use of a relational database instead of plain text files.



## Users
* **Patients**: Symptom reporting form. Physiological Monitoring via Bitalino
* **Doctors**: Can log into the system and review patients’ symptoms and physiological records. Add or modify clinical notes.
* **Administrators**: Monitor server status. Secure shutdown mechanism which requires a password.
## Collaborators
* **[Amalia Rial Plaza](https://github.com/AmaliaRial)**
* **[Rodrigo Fernández Sánchez](https://github.com/RodriFS0)** 
* **[Lorena Cano Díaz-Maroto](https://github.com/loreeenacano)**
* **[Carmen Caballero Herreros](https://github.com/carmeencaballero)**
* **[Anastasia Ricchiuti](https://github.com/anastasiaricchiuti)**


### **For further information about the project please check our our User Manual, if you want to learn about our code please check out our Developer Manual.**
