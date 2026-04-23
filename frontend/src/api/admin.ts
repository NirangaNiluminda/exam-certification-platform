import client from './client';
import type { Exam, ExamRequest, Question, QuestionRequest } from '../types';

type RawExam = Omit<Exam, 'examId'> & { id: string; examId?: string };
const normalize = (e: RawExam): Exam => ({ ...e, examId: e.examId ?? e.id });

export const adminListExams = () =>
  client.get<RawExam[]>('/api/v1/admin/exams').then((r) => r.data.map(normalize));

export const adminGetExam = (examId: string) =>
  client.get<RawExam>(`/api/v1/admin/exams/${examId}`).then((r) => normalize(r.data));

export const adminCreateExam = (data: ExamRequest) =>
  client.post<RawExam>('/api/v1/admin/exams', data).then((r) => normalize(r.data));

export const adminUpdateExam = (examId: string, data: ExamRequest) =>
  client.put<RawExam>(`/api/v1/admin/exams/${examId}`, data).then((r) => normalize(r.data));

export const adminDeleteExam = (examId: string) =>
  client.delete(`/api/v1/admin/exams/${examId}`).then((r) => r.data);

export const adminActivateExam = (examId: string) =>
  client.patch(`/api/v1/admin/exams/${examId}/activate`).then((r) => r.data);

export const adminGetQuestions = (examId: string) =>
  client.get<Question[]>(`/api/v1/admin/exams/${examId}/questions`).then((r) => r.data);

export const adminAddQuestion = (examId: string, data: QuestionRequest) =>
  client.post<Question>(`/api/v1/admin/exams/${examId}/questions`, data).then((r) => r.data);
