package jdbcInterfaces;

import pojos.DiagnosisFile;

import java.sql.SQLException;


import java.io.IOException;
import java.util.List;

public interface DoctorManager {
    void modifyDiagnosisFile(DiagnosisFile diagnosisFile) throws SQLException;
    void downloadFileInComputer(DiagnosisFile diagnosisFile) throws IOException;
    List<DiagnosisFile> listDiagnosisFilesTODO(int idDoctor);
    List<DiagnosisFile>  getAllDiagnosisFilesFromPatient(int idPatient);
    void UpDateDiagnosisFile(DiagnosisFile diagnosisfile)throws SQLException;
    String getFracmentofRecoring(int idDiagnosisFile, int position)throws SQLException ;
    List<Boolean> getSateOfFragmentsOfRecordingByID(int idDiagnosisFile)throws SQLException;

}

