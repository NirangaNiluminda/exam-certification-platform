import client from './client';
import type { Certificate, VerifyResponse } from '../types';

export const getCertificate = (certificateId: string) =>
  client.get<Certificate>(`/api/v1/certificates/${certificateId}`).then((r) => r.data);

export const getCertificateByEnrollment = (enrollmentId: string) =>
  client.get<Certificate>(`/api/v1/certificates/by-enrollment/${enrollmentId}`).then((r) => r.data);

export const verifyCertificate = (verifyCode: string) =>
  client.get<VerifyResponse>(`/api/v1/certificates/verify/${verifyCode}`).then((r) => r.data);
