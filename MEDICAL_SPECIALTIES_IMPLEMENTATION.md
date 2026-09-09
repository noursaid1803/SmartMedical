# SmartMedical - Medical Specialties Implementation Summary

## Overview
This document summarizes the implementation of the medical specialties system with user management, email verification, and file upload validation.

## Implemented Features

### 1. Medical Specialties (6 Specialties)

#### Specialty Entity
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/entity/Specialty.java`
- **Fields**: code, name, icon, description, AI model info, upload validation rules
- **Repository**: `SpecialtyRepository.java`
- **Service**: `SpecialtyService.java` with initialization method
- **Controller**: `SpecialtyController.java` with endpoints:
  - `POST /specialties/initialize` - Initialize all specialties
  - `GET /specialties` - Get all specialties
  - `GET /specialties/active` - Get active specialties
  - `GET /specialties/code/{code}` - Get by code
  - `GET /specialties/{id}` - Get by ID

#### Specialties Implemented:
1. **CERVEAU (Brain Tumors)**
   - Model: 3D U-Net for Brain Tumor Segmentation
   - Accepted files: DICOM, NIfTI, PNG, JPEG
   - Dimensions: 128x128 to 512x512
   - Max file size: 50MB
   - DOI: 10.1007/978-3-030-46640-4_12

2. **SEIN (Breast/Mammography)**
   - Model: Deep Learning for Breast Cancer Detection
   - Accepted files: DICOM, PNG, JPEG
   - Dimensions: 256x256 to 4096x4096
   - Max file size: 30MB
   - DOI: 10.1148/ryai.2020190227

3. **PEAU (Skin Lesions)**
   - Model: EfficientNet-B4 Skin Lesion Classifier
   - Accepted files: PNG, JPEG, JPG
   - Dimensions: 224x224 to 1024x1024
   - Max file size: 10MB
   - DOI: 10.1038/nature21056

4. **OEIL (Diabetic Retinopathy)**
   - Model: ResNet-50 Diabetic Retinopathy Detector
   - Accepted files: PNG, JPEG, JPG, DICOM
   - Dimensions: 512x512 to 2048x2048
   - Max file size: 15MB
   - DOI: 10.1001/jama.2016.17216

5. **ALZHEIMER (Alzheimer's MRI)**
   - Model: 3D Deep Learning for Alzheimer's Disease Diagnosis
   - Accepted files: DICOM, NIfTI, NII
   - Dimensions: 128x128 to 256x256
   - Max file size: 100MB
   - DOI: 10.1016/j.neuroimage.2019.02.048

6. **POUMON (Lung Nodules)**
   - Model: UNet 2D Pulmonary Nodule Segmentation
   - Accepted files: DICOM, NIfTI, NII, PNG, JPEG
   - Dimensions: 256x256 to 1024x1024
   - Max file size: 50MB
   - DOI: 10.1002/mp.13945

### 2. Doctor Management with Email Verification

#### Updated Doctor Entity
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/entity/Doctor.java`
- **New fields**: `specialtyCode`, `isVerified`, `verificationCode`, `verificationCodeExpiresAt`, `tempPassword`, `tempPasswordExpiresAt`, `isFirstLogin`

#### Updated DoctorRequest DTO
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/dto/DoctorRequest.java`
- **New field**: `specialtyCode`

#### Updated DoctorService
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/service/DoctorService.java`
- **Changes**:
  - Creates user account with temporary password
  - Generates verification code (6 digits)
  - Sends welcome email with credentials and verification code
  - Sets `isFirstLogin = true` to force password change

#### Email Service
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/service/EmailService.java`
- **Methods**:
  - `sendDoctorWelcomeEmail()` - Sends credentials and verification code
  - `sendPatientWelcomeEmail()` - Sends patient credentials
  - `sendPasswordChangedConfirmation()` - Confirms password change

### 3. Patient Registration with Email Verification

#### Updated Patient Entity
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/entity/Patient.java`
- **New fields**: `isVerified`, `verificationCode`, `verificationCodeExpiresAt`, `tempPassword`, `tempPasswordExpiresAt`, `isFirstLogin`

