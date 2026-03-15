import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useParams, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminCreateExam, adminUpdateExam, adminGetExam } from '../../api/admin';
import type { ExamRequest } from '../../types';

const AdminExamFormPage: React.FC = () => {
  const { examId } = useParams<{ examId?: string }>();
  const isEdit = !!examId;
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { register, handleSubmit, reset, formState: { errors } } = useForm<ExamRequest>();

  // Load existing exam for edit
  const { data: existing } = useQuery({
    queryKey: ['admin-exam', examId],
    queryFn: () => adminGetExam(examId!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing) reset(existing);
  }, [existing, reset]);

  const mutation = useMutation({
    mutationFn: (data: ExamRequest) =>
      isEdit ? adminUpdateExam(examId!, data) : adminCreateExam(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-exams'] });
      navigate('/admin/exams');
    },
  });

  const onSubmit = (data: ExamRequest) => mutation.mutate(data);

  const field = (label: string, name: keyof ExamRequest, type = 'text', placeholder = '') => (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        {...register(name, { required: `${label} is required`, valueAsNumber: type === 'number' })}
        type={type}
        placeholder={placeholder}
        step={type === 'number' ? 'any' : undefined}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
      {errors[name] && <p className="text-red-500 text-xs mt-1">{errors[name]?.message as string}</p>}
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <button onClick={() => navigate('/admin/exams')} className="text-indigo-600 text-sm hover:underline mb-4 block">
        ← Back to exams
      </button>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <h1 className="text-xl font-bold text-gray-900 mb-6">
          {isEdit ? 'Edit Exam' : 'Create New Exam'}
        </h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {field('Title', 'title', 'text', 'e.g. Cloud Computing Fundamentals')}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea
              {...register('description', { required: 'Description is required' })}
              rows={3}
              placeholder="Exam description…"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            {field('Duration (minutes)', 'durationMinutes', 'number', '90')}
            {field('Price ($)', 'price', 'number', '49.99')}
          </div>

          <div className="grid grid-cols-2 gap-4">
            {field('Passing Score (%)', 'passingScore', 'number', '70')}
            {field('Total Marks', 'totalMarks', 'number', '100')}
          </div>

          {mutation.isError && (
            <p className="text-red-500 text-sm bg-red-50 rounded-lg px-3 py-2">
              Failed to save exam. Please try again.
            </p>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={mutation.isPending}
              className="flex-1 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white py-2 rounded-lg font-medium text-sm"
            >
              {mutation.isPending ? 'Saving…' : isEdit ? 'Update Exam' : 'Create Exam'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/admin/exams')}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AdminExamFormPage;
