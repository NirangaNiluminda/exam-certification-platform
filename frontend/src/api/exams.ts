import client from './client';
import type { Exam, Question, SlotStartResponse, SlotQuestionsResponse, SubmitExamRequest } from '../types';

type RawExam = Omit<Exam, 'examId'> & { id: string; examId?: string };
const normalizeExam = (e: RawExam): Exam => ({ ...e, examId: e.examId ?? e.id });

type RawQuestion = Omit<Question, 'questionId'> & { id: string; questionId?: string };
const normalizeQuestion = (q: RawQuestion): Question => ({ ...q, questionId: q.questionId ?? q.id });

export const listExams = () =>
  client.get<RawExam[]>('/api/v1/exams').then((r) => r.data.map(normalizeExam));

export const getExam = (examId: string) =>
  client.get<RawExam>(`/api/v1/exams/${examId}`).then((r) => normalizeExam(r.data));

export const startSlot = (slotId: string) =>
  client.post<SlotStartResponse>(`/api/v1/exams/slots/${slotId}/start`).then((r) => r.data);

export const getSlotQuestions = (slotId: string) =>
  client.get<SlotQuestionsResponse>(`/api/v1/exams/slots/${slotId}/questions`).then((r) => ({
    ...r.data,
    questions: (r.data.questions as unknown as RawQuestion[]).map(normalizeQuestion),
  }));

export const submitAnswers = (slotId: string, data: SubmitExamRequest) =>
  client.post(`/api/v1/exams/slots/${slotId}/submit`, data).then((r) => r.data);
