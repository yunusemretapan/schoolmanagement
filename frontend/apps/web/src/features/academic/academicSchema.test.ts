import { ApiError, ERROR_CODES } from '@schoolos/api-client';
import { describe, expect, it } from 'vitest';
import { daysInclusive, formFieldErrors, nameError, serverFieldErrors, termErrors } from './academicSchema';

/** Spec 0005, Req. 4, 7 / AC-4, AC-7: the same limits as the server, before anything is sent. */
describe('academicSchema', () => {
  it('names are measured after trimming: 60, or 20 for a class group', () => {
    expect(nameError('x'.repeat(60), 60)).toBeUndefined();
    expect(nameError(`  ${'x'.repeat(60)}  `, 60)).toBeUndefined();
    expect(nameError('x'.repeat(61), 60)).toBe('academic.errors.tooLong60');
    expect(nameError('x'.repeat(20), 20)).toBeUndefined();
    expect(nameError('x'.repeat(21), 20)).toBe('academic.errors.tooLong20');
    expect(nameError('   ', 60)).toBe('academic.errors.required');
  });

  it('term dates: end after start, 2000–2100, at most 731 days', () => {
    const ok = { name: 'Y', startsOn: '2026-09-01', endsOn: '2028-08-31' };
    expect(daysInclusive(ok.startsOn, ok.endsOn)).toBe(731);
    expect(termErrors(ok)).toEqual({});
    expect(termErrors({ ...ok, endsOn: '2028-09-01' })).toEqual({ endsOn: 'academic.errors.termTooLong' });
    expect(termErrors({ ...ok, endsOn: ok.startsOn })).toEqual({ endsOn: 'academic.errors.endBeforeStart' });
    expect(termErrors({ name: 'Y', startsOn: '1999-12-31', endsOn: '2000-06-30' })).toEqual({
      startsOn: 'academic.errors.dateRange',
    });
    expect(termErrors({ name: 'Y', startsOn: '2100-06-01', endsOn: '2101-01-01' })).toEqual({
      endsOn: 'academic.errors.dateRange',
    });
    expect(termErrors({ name: 'Y', startsOn: '2000-01-01', endsOn: '2000-06-30' })).toEqual({});
  });

  it('server field codes become the same messages', () => {
    const error = new ApiError(400, ERROR_CODES.validationFailed, { name: 'SIZE', endsOn: 'TOO_LONG' });
    expect(serverFieldErrors(error)).toEqual({
      name: 'academic.errors.tooLong60',
      endsOn: 'academic.errors.termTooLong',
    });
    expect(serverFieldErrors(new ApiError(422, ERROR_CODES.termOverlap))).toEqual({});
  });

  it('a taken name and an overlap are shown under their field (review 1, L-5)', () => {
    expect(formFieldErrors(new ApiError(422, ERROR_CODES.academicNameTaken))).toEqual({
      name: 'academic.errors.nameTaken',
    });
    expect(formFieldErrors(new ApiError(422, ERROR_CODES.termOverlap))).toEqual({
      startsOn: 'academic.errors.termOverlap',
    });
    expect(formFieldErrors(new ApiError(400, ERROR_CODES.validationFailed, { name: 'SIZE' }), 20)).toEqual({
      name: 'academic.errors.tooLong20',
    });
    expect(formFieldErrors(new ApiError(422, ERROR_CODES.lastCampus))).toEqual({});
  });
});
