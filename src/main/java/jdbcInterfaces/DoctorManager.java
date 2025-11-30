package jdbcInterfaces;

import pojos.DiagnosisFile;
import pojos.Doctor;
import pojos.Patient;

import java.sql.SQLException;


import java.io.IOException;
import java.util.List;

public interface DoctorManager {
    void modifyDiagnosisFile(DiagnosisFile diagnosisFile) throws SQLException;

    Doctor getDoctorbyUserId(int userId) throws SQLException;
    Patient getPatientByDiganosisFileID(int idDiagnosisFile)throws SQLException;
    void downloadFileInComputer(DiagnosisFile diagnosisFile) throws IOException;
    List<DiagnosisFile> listDiagnosisFilesTODO();
    List<DiagnosisFile>  getAllDiagnosisFilesFromPatient(int idPatient);
    void UpDateDiagnosisFile(DiagnosisFile diagnosisfile)throws SQLException;
    String getFragmentOfRecording(int idDiagnosisFile, int position)throws SQLException ;
    List<Boolean> getSateOfFragmentsOfRecordingByID(int idDiagnosisFile)throws SQLException;
    void addDoctor(Doctor doctor) throws SQLException;
    List<String> getAllPatientsInsuranceNumberbyDoctor()throws SQLException;
    List<String> getAllFragmentsOfRecording(int id_DiagnosisFile) throws SQLException;
    DiagnosisFile getDiagnosisFileByID(int idDiagnosisFile) throws SQLException;

}

