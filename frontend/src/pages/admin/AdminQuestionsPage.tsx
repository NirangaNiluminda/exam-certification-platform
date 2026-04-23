import { useForm } from 'react-hook-form';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminGetQuestions, adminAddQuestion, adminGetExam } from '../../api/admin';
import type { QuestionRequest } from '../../types';

const AdminQuestionsPage: React.FC = () => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm<QuestionRequest>({
    defaultValues: { marks: 10, sortOrder: 1, correctOption: 'A' },
  });

  const isValidId = !!examId && examId !== 'undefined';

  const { data: exam } = useQuery({
    queryKey: ['admin-exam', examId],
    queryFn: () => adminGetExam(examId!),
    enabled: isValidId,
  });

  const { data: questions, isLoading } = useQuery({
    queryKey: ['admin-questions', examId],
    queryFn: () => adminGetQuestions(examId!),
    enabled: isValidId,
  });

  const addMutation = useMutation({
    mutationFn: (data: QuestionRequest) => adminAddQuestion(examId!, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-questions', examId] });
      reset({ marks: 10, sortOrder: (questions?.length ?? 0) + 2, correctOption: 'A' });
    },
  });

  const onSubmit = (data: QuestionRequest) => addMutation.mutate(data);

  if (!isValidId) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <p className="text-red-500 mb-4">Invalid exam ID. Please select an exam from the list.</p>
        <button onClick={() => navigate('/admin/exams')} className="text-indigo-600 hover:underline text-sm">
          ← Back to exams
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <button onClick={() => navigate('/admin/exams')} className="text-indigo-600 text-sm hover:underline mb-4 block">
        ← Back to exams
      </button>

      <h1 className="text-xl font-bold text-gray-900 mb-1">
        Questions — {exam?.title ?? 'Loading…'}
      </h1>
      <p className="text-gray-500 text-sm mb-6">{questions?.length ?? 0} questions total</p>

      {/* Existing questions */}
      {isLoading ? (
        <div className="space-y-3 mb-8">
          {[1, 2].map((i) => <div key={i} className="bg-white rounded-xl border h-24 animate-pulse" />)}
        </div>
      ) : (
        <div className="space-y-3 mb-8">
          {questions?.map((q, idx) => (
            <div key={q.questionId} className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="flex gap-3 mb-2">
                <span className="bg-indigo-600 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center flex-shrink-0">
                  {idx + 1}
                </span>
                <p className="text-sm font-medium text-gray-800">{q.questionText}</p>
                <span className="ml-auto text-xs text-gray-400">{q.marks} marks</span>
              </div>
              <div className="grid grid-cols-2 gap-1 ml-8 text-xs text-gray-500">
                {(['A', 'B', 'C', 'D'] as const).map((opt) => (
                  <span key={opt} className={opt === q.correctOption ? 'text-green-700 font-medium' : ''}>
                    {opt}. {q[`option${opt}` as keyof typeof q] as string}
                    {opt === q.correctOption && ' ✓'}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add question form */}
      <div className="bg-white rounded-2xl border border-gray-200 p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-4">Add New Question</h2>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Question Text</label>
            <textarea
              {...register('questionText', { required: 'Question text is required' })}
              rows={2}
              placeholder="What is…?"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {errors.questionText && <p className="text-red-500 text-xs mt-1">{errors.questionText.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            {(['A', 'B', 'C', 'D'] as const).map((opt) => (
              <div key={opt}>
                <label className="block text-sm font-medium text-gray-700 mb-1">Option {opt}</label>
                <input
                  {...register(`option${opt}` as keyof QuestionRequest, { required: `Option ${opt} is required` })}
                  placeholder={`Option ${opt}`}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            ))}
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Correct Option</label>
              <select
                {...register('correctOption', { required: true })}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="A">A</option>
                <option value="B">B</option>
                <option value="C">C</option>
                <option value="D">D</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Marks</label>
              <input
                {...register('marks', { required: true, valueAsNumber: true })}
                type="number"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
              <input
                {...register('sortOrder', { required: true, valueAsNumber: true })}
                type="number"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          {addMutation.isError && (
            <p className="text-red-500 text-sm bg-red-50 rounded-lg px-3 py-2">Failed to add question.</p>
          )}

          <button
            type="submit"
            disabled={addMutation.isPending}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white py-2 rounded-lg font-medium text-sm"
          >
            {addMutation.isPending ? 'Adding…' : 'Add Question'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AdminQuestionsPage;
