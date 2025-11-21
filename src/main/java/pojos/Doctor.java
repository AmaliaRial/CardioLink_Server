package pojos;

import pojos.enums.Sex;
import java.util.ArrayList;
import java.sql.Date;
import java.util.Objects;
import java.util.List;

public class Doctor {
    private int idDoctor;
    private int userId;
    private String nameDoctor;
    private String surnameDoctor;
    private String dniDoctor;
    private Date dobDoctor;
    private String emailDoctor;
    private Sex sexDoctor;
    private String specialty;
    private String licenseNumber;



    public Doctor(int idDoc, int userId, String name, String surname, String dni, Date dob, String email, Sex sex, String licenseNumber, String specialty) {
        this.idDoctor = idDoc;
        this.userId = userId;
        this.nameDoctor = name;
        this.surnameDoctor = surname;
        this.dniDoctor = dni;
        this.dobDoctor = dob;
        this.emailDoctor = email;
        this.sexDoctor = sex;
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;

    }
    public Doctor(int userId, String name, String surname, String dni, Date dob, String email, Sex sex, String licenseNumber, String specialty) {
        this.userId = userId;
        this.nameDoctor = name;
        this.surnameDoctor = surname;
        this.dniDoctor = dni;
        this.dobDoctor = dob;
        this.emailDoctor = email;
        this.sexDoctor = sex;
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;

    }

    public int getIdDoctor() {
        return idDoctor;}
    public void setIdDoctor(int idDoctor) {
        this.idDoctor = idDoctor;}

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    public String getSpecialty() {
        return specialty;
    }
    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getNameDoctor() {
        return nameDoctor;}
    public void setNameDoctor(String nameDoctor) {
        this.nameDoctor = nameDoctor;}
    public String getSurnameDoctor() {
        return surnameDoctor;}
    public void setSurnameDoctor(String surnameDoctor) {
        this.surnameDoctor = surnameDoctor;}
    public String getDniDoctor() {
        return dniDoctor;}
    public void setDniDoctor(String dniDoctor) {
        this.dniDoctor = dniDoctor;}
    public Date getDobDoctor() {
        return dobDoctor;}
    public void setDobDoctor(Date dobDoctor) {
        this.dobDoctor = dobDoctor;}
    public String getEmailDoctor() {
        return emailDoctor;}
    public void setEmailDoctor(String emailDoctor) {
        this.emailDoctor = emailDoctor;}
    public Sex getSexDoctor() {
        return sexDoctor;}
    public void setIdDoctor(Sex sexDoctor) {
        this.sexDoctor = sexDoctor;}



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return idDoctor == doctor.idDoctor &&
                Objects.equals(nameDoctor, doctor.nameDoctor) &&
                Objects.equals(surnameDoctor, doctor.surnameDoctor) &&
                Objects.equals(dniDoctor, doctor.dniDoctor) &&
                Objects.equals(dobDoctor, doctor.dobDoctor) &&
                Objects.equals(emailDoctor, doctor.emailDoctor) &&
                sexDoctor == doctor.sexDoctor &&
                Objects.equals(specialty, doctor.specialty) &&
                Objects.equals(licenseNumber, doctor.licenseNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDoctor, nameDoctor, surnameDoctor, dniDoctor, dobDoctor, emailDoctor, sexDoctor, specialty, licenseNumber);
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "idDoctor=" + idDoctor +
                ", name='" + nameDoctor + '\'' +
                ", surname='" + surnameDoctor + '\'' +
                ", dni='" + dniDoctor + '\'' +
                ", dob=" + dobDoctor +
                ", email='" + emailDoctor + '\'' +
                ", sex=" + sexDoctor +
                ", specialty=" + specialty +
                ", licenseNumber='" + licenseNumber + '\'' +
                '}';
    }


}
