import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { startSlot, getSlotQuestions, submitAnswers } from '../api/exams';
import type { AnswerSubmission } from '../types';

const TakeExamPage: React.FC = () => {
  const { slotId } = useParams<{ slotId: string }>();
  const navigate = useNavigate();
  const [answers, setAnswers] = useState<Record<string, 'A' | 'B' | 'C' | 'D'>>({});
  const [deadline, setDeadline] = useState<Date | null>(null);
  const [timeLeft, setTimeLeft] = useState<string>('');
  const [submitted, setSubmitted] = useState(false);

  // Step 1: start the slot
  const startMutation = useMutation({
    mutationFn: () => startSlot(slotId!),
    onSuccess: (data) => {
      setDeadline(new Date(data.deadlineAt));
    },
  });

  // Step 2: fetch questions (enabled once slot is started)
  const { data: slotData } = useQuery({
    queryKey: ['slot-questions', slotId],
    queryFn: () => getSlotQuestions(slotId!),
    enabled: startMutation.isSuccess,
  });

  // Start the slot on mount
  useEffect(() => {
    if (slotId) startMutation.mutate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slotId]);

  // Countdown timer
  useEffect(() => {
    if (!deadline) return;
    const interval = setInterval(() => {
      const diff = deadline.getTime() - Date.now();
      if (diff <= 0) {
        clearInterval(interval);
        setTimeLeft('Time up!');
        return;
      }
      const mins = Math.floor(diff / 60000);
      const secs = Math.floor((diff % 60000) / 1000);
      setTimeLeft(`${mins}:${secs.toString().padStart(2, '0')}`);
    }, 1000);
    return () => clearInterval(interval);
  }, [deadline]);

  // Step 3: submit
  const submitMutation = useMutation({
    mutationFn: () => {
      const payload: AnswerSubmission[] = Object.entries(answers).map(([questionId, selectedOption]) => ({
        questionId,
        selectedOption,
      }));
      return submitAnswers(slotId!, { answers: payload });
    },
    onSuccess: () => {
      setSubmitted(true);
    },
  });

  if (submitted) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <div className="text-6xl mb-4">🎉</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Exam Submitted!</h1>
        <p className="text-gray-500 mb-6">
          Your answers are being graded. If you pass, a certificate will be issued automatically.
        </p>
        <button
          onClick={() => navigate('/enrollments')}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg"
        >
          Back to My Enrollments
        </button>
      </div>
    );
  }

  if (startMutation.isPending || !slotData) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12 text-center text-gray-500">
        <div className="animate-spin text-4xl mb-4">⏳</div>
        Starting your exam session…
      </div>
    );
  }

  if (startMutation.isError) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-12">
        <div className="bg-red-50 text-red-700 rounded-lg px-4 py-3">
          Failed to start exam. The slot may have already been used or expired.
        </div>
      </div>
    );
  }

  const questions = slotData?.questions ?? [];
  const answeredCount = Object.keys(answers).length;

  return (
    <div className="max-w-3xl mx-auto px-4 py-6">
      {/* Header bar */}
      <div className="sticky top-0 bg-white border-b border-gray-200 py-3 mb-6 flex items-center justify-between z-10 px-4 rounded-xl shadow-sm">
        <span className="text-sm text-gray-600">
          {answeredCount}/{questions.length} answered
        </span>
        <span className={`font-bold text-lg ${timeLeft === 'Time up!' ? 'text-red-600' : 'text-indigo-700'}`}>
          ⏱ {timeLeft}
        </span>
        <button
          onClick={() => submitMutation.mutate()}
          disabled={submitMutation.isPending || answeredCount === 0}
          className="bg-green-600 hover:bg-green-700 disabled:bg-gray-300 text-white text-sm px-4 py-1.5 rounded-lg font-medium"
        >
          {submitMutation.isPending ? 'Submitting…' : 'Submit Exam'}
        </button>
      </div>

      {/* Questions */}
      <div className="space-y-6">
        {questions.map((q, idx) => (
          <div key={q.questionId} className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex gap-3 mb-4">
              <span className="bg-indigo-600 text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center flex-shrink-0">
                {idx + 1}
              </span>
              <p className="text-gray-800 font-medium">{q.questionText}</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {(['A', 'B', 'C', 'D'] as const).map((opt) => {
                const optText = q[`option${opt}` as keyof typeof q] as string;
                const selected = answers[q.questionId] === opt;
                return (
                  <button
                    key={opt}
                    onClick={() => setAnswers((prev) => ({ ...prev, [q.questionId]: opt }))}
                    className={`text-left px-4 py-3 rounded-lg border text-sm transition-colors ${
                      selected
                        ? 'bg-indigo-600 border-indigo-600 text-white'
                        : 'border-gray-200 hover:border-indigo-400 text-gray-700'
                    }`}
                  >
                    <span className="font-bold mr-2">{opt}.</span>
                    {optText}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {submitMutation.isError && (
        <div className="mt-4 bg-red-50 text-red-700 rounded-lg px-4 py-3 text-sm">
          Submission failed. Please try again.
        </div>
      )}
    </div>
  );
};

export default TakeExamPage;
