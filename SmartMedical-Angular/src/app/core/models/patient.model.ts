export interface Patient {
  id?: number;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  dateOfBirth?: Date;
  address?: string;
  medicalHistory?: string;
  age?: number;
  gender?: string;
  allergies?: string;
  currentMedications?: string;
  createdAt?: Date;
  updatedAt?: Date;
}
