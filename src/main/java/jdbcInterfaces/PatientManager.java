package jdbcInterfaces;

import pojos.DiagnosisFile;
import pojos.Patient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PatientManager {
    void addPatient(Patient p) throws SQLException;
    Patient getPatientByUserId(int userId) throws SQLException;
    List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient)throws SQLException;
    String getFragmentOfRecording(int idDiagnosisFile, int position)throws SQLException;
    List<Boolean> getSateOfFragmentsOfRecordingByID(int idDiagnosisFile)throws SQLException;
    void AddNewDiagnosisFile(DiagnosisFile diagnosisFile)throws SQLException;
    int returnIdOfLastDiagnosisFile()throws SQLException;
    void saveFragmentOfRecording(int idDiagnosisFile, String fragmentData,int sequence)throws SQLException;
    int getNextSequenceNumber(Connection c, int idDiagnosisFile);
    void updateSymptomsInDiagnosisFile(int idDiagnosisFile, String selectedSymptoms);
    String getDoctornameByPatient(Patient loggedPatient);
    List<String> getAllFragmentsOfRecording(int id_DiagnosisFile) throws SQLException;
}