#### New DTOs
- **PatientRegistrationRequest**: Full patient registration form with chronic diseases
- **PatientRegistrationResponse**: Response with verification status

#### Updated PatientService
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/service/PatientService.java`
- **New methods**:
  - `registerPatient()` - Registers patient with email verification
  - `verifyPatientCode()` - Verifies email confirmation code
  - `changePatientPassword()` - Changes password on first login

#### Updated PatientController
- **Location**: `AUTH-SERVICE/src/main/java/tn/esprit/authservice/controller/PatientController.java`
- **New endpoints**:
  - `POST /patients/register` - Register patient with email
  - `POST /patients/verify` - Verify email code
  - `POST /patients/change-password` - Change password (first login)

### 4. File Upload Validation Based on Specialty

#### New ScanValidationService
- **Location**: `SCAN-SERVICE/src/main/java/tn/esprit/scanservice/service/ScanValidationService.java`
- **Features**:
  - Validates file size per specialty
  - Validates file type per specialty
  - Validates image dimensions per specialty
  - Validates detected organ matches specialty
  - Returns detailed validation results

#### Updated Scan Entity
- **Location**: `SCAN-SERVICE/src/main/java/tn/esprit/scanservice/entity/Scan.java`
- **New fields**: `specialtyCode`, `isValidated`, `validationMessage`, `detectedOrgan`, `confidence`

#### Updated ScanController
- **Location**: `SCAN-SERVICE/src/main/java/tn/esprit/scanservice/controller/ScanController.java`
- **New endpoint**: `POST /scan/upload` - Upload with validation
  - Parameters: file, patientId, doctorId, specialtyCode, detectedOrgan, confidence
  - Validates against specialty rules
  - Returns validation results

#### Updated ScanService
- **Location**: `SCAN-SERVICE/src/main/java/tn/esprit/scanservice/service/ScanService.java`
- **New method**: `getByDoctorAndSpecialty()` - Get scans by doctor and specialty

#### Updated ScanRepository
- **Location**: `SCAN-SERVICE/src/main/java/tn/esprit/scanservice/Repository/ScanRepository.java`
- **New methods**: `findByDoctorId()`, `findByDoctorIdAndSpecialtyCode()`

## User Flow

### Admin Flow
1. Admin logs in
2. Admin creates doctor via `POST /doctors` with specialtyCode
3. System sends email to doctor with:
   - Temporary password
   - Verification code
   - Instructions to verify and change password

### Doctor Flow
1. Doctor receives email
2. Doctor logs in with temporary password
3. System prompts for verification code
4. Doctor enters verification code
5. System forces password change (first login)
6. Doctor can now:
   - Add patients via `POST /patients/register`
   - Upload scans via `POST /scan/upload` with specialty validation

### Patient Flow
1. Doctor registers patient via `POST /patients/register`
2. System sends email to patient with:
   - Temporary password
   - Verification code
3. Patient receives email
4. Patient logs in with temporary password
5. Patient enters verification code
6. Patient changes password (first login)
7. Patient can now access their medical records

## API Endpoints Summary

### Specialty Endpoints
- `POST /specialties/initialize` - Initialize specialties
- `GET /specialties` - Get all specialties
- `GET /specialties/active` - Get active specialties
- `GET /specialties/code/{code}` - Get by code
- `GET /specialties/{id}` - Get by ID

### Doctor Endpoints
- `POST /doctors` - Create doctor (with specialtyCode)
- `GET /doctors` - Get all doctors
- `GET /doctors/active` - Get active doctors
- `GET /doctors/{id}` - Get by ID
- `GET /doctors/specialization/{specialization}` - Get by specialization
- `POST /doctors/verify` - Verify doctor code
- `POST /doctors/change-password` - Change doctor password

### Patient Endpoints
- `POST /patients/register` - Register patient with email
- `POST /patients/verify` - Verify patient code
- `POST /patients/change-password` - Change patient password
- `GET /patients` - Get all patients
- `GET /patients/{id}` - Get by ID
- `GET /patients/search` - Search patients

### Scan Endpoints
- `POST /scan/upload` - Upload scan with validation
- `GET /scan` - Get all scans
- `GET /scan/patient/{id}` - Get by patient
- `GET /scan/doctor/{doctorId}/specialty/{specialtyCode}` - Get by doctor and specialty
- `DELETE /scan/{id}` - Delete scan

## Database Schema Changes

### Specialty Collection
```json
{
  "_id": "string",
  "code": "CERVEAU|SEIN|PEAU|OEIL|ALZHEIMER|POUMON",
  "name": "string",
  "icon": "string",
  "description": "string",
  "modelName": "string",
  "modelArchitecture": "string",
  "dataset": "string",
  "referencePaper": "string",
  "doi": "string",
  "acceptedFileTypes": ["string"],
  "acceptedOrgans": ["string"],
  "minImageWidth": "integer",
  "minImageHeight": "integer",
  "maxImageWidth": "integer",
  "maxImageHeight": "integer",
  "maxFileSizeBytes": "long",
  "active": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### Doctor Collection (Updated)
```json
{
  // ... existing fields ...
  "specialtyCode": "string",
  "isVerified": "boolean",
  "verificationCode": "string",
  "verificationCodeExpiresAt": "datetime",
  "tempPassword": "string",
  "tempPasswordExpiresAt": "datetime",
  "isFirstLogin": "boolean"
}
```

### Patient Collection (Updated)
```json
{
  // ... existing fields ...
  "isVerified": "boolean",
  "verificationCode": "string",
  "verificationCodeExpiresAt": "datetime",
  "tempPassword": "string",
  "tempPasswordExpiresAt": "datetime",
  "isFirstLogin": "boolean"
}
```

### Scan Collection (Updated)
```json
{
  // ... existing fields ...
  "specialtyCode": "string",
  "isValidated": "boolean",
  "validationMessage": "string",
  "detectedOrgan": "string",
  "confidence": "double"
}
```

## Security Features

1. **Email Verification**: Both doctors and patients must verify email
2. **Temporary Password**: Generated temporary passwords expire in 24 hours
3. **First Login Password Change**: Forced password change on first login
4. **File Validation**: Uploads validated against specialty rules
5. **Organ Detection**: AI organ detection validates against specialty
6. **Size Limits**: File size limits per specialty
7. **Dimension Validation**: Image dimension validation per specialty

## Next Steps

1. Test the complete user flow (admin → doctor → patient)
2. Integrate with AI Analysis Service for organ detection
3. Implement file storage for uploaded scans
4. Add frontend components for specialty selection
5. Add frontend components for email verification UI
6. Add frontend components for password change UI
7. Implement specialty-based patient assignment
8. Add audit logging for security events

## Configuration Required

Ensure the following is configured in `application.properties` or `application.yml`:

```properties
# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Testing Commands

### Initialize Specialties
```bash
curl -X POST http://localhost:8081/specialties/initialize
```

### Create Doctor
```bash
curl -X POST http://localhost:8081/doctors \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jean",
    "lastName": "Dupont",
    "email": "jean.dupont@example.com",
    "specialization": "Neurologie",
    "specialtyCode": "CERVEAU",
    "licenseNumber": "MED12345",
    "hospital": "Hôpital Central",
    "phone": "+33612345678"
  }'
```

### Register Patient
```bash
curl -X POST http://localhost:8081/patients/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Marie",
    "lastName": "Martin",
    "email": "marie.martin@example.com",
    "phone": "+33687654321",
    "dateOfBirth": "1980-05-15",
    "gender": "F",
    "chronicDiseases": ["Diabète Type 2", "Hypertension"],
    "allergies": ["Pénicilline"]
  }'
```

### Upload Scan with Validation
```bash
curl -X POST http://localhost:8085/scan/upload \
  -F "file=@brain_scan.dicom" \
  -F "patientId=patient123" \
  -F "doctorId=doctor456" \
  -F "specialtyCode=CERVEAU" \
  -F "detectedOrgan=cerveau" \
  -F "confidence=0.95"
```

## Notes

- All temporary passwords and verification codes expire in 24 hours
- Email service must be configured for verification to work
- File validation is performed before storage
- Specialty codes are uppercase: CERVEAU, SEIN, PEAU, OEIL, ALZHEIMER, POUMON
- The system uses MongoDB for data persistence
- All services communicate via REST APIs
