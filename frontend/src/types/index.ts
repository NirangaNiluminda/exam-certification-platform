// ── Auth ──────────────────────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  userId: string;
  email: string;
  role: 'STUDENT' | 'ADMIN' | 'EXAMINER';
}

// ── Exams ─────────────────────────────────────────────────────────────────────
export interface Exam {
  examId: string;
  title: string;
  description: string;
  durationMinutes: number;
  passingScore: number;
  totalMarks: number;
  price: number;
  active?: boolean;
}

export interface ExamRequest {
  title: string;
  description: string;
  durationMinutes: number;
  passingScore: number;
  totalMarks: number;
  price: number;
}

export interface Question {
  questionId: string;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  marks: number;
  sortOrder: number;
  correctOption?: string; // only in admin view
}

export interface QuestionRequest {
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctOption: string;
  marks: number;
  sortOrder: number;
}

// ── Exam Slots ────────────────────────────────────────────────────────────────
export interface SlotStartResponse {
  slotId: string;
  startedAt: string;
  deadlineAt: string;
  durationMinutes: number;
}

export interface SlotQuestionsResponse {
  slotId: string;
  deadlineAt: string;
  questions: Question[];
}

export interface AnswerSubmission {
  questionId: string;
  selectedOption: 'A' | 'B' | 'C' | 'D';
}

export interface SubmitExamRequest {
  answers: AnswerSubmission[];
}

// ── Enrollments ───────────────────────────────────────────────────────────────
export type EnrollmentStatus = 'PENDING' | 'PAYMENT_COMPLETED' | 'EXAM_SCHEDULED' | 'EXAM_SUBMITTED' | 'GRADED' | 'CERTIFIED' | 'FAILED' | 'CANCELLED';

export interface Enrollment {
  id: string;
  studentId: string;
  examId: string;
  status: EnrollmentStatus;
  enrolledAt: string;
  slotId?: string;
}

export interface EnrollRequest {
  examId: string;
  examPrice: number;
}

// ── Certificates ──────────────────────────────────────────────────────────────
export interface Certificate {
  certificateId: string;
  studentName: string;
  examTitle: string;
  scorePercentage: number;
  issuedAt: string;
  verifyCode: string;
  downloadUrl: string;
}

export interface VerifyResponse {
  valid: boolean;
  certificateId: string;
  studentName: string;
  examTitle: string;
  scorePercentage: number;
  issuedAt: string;
}
