import client from './client';
import type { Exam, SlotStartResponse, SlotQuestionsResponse, SubmitExamRequest } from '../types';

export const listExams = () =>
  client.get<Exam[]>('/api/v1/exams').then((r) => r.data);

export const getExam = (examId: string) =>
  client.get<Exam>(`/api/v1/exams/${examId}`).then((r) => r.data);

export const startSlot = (slotId: string) =>
  client.post<SlotStartResponse>(`/api/v1/exams/slots/${slotId}/start`).then((r) => r.data);

export const getSlotQuestions = (slotId: string) =>
  client.get<SlotQuestionsResponse>(`/api/v1/exams/slots/${slotId}/questions`).then((r) => r.data);

export const submitAnswers = (slotId: string, data: SubmitExamRequest) =>
  client.post(`/api/v1/exams/slots/${slotId}/submit`, data).then((r) => r.data);
