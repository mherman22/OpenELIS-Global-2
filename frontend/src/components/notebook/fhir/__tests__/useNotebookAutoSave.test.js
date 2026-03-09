import { renderHook, act } from "@testing-library/react";
import { useNotebookAutoSave } from "../hooks/useNotebookAutoSave";

beforeEach(() => {
  jest.useFakeTimers();
  jest.spyOn(global, "fetch").mockResolvedValue({ ok: true });
});

afterEach(() => {
  jest.useRealTimers();
  jest.restoreAllMocks();
});

const defaultQuestionnaire = { resourceType: "Questionnaire" };
const defaultFormData = { field1: "value1" };
const entryId = "entry-123";
const pageId = "page-456";
const specimenId = "spec-789";

// T049
test("useNotebookAutoSave calls POST endpoint after 2s debounce when formData changes", async () => {
  const { rerender } = renderHook(
    ({ formData }) =>
      useNotebookAutoSave(formData, defaultQuestionnaire, entryId, pageId, specimenId),
    { initialProps: { formData: defaultFormData } },
  );

  // Change formData to trigger the effect
  rerender({ formData: { field1: "updated" } });

  // Timer has not fired yet — fetch should NOT be called
  expect(global.fetch).not.toHaveBeenCalled();

  // Advance timer by 2 seconds
  await act(async () => {
    jest.advanceTimersByTime(2000);
  });

  expect(global.fetch).toHaveBeenCalledTimes(1);
  expect(global.fetch).toHaveBeenCalledWith(
    `/rest/notebook/entry/${entryId}/page/${pageId}/questionnaire-response`,
    expect.objectContaining({
      method: "POST",
      headers: { "Content-Type": "application/json" },
    }),
  );

  const callBody = JSON.parse(global.fetch.mock.calls[0][1].body);
  expect(callBody.status).toBe("in-progress");
  expect(callBody.specimenId).toBe(specimenId);
  expect(callBody.responses).toEqual({ field1: "updated" });
});

// T050
test("useNotebookAutoSave does not call POST on initial mount", async () => {
  renderHook(() =>
    useNotebookAutoSave(
      defaultFormData,
      defaultQuestionnaire,
      entryId,
      pageId,
      specimenId,
    ),
  );

  // Advance timer — should NOT fire on initial mount
  await act(async () => {
    jest.advanceTimersByTime(3000);
  });

  expect(global.fetch).not.toHaveBeenCalled();
});

test("useNotebookAutoSave debounces — only fires once after rapid changes", async () => {
  const { rerender } = renderHook(
    ({ formData }) =>
      useNotebookAutoSave(formData, defaultQuestionnaire, entryId, pageId, specimenId),
    { initialProps: { formData: { field1: "v1" } } },
  );

  rerender({ formData: { field1: "v2" } });
  jest.advanceTimersByTime(500);
  rerender({ formData: { field1: "v3" } });
  jest.advanceTimersByTime(500);
  rerender({ formData: { field1: "v4" } });

  await act(async () => {
    jest.advanceTimersByTime(2000);
  });

  expect(global.fetch).toHaveBeenCalledTimes(1);
});

test("useNotebookAutoSave returns draftStatus idle initially", () => {
  const { result } = renderHook(() =>
    useNotebookAutoSave(
      defaultFormData,
      defaultQuestionnaire,
      entryId,
      pageId,
      specimenId,
    ),
  );
  expect(result.current.draftStatus).toBe("idle");
});

test("useNotebookAutoSave does not fire when required params are missing", async () => {
  const { rerender } = renderHook(
    ({ formData }) =>
      useNotebookAutoSave(formData, defaultQuestionnaire, null, null, null),
    { initialProps: { formData: defaultFormData } },
  );

  rerender({ formData: { field1: "updated" } });

  await act(async () => {
    jest.advanceTimersByTime(2000);
  });

  expect(global.fetch).not.toHaveBeenCalled();
});
