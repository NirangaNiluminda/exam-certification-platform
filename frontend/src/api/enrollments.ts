import client from './client';
import type { Enrollment, EnrollRequest } from '../types';

export const enroll = (data: EnrollRequest) =>
  client.post<Enrollment>('/api/v1/enrollments', data).then((r) => r.data);

export const getMyEnrollments = () =>
  client.get<Enrollment[]>('/api/v1/enrollments/me').then((r) => r.data);

export const getEnrollment = (id: string) =>
  client.get<Enrollment>(`/api/v1/enrollments/${id}`).then((r) => r.data);
