import client from './client';
import type { Exam, ExamRequest, Question, QuestionRequest } from '../types';

export const adminListExams = () =>
  client.get<Exam[]>('/api/v1/admin/exams').then((r) => r.data);

export const adminGetExam = (examId: string) =>
  client.get<Exam>(`/api/v1/admin/exams/${examId}`).then((r) => r.data);

export const adminCreateExam = (data: ExamRequest) =>
  client.post<Exam>('/api/v1/admin/exams', data).then((r) => r.data);

export const adminUpdateExam = (examId: string, data: ExamRequest) =>
  client.put<Exam>(`/api/v1/admin/exams/${examId}`, data).then((r) => r.data);

export const adminDeleteExam = (examId: string) =>
  client.delete(`/api/v1/admin/exams/${examId}`).then((r) => r.data);

export const adminGetQuestions = (examId: string) =>
  client.get<Question[]>(`/api/v1/admin/exams/${examId}/questions`).then((r) => r.data);

export const adminAddQuestion = (examId: string, data: QuestionRequest) =>
  client.post<Question>(`/api/v1/admin/exams/${examId}/questions`, data).then((r) => r.data);
