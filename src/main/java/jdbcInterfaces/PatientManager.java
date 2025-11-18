package jdbcInterfaces;

import pojos.DiagnosisFile;
import pojos.Patient;

import java.sql.SQLException;
import java.util.List;

public interface PatientManager {
    void addPatient(Patient p) throws SQLException;
    Patient getPatientByUserId(int userId) throws SQLException;
    List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient);
}
